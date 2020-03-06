package dk.sdu.cloud.activity.api

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import dk.sdu.cloud.file.api.AccessRight
import dk.sdu.cloud.service.Page
import dk.sdu.cloud.service.PaginationRequest
import dk.sdu.cloud.service.TYPE_PROPERTY
import dk.sdu.cloud.service.WithPaginationRequest
import org.omg.PortableInterceptor.ACTIVE

private const val TYPE_DOWNLOAD = "download"
private const val TYPE_DELETED = "deleted"
private const val TYPE_FAVORITE = "favorite"
private const val TYPE_INSPECTED = "inspected"
private const val TYPE_MOVED = "moved"

@Suppress("EnumEntryName") // backwards-compatibility
enum class ActivityEventType {
    download,
    updated,
    deleted,
    favorite,
    inspected,
    moved,
    copy,
    usedInApp,
    directoryCreated,
    reclassify,
    upload,
    updatedACL,
    sharedWith,
    appRun
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = TYPE_PROPERTY
)
@JsonSubTypes(
    JsonSubTypes.Type(value = ActivityEvent.Download::class, name = TYPE_DOWNLOAD),
    JsonSubTypes.Type(value = ActivityEvent.Deleted::class, name = TYPE_DELETED),
    JsonSubTypes.Type(value = ActivityEvent.Favorite::class, name = TYPE_FAVORITE),
    JsonSubTypes.Type(value = ActivityEvent.Inspected::class, name = TYPE_INSPECTED),
    JsonSubTypes.Type(value = ActivityEvent.Moved::class, name = TYPE_MOVED)
)
sealed class ActivityEvent {
    // NOTE(Dan): Please consult the README before you add new entries here. This should only contain
    // events related to file activity

    // When adding new entries here, you will also need to add entries in:
    // ActivityEventDao

    abstract val timestamp: Long
    abstract val filePath: String
    abstract val username: String

    // TODO We cannot reliably track who uploaded a file (due to bulk uploads)

    data class Reclassify(
        override val username: String,
        override val timestamp: Long,
        override val filePath: String,
        val newSensitivity: String
    ) : ActivityEvent()

    data class DirectoryCreated(
        override val username: String,
        override val timestamp: Long,
        override val filePath: String
    ) : ActivityEvent()

    data class Download(
        override val username: String,
        override val timestamp: Long,
        override val filePath: String
    ) : ActivityEvent()

    data class Copy(
        override val username: String,
        override val timestamp: Long,
        override val filePath: String,
        val copyFilePath: String
    ) : ActivityEvent()

    data class Uploaded(
        override val username: String,
        override val timestamp: Long,
        override val filePath: String
    ) : ActivityEvent()

    data class UpdatedAcl(
        override val username: String,
        override val timestamp: Long,
        override val filePath: String,
        val rightsAndUser: List<Pair<Set<AccessRight>, String>>
    ) : ActivityEvent()

    data class Favorite(
        override val username: String,
        val isFavorite: Boolean,
        override val timestamp: Long,
        override val filePath: String
    ) : ActivityEvent()

    data class Inspected(
        override val username: String,
        override val timestamp: Long,
        override val filePath: String
    ) : ActivityEvent()

    data class Moved(
        override val username: String,
        val newName: String,
        override val timestamp: Long,
        override val filePath: String
    ) : ActivityEvent()

    data class Deleted(
        override val username: String,
        override val timestamp: Long,
        override val filePath: String
    ) : ActivityEvent()

    data class SingleFileUsedByApplication(
        override val username: String, //used By
        override val timestamp: Long,
        override val filePath: String,
        val applicationName: String,
        val applicationVersion: String
    ) : ActivityEvent()

    data class AllFilesUsedByApplication(
        override val username: String, //used By
        override val timestamp: Long,
        override val filePath: String,
        val applicationName: String,
        val applicationVersion: String
    ) : ActivityEvent()

    data class SharedWith(
        override val username: String,
        override val timestamp: Long,
        override val filePath: String,
        val sharedWith: String,
        val status: Set<AccessRight>
    ) : ActivityEvent()
}

val ActivityEvent.type: ActivityEventType get() = when (this) {
    is ActivityEvent.Download -> ActivityEventType.download
    is ActivityEvent.Favorite -> ActivityEventType.favorite
    is ActivityEvent.Inspected -> ActivityEventType.inspected
    is ActivityEvent.Moved -> ActivityEventType.moved
    is ActivityEvent.Deleted -> ActivityEventType.deleted
    is ActivityEvent.UsedByApplication -> ActivityEventType.usedInApp
    is ActivityEvent.DirectoryCreated -> ActivityEventType.directoryCreated
    is ActivityEvent.UpdatedAcl -> ActivityEventType.updatedACL
    is ActivityEvent.Uploaded -> ActivityEventType.upload
    is ActivityEvent.Reclassify -> ActivityEventType.reclassify
    is ActivityEvent.Copy -> ActivityEventType.copy
    is ActivityEvent.SharedWith -> ActivityEventType.sharedWith
    is ActivityEvent.AppRun -> ActivityEventType.appRun
}

data class ActivityEventGroup(
    val type: ActivityEventType,
    val newestTimestamp: Long,
    val numberOfHiddenResults: Long?,
    val items: List<ActivityEvent>
)

data class ListActivityByIdRequest(
    val id: String,
    override val itemsPerPage: Int?,
    override val page: Int?
) : WithPaginationRequest
typealias ListActivityByIdResponse = Page<ActivityEvent>


data class ListActivityByPathRequest(
    val path: String,
    override val itemsPerPage: Int?,
    override val page: Int?
) : WithPaginationRequest
typealias ListActivityByPathResponse = Page<ActivityEvent>


typealias ListActivityByUserRequest = PaginationRequest
typealias ListActivityByUserResponse = Page<ActivityEvent>
