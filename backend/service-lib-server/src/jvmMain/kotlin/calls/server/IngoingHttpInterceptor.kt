package dk.sdu.cloud.calls.server

import dk.sdu.cloud.calls.*
import dk.sdu.cloud.debug.*
import dk.sdu.cloud.defaultMapper
import dk.sdu.cloud.logThrowable
import dk.sdu.cloud.micro.Micro
import dk.sdu.cloud.micro.featureOrNull
import dk.sdu.cloud.service.Loggable
import io.ktor.application.*
import io.ktor.content.*
import io.ktor.http.*
import io.ktor.http.HttpMethod
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.util.pipeline.*
import io.ktor.utils.io.errors.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.concurrent.TimeUnit

class IngoingHttpInterceptor(
    private val engine: ApplicationEngine,
    private val rpcServer: RpcServer,
    private val micro: Micro,
) : IngoingRequestInterceptor<HttpCall, HttpCall.Companion> {
    override val companion: HttpCall.Companion = HttpCall

    // NOTE(Dan): This needs to be lazy to make sure the debug system has been initialized
    private val debug by lazy { micro.featureOrNull(DebugSystemFeature) }

    override fun onStop() {
        engine.stop(gracePeriod = 0L, timeout = 30L, timeUnit = TimeUnit.SECONDS)
    }

    override fun addCallListenerForCall(call: CallDescription<*, *, *>) {
        val httpDescription = call.httpOrNull ?: return

        engine.application.routing {
            // toKtorTemplate performs a plain one-to-one mapping of the http/path block semantics to Ktor routing
            // template

            route(httpDescription.path.toPath(), HttpMethod(httpDescription.method.value)) {
                handle {
                    val ctx = HttpCall(this as PipelineContext<Any, ApplicationCall>)
                    val debugCtx = DebugContext.create()
                    withContext(DebugCoroutineContext(debugCtx)) {
                        debug.everythingD("Begun parsing of ${call.fullName}", Unit, debugCtx)
                        try {
                            // Calls the handler provided by 'implement'
                            @Suppress("UNCHECKED_CAST")
                            rpcServer.handleIncomingCall(
                                this@IngoingHttpInterceptor,
                                call,
                                ctx
                            )
                        } catch (ex: IOException) {
                            log.debug("Caught IOException:")
                            log.debug(ex.stackTraceToString())
                            throw RPCException.fromStatusCode(dk.sdu.cloud.calls.HttpStatusCode.BadRequest)
                        }
                    }
                }
            }
        }
    }

    override suspend fun <R : Any> parseRequest(ctx: HttpCall, call: CallDescription<R, *, *>): R {
        try {
            val http = call.http

            when {
                call.requestType == Unit.serializer() -> {
                    @Suppress("UNCHECKED_CAST")
                    return Unit as R
                }

                http.body is HttpBody.BoundToEntireRequest<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    val receiveOrNull = ctx.call.receiveOrNull<String>()?.takeIf { it.isNotEmpty() }
                    return (
                        if (receiveOrNull != null) defaultMapper.decodeFromString(call.requestType, receiveOrNull)
                        else null
                        ) ?: throw RPCException.fromStatusCode(dk.sdu.cloud.calls.HttpStatusCode.BadRequest)
                }

                http.params != null -> {
                    return ParamsParsing(ctx.context, call).decodeSerializableValue(call.requestType)
                }

                http.headers != null -> {
                    return HeaderParsing(ctx.context, call).decodeSerializableValue(call.requestType)
                }

                else -> throw RPCException(
                    "Unable to deserialize request. No source of input!",
                    dk.sdu.cloud.calls.HttpStatusCode.InternalServerError
                )
            }
        } catch (ex: Throwable) {
            when {
                ex.cause is RPCException -> {
                    throw ex.cause!!
                }
                ex is RPCException -> {
                    throw ex
                }
                else -> {
                    log.debug(ex.stackTraceToString())
                    debug.logThrowable("Failed to parse request", ex, MessageImportance.IMPLEMENTATION_DETAIL)
                    throw RPCException("Bad request", dk.sdu.cloud.calls.HttpStatusCode.BadRequest)
                }
            }
        }
    }

    override suspend fun <R : Any, S : Any, E : Any> produceResponse(
        ctx: HttpCall,
        call: CallDescription<R, S, E>,
        callResult: OutgoingCallResponse<S, E>,
    ) {
        ctx.call.response.status(callResult.statusCode)

        when (callResult) {
            is OutgoingCallResponse.Ok -> {
                ctx.call.respond(
                    TextContent(
                        defaultMapper.encodeToString(call.successType, callResult.result),
                        ContentType.Application.Json.withCharset(Charsets.UTF_8)
                    )
                )
            }

            is OutgoingCallResponse.Error -> {
                if (callResult.error == null) {
                    ctx.call.respond(callResult.statusCode)
                } else {
                    ctx.call.respond(
                        TextContent(
                            defaultMapper.encodeToString(call.errorType, callResult.error),
                            ContentType.Application.Json.withCharset(Charsets.UTF_8)
                        )
                    )
                }
            }

            is OutgoingCallResponse.AlreadyDelivered -> return
        }
    }

    companion object : Loggable {
        override val log = logger()
    }
}
