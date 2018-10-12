package dk.sdu.cloud.zenodo

import dk.sdu.cloud.auth.api.RefreshingJWTCloudFeature
import dk.sdu.cloud.auth.api.refreshingJwtCloud
import dk.sdu.cloud.service.HibernateFeature
import dk.sdu.cloud.service.Micro
import dk.sdu.cloud.service.configuration
import dk.sdu.cloud.service.hibernateDatabase
import dk.sdu.cloud.service.initWithDefaultFeatures
import dk.sdu.cloud.service.install
import dk.sdu.cloud.service.kafka
import dk.sdu.cloud.service.runScriptHandler
import dk.sdu.cloud.service.serverProvider
import dk.sdu.cloud.service.serviceInstance
import dk.sdu.cloud.zenodo.api.ZenodoServiceDescription

data class Configuration(
    val zenodo: ZenodoAPIConfiguration,
    val production: Boolean
)

data class ZenodoAPIConfiguration(
    val clientId: String,
    val clientSecret: String
) {
    override fun toString(): String {
        return "ZenodoAPIConfiguration()"
    }
}

fun main(args: Array<String>) {
    val micro = Micro().apply {
        initWithDefaultFeatures(ZenodoServiceDescription, args)
        install(HibernateFeature)
        install(RefreshingJWTCloudFeature)
    }

    if (micro.runScriptHandler()) return

    val configuration = micro.configuration.requestChunkAt<Configuration>("service")

    Server(
        micro.hibernateDatabase,
        micro.refreshingJwtCloud,
        micro.kafka,
        configuration,
        micro.serverProvider,
        micro.serviceInstance
    ).start()
}
