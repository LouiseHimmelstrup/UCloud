package dk.sdu.cloud.app.orchestrator.services

import dk.sdu.cloud.SecurityPrincipal
import dk.sdu.cloud.accounting.api.UCLOUD_PROVIDER
import dk.sdu.cloud.app.orchestrator.api.Compute
import dk.sdu.cloud.app.orchestrator.api.ComputeProvider
import dk.sdu.cloud.app.orchestrator.api.ComputeProviderManifest
import dk.sdu.cloud.calls.RPCException
import dk.sdu.cloud.calls.client.AuthenticatedClient
import dk.sdu.cloud.service.Actor
import dk.sdu.cloud.service.safeUsername
import io.ktor.http.*

data class ProviderCommunication(
    val api: Compute,
    val client: AuthenticatedClient,
    val provider: String,
)

class Providers(
    private val serviceClient: AuthenticatedClient,
    private val hardcodedProvider: ComputeProviderManifest,
) {
    private val ucloudCompute = Compute("kubernetes")

    suspend fun prepareCommunication(actor: Actor): ProviderCommunication {
        return prepareCommunication(actor.safeUsername().removePrefix(PROVIDER_USERNAME_PREFIX))
    }

    suspend fun prepareCommunication(provider: String): ProviderCommunication {
        if (provider != UCLOUD_PROVIDER) {
            throw RPCException("Unknown provider: $provider", HttpStatusCode.InternalServerError)
        }

        return ProviderCommunication(ucloudCompute, serviceClient, provider)
    }

    suspend fun verifyProvider(provider: String, principal: SecurityPrincipal) {
        if (provider != UCLOUD_PROVIDER) {
            throw RPCException("Unknown provider: $provider", HttpStatusCode.InternalServerError)
        }

        if (principal.username != "_app-kubernetes") {
            throw RPCException.fromStatusCode(HttpStatusCode.Forbidden)
        }
    }

    suspend fun fetchManifest(provider: String): ComputeProviderManifest {
        if (provider != UCLOUD_PROVIDER) {
            throw RPCException("Unknown provider: $provider", HttpStatusCode.InternalServerError)
        }

        return hardcodedProvider
    }

    companion object {
        const val PROVIDER_USERNAME_PREFIX = "#P"
    }
}
