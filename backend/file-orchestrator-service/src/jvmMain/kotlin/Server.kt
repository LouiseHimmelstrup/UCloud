package dk.sdu.cloud.file.orchestrator

import dk.sdu.cloud.accounting.util.asController
import dk.sdu.cloud.auth.api.authenticator
import dk.sdu.cloud.calls.client.OutgoingHttpCall
import dk.sdu.cloud.calls.client.call
import dk.sdu.cloud.calls.client.orThrow
import dk.sdu.cloud.file.orchestrator.api.FileCollectionsProvider
import dk.sdu.cloud.file.orchestrator.api.FilesProvider
import dk.sdu.cloud.file.orchestrator.rpc.*
import dk.sdu.cloud.file.orchestrator.service.*
import dk.sdu.cloud.micro.Micro
import dk.sdu.cloud.micro.backgroundScope
import dk.sdu.cloud.micro.databaseConfig
import dk.sdu.cloud.service.CommonServer
import dk.sdu.cloud.service.configureControllers
import dk.sdu.cloud.service.db.async.AsyncDBSessionFactory
import dk.sdu.cloud.service.startServices

class Server(override val micro: Micro) : CommonServer {
    override val log = logger()

    override fun start() {
        val db = AsyncDBSessionFactory(micro.databaseConfig)
        val serviceClient = micro.authenticator.authenticateClient(OutgoingHttpCall)
        val providers = StorageProviders(serviceClient) { comms ->
            StorageCommunication(
                comms.client,
                comms.wsClient,
                comms.provider,
                FilesProvider(comms.provider.id),
                FileCollectionsProvider(comms.provider.id)
            )
        }
        val providerSupport = StorageProviderSupport(providers, serviceClient) { comms ->
            comms.fileCollectionsApi.retrieveProducts.call(Unit, comms.client).orThrow().responses
        }
        val projectCache = ProjectCache(serviceClient)
        val metadataTemplates = MetadataTemplates(db, projectCache)
        val metadataService = MetadataService(db, projectCache, metadataTemplates)
        val filesService = FilesService(providers, providerSupport, projectCache, metadataService)
        val fileCollections = FileCollectionService(db, providers, providerSupport, serviceClient)
        val shares = ShareService(db, serviceClient, micro.backgroundScope)

        configureControllers(
            FileMetadataController(metadataService),
            FileController(filesService),
            fileCollections.asController(),
            FileMetadataTemplateController(metadataTemplates),
            ShareController(shares)
        )

        startServices()
    }
}
