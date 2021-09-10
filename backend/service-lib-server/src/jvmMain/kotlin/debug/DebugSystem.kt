package dk.sdu.cloud.debug

import calls.server.RpcCoroutineContext
import dk.sdu.cloud.AccessRight
import dk.sdu.cloud.calls.*
import dk.sdu.cloud.CommonErrorMessage
import dk.sdu.cloud.Roles
import dk.sdu.cloud.SecurityPrincipal
import dk.sdu.cloud.ServiceDescription
import dk.sdu.cloud.calls.CallDescriptionContainer
import dk.sdu.cloud.calls.client.AtomicInteger
import dk.sdu.cloud.calls.client.IngoingCallResponse
import dk.sdu.cloud.calls.client.OutgoingCall
import dk.sdu.cloud.calls.client.OutgoingCallFilter
import dk.sdu.cloud.calls.client.outgoingTargetHost
import dk.sdu.cloud.calls.server.*
import dk.sdu.cloud.defaultMapper
import dk.sdu.cloud.micro.*
import dk.sdu.cloud.service.Time
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.coroutines.coroutineContext

@Serializable
sealed class DebugContext {
    abstract val id: String
    abstract val parent: String?

    @SerialName("server")
    @Serializable
    data class Server(override val id: String, override val parent: String? = null) : DebugContext()

    @SerialName("client")
    @Serializable
    data class Client(override val id: String, override val parent: String? = null) : DebugContext()

    @SerialName("job")
    @Serializable
    data class Job(override val id: String, override val parent: String? = null) : DebugContext()
}

@Serializable
sealed class DebugMessage {
    abstract val context: DebugContext
    abstract val timestamp: Long
    abstract val principal: SecurityPrincipal?
    abstract val importance: MessageImportance
    abstract val messageType: MessageType
    abstract val id: Int

    @SerialName("client_request")
    @Serializable
    data class ClientRequest(
        override val context: DebugContext,
        override val timestamp: Long,
        override val principal: SecurityPrincipal?,
        override val importance: MessageImportance,
        val call: String?,
        val payload: JsonElement?,
        val resolvedHost: String,
    ) : DebugMessage() {
        override val messageType = MessageType.CLIENT
        override val id = idGenerator.getAndIncrement()
    }

    @SerialName("client_response")
    @Serializable
    data class ClientResponse(
        override val context: DebugContext,
        override val timestamp: Long,
        override val principal: SecurityPrincipal?,
        override val importance: MessageImportance,
        val call: String?,
        val response: JsonElement?,
        val responseCode: Int
    ) : DebugMessage() {
        override val messageType = MessageType.CLIENT
        override val id = idGenerator.getAndIncrement()
    }

    @SerialName("server_request")
    @Serializable
    data class ServerRequest(
        override val context: DebugContext,
        override val timestamp: Long,
        override val principal: SecurityPrincipal?,
        override val importance: MessageImportance,
        val call: String?,
        val payload: JsonElement?,
    ) : DebugMessage() {
        override val messageType = MessageType.SERVER
        override val id = idGenerator.getAndIncrement()
    }

    @SerialName("server_response")
    @Serializable
    data class ServerResponse(
        override val context: DebugContext,
        override val timestamp: Long,
        override val principal: SecurityPrincipal?,
        override val importance: MessageImportance,
        val call: String?,
        val response: JsonElement?,
        val responseCode: Int
    ) : DebugMessage() {
        override val messageType = MessageType.SERVER
        override val id = idGenerator.getAndIncrement()
    }

    @SerialName("database_connection")
    @Serializable
    data class DatabaseConnection(
        override val context: DebugContext,
        val isOpen: Boolean,
        override val timestamp: Long = Time.now(),
        override val principal: SecurityPrincipal? = null,
        override val importance: MessageImportance = MessageImportance.TELL_ME_EVERYTHING,
    ) : DebugMessage() {
        override val messageType = MessageType.DATABASE
        override val id = idGenerator.getAndIncrement()
    }

    enum class DBTransactionEvent {
        OPEN,
        COMMIT,
        ROLLBACK
    }

    @SerialName("database_transaction")
    @Serializable
    data class DatabaseTransaction(
        override val context: DebugContext,
        val event: DBTransactionEvent,
        override val timestamp: Long = Time.now(),
        override val principal: SecurityPrincipal? = null,
        override val importance: MessageImportance = MessageImportance.TELL_ME_EVERYTHING,
    ) : DebugMessage() {
        override val messageType = MessageType.DATABASE
        override val id = idGenerator.getAndIncrement()
    }

    @SerialName("database_query")
    @Serializable
    data class DatabaseQuery(
        override val context: DebugContext,
        val query: String,
        val parameters: JsonObject,
        override val importance: MessageImportance = MessageImportance.THIS_IS_NORMAL,
        override val timestamp: Long = Time.now(),
        override val principal: SecurityPrincipal? = null,
    ) : DebugMessage() {
        override val messageType = MessageType.DATABASE
        override val id = idGenerator.getAndIncrement()
    }

    @SerialName("database_response")
    @Serializable
    data class DatabaseResponse(
        override val context: DebugContext,
        override val importance: MessageImportance = MessageImportance.IMPLEMENTATION_DETAIL,
        override val timestamp: Long = Time.now(),
        override val principal: SecurityPrincipal? = null,
    ) : DebugMessage() {
        override val messageType = MessageType.DATABASE
        override val id = idGenerator.getAndIncrement()
    }

    companion object {
        val idGenerator = AtomicInteger(0)
    }
}

enum class MessageImportance {
    /**
     * You (the developer/operator) only want to see this message if something is badly misbehaving, and you are
     * desperate for clues. It should primarily contain the insignificant details.
     */
    TELL_ME_EVERYTHING,

    /**
     * You (the developer/operator) want to see this message if something is mildly misbehaving. It should contain the
     * most important implementation details.
     */
    IMPLEMENTATION_DETAIL,

    /**
     * Indicates that an ordinary event has occurred. Most RPCs, which aren't chatty, fall into this category.
     */
    THIS_IS_NORMAL,

    /**
     * Indicates that something might be wrong, but not for certain. A developer/operator might want to see this, but
     * it is not critical.
     */
    THIS_IS_ODD,

    /**
     * A clear message that something is wrong. A developer/operator want to see this as soon as possible, but it can
     * probably wait until the next morning.
     */
    THIS_IS_WRONG,

    /**
     * This should never happen. This event needs to be investigated immediately.
     *
     * Only pick this category if you feel comfortable waking up your co-workers in the middle of the night to tell
     * them about this.
     */
    THIS_IS_DANGEROUS
}

@Serializable
sealed class DebugListenRequest {
    @SerialName("init")
    @Serializable
    object Init : DebugListenRequest()

    @SerialName("context_filter")
    @Serializable
    class SetContextFilter(
        val ids: List<String>?,
        val minimumLevel: MessageImportance? = null,
        val types: List<MessageType>? = null,
        val query: String? = null,
    ) : DebugListenRequest()

    @SerialName("clear")
    @Serializable
    object Clear : DebugListenRequest()
}

@Serializable
sealed class DebugSystemListenResponse {
    @SerialName("append")
    @Serializable
    data class Append(val messages: List<DebugMessage>) : DebugSystemListenResponse()

    @SerialName("clear")
    @Serializable
    object Clear : DebugSystemListenResponse()

    @SerialName("acknowledge")
    @Serializable
    object Acknowledge : DebugSystemListenResponse()
}

object DebugApi : CallDescriptionContainer("debug") {
    val listen = call<DebugListenRequest, DebugSystemListenResponse, CommonErrorMessage>("listen") {
        auth {
            roles = Roles.PRIVILEGED
            access = AccessRight.READ
        }

        websocket("/api/debug")
    }
}

enum class MessageType {
    SERVER,
    DATABASE,
    CLIENT
}

class DebugSystem : MicroFeature {
    private var developmentMode: Boolean = false
    private val mutex = Mutex()
    private val contextGraph = HashMap<String, ArrayList<String>>()
    private val reverseContext = HashMap<String, ArrayList<String>>()
    private val messages = ArrayList<DebugMessage>()

    override fun init(ctx: Micro, serviceDescription: ServiceDescription, cliArgs: List<String>) {
        developmentMode = ctx.developmentModeEnabled
        configure(ctx.server)

        val key = AttributeKey<String>("debug-id")
        ctx.client.attachFilter(object : OutgoingCallFilter.BeforeCall() {
            private val baseKey = "Client-"
            private val idGenerator = AtomicInteger(0)
            override fun canUseContext(ctx: OutgoingCall): Boolean = true

            override suspend fun run(context: OutgoingCall, callDescription: CallDescription<*, *, *>, request: Any?) {
                @Suppress("UNCHECKED_CAST") val call = callDescription as CallDescription<Any, Any, Any>
                val id = baseKey + idGenerator.getAndIncrement()
                context.attributes[key] = id
                sendMessage(
                    DebugMessage.ClientRequest(
                        DebugContext.Client(
                            id,
                            rpcContext()?.call?.jobIdOrNull
                        ),
                        Time.now(),
                        null,
                        MessageImportance.THIS_IS_NORMAL,
                        callDescription.fullName,
                        if (request == null) JsonNull
                        else defaultMapper.encodeToJsonElement(call.requestType, request),
                        context.attributes.outgoingTargetHost.toString(),
                    )
                )
            }
        })

        ctx.client.attachFilter(object : OutgoingCallFilter.AfterCall() {
            override fun canUseContext(ctx: OutgoingCall): Boolean = true

            override suspend fun run(
                context: OutgoingCall,
                callDescription: CallDescription<*, *, *>,
                response: IngoingCallResponse<*, *>
            ) {
                val id = context.attributes[key]
                @Suppress("UNCHECKED_CAST") val call = callDescription as CallDescription<Any, Any, Any>
                sendMessage(
                    DebugMessage.ClientResponse(
                        DebugContext.Client(
                            id,
                            rpcContext()?.call?.jobIdOrNull
                        ),
                        Time.now(),
                        null,
                        MessageImportance.THIS_IS_NORMAL,
                        callDescription.fullName,
                        when (response) {
                            is IngoingCallResponse.Error -> {
                                if (response.error == null) {
                                    JsonNull
                                } else {
                                    defaultMapper.encodeToJsonElement(call.errorType, response.error!!)
                                }
                            }
                            is IngoingCallResponse.Ok -> {
                                defaultMapper.encodeToJsonElement(call.successType, response.result)
                            }
                        },
                        response.statusCode.value
                    )
                )
            }
        })

        ctx.server.attachFilter(object : IngoingCallFilter.AfterParsing() {
            override fun canUseContext(ctx: IngoingCall): Boolean = true
            override suspend fun run(context: IngoingCall, call: CallDescription<*, *, *>, request: Any) {
                if (call.fullName == "debug.listen") return
                @Suppress("UNCHECKED_CAST")
                call as CallDescription<Any, Any, Any>

                sendMessage(
                    DebugMessage.ServerRequest(
                        DebugContext.Server(context.jobIdOrNull ?: "Unknown"),
                        Time.now(),
                        context.securityPrincipalOrNull,
                        MessageImportance.IMPLEMENTATION_DETAIL,
                        call.fullName,
                        defaultMapper.encodeToJsonElement(call.requestType, request),
                    )
                )
            }
        })

        ctx.server.attachFilter(object : IngoingCallFilter.AfterResponse() {
            override fun canUseContext(ctx: IngoingCall): Boolean = true
            override suspend fun run(
                context: IngoingCall,
                call: CallDescription<*, *, *>,
                request: Any?,
                result: OutgoingCallResponse<*, *>
            ) {
                if (call.fullName == "debug.listen") return
                @Suppress("UNCHECKED_CAST")
                call as CallDescription<Any, Any, Any>

                sendMessage(
                    DebugMessage.ServerResponse(
                        DebugContext.Server(context.jobIdOrNull ?: "Unknown"),
                        Time.now(),
                        rpcContext()?.call?.securityPrincipalOrNull,
                        MessageImportance.THIS_IS_NORMAL,
                        call.fullName,
                        when (val res = result) {
                            is OutgoingCallResponse.Ok -> {
                                defaultMapper.encodeToJsonElement(call.successType, res.result)
                            }

                            is OutgoingCallResponse.Error -> {
                                if (res.error != null) {
                                    defaultMapper.encodeToJsonElement(call.errorType, res.error)
                                } else {
                                    JsonNull
                                }
                            }

                            else -> JsonNull
                        },
                        result.statusCode.value
                    )
                )
            }
        })
    }

    data class DebugSession(
        val streamId: String,
        val session: WSSession,
        var query: String? = null,
        var filterTypes: List<MessageType> = listOf(MessageType.SERVER),
        var minimumLevel: MessageImportance = MessageImportance.THIS_IS_NORMAL,
        var interestedIn: List<String>? = null,
    )

    private val sessions = ArrayList<DebugSession>()

    private fun configure(server: RpcServer) {
        if (!developmentMode) return

        val key = AttributeKey<DebugSession>("debug-session")
        server.implement(DebugApi.listen) {
            withContext<WSCall> {
                when (request) {
                    DebugListenRequest.Init -> {
                        val debugSession = DebugSession(ctx.streamId, ctx.session)
                        ctx.session.attributes[key] = debugSession
                        sessions.add(debugSession)

                        ctx.session.addOnCloseHandler {
                            mutex.withLock {
                                val idx = sessions.indexOfFirst { it.session == ctx.session }
                                if (idx != -1) {
                                    sessions.removeAt(idx)
                                }
                            }
                        }

                        while (coroutineContext.isActive) {
                            delay(500)
                        }

                        okContentAlreadyDelivered()
                    }

                    is DebugListenRequest.SetContextFilter -> {
                        val debugSession = ctx.session.attributes[key]
                        debugSession.interestedIn = request.ids
                        if (request.minimumLevel != null) debugSession.minimumLevel = request.minimumLevel
                        if (request.types != null) debugSession.filterTypes = request.types
                        if (request.query != null) debugSession.query = request.query

                        mutex.withLock {
                            debugSession.session.sendMessage(debugSession.streamId, DebugSystemListenResponse.Clear,
                                DebugSystemListenResponse.serializer())

                            debugSession.session.sendMessage(debugSession.streamId, DebugSystemListenResponse.Append(
                                messages.filter { shouldSendMessage(debugSession, it) }
                            ), DebugSystemListenResponse.serializer())
                        }

                        ok(DebugSystemListenResponse.Acknowledge)
                    }

                    DebugListenRequest.Clear -> {
                        val debugSession = ctx.session.attributes[key]
                        mutex.withLock {
                            messages.clear()
                            debugSession.session.sendMessage(
                                debugSession.streamId, DebugSystemListenResponse.Clear,
                                DebugSystemListenResponse.serializer()
                            )

                        }
                        ok(DebugSystemListenResponse.Acknowledge)
                    }
                }
            }
        }
    }

    private fun shouldSendMessage(session: DebugSession, message: DebugMessage): Boolean {
        val allIds = session.interestedIn?.flatMap { (reverseContext[it] ?: emptyList()) + it }?.toSet()
        if (allIds != null && message.context.id !in allIds) return false
        if (message.importance.ordinal < session.minimumLevel.ordinal) return false
        if (message.messageType !in session.filterTypes) return false
        val query = session.query?.takeIf { it.isNotBlank() }?.lowercase()
        if (query != null) {
            val matchesUsername = (message.principal?.username ?: "")?.lowercase()?.contains(query)
            val callName = when (message) {
                is DebugMessage.ClientRequest -> message.call
                is DebugMessage.ClientResponse -> message.call
                is DebugMessage.ServerRequest -> message.call
                is DebugMessage.ServerResponse -> message.call
                else -> null
            }?.lowercase() ?: ""
            val matchesCall = callName.contains(query)

            if (!matchesUsername && !matchesCall) return false
        }
        return true
    }

    suspend fun sendMessage(message: DebugMessage) {
        if (!developmentMode) {
            // Log the message in some way
        } else {
            mutex.withLock {
                val existingEntry = contextGraph[message.context.id]
                if (existingEntry == null) {
                    val chain = ArrayList<String>()
                    val parent = message.context.parent
                    if (parent != null) {
                        val elements = contextGraph[parent]
                        elements?.forEach {
                            reverseContext[it]?.add(message.context.id)
                        }
                        if (elements != null) chain.addAll(elements)
                    }
                    chain.add(message.context.id)

                    contextGraph[message.context.id] = chain
                    reverseContext[message.context.id] = ArrayList()
                }

                messages.add(message)

                sessions.forEach { session ->
                    if (shouldSendMessage(session, message)) {
                        session.session.sendMessage(
                            session.streamId,
                            DebugSystemListenResponse.Append(listOf(message)),
                            DebugSystemListenResponse.serializer()
                        )
                    }
                }
            }
        }
    }

    companion object : MicroFeatureFactory<DebugSystem, Unit> {
        override fun create(config: Unit): DebugSystem = DebugSystem()
        override val key = MicroAttributeKey<DebugSystem>("debug-system")
    }
}

suspend inline fun rpcContext(): RpcCoroutineContext? = coroutineContext[RpcCoroutineContext]
