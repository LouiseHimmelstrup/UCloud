package dk.sdu.cloud.service

import dk.sdu.cloud.micro.Micro
import dk.sdu.cloud.micro.ServerFeature
import dk.sdu.cloud.micro.eventStreamService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

interface BaseServer {
    fun start()
    fun stop()
}

private const val STOP_TIMEOUT = 30L

/**
 * An interface for a normal servers bootstrapped by [Micro].
 *
 * It provides utility functions for starting and stopping services. Services can be started with [startServices] and
 * stopped with [stopServices]. The underlying services that are stopped depend on which additional services are
 * required.
 */
interface CommonServer : BaseServer, Loggable {
    val micro: Micro

    override fun stop() {
        stopServices()
    }
}

val CommonServer.isRunning: Boolean
    get() {
        val serverFeature = micro.featureOrNull(ServerFeature)
        val isRpcServerRunning: Boolean? = serverFeature?.server?.isRunning

        return isRpcServerRunning != false
    }

fun CommonServer.startServices(wait: Boolean = true) = runBlocking {
    launch(Dispatchers.Default) {
        log.info("Starting Event Stream Services")
        try {
            micro.eventStreamService.start()
            log.info("Done?")
        } catch (ex: Exception) {
            log.error("Caught fatal exception in Event Stream Services")
            log.error(ex.stackTraceToString())
            stopServices()
        }
    }

    val serverFeature = micro.featureOrNull(ServerFeature)
    if (serverFeature != null) {
        launch {
            log.info("Starting RPC server...")
            serverFeature.server.start()
            log.info("RPC server started!")
        }
    }

    for (i in 1..300) {
        if (isRunning) break
        delay(100)
    }

    if (wait) {
        while (isRunning) {
            runCatching { Thread.sleep(1_000) }
        }

        // We are catching exceptions silently since we might already have called stop().
        runCatching { stop() }
    }
}

fun CommonServer.stopServices() {
    val serverFeature = micro.featureOrNull(ServerFeature)
    if (serverFeature != null) {
        log.info("Stopping RPC server")
        serverFeature.server.stop()
    }

    runBlocking {
        micro.eventStreamService.stop()
    }
}

fun EventConsumer<*>.installShutdownHandler(server: CommonServer) {
    with(server) {
        onExceptionCaught { ex ->
            log.warn("Caught fatal exception in event consumer!")
            log.warn(ex.stackTraceToString())
            server.stop()
        }
    }
}
