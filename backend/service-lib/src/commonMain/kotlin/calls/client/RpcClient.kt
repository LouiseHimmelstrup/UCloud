package dk.sdu.cloud.calls.client

import dk.sdu.cloud.calls.AttributeContainer
import dk.sdu.cloud.calls.CallDescription
import dk.sdu.cloud.service.Loggable
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.util.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlin.reflect.KClass

interface OutgoingCall {
    val attributes: AttributeContainer
}

interface OutgoingCallCompanion<Ctx : OutgoingCall> {
    val klass: KClass<Ctx>
    val attributes: AttributeContainer
}

interface OutgoingRequestInterceptor<Ctx : OutgoingCall, Companion : OutgoingCallCompanion<Ctx>> {
    val companion: Companion

    suspend fun <R : Any, S : Any, E : Any> prepareCall(
        call: CallDescription<R, S, E>,
        request: R
    ): Ctx

    suspend fun <R : Any, S : Any, E : Any> finalizeCall(
        call: CallDescription<R, S, E>,
        request: R,
        ctx: Ctx
    ): IngoingCallResponse<S, E>
}

interface OutgoingContextFilter {
    fun canUseContext(ctx: OutgoingCall): Boolean
}

sealed class OutgoingCallFilter : OutgoingContextFilter {
    abstract class BeforeCall : OutgoingCallFilter() {
        abstract suspend fun run(context: OutgoingCall, callDescription: CallDescription<*, *, *>)
    }
}

class RpcClient {
    private val requestInterceptors = HashMap<OutgoingCallCompanion<*>, OutgoingRequestInterceptor<*, *>>()
    private val callFilters = ArrayList<OutgoingCallFilter>()

    fun attachFilter(filter: OutgoingCallFilter) {
        log.trace("Attaching filter: $filter")
        callFilters.add(filter)
    }

    fun <Ctx : OutgoingCall, Companion : OutgoingCallCompanion<Ctx>> attachRequestInterceptor(
        interceptor: OutgoingRequestInterceptor<Ctx, Companion>
    ) {
        log.trace("Attaching interceptor for ${interceptor.companion}: $interceptor")
        requestInterceptors[interceptor.companion] = interceptor
    }

    suspend fun <R : Any, S : Any, E : Any, Ctx : OutgoingCall, Companion : OutgoingCallCompanion<Ctx>> call(
        callDescription: CallDescription<R, S, E>,
        request: R,
        backend: Companion,
        beforeHook: (suspend (Ctx) -> Unit)? = null,
        afterHook: (suspend (Ctx) -> Unit)? = null
    ): IngoingCallResponse<S, E> {
        val interceptor =
            requestInterceptors[backend] ?: throw IllegalStateException("No handler exists for this backend: $backend")

        @Suppress("UNCHECKED_CAST")
        interceptor as OutgoingRequestInterceptor<Ctx, Companion>

        val ctx = interceptor.prepareCall(callDescription, request)
        beforeHook?.invoke(ctx)

        callFilters.filterIsInstance<OutgoingCallFilter.BeforeCall>().forEach {
            if (it.canUseContext(ctx)) {
                it.run(ctx, callDescription)
            }
        }

        afterHook?.invoke(ctx)

        return interceptor.finalizeCall(callDescription, request, ctx)
    }

    companion object : Loggable {
        override val log = logger()
    }
}

suspend fun <R : Any, S : Any, E : Any> CallDescription<R, S, E>.call(
    request: R,
    clientAndBackend: ClientAndBackend
): IngoingCallResponse<S, E> {
    @Suppress("UNCHECKED_CAST")
    return clientAndBackend.client.call(
        this,
        request,
        clientAndBackend.backend as OutgoingCallCompanion<OutgoingCall>
    )
}

data class ClientAndBackend(
    val client: RpcClient,
    val backend: OutgoingCallCompanion<*>
)

data class AuthenticatedClient(
    val client: RpcClient,
    val backend: OutgoingCallCompanion<*>,
    val afterHook: (suspend (OutgoingCall) -> Unit)? = null,
    val authenticator: suspend (OutgoingCall) -> Unit
) {
    @Deprecated("replaced with backend", ReplaceWith("backend"))
    val companion: OutgoingCallCompanion<*> get() = backend

    companion object {
        fun <Ctx : OutgoingCall, Companion : OutgoingCallCompanion<Ctx>> create(
            client: RpcClient,
            companion: Companion,
            authenticator: suspend (Ctx) -> Unit,
            afterHook: (suspend (Ctx) -> Unit)? = null
        ): AuthenticatedClient {
            return AuthenticatedClient(
                client,
                companion,
                authenticator = {
                    @Suppress("UNCHECKED_CAST")
                    authenticator(it as Ctx)
                },
                afterHook = {
                    @Suppress("UNCHECKED_CAST")
                    afterHook?.invoke(it as Ctx)
                }
            )
        }
    }
}

fun AuthenticatedClient.withHooks(
    beforeHook: suspend (OutgoingCall) -> Unit = {},
    afterHooks: suspend (OutgoingCall) -> Unit = {}
): AuthenticatedClient {
    return AuthenticatedClient(
        client,
        backend,
        authenticator = {
            this.authenticator(it)
            beforeHook(it)
        },
        afterHook = {
            this.afterHook?.invoke(it)
            afterHooks(it)
        }
    )
}

fun AuthenticatedClient.withHttpBody(
    contentType: ContentType,
    contentLength: Long?,
    channel: ByteReadChannel
): AuthenticatedClient {
    return withHooks(
        beforeHook = {
            val call = (it as OutgoingHttpCall)
            call.builder.body = object : OutgoingContent.ReadChannelContent() {
                override val contentType: ContentType = contentType
                override val contentLength: Long? = contentLength
                @OptIn(ExperimentalIoApi::class, KtorExperimentalAPI::class)
                override fun readFrom(): ByteReadChannel = channel
            }
        }
    )
}

fun AuthenticatedClient.withHttpBody(
    text: String,
    contentType: ContentType = ContentType.Text.Plain
): AuthenticatedClient {
    val encoded = text.encodeToByteArray()
    return withHttpBody(contentType, encoded.size.toLong(), ByteReadChannel(encoded))
}

fun AuthenticatedClient.withFixedHost(hostInfo: HostInfo): AuthenticatedClient {
    return AuthenticatedClient(
        client,
        backend,
        authenticator = {
            authenticator(it)
        },
        afterHook = {
            it.attributes.outgoingTargetHost = hostInfo
        }
    )
}

fun AuthenticatedClient.withProject(project: String): AuthenticatedClient {
    return AuthenticatedClient(
        client,
        backend,
        authenticator = {
            authenticator(it)
            it.project = project
        },
        afterHook = {
            afterHook?.invoke(it)
        }
    )
}

fun AuthenticatedClient.withoutAuthentication(): ClientAndBackend = ClientAndBackend(client, backend)

suspend fun <R : Any, S : Any, E : Any> CallDescription<R, S, E>.call(
    request: R,
    authenticatedBackend: AuthenticatedClient
): IngoingCallResponse<S, E> {
    @Suppress("UNCHECKED_CAST")
    return authenticatedBackend.client.call(
        this,
        request,
        authenticatedBackend.backend as OutgoingCallCompanion<OutgoingCall>,
        authenticatedBackend.authenticator,
        authenticatedBackend.afterHook
    )
}
