package dk.sdu.cloud.file.trash

import dk.sdu.cloud.auth.api.RefreshingJWTCloudFeature
import dk.sdu.cloud.file.trash.api.FileTrashServiceDescription
import dk.sdu.cloud.micro.*
import dk.sdu.cloud.micro.Micro
import dk.sdu.cloud.micro.initWithDefaultFeatures
import dk.sdu.cloud.micro.install
import dk.sdu.cloud.micro.runScriptHandler

fun main(args: Array<String>) {
    val micro = Micro().apply {
        initWithDefaultFeatures(FileTrashServiceDescription, args)
        install(RefreshingJWTCloudFeature)
        install(BackgroundScopeFeature)
        install(HealthCheckFeature)
    }

    if (micro.runScriptHandler()) return

    Server(micro).start()
}
