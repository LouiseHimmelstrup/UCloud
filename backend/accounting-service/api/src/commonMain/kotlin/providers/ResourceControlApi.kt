package dk.sdu.cloud.accounting.api.providers

import dk.sdu.cloud.CommonErrorMessage
import dk.sdu.cloud.FindByStringId
import dk.sdu.cloud.Roles
import dk.sdu.cloud.accounting.api.Product
import dk.sdu.cloud.calls.*
import dk.sdu.cloud.provider.api.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.serializer
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@Serializable
data class ResourceChargeCredits(
    @UCloudApiDoc("The ID of the `Resource`")
    val id: String,

    @UCloudApiDoc(
        "The ID of the charge\n\n" +
            "This charge ID must be unique for the `Resource`, UCloud will reject charges which are not unique."
    )
    val chargeId: String,

    @UCloudApiDoc("Amount of units to charge the user")
    val units: Long
)

@Serializable
data class ResourceChargeCreditsResponse(
    @UCloudApiDoc(
        "A list of resources which could not be charged due to lack of funds. " +
            "If all resources were charged successfully then this will empty."
    )
    val insufficientFunds: List<FindByStringId>,

    @UCloudApiDoc(
        "A list of resources which could not be charged due to it being a duplicate charge. " +
            "If all resources were charged successfully this will be empty."
    )
    val duplicateCharges: List<FindByStringId>,
)

@Serializable
data class ProviderRegisteredResource<Spec : ResourceSpecification>(
    val spec: Spec,
    val providerGeneratedId: String? = null,
)

@OptIn(ExperimentalStdlibApi::class)
@TSSkipCodegen
abstract class ResourceControlApi<
    Res : Resource<*>,
    Spec : ResourceSpecification,
    Update : ResourceUpdate,
    Flags : ResourceIncludeFlags,
    Status : ResourceStatus,
    Prod : Product,
    Support : ProductSupport>(namespace: String) : CallDescriptionContainer("$namespace.control") {
    val baseContext = "/api/${namespace.replace(".", "/")}/control"

    abstract val typeInfo: ResourceTypeInfo<Res, Spec, Update, Flags, Status, Prod, Support>

    val retrieve: CallDescription<ResourceRetrieveRequest<Flags>, Res, CommonErrorMessage>
        get() = call(
            name = "retrieve",
            handler = {
                httpRetrieve(
                    ResourceRetrieveRequest.serializer(typeInfo.flagsSerializer),
                    typeOf<ResourceRetrieveRequest<Flags>>(),
                    baseContext,
                    roles = Roles.PROVIDER
                )
            },
            requestType = ResourceRetrieveRequest.serializer(typeInfo.flagsSerializer),
            successType = typeInfo.resSerializer,
            errorType = CommonErrorMessage.serializer(),
            requestClass = typeOf<ResourceRetrieveRequest<Flags>>(),
            successClass = typeInfo.resType,
            errorClass = typeOf<CommonErrorMessage>()
        )

    val update: CallDescription<BulkRequest<ResourceUpdateAndId<Update>>, Unit, CommonErrorMessage>
        get() = call(
            name = "update",
            handler = {
                httpUpdate(
                    BulkRequest.serializer(ResourceUpdateAndId.serializer(typeInfo.updateSerializer)),
                    baseContext,
                    "update",
                    roles = Roles.PROVIDER
                )
            },
            requestType = BulkRequest.serializer(ResourceUpdateAndId.serializer(typeInfo.updateSerializer)),
            successType = Unit.serializer(),
            errorType = CommonErrorMessage.serializer(),
            requestClass = typeOf<BulkRequest<ResourceUpdateAndId<Update>>>(),
            successClass = typeOf<Unit>(),
            errorClass = typeOf<CommonErrorMessage>(),
        )

    val chargeCredits: CallDescription<BulkRequest<ResourceChargeCredits>, ResourceChargeCreditsResponse,
        CommonErrorMessage>
        get() = call(
            name = "chargeCredits",
            handler = {
                httpUpdate(
                    BulkRequest.serializer(ResourceChargeCredits.serializer()),
                    baseContext,
                    "chargeCredits",
                    roles = Roles.PROVIDER
                )
            },
            requestType = BulkRequest.serializer(ResourceChargeCredits.serializer()),
            successType = ResourceChargeCreditsResponse.serializer(),
            errorType = CommonErrorMessage.serializer(),
            requestClass = typeOf<BulkRequest<ResourceChargeCredits>>(),
            successClass = typeOf<ResourceChargeCreditsResponse>(),
            errorClass = typeOf<CommonErrorMessage>()
        )

    val register: CallDescription<BulkRequest<ProviderRegisteredResource<Spec>>, BulkResponse<FindByStringId>,
        CommonErrorMessage>
        get() = call(
            name = "register",
            handler = {
                httpUpdate(
                    BulkRequest.serializer(ProviderRegisteredResource.serializer(typeInfo.specSerializer)),
                    baseContext,
                    "register",
                    roles = Roles.PROVIDER
                )
            },
            requestType = BulkRequest.serializer(ProviderRegisteredResource.serializer(typeInfo.specSerializer)),
            successType = serializer(),
            errorType = serializer(),
            requestClass = typeOf<BulkRequest<Spec>>(),
            successClass = typeOf<BulkResponse<FindByStringId>>(),
            errorClass = typeOf<CommonErrorMessage>()
        )
}
