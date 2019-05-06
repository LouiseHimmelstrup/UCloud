package dk.sdu.cloud.app.kubernetes.services

import dk.sdu.cloud.app.api.QueryInternalWebParametersResponse
import dk.sdu.cloud.app.api.VerifiedJob
import dk.sdu.cloud.calls.RPCException
import dk.sdu.cloud.service.Loggable
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.headers
import io.ktor.client.utils.EmptyContent
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.request.header
import io.ktor.request.httpMethod
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.host
import io.ktor.routing.route
import io.ktor.util.pipeline.PipelineContext
import io.ktor.util.toMap
import io.ktor.websocket.webSocket
import kotlinx.coroutines.io.ByteReadChannel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.collections.set

private data class TunnelWithUsageTracking(val tunnel: Tunnel, var lastUsed: Long)

class WebService(
    private val podService: PodService,
    private val prefix: String = "app-",
    private val domain: String = "cloud.sdu.dk"
) {
    private val client = HttpClient(OkHttp)

    private val jobIdToJob = HashMap<String, VerifiedJob>()

    // A bit primitive, should work for most cases.
    private fun String.escapeToRegex(): String = replace(".", "\\.")

    private val openTunnels = HashMap<String, TunnelWithUsageTracking>()
    private val openTunnelsMutex = Mutex()

    fun install(routing: Route): Unit = with(routing) {
        route("{path...}") {
            host(Regex("${prefix.escapeToRegex()}.*\\.${domain.escapeToRegex()}")) {
                webSocket {
                    val path = call.request.path()
                    log.info("Handling websocket at $path")
                    val host = call.request.header(HttpHeaders.Host) ?: ""
                    val id = host.substringAfter(prefix).substringBefore(".")
                    if (!host.startsWith(prefix)) {
                        call.respondText(status = HttpStatusCode.NotFound) { "Not found" }
                        return@webSocket
                    }

                    val tunnel = createTunnel(id)
                    runWSProxy(tunnel, path = path)
                }

                handle {
                    val host = call.request.header(HttpHeaders.Host) ?: ""
                    val id = host.substringAfter(prefix).substringBefore(".")
                    if (!host.startsWith(prefix)) {
                        call.respondText(status = HttpStatusCode.NotFound) { "Not found" }
                        return@handle
                    }

                    val tunnel = createTunnel(id)
                    proxyResponseToClient(proxyToServer(tunnel))
                }
            }
        }

        return@with
    }

    private suspend fun PipelineContext<Unit, ApplicationCall>.proxyToServer(tunnel: Tunnel): HttpClientCall {
        val requestPath = call.parameters.getAll("path")?.joinToString("/") ?: "/"
        val requestQueryParameters = call.request.queryParameters
        val method = call.request.httpMethod
        val requestCookies = HashMap(call.request.cookies.rawCookies).apply {
            // Remove authentication tokens
            remove("refreshToken")
            remove("csrfToken")
        }

        val requestHeaders = call.request.headers.toMap().mapKeys { it.key.toLowerCase() }.toMutableMap().apply {
            remove(HttpHeaders.Referrer.toLowerCase())
            remove(HttpHeaders.ContentLength.toLowerCase())
            remove(HttpHeaders.ContentType.toLowerCase())
            remove(HttpHeaders.TransferEncoding.toLowerCase())
            remove(HttpHeaders.Cookie.toLowerCase())
            remove(HttpHeaders.Upgrade.toLowerCase())
            // TODO Add referer?
        }

        // Cookies: Should not send refreshToken. Should send app specific ones.
        // Referer: Should normalize referer.
        // X-Forwarded?

        val requestContentLength = call.request.header(HttpHeaders.ContentLength)?.toLongOrNull()
        val requestContentType = call.request.header(HttpHeaders.ContentType)?.let {
            runCatching {
                ContentType.parse(it)
            }.getOrNull()
        } ?: ContentType.Application.OctetStream

        // TODO Perhaps a bit too primitive
        val hasRequestBody = requestContentLength != null ||
                call.request.header(HttpHeaders.TransferEncoding) != null

        val requestBody: OutgoingContent = if (!hasRequestBody) {
            EmptyContent
        } else {
            object : OutgoingContent.ReadChannelContent() {
                override val contentLength: Long? = requestContentLength
                override val contentType: ContentType = requestContentType
                override fun readFrom(): ByteReadChannel = call.request.receiveChannel()
            }
        }

        val request = HttpRequestBuilder().apply {
            this.method = method
            this.url {
                protocol = URLProtocol.HTTP
                host = "127.0.0.1"
                port = tunnel.localPort
                encodedPath = requestPath
                parameters.appendAll(requestQueryParameters)
            }

            this.body = requestBody
            this.headers {
                requestHeaders.forEach { (header, values) ->
                    appendAll(header, values)
                }

                append(
                    HttpHeaders.Cookie,
                    requestCookies.entries.joinToString(";") { "${it.key}=${it.value}" }
                )
            }
        }

        return client.execute(request)
    }

    private suspend fun PipelineContext<Unit, ApplicationCall>.proxyResponseToClient(clientCall: HttpClientCall) =
        with(clientCall) {
            val statusCode = response.status
            val responseHeaders = response.headers.toMap().mapKeys { it.key.toLowerCase() }.toMutableMap().apply {
                // TODO
                remove(HttpHeaders.Server.toLowerCase())
                remove(HttpHeaders.ContentLength.toLowerCase())
                remove(HttpHeaders.ContentType.toLowerCase())
                remove(HttpHeaders.TransferEncoding.toLowerCase())
                remove(HttpHeaders.Upgrade.toLowerCase())
            }

            val responseContentLength = response.contentLength()
            val responseContentType = response.contentType()

            val hasResponseBody =
                responseContentLength != null || response.headers[HttpHeaders.TransferEncoding] != null

            val responseBody: OutgoingContent = if (!hasResponseBody) {
                EmptyContent
            } else {
                object : OutgoingContent.ReadChannelContent() {
                    override val contentLength: Long? = responseContentLength
                    override val contentType: ContentType? = responseContentType
                    override fun readFrom(): ByteReadChannel = response.content
                }
            }

            responseHeaders.forEach { (header, values) ->
                values.forEach { value ->
                    call.response.headers.append(header, value)
                }
            }

            call.respond(statusCode, responseBody)
        }

    fun queryParameters(job: VerifiedJob): QueryInternalWebParametersResponse {
        jobIdToJob[job.id] = job
        return QueryInternalWebParametersResponse(
            "$prefix${job.id}.$domain"
        )
    }

    private suspend fun createTunnel(incomingId: String): Tunnel {
        openTunnelsMutex.withLock {
            val jobId = incomingId // Slightly less secure, but should work for prototype
            val existing = openTunnels[jobId]
            if (existing != null) {
                existing.lastUsed = System.currentTimeMillis()
                return existing.tunnel
            } else {
                val job = jobIdToJob[jobId] ?: throw RPCException.fromStatusCode(HttpStatusCode.NotFound)
                val remotePort = job.application.invocation.web?.port ?: 80
                log.info("Creating tunnel to $jobId with remote port $remotePort")
                val newTunnel = podService.createTunnel(jobId, remotePort)
                openTunnels[jobId] = TunnelWithUsageTracking(newTunnel, System.currentTimeMillis())
                return newTunnel
            }
        }
    }

    companion object : Loggable {
        override val log = logger()
    }
}
