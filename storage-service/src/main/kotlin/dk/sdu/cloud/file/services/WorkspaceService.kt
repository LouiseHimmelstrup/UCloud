package dk.sdu.cloud.file.services

import dk.sdu.cloud.calls.RPCException
import dk.sdu.cloud.file.api.WorkspaceMount
import dk.sdu.cloud.file.services.linuxfs.Chown
import dk.sdu.cloud.file.services.linuxfs.LinuxFSRunner
import dk.sdu.cloud.file.services.linuxfs.StandardCLib
import dk.sdu.cloud.file.services.linuxfs.translateAndCheckFile
import dk.sdu.cloud.service.Loggable
import dk.sdu.cloud.service.stackTraceToString
import io.ktor.http.HttpStatusCode
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermission
import java.util.*
import kotlin.streams.toList

data class CreatedWorkspace(val workspaceId: String, val failures: List<WorkspaceMount>)

class WorkspaceService(
    fsRoot: File,
    private val fileScanner: FileScanner<*>,

    private val userDao: StorageUserDao<Long>,

    private val fsCommandRunnerFactory: FSCommandRunnerFactory<LinuxFSRunner>
) {
    private val fsRoot = fsRoot.absoluteFile
    private val workspaceFile = File(fsRoot, WORKSPACE_PATH)

    fun workspace(id: String) = File(workspaceFile, id).toPath().normalize().toAbsolutePath()

    suspend fun create(user: String, mounts: List<WorkspaceMount>, allowFailures: Boolean): CreatedWorkspace {
        return fsCommandRunnerFactory.withContext(user) { ctx ->
            ctx.submit {
                ctx.requireContext()

                val workspaceId = UUID.randomUUID().toString()
                val workspace = workspace(workspaceId).also { Files.createDirectories(it) }
                // TODO chmod this folder

                fun transferFile(file: Path, destination: Path, relativeTo: Path) {
                    val resolvedDestination = if (Files.isSameFile(file, relativeTo)) {
                        destination
                    } else {
                        destination.resolve(relativeTo.relativize(file))
                    }

                    if (Files.isDirectory(file)) {
                        Files.createDirectories(resolvedDestination)

                        // Set the birth attribute to indicate to the system later that this file is a mount
                        // See the transfer method for more details.
                        StandardCLib.setxattr(
                            resolvedDestination.toFile().absolutePath,
                            "user.$XATTR_BIRTH",
                            (System.currentTimeMillis() / 1000).toString(),
                            false
                        )

                        Files.list(file).forEach {
                            transferFile(it, destination, relativeTo)
                        }
                    } else {
                        val resolvedFile = if (Files.isSymbolicLink(file)) {
                            Files.readSymbolicLink(file)
                        } else {
                            file
                        }

                        Files.copy(resolvedFile, resolvedDestination, StandardCopyOption.REPLACE_EXISTING)
                    }
                }

                val failures = ArrayList<WorkspaceMount>()
                mounts.forEach {
                    try {
                        val file = File(translateAndCheckFile(fsRoot, it.source)).toPath()
                        transferFile(file, workspace.resolve(it.destination), file)
                    } catch (ex: Throwable) {
                        log.info("Failed to add ${it.source}. ${ex.message}")
                        log.debug(ex.stackTraceToString())

                        failures.add(it)
                    }
                }

                if (failures.isNotEmpty() && !allowFailures) {
                    delete(workspaceId)
                    throw RPCException("Workspace creation had failures: $failures", HttpStatusCode.BadRequest)
                }

                CreatedWorkspace(workspaceId, failures)
            }
        }

    }

    suspend fun transfer(
        user: String,
        workspaceId: String,
        fileGlobs: List<String>,
        destination: String
    ): List<String> {
        val workspace = workspace(workspaceId)
        if (!Files.exists(workspace)) throw RPCException.fromStatusCode(HttpStatusCode.NotFound)

        val uid = userDao.findStorageUser(user) ?: throw RPCException.fromStatusCode(HttpStatusCode.Unauthorized)

        val matchers = fileGlobs.map { FileSystems.getDefault().getPathMatcher("glob:$it") }
        val destinationDir = File(translateAndCheckFile(fsRoot, destination)).toPath()

        val transferred = ArrayList<String>()

        Files.list(workspace)
            .filter { path ->
                val result = matchers.any { it.matches(path.fileName) }
                log.debug("Checking if we match $path. Do we? $result")
                result
            }
            .toList()
            .forEach {
                log.debug("Transferring file: $it")
                try {
                    val resolvedDestination = destinationDir.resolve(workspace.relativize(it))
                    log.debug("resolvedDestination: $resolvedDestination")
                    if (resolvedDestination.startsWith(destinationDir)) {
                        val path = "/" + fsRoot.toPath().relativize(resolvedDestination).toFile().path

                        Chown.setOwner(it, uid.toInt(), uid.toInt())
                        Files.move(it, resolvedDestination)
                        transferred.add(path)
                    } else {
                        log.info("Resolved destination is not within destination directory! $it $resolvedDestination")
                    }
                } catch (ex: Throwable) {
                    log.info("Failed to transfer $it. ${ex.message}")
                    log.debug(ex.stackTraceToString())
                }
            }

        log.debug("Transferred ${transferred.size} files")
        transferred.forEach { path ->
            log.debug("Fixing permissions of $path")
            val realPath = File(translateAndCheckFile(fsRoot, path)).toPath()
            Files.walk(realPath).forEach { file ->
                Chown.setOwner(file, uid.toInt(), uid.toInt())
                Files.setPosixFilePermissions(
                    file, setOf(
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_EXECUTE,
                        PosixFilePermission.GROUP_WRITE,
                        PosixFilePermission.GROUP_READ,
                        PosixFilePermission.GROUP_EXECUTE
                    )
                )
            }

            log.debug("Scanning external files")
            fileScanner.scanFilesCreatedExternally(path)
            log.debug("Scanning external files done")
        }

        log.debug("Done!")

        return transferred
    }

    fun delete(workspaceId: String) {
        val workspace = workspace(workspaceId)
        if (!Files.exists(workspace)) throw RPCException.fromStatusCode(HttpStatusCode.NotFound)
        workspace.toFile().deleteRecursively()
    }

    companion object : Loggable {
        override val log = logger()

        const val WORKSPACE_PATH = "workspace"
    }
}