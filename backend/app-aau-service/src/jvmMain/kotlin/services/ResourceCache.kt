package dk.sdu.cloud.app.aau.services

import dk.sdu.cloud.accounting.api.Product
import dk.sdu.cloud.accounting.api.providers.ResourceRetrieveRequest
import dk.sdu.cloud.app.aau.ClientHolder
import dk.sdu.cloud.app.orchestrator.api.*
import dk.sdu.cloud.app.store.api.Application
import dk.sdu.cloud.app.store.api.NameAndVersion
import dk.sdu.cloud.calls.client.call
import dk.sdu.cloud.calls.client.orThrow
import dk.sdu.cloud.service.SimpleCache

data class ResolvedJobResources(
    val product: Product.Compute,
    val application: Application,
)

class ResourceCache(private val client: ClientHolder) {
    private val products = SimpleCache<String, Product.Compute>(
        maxAge = 1000 * 60 * 60 * 24L,
        lookup = { null }
    )
    private val applications = SimpleCache<String, Application>(
        maxAge = 1000 * 60 * 60 * 24 * 100L,
        lookup = { null }
    )

    private suspend fun cache(job: Job) {
        val resolvedProduct = job.status.resolvedProduct
        if (resolvedProduct != null) {
            products.insert(key(job.specification.product), resolvedProduct)
        }

        val resolvedApplication = job.status.resolvedApplication
        if (resolvedApplication != null) {
            applications.insert(key(job.specification.application), resolvedApplication)
        }
    }

    suspend fun findResources(job: Job): ResolvedJobResources {
        val product = products.get(key(job.specification.product))
        val application = applications.get(key(job.specification.application))

        if (product != null && application != null) {
            return ResolvedJobResources(product, application)
        }

        val retrievedJob = JobsControl.retrieve.call(
            ResourceRetrieveRequest(job.id, JobIncludeFlags(includeProduct = true, includeApplication = true)),
            client.client
        ).orThrow()

        cache(retrievedJob)

        return ResolvedJobResources(
            retrievedJob.status.resolvedProduct ?: error("No product returned"),
            retrievedJob.status.resolvedApplication ?: error("No application returned")
        )
    }

    private fun key(product: ComputeProductReference): String {
        return "${product.id}/${product.provider}"
    }

    private fun key(application: NameAndVersion): String {
        return "${application.name}/${application.version}"
    }
}