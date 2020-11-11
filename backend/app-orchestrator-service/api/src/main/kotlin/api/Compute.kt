package dk.sdu.cloud.app.orchestrator.api

import dk.sdu.cloud.CommonErrorMessage
import dk.sdu.cloud.FindByStringId
import dk.sdu.cloud.calls.BulkRequest
import dk.sdu.cloud.calls.CallDescriptionContainer
import dk.sdu.cloud.calls.UCloudApiExperimental
import dk.sdu.cloud.calls.title
import dk.sdu.cloud.calls.*

typealias ComputeCreateRequest = BulkRequest<ComputeCreateRequestItem>
typealias ComputeCreateResponse = Unit
typealias ComputeCreateRequestItem = Job

typealias ComputeDeleteRequest = BulkRequest<ComputeDeleteRequestItem>
typealias ComputeDeleteResponse = Unit
typealias ComputeDeleteRequestItem = FindByStringId

typealias ComputeExtendRequest = BulkRequest<ComputeExtendRequestItem>
typealias ComputeExtendResponse = Unit
typealias ComputeExtendRequestItem = JobsExtendRequest

typealias ComputeSuspendRequest = BulkRequest<ComputeSuspendRequestItem>
typealias ComputeSuspendResponse = Unit
typealias ComputeSuspendRequestItem = FindByStringId

typealias ComputeVerifyRequest = BulkRequest<ComputeVerifyRequestItem>
typealias ComputeVerifyResponse = Unit
typealias ComputeVerifyRequestItem = Job

@UCloudApiExperimental(UCloudApiExperimental.Level.ALPHA)
abstract class Compute(namespace: String) : CallDescriptionContainer("jobs.compute.$namespace") {
    val baseContext = "/api/jobs/compute/$namespace"

    init {
        title = "Compute backend ($namespace)"
    }

    val create = call<ComputeCreateRequest, ComputeCreateResponse, CommonErrorMessage>("create") {
        httpCreate(baseContext)

        documentation {
            summary = "Start a compute job"
        }
    }

    val delete = call<ComputeDeleteRequest, ComputeDeleteResponse, CommonErrorMessage>("delete") {
        httpDelete(baseContext)

        documentation {
            summary = "Request job cancellation and destruction"
        }
    }

    val extend = call<ComputeExtendRequest, ComputeExtendResponse, CommonErrorMessage>("extend") {
        httpUpdate(baseContext, "extend")

        documentation {
            summary = "Extend the duration of a job"
        }
    }

    val suspend = call<ComputeSuspendRequest, ComputeSuspendResponse, CommonErrorMessage>("suspend") {
        httpUpdate(baseContext, "suspend")

        documentation {
            summary = "Suspend a job"
        }
    }

    val verify = call<ComputeVerifyRequest, ComputeVerifyResponse, CommonErrorMessage>("verify") {
        httpVerify(baseContext)

        documentation {
            summary = "Verify UCloud data is synchronized with provider"
            description = """
                This call is periodically executed by UCloud against all active providers. It is the job of the
                provider to ensure that the jobs listed in the request are in its local database. If some of the
                jobs are not in the provider's database then this should be treated as a job which is no longer valid.
                The compute backend should trigger normal cleanup code and notify UCloud about the job's termination.
                
                The backend should _not_ attempt to start the job.
            """.trimIndent()
        }
    }
}
