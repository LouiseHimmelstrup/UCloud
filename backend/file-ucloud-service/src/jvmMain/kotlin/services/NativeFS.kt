package dk.sdu.cloud.file.ucloud.services

import com.sun.jna.Native
import com.sun.jna.Platform
import dk.sdu.cloud.file.orchestrator.api.FileType
import dk.sdu.cloud.file.orchestrator.api.WriteConflictPolicy
import dk.sdu.cloud.file.orchestrator.api.joinPath
import dk.sdu.cloud.service.Loggable
import io.ktor.utils.io.pool.*
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.channels.Channels
import java.nio.file.*
import java.nio.file.attribute.FileTime
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

data class NativeStat(
    val size: Long,
    val modifiedAt: Long,
    val fileType: FileType,
    val ownerUid: Int,
    val ownerGid: Int,
    val mode: Int,
)

sealed class CopyResult {
    object CreatedFile : CopyResult()
    class CreatedDirectory(val outputFile: InternalFile) : CopyResult()
    object NothingToCreate : CopyResult()
}

const val LINUX_FS_USER_UID = 11042

object NativeFS : Loggable {
    private const val O_NOFOLLOW = 0x20000
    private const val O_TRUNC = 0x200
    private const val O_CREAT = 0x40
    private const val O_EXCL = 0x80
    private const val O_WRONLY = 0x1
    private const val O_RDONLY = 0x0
    private const val O_DIRECTORY = 0x10000
    private const val ENOENT = 2
    private const val ENOTEMPTY = 39
    const val DEFAULT_DIR_MODE = 488 // 0750
    const val DEFAULT_FILE_MODE = 416 // 0640
    private const val AT_REMOVEDIR = 0x200
    private const val S_ISREG = 0x8000

    var disableChown = false

    override val log = logger()

    private fun openFile(file: InternalFile, flag: Int = 0): Int {
        with(CLibrary.INSTANCE) {
            val components = file.components()
            val fileDescriptors = IntArray(components.size) { -1 }
            try {
                fileDescriptors[0] = open("/${components[0]}", O_NOFOLLOW, 0)
                for (i in 1 until fileDescriptors.size) {
                    val previousFd = fileDescriptors[i - 1]
                    if (previousFd < 0) {
                        throw FSException.NotFound()
                    }

                    val opts =
                        if (i == fileDescriptors.lastIndex) O_NOFOLLOW or flag
                        else O_NOFOLLOW
                    fileDescriptors[i] = openat(previousFd, components[i], opts, DEFAULT_FILE_MODE)
                    close(previousFd)
                }
            } catch (ex: Throwable) {
                ex.printStackTrace()
                fileDescriptors.closeAll()
                throw ex
            }

            if (fileDescriptors.last() < 0) {
                throwExceptionBasedOnStatus(Native.getLastError())
            }

            return fileDescriptors.last()
        }
    }

    private fun IntArray.closeAll() {
        for (descriptor in this) {
            if (descriptor > 0) {
                CLibrary.INSTANCE.close(descriptor)
            }
        }
    }

    fun copy(
        source: InternalFile,
        destination: InternalFile,
        conflictPolicy: WriteConflictPolicy,
        owner: Int? = LINUX_FS_USER_UID,
    ): CopyResult {
        if (Platform.isLinux()) {
            with(CLibrary.INSTANCE) {
                val sourceFd = openFile(source)
                val parentFile = destination.parent()
                val destParentFd = openFile(parentFile)
                try {
                    if (sourceFd == -1 || destParentFd == -1) throw FSException.NotFound()
                    val sourceStat = nativeStat(sourceFd, autoClose = false)
                    val fileName = destination.fileName()
                    if (sourceStat.fileType == FileType.FILE) {
                        val (destFilename, destFd) = createAccordingToPolicy(
                            destParentFd,
                            fileName,
                            conflictPolicy,
                            isDirectory = false
                        )
                        if (destFd < 0) throw FSException.CriticalException("Unable to create file")

                        val ins = LinuxInputStream(sourceFd) // Closed later
                        LinuxOutputStream(destFd).use { outs ->
                            ins.copyTo(outs)
                            fchmod(destFd, sourceStat.mode)
                        }
                        return CopyResult.CreatedFile
                    } else if (sourceStat.fileType == FileType.DIRECTORY) {
                        val result = createAccordingToPolicy(destParentFd, fileName, conflictPolicy, isDirectory = true)

                        try {
                            if (owner != null) fchown(result.second, owner, owner)
                        } finally {
                            close(result.second)
                        }

                        return CopyResult.CreatedDirectory(
                            InternalFile(joinPath(parentFile.path.removeSuffix("/"), result.first).removeSuffix("/"))
                        )
                    } else {
                        return CopyResult.NothingToCreate
                    }
                } finally {
                    close(sourceFd)
                    close(destParentFd)
                }
            }
        } else {
            val file = File(source.path)
            Files.copy(
                file.toPath(),
                File(destination.path).toPath(),
                *(if (conflictPolicy == WriteConflictPolicy.REPLACE) arrayOf(StandardCopyOption.REPLACE_EXISTING)
                else emptyArray())
            )
            return if (file.isDirectory) CopyResult.CreatedDirectory(destination)
            else CopyResult.CreatedFile
        }
    }

    fun listFiles(file: InternalFile): List<String> {
        if (Platform.isLinux()) {
            with(CLibrary.INSTANCE) {
                val fd = openFile(file)
                if (fd < 0) {
                    close(fd)
                    throw FSException.NotFound()
                }

                val dir = fdopendir(fd)
                if (dir == null) {
                    close(fd)
                    throw FSException.IsDirectoryConflict()
                }

                val result = ArrayList<String>()
                while (true) {
                    val ent = readdir(dir) ?: break
                    // Read unsized string at end of struct. The ABI for this function leaves the size completely
                    // undefined.
                    val name = ent.pointer.getString(19) // 19 (bytes) is the offset in the struct
                    if (name == "." || name == "..") continue
                    result.add(name)
                }

                closedir(dir) // no need for close(fd) since closedir(dir) already does this
                return result
            }
        } else {
            return File(file.path).list()?.toList() ?: emptyList()
        }
    }

    private fun nativeStat(fd: Int, autoClose: Boolean = true): NativeStat {
        require(fd >= 0)
        val st = stat()
        st.write()
        val err = CLibrary.INSTANCE.__fxstat64(1, fd, st.pointer)
        st.read()

        if (autoClose) {
            CLibrary.INSTANCE.close(fd)
        }
        if (err < 0) {
            throw FSException.NotFound()
        }

        return NativeStat(
            st.st_size,
            (st.m_sec * 1000) + (st.m_nsec / 1_000_000),
            if (st.st_mode and S_ISREG == 0) FileType.DIRECTORY else FileType.FILE,
            st.st_uid,
            st.st_gid,
            st.st_mode
        )
    }

    private fun createAccordingToPolicy(
        parentFd: Int,
        desiredFileName: String,
        conflictPolicy: WriteConflictPolicy,
        isDirectory: Boolean,
    ): Pair<String, Int> {
        val mode = if (isDirectory) DEFAULT_DIR_MODE else DEFAULT_FILE_MODE

        fun createDirAndOpen(name: String): Pair<String, Int>? {
            // If it doesn't exist everything is good. Create the directory and return the name + fd.
            val status = CLibrary.INSTANCE.mkdirat(parentFd, name, DEFAULT_DIR_MODE)
            if (status >= 0) {
                val fd = CLibrary.INSTANCE.openat(parentFd, name, O_NOFOLLOW, DEFAULT_DIR_MODE)
                if (fd >= 0) return Pair(name, fd)

                // Very unexpected, but technically possible. Fall through to the naming step.
            }

            // The name was taken before we could complete our operation. Fall through to naming step.
            return null
        }

        var oflags = O_NOFOLLOW
        if (!isDirectory) {
            oflags = oflags or O_TRUNC or O_CREAT or O_WRONLY
            if (conflictPolicy != WriteConflictPolicy.REPLACE) oflags = oflags or O_EXCL
        } else {
            oflags = oflags or O_DIRECTORY
        }

        val desiredFd = CLibrary.INSTANCE.openat(parentFd, desiredFileName, oflags, mode)
        if (!isDirectory) {
            if (desiredFd >= 0) return Pair(desiredFileName, desiredFd)
        } else {
            // If it exists and we allow overwrite then just return the open directory
            if (
                (conflictPolicy == WriteConflictPolicy.REPLACE || conflictPolicy == WriteConflictPolicy.MERGE_RENAME) &&
                desiredFd >= 0
            ) {
                return Pair(desiredFileName, desiredFd)
            } else if (desiredFd < 0) {
                val result = createDirAndOpen(desiredFileName)
                if (result != null) return result
            } else {
                CLibrary.INSTANCE.close(desiredFd) // We don't need this one
            }

            // We need to create a differently named directory (see below)
        }

        if (conflictPolicy == WriteConflictPolicy.REJECT) throw FSException.AlreadyExists()
        check(conflictPolicy == WriteConflictPolicy.RENAME || conflictPolicy == WriteConflictPolicy.MERGE_RENAME)

        for (attempt in 1 until 10_000) { // NOTE(Dan): We put an upper-limit to avoid looping 'forever'
            val filenameWithoutExtension = desiredFileName.substringBeforeLast('.')
            val extension = desiredFileName.substringAfterLast('.')
            val hasExtension = desiredFileName.length != filenameWithoutExtension.length

            val newName = buildString {
                append(filenameWithoutExtension)
                append("(")
                append(attempt)
                append(")")
                if (hasExtension) {
                    append('.')
                    append(extension)
                }
            }
            val attemptedFd = CLibrary.INSTANCE.openat(parentFd, newName, oflags, mode)
            if (!isDirectory) {
                if (attemptedFd >= 0) return Pair(newName, attemptedFd)
            } else {
                val result = createDirAndOpen(newName)
                if (result != null) return result
            }
        }

        throw FSException.BadRequest("Too many files with this name exist: '$desiredFileName'")
    }

    private fun setMetadataForNewFile(
        fd: Int,
        owner: Int? = LINUX_FS_USER_UID,
        permissions: Int?,
    ) {
        require(fd >= 0)
        if (owner != null) CLibrary.INSTANCE.fchown(fd, owner, owner)
        if (permissions != null) CLibrary.INSTANCE.fchmod(fd, permissions)
    }

    fun openForWriting(
        file: InternalFile,
        allowOverwrite: Boolean,
        owner: Int? = LINUX_FS_USER_UID,
        permissions: Int?,
    ): OutputStream {
        if (Platform.isLinux()) {
            var opts = O_TRUNC or O_CREAT or O_WRONLY
            if (!allowOverwrite) {
                opts = opts or O_EXCL
            }

            val fd = openFile(file, opts)
            if (fd < 0) {
                CLibrary.INSTANCE.close(fd)
                throw FSException.NotFound()
            }

            setMetadataForNewFile(fd, owner, permissions)
            return LinuxOutputStream(fd)
        } else {
            val options = HashSet<OpenOption>()
            options.add(StandardOpenOption.TRUNCATE_EXISTING)
            options.add(StandardOpenOption.WRITE)
            options.add(LinkOption.NOFOLLOW_LINKS)
            if (!allowOverwrite) {
                options.add(StandardOpenOption.CREATE_NEW)
            } else {
                options.add(StandardOpenOption.CREATE)
            }

            try {
                val systemPath = File(file.path).toPath()
                return Channels.newOutputStream(
                    Files.newByteChannel(
                        systemPath,
                        options,
                        PosixFilePermissions.asFileAttribute(DEFAULT_POSIX_FILE_MODE)
                    )
                )
            } catch (ex: FileAlreadyExistsException) {
                throw FSException.AlreadyExists()
            } catch (ex: Throwable) {
                if (ex.message?.contains("Is a directory") == true) {
                    throw FSException.BadRequest("Upload target is a not a directory")
                } else {
                    throw ex
                }
            }
        }
    }

    fun openForReading(file: InternalFile): InputStream {
        return if (Platform.isLinux()) {
            LinuxInputStream(openFile(file, O_RDONLY)).buffered()
        } else {
            FileInputStream(file.path)
        }
    }

    fun delete(file: InternalFile) {
        if (Platform.isLinux()) {
            val fd = openFile(file.parent())
            if (fd < 0) throw FSException.NotFound()
            try {
                if (CLibrary.INSTANCE.unlinkat(fd, file.fileName(), AT_REMOVEDIR) < 0) {
                    if (CLibrary.INSTANCE.unlinkat(fd, file.fileName(), 0) < 0) {
                        if (Native.getLastError() == ENOTEMPTY) {
                            throw FSException.BadRequest()
                        }

                        throw FSException.NotFound()
                    }
                }
            } finally {
                CLibrary.INSTANCE.close(fd)
            }
        } else {
            Files.delete(File(file.path).toPath())
        }
    }

    fun getExtendedAttribute(file: InternalFile, attribute: String): String {
        return if (Platform.isLinux()) {
            val fd = openFile(file)
            try {
                getExtendedAttribute(fd, attribute)
            } finally {
                CLibrary.INSTANCE.close(fd)
            }
        } else {
            DefaultByteArrayPool.useInstance {
                val read = XAttrOSX.INSTANCE.getxattr(file.path, attribute, it, it.size, 0, 0)
                if (read < 0) throw NativeException(Native.getLastError())
                String(it, 0, read, Charsets.UTF_8)
            }
        }
    }

    private fun getExtendedAttribute(fd: Int, attribute: String): String {
        require(Platform.isLinux())
        return DefaultByteArrayPool.useInstance { buf ->
            val xattrSize = CLibrary.INSTANCE.fgetxattr(fd, attribute, buf, buf.size)
            if (xattrSize < 0) throw NativeException(Native.getLastError())
            String(buf, 0, xattrSize, Charsets.UTF_8)
        }
    }

    /*
    fun setExtendedAttribute(path: File, attribute: String, value: String, allowOverwrite: Boolean): Int {
        val bytes = value.toByteArray()

        if (Platform.isLinux()) {
            val fd = openFile(path.absolutePath)
            try {
                return CLibrary.INSTANCE.fsetxattr(fd, attribute, bytes, bytes.size, if (!allowOverwrite) 1 else 0)
            } finally {
                CLibrary.INSTANCE.close(fd)
            }
        } else {
            return XAttrOSX.INSTANCE.setxattr(
                path.absolutePath,
                attribute,
                bytes,
                bytes.size,
                0,
                if (!allowOverwrite) 2 else 0
            )
        }
    }

    fun removeExtendedAttribute(path: File, attribute: String) {
        if (Platform.isLinux()) {
            val fd = openFile(path.absolutePath)
            try {
                CLibrary.INSTANCE.fremovexattr(fd, attribute)
            } finally {
                CLibrary.INSTANCE.close(fd)
            }
        } else {
            XAttrOSX.INSTANCE.removexattr(path.absolutePath, attribute, 0)
        }
    }
     */


    fun readNativeFilePermissons(file: InternalFile): Int {
        return if (Platform.isLinux()) {
            val fd = openFile(file)
            if (fd < 0) throw FSException.NotFound()
            val st = stat()
            st.write()
            val err = CLibrary.INSTANCE.__fxstat64(1, fd, st.pointer)
            st.read()
            CLibrary.INSTANCE.close(fd)
            if (err < 0) {
                throw FSException.NotFound()
            }
            st.st_mode
        } else {
            // rw-rw-rw- in octal
            304472
        }
    }

    fun stat(file: InternalFile): NativeStat {
        if (Platform.isLinux()) {
            val fd = openFile(file)
            if (fd < 0) throw FSException.NotFound()
            return nativeStat(fd)
        } else {
            if (Files.isSymbolicLink(File(file.path).toPath())) throw FSException.NotFound()

            val basicAttributes = run {
                val opts = listOf("size", "lastModifiedTime", "isDirectory")
                Files.readAttributes(File(file.path).toPath(), opts.joinToString(","), LinkOption.NOFOLLOW_LINKS)
            }

            val size = basicAttributes.getValue("size") as Long
            val fileType = if (basicAttributes.getValue("isDirectory") as Boolean) {
                FileType.DIRECTORY
            } else {
                FileType.FILE
            }

            val modifiedAt = (basicAttributes.getValue("lastModifiedTime") as FileTime).toMillis()
            return NativeStat(size, modifiedAt, fileType, LINUX_FS_USER_UID, LINUX_FS_USER_UID, DEFAULT_FILE_MODE)
        }
    }


    fun chmod(file: InternalFile, mode: Int) {
        if (!Platform.isLinux()) return
        val fd = openFile(file)
        try {
            CLibrary.INSTANCE.fchmod(fd, mode)
        } finally {
            CLibrary.INSTANCE.close(fd)
        }
    }

    fun chown(file: InternalFile, uid: Int, gid: Int) {
        if (!Platform.isLinux() || disableChown) return
        val fd = openFile(file)
        try {
            CLibrary.INSTANCE.fchown(fd, uid, gid)
        } finally {
            CLibrary.INSTANCE.close(fd)
        }
    }

    fun changeFilePermissions(file: InternalFile, mode: Int, uid: Int, gid: Int) {
        if (!Platform.isLinux()) return
        val fd = openFile(file)
        try {
            CLibrary.INSTANCE.fchmod(fd, mode)
            CLibrary.INSTANCE.fchown(fd, uid, gid)
        } finally {
            CLibrary.INSTANCE.close(fd)
        }
    }

    fun createDirectories(file: InternalFile, owner: Int? = LINUX_FS_USER_UID) {
        if (Platform.isLinux()) {
            with(CLibrary.INSTANCE) {
                val components = file.components()
                if (components.isEmpty()) throw IllegalArgumentException("Path is empty")

                val fileDescriptors = IntArray(components.size - 1) { -1 }
                var didCreatePrevious = false
                try {
                    fileDescriptors[0] = open("/${components[0]}", O_NOFOLLOW, 0)
                    var i = 1
                    while (i < fileDescriptors.size) {
                        val previousFd = fileDescriptors[i - 1]
                        if (previousFd < 0) {
                            throw FSException.NotFound()
                        }

                        if (didCreatePrevious && owner != null) {
                            fchown(previousFd, owner, owner)
                            didCreatePrevious = false
                        }

                        fileDescriptors[i] = openat(fileDescriptors[i - 1], components[i], O_NOFOLLOW, 0)

                        if (fileDescriptors[i] < 0 && Native.getLastError() == ENOENT) {
                            val err = mkdirat(fileDescriptors[i - 1], components[i], DEFAULT_DIR_MODE)
                            if (err < 0) throw FSException.NotFound()
                        } else {
                            i++
                        }
                    }

                    val finalFd = fileDescriptors.last()
                    if (finalFd < 0) throwExceptionBasedOnStatus(Native.getLastError())

                    val error = mkdirat(finalFd, components.last(), DEFAULT_DIR_MODE)
                    if (error != 0) {
                        throwExceptionBasedOnStatus(Native.getLastError())
                    }

                    if (owner != null) {
                        val fd = openat(finalFd, components.last(), 0, 0)
                        fchown(fd, owner, owner)
                        close(fd)
                    }
                } finally {
                    for (descriptor in fileDescriptors) {
                        if (descriptor > 0) {
                            close(descriptor)
                        }
                    }
                }
            }
        } else {
            File(file.path).mkdirs()
        }
    }

    fun move(source: InternalFile, destination: InternalFile, replaceExisting: Boolean) {
        if (Platform.isLinux()) {
            val fromParent = openFile(source.parent())
            val toParent = openFile(destination.parent())

            try {
                if (fromParent == -1 || toParent == -1) {
                    throw FSException.NotFound()
                }

                val doesToExist = if (!replaceExisting) {
                    val fd = CLibrary.INSTANCE.openat(toParent, destination.fileName(), 0, 0)
                    if (fd >= 0) {
                        CLibrary.INSTANCE.close(fd)
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }

                if (doesToExist) {
                    throw FSException.AlreadyExists()
                }

                val err = CLibrary.INSTANCE.renameat(fromParent, source.fileName(), toParent, destination.fileName())
                if (err < 0) {
                    throwExceptionBasedOnStatus(Native.getLastError())
                }
            } finally {
                CLibrary.INSTANCE.close(fromParent)
                CLibrary.INSTANCE.close(toParent)
            }
        } else {
            val opts = run {
                val extraOpts: Array<CopyOption> =
                    if (replaceExisting) arrayOf(StandardCopyOption.REPLACE_EXISTING)
                    else emptyArray()

                extraOpts + arrayOf(LinkOption.NOFOLLOW_LINKS)
            }

            Files.move(File(source.path).toPath(), File(destination.path).toPath(), *opts)
        }
    }
}

class NativeException(val statusCode: Int) : RuntimeException("Native exception, code: $statusCode")

val DEFAULT_POSIX_FILE_MODE = setOf(
    PosixFilePermission.OWNER_READ,
    PosixFilePermission.OWNER_WRITE,
    PosixFilePermission.GROUP_READ,
    PosixFilePermission.GROUP_WRITE
)

val DEFAULT_POSIX_DIRECTORY_MODE = setOf(
    PosixFilePermission.OWNER_READ,
    PosixFilePermission.OWNER_WRITE,
    PosixFilePermission.OWNER_EXECUTE,
    PosixFilePermission.GROUP_READ,
    PosixFilePermission.GROUP_WRITE,
    PosixFilePermission.GROUP_EXECUTE,
    PosixFilePermission.OTHERS_EXECUTE
)