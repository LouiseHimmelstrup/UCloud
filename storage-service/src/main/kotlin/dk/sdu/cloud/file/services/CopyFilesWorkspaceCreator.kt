package dk.sdu.cloud.file.services

import dk.sdu.cloud.calls.RPCException
import dk.sdu.cloud.file.api.AccessRight
import dk.sdu.cloud.file.api.LINUX_FS_USER_UID
import dk.sdu.cloud.file.api.WorkspaceMode
import dk.sdu.cloud.file.api.WorkspaceMount
import dk.sdu.cloud.file.services.acl.AclService
import dk.sdu.cloud.file.services.acl.requirePermission
import dk.sdu.cloud.file.services.linuxfs.Chown
import dk.sdu.cloud.file.services.linuxfs.LinuxFS
import dk.sdu.cloud.file.services.linuxfs.listAndClose
import dk.sdu.cloud.file.services.linuxfs.runAndRethrowNIOExceptions
import dk.sdu.cloud.file.services.linuxfs.translateAndCheckFile
import dk.sdu.cloud.file.util.FSException
import dk.sdu.cloud.service.Loggable
import dk.sdu.cloud.service.stackTraceToString
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.channels.Channels
import java.nio.file.CopyOption
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions
import java.util.*

class CopyFilesWorkspaceCreator<Ctx : FSUserContext>(
    private val fsRoot: File,
    private val fileScanner: FileScanner<Ctx>,
    private val aclService: AclService<*>,
    private val coreFileSystem: CoreFileSystemService<Ctx>,
    private val processRunner: FSCommandRunnerFactory<Ctx>
) : WorkspaceCreator{
    override suspend fun create(
        user: String,
        mounts: List<WorkspaceMount>,
        allowFailures: Boolean,
        createSymbolicLinkAt: String
    ): CreatedWorkspace = runAndRethrowNIOExceptions {
        val workspaceId = UUID.randomUUID().toString()
        val workspace = workspaceFile(fsRoot, workspaceId).also {
            Files.createDirectories(it)
            Files.setPosixFilePermissions(
                it,
                setOf(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE
                )
            )
        }
        val inputWorkspace = workspace.resolve("input").also { Files.createDirectories(it) }
        val outputWorkspace = workspace.resolve("output").also { Files.createDirectories(it) }
        val symLinkPath = createSymbolicLinkAt.let { File(it).absoluteFile.toPath() }

        val manifest = WorkspaceManifest(user, mounts, createSymbolicLinkAt, mode = WorkspaceMode.COPY_FILES)
        manifest.write(workspace)

        val failures = ArrayList<WorkspaceMount>()
        mounts.forEach {
            aclService.requirePermission(
                it.source,
                user,
                if (it.readOnly) AccessRight.READ else AccessRight.WRITE
            )

            try {
                val file = File(translateAndCheckFile(fsRoot, it.source)).toPath()
                transferFileToWorkspaceNoAccessCheck(
                    inputWorkspace,
                    outputWorkspace,
                    symLinkPath,
                    file,
                    file,
                    it.destination,
                    it.readOnly
                )
            } catch (ex: Throwable) {
                log.info("Failed to add ${it.source}. ${ex.message}")
                log.debug(ex.stackTraceToString())

                failures.add(it)
            }
        }

        if (failures.isNotEmpty() && !allowFailures) {
            delete(workspaceId, manifest)
            throw RPCException("Workspace creation had failures: $failures", HttpStatusCode.BadRequest)
        }

        CreatedWorkspace(workspaceId, failures)
    }

    private fun transferFileToWorkspaceNoAccessCheck(
        inputWorkspace: Path,
        outputWorkspace: Path,
        symLinkPath: Path,

        file: Path,
        rootPath: Path,
        initialDestination: String,
        readOnly: Boolean
    ) {
        val isInitialFile = Files.isSameFile(file, rootPath)
        val relativePath = rootPath.relativize(file)

        val inputRoot = inputWorkspace.resolve(initialDestination)
        val outputRoot = outputWorkspace.resolve(initialDestination)
        val symlinkRoot = symLinkPath.resolve(initialDestination)

        val inputDestinationPath =
            if (isInitialFile) inputRoot
            else inputRoot.resolve(relativePath)

        val outputDestinationPath =
            if (isInitialFile) outputRoot
            else outputRoot.resolve(relativePath)

        if (Files.isDirectory(file)) {
            if (readOnly) Files.createDirectories(inputDestinationPath)
            Files.createDirectories(outputDestinationPath)

            file.listAndClose().forEach {
                transferFileToWorkspaceNoAccessCheck(
                    inputWorkspace,
                    outputWorkspace,
                    symLinkPath,
                    it,
                    rootPath,
                    initialDestination,
                    readOnly
                )
            }
        } else {
            val resolvedFile =
                if (Files.isSymbolicLink(file)) Files.readSymbolicLink(file)
                else file

            if (readOnly) {
                Files.createLink(inputDestinationPath, resolvedFile)
                Files.createSymbolicLink(outputDestinationPath, symlinkRoot.resolve(relativePath))
            } else {
                Files.copy(resolvedFile, outputDestinationPath, StandardCopyOption.COPY_ATTRIBUTES)
            }
        }
    }

    override suspend fun transfer(
        id: String,
        manifest: WorkspaceManifest,
        replaceExisting: Boolean,
        matchers: List<PathMatcher>,
        destination: String,
        defaultDestinationDir: Path
    ): List<String> {
        // TODO This might need to clear ACLs from user code. Technically this won't be needed under new system but would
        //  be needed if we change stuff around again.
        val workspace = workspaceFile(fsRoot, id)
        val outputWorkspace = workspace.resolve("output")

        val transferred = ArrayList<String>()

        val copyOptions = run {
            val opts = ArrayList<CopyOption>()
            if (replaceExisting) opts += StandardCopyOption.REPLACE_EXISTING
            opts.toTypedArray()
        }

        /**
         * Recurse through the destination directory, looking for files which are not present in the
         * workspace directory (meaning that they were deleted from the workspace), and delete from the
         * destination directory accordingly.
         */
        fun cleanDestinationDirectory(ctx: Ctx, destDirectoryFile: File, workspaceDirectoryFile: File) {
            if (destDirectoryFile.list() != null) {
                val listFiles = destDirectoryFile.listFiles()
                listFiles?.forEach { destFile ->
                    if (!File(workspaceDirectoryFile.toString(), destFile.name).exists()) {
                        log.debug("File ${destFile.name} not found in workspace. Deleting file.")
                        val destFileCloudPath = destFile.toPath().toCloudPath()
                        runBlocking {
                            val destFileStat = coreFileSystem.statOrNull(
                                ctx,
                                destFileCloudPath,
                                setOf(FileAttribute.TIMESTAMPS)
                            )
                            if (destFileStat != null) {
                                if (destFileStat.timestamps.modified <= manifest.createdAt) {
                                    coreFileSystem.delete(ctx, destFileCloudPath)
                                }
                            }
                        }
                    } else if (destFile.isDirectory) {
                        cleanDestinationDirectory(
                            ctx,
                            destFile,
                            File(workspaceDirectoryFile, destFile.name)
                        )
                    }
                }
            }
        }

        suspend fun cleanDestinationDirectory(username: String, destDirectoryFile: File, workspaceDirectoryFile: File) {
            processRunner.withContext(username) { ctx ->
                cleanDestinationDirectory(ctx, destDirectoryFile, workspaceDirectoryFile)
            }
        }

        fun transferFile(sourceFile: Path, destinationDir: Path, relativeTo: Path = outputWorkspace): Path {
            val resolvedDestination = destinationDir.resolve(relativeTo.relativize(sourceFile))
            if (!resolvedDestination.startsWith(destinationDir)) {
                throw IllegalArgumentException("Resolved destination isn't within allowed target")
            }

            if (Files.isDirectory(sourceFile)) {
                val targetIsDir = Files.isDirectory(resolvedDestination)
                if (targetIsDir) {
                    sourceFile.listAndClose().forEach { child ->
                        transferFile(child, destinationDir, relativeTo)
                    }
                } else {
                    transferred.add(resolvedDestination.toCloudPath())
                    Files.move(sourceFile, resolvedDestination, *copyOptions)
                }
            } else {
                if (Files.exists(resolvedDestination)) {
                    val lastModifiedTime = runCatching { Files.getLastModifiedTime(resolvedDestination) }.getOrNull()
                    if (Files.getLastModifiedTime(sourceFile) != lastModifiedTime) {
                        // Truncate to preserve inode
                        val options = HashSet<OpenOption>()
                        options.add(StandardOpenOption.TRUNCATE_EXISTING)
                        options.add(StandardOpenOption.WRITE)
                        options.add(StandardOpenOption.CREATE)

                        val os = Channels.newOutputStream(
                            Files.newByteChannel(
                                resolvedDestination,
                                options,
                                PosixFilePermissions.asFileAttribute(LinuxFS.DEFAULT_FILE_MODE)
                            )
                        )

                        val ins = Channels.newInputStream(
                            Files.newByteChannel(
                                sourceFile,
                                StandardOpenOption.READ
                            )
                        )

                        // No need to copy list since the original file is simply updated.
                        ins.use { os.use { ins.copyTo(os) } }
                        transferred.add(resolvedDestination.toCloudPath())
                    } else {
                        log.trace("Don't need to copy file. It has not been modified.")
                    }
                } else {
                    transferred.add(resolvedDestination.toCloudPath())
                    Files.move(sourceFile, resolvedDestination, *copyOptions)
                }
            }
            return resolvedDestination
        }

        val filesWithMounts = outputWorkspace.listAndClose()
            .asSequence()
            .mapNotNull { path ->
                // Filter out files which should not be copied and associate a mount to them (if one exists)
                // We do not copy symlinks and will not move files if the mount does not allow for merges.
                if (Files.isSymbolicLink(path)) return@mapNotNull null

                val existingMount = run {
                    val fileName = path.toFile().name
                    manifest.mounts.find { fileName == File(it.destination).name }
                }

                if (existingMount != null && !existingMount.allowMergeDuringTransfer) return@mapNotNull null
                if (existingMount != null && existingMount.readOnly) return@mapNotNull null

                val matchesGlob = matchers.any { it.matches(path.fileName) }

                if (existingMount != null || matchesGlob) {
                    if (Files.isDirectory(path)) {
                        Pair(path, existingMount)
                    } else {
                        Pair(path, null)
                    }
                } else {
                    null
                }
            }
            .toList()

        // Check if we have permissions and write them into the map. We will check for each file if it is okay to
        // transfer. That way we will still allow files we have permissions for.
        val hasWritePermissionsToPath = HashMap<String, Boolean>()

        run {
            // Check if we are allowed to write to all the mounts we wish to
            val allMountsWeWillWriteTo = filesWithMounts.mapNotNull { it.second?.source }.toSet()

            allMountsWeWillWriteTo.forEach {
                hasWritePermissionsToPath[it] =
                    aclService.hasPermission(it, manifest.username, AccessRight.WRITE)
            }
        }

        run {
            // Check if we are allowed to write to defaultDestinationDir
            hasWritePermissionsToPath[destination] =
                aclService.hasPermission(destination, manifest.username, AccessRight.WRITE)
        }

        filesWithMounts.forEach { (currentFile, mount) ->
            log.debug("Transferring file: $currentFile")

            try {
                // We start the transfer by removing all symbolic links (backed by hard-linked read-only files)
                // and fixing permissions of files we need to transfer.

                Files.walk(currentFile).forEach { child ->
                    if (Files.isSymbolicLink(child)) {
                        Files.deleteIfExists(child)
                    } else {
                        Chown.setOwner(child, LINUX_FS_USER_UID, LINUX_FS_USER_UID)
                        Files.setPosixFilePermissions(
                            child, setOf(
                                PosixFilePermission.OWNER_WRITE,
                                PosixFilePermission.OWNER_READ,
                                PosixFilePermission.OWNER_EXECUTE,
                                PosixFilePermission.GROUP_WRITE,
                                PosixFilePermission.GROUP_READ,
                                PosixFilePermission.GROUP_EXECUTE
                            )
                        )
                    }
                }

                // We will put the file in the mount (if one exists) otherwise it will go in the default destination
                if (mount == null) {
                    if (hasWritePermissionsToPath[destination] != true) throw FSException.PermissionException()

                    // The file is then transferred to the new system and recorded for later use
                    transferFile(currentFile, defaultDestinationDir)
                } else {
                    if (hasWritePermissionsToPath[mount.source] != true) throw FSException.PermissionException()
                    val destinationDir = File(translateAndCheckFile(fsRoot, mount.source)).toPath()

                    // Then we remove files in the destination directory which no longer exists in the workspace.
                    // We also do a filter (in clean directory) which ensures that new files are not deleted.
                    runBlocking {
                        cleanDestinationDirectory(
                            manifest.username,
                            destinationDir.toFile(),
                            currentFile.toFile()
                        )
                    }

                    runCatching {
                        Files.createDirectories(destinationDir) // Ensure that directory exists
                    }

                    currentFile.listAndClose().forEach { child ->
                        transferFile(child, destinationDir, relativeTo = currentFile)
                    }
                }
            } catch (ex: Throwable) {
                log.info("Failed to transfer $currentFile. ${ex.message}")
                log.debug(ex.stackTraceToString())
            }
        }

        log.debug("Transferred ${transferred.size} files")
        transferred.forEach { path ->
            log.debug("Scanning external files")
            fileScanner.scanFilesCreatedExternally(path)
            log.debug("Scanning external files done")
        }

        log.debug("Done!")

        return transferred
    }

    override suspend fun delete(id: String, manifest: WorkspaceManifest) {
        val workspace = workspaceFile(fsRoot, id)
        workspace.toFile().deleteRecursively()
    }

    private fun Path.toCloudPath(): String = "/" + fsRoot.toPath().relativize(this).toFile().path

    companion object : Loggable {
        override val log = logger()
    }
}