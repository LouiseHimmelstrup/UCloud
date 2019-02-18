package dk.sdu.cloud.calls.server

import dk.sdu.cloud.calls.AttributeKey
import dk.sdu.cloud.calls.CallDescription
import dk.sdu.cloud.service.Loggable
import dk.sdu.cloud.calls.RPCException
import io.ktor.application.call
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.request.header
import java.util.*

class JobIdInterceptor(private val requireJobId: Boolean) {
    fun register(server: RpcServer) {
        server.attachFilter(object : IngoingCallFilter.BeforeParsing() {
            override fun canUseContext(ctx: IngoingCall): Boolean = true

            override fun run(context: IngoingCall, call: CallDescription<*, *, *>) {
                val readJobId = readJobId(context)
                val readCausedBy = readCausedBy(context)

                when {
                    readJobId != null -> context.jobId = readJobId

                    requireJobId -> {
                        log.warn("Missing Job ID (required)")
                        throw RPCException.fromStatusCode(HttpStatusCode.InternalServerError)
                    }

                    else -> context.jobId = "MISSING-${UUID.randomUUID()}"
                }

                if (readCausedBy != null) context.causedBy = readCausedBy
            }
        })
    }

    private fun readJobId(context: IngoingCall): String? {
        return when (context) {
            is HttpCall -> context.call.request.header(HttpHeaders.JobId)
            else -> throw IllegalStateException("Unable to read job id for context: $context")
        }
    }

    private fun readCausedBy(context: IngoingCall): String? {
        return when (context) {
            is HttpCall -> context.call.request.header(HttpHeaders.CausedBy)
            else -> throw IllegalStateException("Unable to read caused by for context: $context")
        }
    }

    companion object : Loggable {
        override val log = logger()

        internal val jobIdKey = AttributeKey<String>("job-id")
        internal val causedByKey = AttributeKey<String>("caused-by")
    }
}

val HttpHeaders.JobId: String
    get() = "Job-Id"

val HttpHeaders.CausedBy: String
    get() = "Caused-By"

var IngoingCall.jobId: String
    get() = attributes[JobIdInterceptor.jobIdKey]
    set(value) {
        attributes[JobIdInterceptor.jobIdKey] = value
    }

var IngoingCall.causedBy: String?
    get() = attributes.getOrNull(JobIdInterceptor.causedByKey)
    set(value) {
        if (value != null) attributes[JobIdInterceptor.causedByKey] = value
        else {
            attributes.remove(JobIdInterceptor.causedByKey)
        }
    }
