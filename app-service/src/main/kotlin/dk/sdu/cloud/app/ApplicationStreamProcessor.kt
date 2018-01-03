package dk.sdu.cloud.app

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dk.sdu.cloud.app.api.AppServiceDescription
import dk.sdu.cloud.app.http.AppController
import dk.sdu.cloud.app.http.JobController
import dk.sdu.cloud.app.http.ToolController
import dk.sdu.cloud.app.processors.SlurmAggregate
import dk.sdu.cloud.app.processors.SlurmProcessor
import dk.sdu.cloud.app.processors.StartProcessor
import dk.sdu.cloud.app.services.*
import dk.sdu.cloud.app.services.ssh.SSHConnectionPool
import dk.sdu.cloud.app.services.ssh.SimpleSSHConfig
import dk.sdu.cloud.service.*
import dk.sdu.cloud.service.KafkaUtil.retrieveKafkaProducerConfiguration
import dk.sdu.cloud.service.KafkaUtil.retrieveKafkaStreamsConfiguration
import dk.sdu.cloud.storage.Error
import dk.sdu.cloud.storage.ext.StorageConnection
import dk.sdu.cloud.storage.ext.StorageConnectionFactory
import dk.sdu.cloud.storage.ext.irods.IRodsConnectionInformation
import dk.sdu.cloud.storage.ext.irods.IRodsStorageConnectionFactory
import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.ApplicationRequest
import io.ktor.request.authorization
import io.ktor.response.respond
import io.ktor.routing.route
import io.ktor.util.AttributeKey
import kotlinx.coroutines.experimental.runBlocking
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.irods.jargon.core.connection.AuthScheme
import org.irods.jargon.core.connection.ClientServerNegotiationPolicy
import org.slf4j.LoggerFactory
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

data class HPCConfig(
        val connection: RawConnectionConfig,
        val ssh: SimpleSSHConfig,
        val storage: StorageConfiguration,
        val rpc: RPCConfiguration
)

data class StorageConfiguration(val host: String, val port: Int, val zone: String)
data class RPCConfiguration(val secretToken: String)

private val storageConnectionKey = AttributeKey<StorageConnection>("StorageSession")
val ApplicationCall.storageConnection get() = attributes[storageConnectionKey]

class ApplicationStreamProcessor(
        private val config: HPCConfig,
        private val storageConnectionFactory: StorageConnectionFactory
) {
    private val log = LoggerFactory.getLogger(ApplicationStreamProcessor::class.java)

    private var initialized = false
    private lateinit var rpcServer: HTTPServer
    private lateinit var streamProcessor: KafkaStreams
    private lateinit var scheduledExecutor: ScheduledExecutorService
    private lateinit var slurmPollAgent: SlurmPollAgent

    fun start() {
        // TODO How do we handle deserialization exceptions???
        // TODO Create some component interfaces
        // TODO This would most likely be a lot better if we could use DI in this
        if (initialized) throw IllegalStateException("Already started!")

        val connConfig = config.connection.processed
        val instance = AppServiceDescription.instance(connConfig)

        val (zk, node) = runBlocking {
            val zk = ZooKeeperConnection(connConfig.zookeeper.servers).connect()
            val node = zk.registerService(instance)

            Pair(zk, node)
        }

        log.info("Init Core Services")
        val streamBuilder = StreamsBuilder()
        val producer = KafkaProducer<String, String>(retrieveKafkaProducerConfiguration(connConfig))
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor()

        log.info("Init Application Services")
        val sshPool = SSHConnectionPool(config.ssh)

        val hpcStore = HPCStore(connConfig.service.hostname, connConfig.service.port, config.rpc)
        val streamService = HPCStreamService(streamBuilder, producer)
        val sbatchGenerator = SBatchGenerator()
        slurmPollAgent = SlurmPollAgent(sshPool, scheduledExecutor, 0L, 15L, TimeUnit.SECONDS)

        log.info("Init Event Processors")
        val slurmProcessor = SlurmProcessor(hpcStore, sshPool, storageConnectionFactory, slurmPollAgent, streamService)
        val slurmAggregate = SlurmAggregate(streamService, slurmPollAgent)
        val startProcessor = StartProcessor(storageConnectionFactory, sbatchGenerator, sshPool, config.ssh.user,
                streamService)

        log.info("Starting Event Processors")
        slurmProcessor.init()
        slurmAggregate.init()
        startProcessor.init()

        log.info("Starting Application Services")
        slurmPollAgent.start()

        log.info("Starting Core Services")
        streamProcessor = KafkaStreams(streamBuilder.build(), retrieveKafkaStreamsConfiguration(connConfig))
        streamProcessor.start()
        streamProcessor.addShutdownHook()

        log.info("Starting HTTP Server")
        rpcServer = HTTPServer(connConfig.service.hostname, connConfig.service.port)
        rpcServer.start {
            hpcStore.init(streamProcessor, this)

            route("api") {
                route("hpc") {
                    fun ApplicationRequest.bearer(): String? {
                        val auth = authorization() ?: return null
                        if (!auth.startsWith("Bearer ")) return null
                        return auth.substringAfter("Bearer ")
                    }

                    intercept(ApplicationCallPipeline.Infrastructure) {
                        val rawToken = call.request.bearer()
                        val token = rawToken?.let { TokenValidation.validateOrNull(it) } ?:
                                return@intercept run {
                                    call.respond(HttpStatusCode.Unauthorized)
                                    finish()
                                }

                        // TODO We can likely remove this for most paths
                        val connection = storageConnectionFactory.createForAccount(token.subject,
                                token.token).capture() ?: return@intercept run {
                            call.respond(HttpStatusCode.Unauthorized)
                            finish()
                            return@intercept
                        }

                        call.attributes.put(storageConnectionKey, connection)
                    }

                    AppController(ApplicationDAO).configure(this)
                    JobController(hpcStore).configure(this)
                    ToolController(ToolDAO).configure(this)
                }
            }
        }

        log.info("Ready!")
        initialized = true

        runBlocking { zk.markServiceAsReady(node, instance) }
    }

    fun stop() {
        rpcServer.stop()
        slurmPollAgent.stop()
        streamProcessor.close()
        scheduledExecutor.shutdown()
    }

    private fun KafkaStreams.addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(Thread { this.close() })
    }
}

fun <T : Any> Error.Companion.internalError(): Error<T> = Error(500, "Internal error")
fun Exception.stackTraceToString(): String = StringWriter().apply { printStackTrace(PrintWriter(this)) }.toString()

fun main(args: Array<String>) {
    val mapper = jacksonObjectMapper()
    val hpcConfig = mapper.readValue<HPCConfig>(File("hpc_conf.json"))
    hpcConfig.connection.configure(AppServiceDescription, 42200)

    val irodsConnectionFactory = IRodsStorageConnectionFactory(IRodsConnectionInformation(
            host = hpcConfig.storage.host,
            zone = hpcConfig.storage.zone,
            port = hpcConfig.storage.port,
            storageResource = "radosRandomResc",
            sslNegotiationPolicy = ClientServerNegotiationPolicy.SslNegotiationPolicy.CS_NEG_REQUIRE,
            authScheme = AuthScheme.PAM
    ))

    val processor = ApplicationStreamProcessor(hpcConfig, irodsConnectionFactory)
    processor.start()
}

