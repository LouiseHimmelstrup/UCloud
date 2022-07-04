package dk.sdu.cloud.alerting.api

import dk.sdu.cloud.*
import dk.sdu.cloud.calls.CallDescriptionContainer
import dk.sdu.cloud.calls.*
import dk.sdu.cloud.service.ScriptMetadata
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer

@Serializable
data class ScriptsStartRequestItem(val scriptId: String)

@Serializable
data class ScriptInfo(
    val metadata: ScriptMetadata,
    val lastRun: Long,
)

@Serializable
data class ScriptsBrowseRequest(
    override val itemsPerPage: Int? = null,
    override val next: String? = null,
    override val consistency: PaginationRequestV2Consistency? = null,
    override val itemsToSkip: Long? = null
) : WithPaginationRequestV2

// NOTE(Dan): This is put in the alerting-service assuming that this service will eventually be merged with other core
// services. This is probably the best location we currently have, but in the end it should just end up in the "core"
// service, which all other services depend on.
object Scripts : CallDescriptionContainer("scripts") {
    const val baseContext = "/api/scripts"

    val browse = call("browse", ScriptsBrowseRequest.serializer(), PageV2.serializer(ScriptInfo.serializer()), CommonErrorMessage.serializer()) {
        httpBrowse(baseContext, roles = Roles.PRIVILEGED)
    }

    val start = call("start", BulkRequest.serializer(ScriptsStartRequestItem.serializer()), Unit.serializer(), CommonErrorMessage.serializer()) {
        httpUpdate(baseContext, "start", roles = Roles.PRIVILEGED)
    }
}
