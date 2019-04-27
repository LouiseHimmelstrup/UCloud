package dk.sdu.cloud.storage.services

import dk.sdu.cloud.file.api.StorageEvents
import dk.sdu.cloud.file.api.fileName
import dk.sdu.cloud.file.services.*
import dk.sdu.cloud.file.services.linuxfs.LinuxFSRunner
import dk.sdu.cloud.file.services.linuxfs.LinuxFSRunnerFactory
import dk.sdu.cloud.service.NormalizedPaginationRequest
import dk.sdu.cloud.service.test.EventServiceMock
import dk.sdu.cloud.service.test.assertThatInstance
import dk.sdu.cloud.service.test.assertThatProperty
import dk.sdu.cloud.storage.util.linuxFSWithRelaxedMocks
import dk.sdu.cloud.storage.util.mkdir
import dk.sdu.cloud.storage.util.touch
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import dk.sdu.cloud.file.api.*

class SymlinkRenameTest {
    val user = "user"

    data class TestContext(
        val runner: LinuxFSRunnerFactory,
        val fs: LowLevelFileSystemInterface<LinuxFSRunner>,
        val coreFs: CoreFileSystemService<LinuxFSRunner>,
        val lookupService: FileLookupService<LinuxFSRunner>
    )

    private fun initTest(root: File): TestContext {
        BackgroundScope.init()

        val (runner, fs) = linuxFSWithRelaxedMocks(root.absolutePath)
        val eventProducer = StorageEventProducer(EventServiceMock.createProducer(StorageEvents.events), {})
        val coreFs = CoreFileSystemService(fs, eventProducer)
        val fileLookupService = FileLookupService(coreFs)

        return TestContext(runner, fs, coreFs, fileLookupService)
    }

    private fun createRoot(): File = Files.createTempDirectory("sensitivity-test").toFile()

    @Test
    fun `test buggy renaming`() {
        val root = createRoot()
        with(initTest(root)) {
            root.mkdir("home") {
                mkdir("user") {
                    touch("Foo")
                    touch("Foo(1)")
                }

                mkdir("user2") {}
            }

            runner.withBlockingContext(user) { ctx ->
                val created1 = coreFs.createSymbolicLink(ctx, "/home/user/Foo(1)", "/home/user2/Foo(1)")
                val created2 = coreFs.createSymbolicLink(ctx, "/home/user/Foo", "/home/user2/Foo")

                assertEquals("/home/user2/Foo(1)", created1.path)
                assertEquals("/home/user2/Foo", created2.path)

                val lookup = lookupService.listDirectory(ctx, "/home/user2", NormalizedPaginationRequest(null, null))
                assertThatProperty(lookup, { it.items.size }) { it == 2 }
                assertThatInstance(lookup) { it.items.map { it.path.fileName() }.contains("Foo") }
                assertThatInstance(lookup) { it.items.map { it.path.fileName() }.contains("Foo(1)") }
            }
        }
    }
}