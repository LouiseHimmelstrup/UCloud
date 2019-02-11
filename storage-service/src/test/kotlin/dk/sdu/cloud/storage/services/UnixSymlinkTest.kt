package dk.sdu.cloud.storage.services

import dk.sdu.cloud.file.api.FileType
import dk.sdu.cloud.file.SERVICE_USER
import dk.sdu.cloud.file.services.unixfs.FileAttributeParser
import dk.sdu.cloud.file.services.unixfs.UnixFSCommandRunnerFactory
import dk.sdu.cloud.file.services.unixfs.UnixFileSystem
import dk.sdu.cloud.file.services.withBlockingContext
import dk.sdu.cloud.storage.util.simpleStorageUserDao
import org.junit.Ignore
import org.junit.Test
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UnixSymlinkTest {
    @Ignore
    @Test
    fun `test creating a symlink`() {
        val userDao = simpleStorageUserDao()
        val fsRoot = Files.createTempDirectory("ceph-fs").toFile()
        val cephFs = UnixFileSystem(userDao, FileAttributeParser(userDao), fsRoot.absolutePath)
        val factory = UnixFSCommandRunnerFactory(userDao)
        val owner = SERVICE_USER

        factory.withBlockingContext(owner) { ctx ->
            val targetPath = "/target"
            val linkPath = "/link"

            cephFs.openForWriting(ctx, targetPath, false)
            val fileCreated = cephFs.write(ctx) {
                it.write("Hello, World!".toByteArray())
            }.value.single()

            val symlinkCreated = cephFs.createSymbolicLink(ctx, targetPath, linkPath).value.single()

            assertFalse(fileCreated.isLink)
            assertEquals(FileType.FILE, fileCreated.fileType)
            assertEquals(targetPath, fileCreated.path)

            assertTrue(symlinkCreated.isLink)
            assertEquals(fileCreated.id, symlinkCreated.linkTargetId)
            assertEquals(targetPath, symlinkCreated.linkTarget)
        }
    }
}
