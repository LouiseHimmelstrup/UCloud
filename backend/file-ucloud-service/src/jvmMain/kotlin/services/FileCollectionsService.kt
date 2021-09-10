package dk.sdu.cloud.file.ucloud.services

import dk.sdu.cloud.FindByStringId
import dk.sdu.cloud.calls.BulkRequest
import dk.sdu.cloud.calls.BulkResponse
import dk.sdu.cloud.defaultMapper
import dk.sdu.cloud.file.orchestrator.api.*
import dk.sdu.cloud.file.ucloud.services.PathConverter.Companion.PRODUCT_PM_REFERENCE
import dk.sdu.cloud.file.ucloud.services.PathConverter.Companion.PRODUCT_REFERENCE
import dk.sdu.cloud.service.Loggable
import dk.sdu.cloud.service.db.async.DBContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement

class FileCollectionsService(
    private val pathConverter: PathConverter,
    private val db: DBContext,
    private val taskSystem: TaskSystem,
    private val nativeFs: NativeFS,
) {
    suspend fun create(request: BulkRequest<FileCollection>): BulkResponse<FindByStringId?> {
        for (item in request.items) {
            nativeFs.createDirectories(pathConverter.collectionLocation(item.id))
        }
        return BulkResponse(request.items.map { null })
    }

    suspend fun delete(collections: BulkRequest<FileCollection>) {
        taskSystem.submitTask(
            Files.delete.fullName,
            defaultMapper.encodeToJsonElement(
                BulkRequest(
                    collections.items.map { collection ->
                        FindByPath("/${collection.id}")
                    }
                ) as FilesDeleteRequest
            ) as JsonObject
        )
    }

    suspend fun rename(
        renames: BulkRequest<FileCollectionsProviderRenameRequestItem>,
    ) {
        // Do nothing
    }

    companion object : Loggable {
        override val log = logger()
    }
}

val productSupport = listOf(
    FSSupport(
        PRODUCT_REFERENCE,

        FSProductStatsSupport(
            sizeInBytes = true,
            sizeIncludingChildrenInBytes = false,
            modifiedAt = true,
            createdAt = false,
            accessedAt = true,
            unixPermissions = true,
            unixOwner = true,
            unixGroup = true
        ),

        FSCollectionSupport(
            aclModifiable = true,
            usersCanCreate = true,
            usersCanDelete = true,
            usersCanRename = true,
        ),

        FSFileSupport(
            aclModifiable = true,
            trashSupported = true,
            isReadOnly = false
        )
    ),
    FSSupport(
        PRODUCT_PM_REFERENCE,

        FSProductStatsSupport(
            sizeInBytes = true,
            sizeIncludingChildrenInBytes = false,
            modifiedAt = true,
            createdAt = false,
            accessedAt = true,
            unixPermissions = true,
            unixOwner = true,
            unixGroup = true
        ),

        FSCollectionSupport(
            aclModifiable = false,
            usersCanCreate = false,
            usersCanDelete = true,
            usersCanRename = false,
        ),

        FSFileSupport(
            aclModifiable = false,
            trashSupported = true,
            isReadOnly = false
        )
    )
)
