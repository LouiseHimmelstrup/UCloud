package dk.sdu.cloud.app.services


import ch.qos.logback.core.net.server.Client
import dk.sdu.cloud.app.api.VerifiedJob
import dk.sdu.cloud.file.api.FileDescriptions
import dk.sdu.cloud.file.api.FindHomeFolderResponse
import dk.sdu.cloud.file.api.LongRunningResponse
import dk.sdu.cloud.file.api.MultiPartUploadDescriptions
import dk.sdu.cloud.service.test.ClientMock
import kotlinx.coroutines.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JobFileTest{

    @Test
    fun `initialize Result folder test`() {
        val authClient = ClientMock.authenticatedClient
        val service = JobFileService(authClient)

        ClientMock.mockCallSuccess(
            FileDescriptions.findHomeFolder,
            FindHomeFolderResponse("home")
        )
        ClientMock.mockCallSuccess(
            FileDescriptions.createDirectory,
            LongRunningResponse.Result(item = Unit)
        )

        runBlocking {
            service.initializeResultFolder(verifiedJobWithAccessToken)
        }
    }

    @Test
    fun `test accept File - no extract also abosolute path`() {
        val authClient = ClientMock.authenticatedClient
        val service = JobFileService(authClient)

        ClientMock.mockCallSuccess(
            FileDescriptions.findHomeFolder,
            FindHomeFolderResponse("home")
        )

        ClientMock.mockCallSuccess(
            MultiPartUploadDescriptions.upload,
            Unit
        )

        runBlocking {
            service.acceptFile(
                verifiedJobWithAccessToken,
                "/filepath",
                2000,
                ByteReadChannel.Empty,
                false
            )

        }
    }

    @Test
    fun `test accept File - with extract`() {
        val authClient = ClientMock.authenticatedClient
        val service = JobFileService(authClient)

        ClientMock.mockCallSuccess(
            FileDescriptions.findHomeFolder,
            FindHomeFolderResponse("home")
        )

        ClientMock.mockCallSuccess(
            MultiPartUploadDescriptions.upload,
            Unit
        )

        ClientMock.mockCallSuccess(
            FileDescriptions.extract,
            Unit
        )

        runBlocking {
            service.acceptFile(
                verifiedJobWithAccessToken,
                "filepath",
                2000,
                ByteReadChannel.Empty,
                true
            )

        }
    }

    @Test
    fun `jobFolder test`() {
        val authClient = ClientMock.authenticatedClient
        val service = JobFileService(authClient)

        ClientMock.mockCallSuccess(
            FileDescriptions.findHomeFolder,
            FindHomeFolderResponse("home")
        )

        runBlocking {
            val result = service.jobFolder(verifiedJob)
            assertTrue(result.startsWith("/home/Jobs/title"))
        }
    }
}
