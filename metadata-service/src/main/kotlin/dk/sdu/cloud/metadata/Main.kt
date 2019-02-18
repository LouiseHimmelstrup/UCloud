package dk.sdu.cloud.metadata

import dk.sdu.cloud.auth.api.RefreshingJWTCloudFeature
import dk.sdu.cloud.metadata.api.MetadataServiceDescription
import dk.sdu.cloud.micro.HibernateFeature
import dk.sdu.cloud.micro.Micro
import dk.sdu.cloud.micro.configuration
import dk.sdu.cloud.micro.initWithDefaultFeatures
import dk.sdu.cloud.micro.install
import dk.sdu.cloud.micro.runScriptHandler
import java.net.InetAddress
import java.net.UnknownHostException

data class ElasticHostAndPort(
    val host: String,
    val port: Int = 9200
) {
    companion object {
        fun guessDefaults() =
            ElasticHostAndPort(
                host = findValidHostname(listOf("elasticsearch", "localhost"))!!,
                port = 9200
            )

        private fun testHostname(hostname: String): Boolean {
            return try {
                InetAddress.getByName(hostname)
                true
            } catch (ex: UnknownHostException) {
                false
            }
        }

        private fun findValidHostname(hostnames: List<String>): String? {
            return hostnames.find { testHostname(it) }
        }
    }
}

fun main(args: Array<String>) {
    val micro = Micro().apply {
        initWithDefaultFeatures(MetadataServiceDescription, args)
        install(RefreshingJWTCloudFeature)
        install(HibernateFeature)
    }

    if (micro.runScriptHandler()) return

    val configuration = micro.configuration.requestChunkAtOrNull("elastic") ?: ElasticHostAndPort.guessDefaults()

    Server(configuration, micro).start()
}
