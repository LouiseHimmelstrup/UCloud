package dk.sdu.cloud.file.orchestrator.service

import dk.sdu.cloud.ActorAndProject
import dk.sdu.cloud.accounting.api.Product
import dk.sdu.cloud.accounting.api.ProductType
import dk.sdu.cloud.accounting.util.*
import dk.sdu.cloud.calls.client.AuthenticatedClient
import dk.sdu.cloud.file.orchestrator.api.*
import dk.sdu.cloud.provider.api.Permission
import dk.sdu.cloud.service.db.async.*
import kotlinx.serialization.serializer

typealias FolderSvcSuper = ResourceService<SyncFolder, SyncFolder.Spec, SyncFolder.Update, SyncFolderIncludeFlags,
    SyncFolder.Status, Product.Synchronization, SyncFolderSupport, SimpleProviderCommunication>

class SyncFolderService(
    db: AsyncDBSessionFactory,
    providers: Providers<SimpleProviderCommunication>,
    support: ProviderSupport<SimpleProviderCommunication, Product.Synchronization, SyncFolderSupport>,
    serviceClient: AuthenticatedClient,
    private val fileCollectionService: FileCollectionService,
) : FolderSvcSuper(db, providers, support, serviceClient) {
    override val table = SqlObject.Table("file_orchestrator.sync_folders")
    override val defaultSortColumn = SqlObject.Column(table, "resource")
    override val sortColumns: Map<String, SqlObject.Column> = mapOf("resource" to defaultSortColumn)
    override val serializer = serializer<SyncFolder>()
    override val updateSerializer = serializer<SyncFolder.Update>()
    override val productArea = ProductType.SYNCHRONIZATION

    override fun userApi() = SyncFolders
    override fun controlApi() = SyncFolderControl
    override fun providerApi(comms: ProviderComms) = SyncFolderProvider(comms.provider.id)

    override suspend fun createSpecifications(
        actorAndProject: ActorAndProject,
        idWithSpec: List<Pair<Long, SyncFolder.Spec>>,
        session: AsyncDBConnection,
        allowDuplicates: Boolean
    ) {
        val collectionIds = idWithSpec.map {
            extractPathMetadata(it.second.path).collection
        }.toSet()

        val fileCollections = fileCollectionService.retrieveBulk(
            actorAndProject,
            collectionIds,
            listOf(Permission.Read)
        )

        println(fileCollections)

        session
            .sendPreparedStatement(
                {
                    idWithSpec.split {
                        into("ids") { it.first }
                        into("paths") { it.second.path }
                        into("permissions") { (_, spec) ->
                            val permissions = fileCollections.find {
                                it.id == extractPathMetadata(spec.path).collection
                            }!!.permissions!!.myself


                            if (permissions.contains(Permission.Edit) || permissions.contains(Permission.Admin)) {
                                SynchronizationType.SEND_RECEIVE.name
                            } else {
                                SynchronizationType.SEND_ONLY.name
                            }

                        }
                        into("devices") {
                            "UCLOUD_DEVICE_ID"
                        }
                    }
                    println(idWithSpec.first().second.path)
                },
                """
                    insert into file_orchestrator.sync_folders (resource, device_id, path, sync_type)
                    select unnest(:ids::bigint[]), unnest(:devices::text[]), unnest(:paths::text[]), unnest(:permissions::text[])
                    on conflict (resource) do nothing
                """
            )
    }

    override suspend fun browseQuery(flags: SyncFolderIncludeFlags?, query: String?): PartialQuery {
        println("Browsing syncfolders")
        println("query: $query, filter_path: ${flags?.filterByPath}")
        return PartialQuery(
            {
                setParameter("query", query)
                setParameter("filter_path", flags?.filterByPath)
            },
            """
                select *
                from file_orchestrator.sync_folders
                where
                    (:query::text is null or path ilike ('%' || :query || '%')) and
                    (:filter_path::text is null or :filter_path = path)
            """
        )
    }
}