package dk.sdu.cloud.service.k8

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.selects.select

private val webSocketClient = HttpClient(CIO).config {
    install(io.ktor.client.features.websocket.WebSockets)
    expectSuccess = false
}

class ExecContext(
    val outputs: ReceiveChannel<ExecMessage>,
    val stdin: SendChannel<ByteArray>,
    val resizes: SendChannel<ExecResizeMessage>,
)

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun KubernetesClient.exec(
    resource: KubernetesResourceLocator,
    command: List<String>,
    stdin: Boolean = true,
    tty: Boolean = true,
    stderr: Boolean = true,
    stdout: Boolean = true,
    block: suspend ExecContext.() -> Unit,
) {
    webSocketClient.webSocket(
        request = {
            this.method = HttpMethod.Get
            url(
                buildUrl(
                    resource,
                    mapOf(
                        "stdin" to stdin.toString(),
                        "tty" to tty.toString(),
                        "stdout" to stdout.toString(),
                        "stderr" to stderr.toString(),
                    ),
                    "exec"
                )
            )
            configureRequest(this)
            url(url.fixedClone().let {
                it.copy(
                    parameters = Parameters.build {
                        it.parameters.entries().forEach { (k, values) ->
                            appendAll(k, values)
                        }
                        command.forEach { append("command", it) }
                    },
                    protocol = URLProtocol.WS
                )
            }.toString())
        },

        block = {
            coroutineScope {
                val resizeChannel = Channel<ExecResizeMessage>()
                val ingoingChannel = Channel<ExecMessage>()
                val outgoingChannel = Channel<ByteArray>()

                val resizeJob = launch {
                    while (isActive) {
                        // NOTE(Dan): I have no clue where this is documented.
                        // I found this through a combination of Wireshark and this comment:
                        // https://github.com/fabric8io/kubernetes-client/issues/1374#issuecomment-492884783
                        val nextMessage = resizeChannel.receiveOrNull() ?: break
                        outgoing.send(
                            Frame.Binary(
                                true,
                                byteArrayOf(4) +
                                    """{"Width": ${nextMessage.cols}, "Height": ${nextMessage.rows}}"""
                                        .toByteArray(Charsets.UTF_8)
                            )
                        )
                    }
                }

                val outgoingJob = launch {
                    while (isActive) {
                        val nextMessage = outgoingChannel.receiveOrNull() ?: break
                        outgoing.send(Frame.Binary(true, byteArrayOf(0) + nextMessage))
                    }
                }

                val ingoingJob = launch {
                    while (isActive) {
                        val f = incoming.receiveOrNull() ?: break
                        if (f !is Frame.Binary) continue
                        val stream = ExecStream.allValues.getOrNull(f.buffer.get().toInt()) ?: continue
                        val array = ByteArray(f.buffer.remaining())
                        f.buffer.get(array)
                        ingoingChannel.send(ExecMessage(stream, array))
                    }
                }

                val userJob = launch {
                    ExecContext(ingoingChannel, outgoingChannel, resizeChannel).block()
                }

                select<Unit> {
                    resizeJob.onJoin { runCatching { cancel() } }
                    userJob.onJoin { runCatching { cancel() } }
                    outgoingJob.onJoin { runCatching { cancel() } }
                    ingoingJob.onJoin { runCatching { cancel() } }
                }
            }
        }
    )
}

enum class ExecStream(val id: Int) {
    STDOUT(1),
    STDERR(2),
    INFO(3);

    companion object {
        val allValues = arrayOf<ExecStream?>(null, STDOUT, STDERR, INFO)
    }
}

data class ExecMessage(val stream: ExecStream, val bytes: ByteArray)

data class ExecResizeMessage(val cols: Int, val rows: Int)