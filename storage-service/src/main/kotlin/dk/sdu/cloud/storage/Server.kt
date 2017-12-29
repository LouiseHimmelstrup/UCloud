package dk.sdu.cloud.storage

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.zafarkhaja.semver.Version
import dk.sdu.cloud.auth.api.AuthStreams
import dk.sdu.cloud.auth.api.RefreshingJWTAuthenticator
import dk.sdu.cloud.service.*
import dk.sdu.cloud.storage.api.StorageServiceDescription
import dk.sdu.cloud.storage.ext.irods.IRodsConnectionInformation
import dk.sdu.cloud.storage.ext.irods.IRodsStorageConnectionFactory
import dk.sdu.cloud.storage.processor.UserProcessor
import kotlinx.coroutines.experimental.runBlocking
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.irods.jargon.core.connection.AuthScheme
import org.irods.jargon.core.connection.ClientServerNegotiationPolicy
import org.slf4j.LoggerFactory
import java.io.File

/*
 * This file starts the Storage model. This will start up both the REST service and the Kafka consumer.
 * It might make sense to split these, but currently it seems that it actually makes quite a bit of sense to keep
 * these together. It will also be simpler, so we should do this for now.
 */

data class Configuration(
        val storage: StorageConfiguration,
        val service: ServiceConfiguration,
        val kafka: KafkaConfiguration,
        val zookeeper: ZooKeeperHostInfo,
        val tus: TusConfiguration?
) {
    companion object {
        private val mapper = jacksonObjectMapper()

        fun parseFile(file: File) = mapper.readValue<Configuration>(file)
    }
}

data class StorageConfiguration(
        val host: String,
        val port: Int,
        val zone: String,
        val resource: String,
        val authScheme: String?,
        val sslPolicy: String?
)

data class ServiceConfiguration(val hostname: String, val port: Int, val refreshToken: String)
data class KafkaConfiguration(val servers: List<String>)

data class TusConfiguration(
        val icatJdbcUrl: String,
        val icatUser: String,
        val icatPassword: String
)

fun main(args: Array<String>) {
    val log = LoggerFactory.getLogger("Server")
    log.info("Starting storage service")

    val filePath = File(if (args.size == 1) args[0] else "/etc/storage/conf.json")
    log.info("Reading configuration file from: ${filePath.absolutePath}")
    if (!filePath.exists()) {
        log.error("Could not find log file!")
        System.exit(1)
        return
    }

    val configuration = Configuration.parseFile(filePath)

    val definition = ServiceDefinition(
            StorageServiceDescription.name,
            Version.valueOf(StorageServiceDescription.version)
    )
    val instance = ServiceInstance(definition, configuration.service.hostname, configuration.service.port)

    val (zk, node) = runBlocking {
        val zk = ZooKeeperConnection(listOf(configuration.zookeeper)).connect()
        val node = zk.registerService(instance)
        Pair(zk, node)
    }

    val storageService = with(configuration.storage) {
        IRodsStorageConnectionFactory(
                IRodsConnectionInformation(
                        host = host,
                        port = port,
                        zone = zone,
                        storageResource = resource,
                        authScheme = if (authScheme != null) AuthScheme.valueOf(authScheme) else AuthScheme.STANDARD,
                        sslNegotiationPolicy =
                        if (sslPolicy != null)
                            ClientServerNegotiationPolicy.SslNegotiationPolicy.valueOf(sslPolicy)
                        else
                            ClientServerNegotiationPolicy.SslNegotiationPolicy.CS_NEG_REFUSE
                )
        )
    }

    val cloud = RefreshingJWTAuthenticator(DirectServiceClient(zk), configuration.service.refreshToken)
    val adminAccount = run {
        val currentAccessToken = cloud.retrieveTokenRefreshIfNeeded()
        println(currentAccessToken)
        storageService.createForAccount("_storage", currentAccessToken).orThrow()
    }

    val builder = StreamsBuilder()
    UserProcessor(builder.stream(AuthStreams.UserUpdateStream), adminAccount).init()
    val kafkaStreams = KafkaStreams(builder.build(), KafkaUtil.retrieveKafkaStreamsConfiguration(
            configuration.kafka.servers,
            StorageServiceDescription.name,
            configuration.service.hostname,
            configuration.service.port
    ))

    kafkaStreams.start()
    // TODO Catch exceptions in Kafka
    // TODO We need to be better at crashing the entire thing on critical errors
    // TODO Need to close admin connection when server stops

    val restServer = StorageRestServer(configuration, storageService).create()
    // Start the REST server
    log.info("Starting REST service")
    restServer.start()

    runBlocking { zk.markServiceAsReady(node, instance) }
}

