package dk.sdu.cloud.file.ucloud.services

import dk.sdu.cloud.service.Loggable
import dk.sdu.cloud.service.Time
import dk.sdu.cloud.calls.client.*
import dk.sdu.cloud.accounting.api.*
import dk.sdu.cloud.accounting.api.providers.*
import dk.sdu.cloud.file.orchestrator.api.*
import dk.sdu.cloud.calls.*
import dk.sdu.cloud.service.javaTimeZone
import java.time.*
import java.time.format.*

class UsageScan(
    private val pathConverter: PathConverter,
    private val fs: NativeFS,
    private val fastDirectoryStats: CephFsFastDirectoryStats,
    private val serviceClient: AuthenticatedClient,
) {
    private val globalErrorCounter = AtomicInteger(0)
    private val globalRequestCounter = AtomicInteger(0)

    private val dataPoints = HashMap<UsageDataPoint.Key, UsageDataPoint>()

    data class UsageDataPoint(
        val key: Key,
        val initialResourceId: String,
        var usageInBytes: Long,
    ) {
        data class Key(
            val owner: WalletOwner, 
            // NOTE(Dan): The product ID comes from the first collection we encounter. It is critical that we perform
            // the charge against the category and not one for each product. This is due to how `DIFFERENTIAL_QUOTA`
            // works.
            val category: ProductCategoryId
        )
    }

    suspend fun startScan() {
        val dateString = LocalDate.ofInstant(
            Instant.ofEpochMilli(Time.now()),
            Time.javaTimeZone
        ).format(DateTimeFormatter.ofPattern("YYYY.MM.dd"))

        run {
            val collectionRoot = pathConverter.relativeToInternal(RelativeInternalFile("/collections"))
            val collections = fs.listFiles(collectionRoot).mapNotNull { it.toLongOrNull() }
            collections.chunked(100).forEach { chunk ->
                val resolvedCollections =
                    retrieveCollections(providerGenerated = false, collections.map { it.toString() })
                        ?: return@forEach

                // NOTE(Dan): We assume that if the recursive size comes back as null then this means that the
                // collection has been deleted and thus shouldn't count.
                val sizes = chunk.map {
                    val thisCollection = pathConverter.relativeToInternal(RelativeInternalFile("/collections/${it}"))
                    fastDirectoryStats.getRecursiveSize(thisCollection) ?: 0L
                }

                processChunk(chunk, sizes, resolvedCollections)
            }
        }

        run {
            val collectionRoot = pathConverter.relativeToInternal(RelativeInternalFile("/home"))
            val collections = fs.listFiles(collectionRoot)
            collections.chunked(100).forEach { chunk ->
                val resolvedCollections = retrieveCollections(
                    providerGenerated = true,
                    collections.map { PathConverter.COLLECTION_HOME_PREFIX + it }
                ) ?: return@forEach

                val sizes = chunk.map { filename ->
                    val thisCollection = pathConverter.relativeToInternal(RelativeInternalFile("/home/${filename}"))
                    fastDirectoryStats.getRecursiveSize(thisCollection) ?: 0L
                }

                val mappedChunk = chunk.map { filename ->
                    resolvedCollections
                        .find { it.providerGeneratedId == PathConverter.COLLECTION_HOME_PREFIX + it }
                        ?.id
                        ?.toLongOrNull()
                }

                processChunk(mappedChunk, sizes, resolvedCollections)
            }
        }

        run {
            val collectionRoot = pathConverter.relativeToInternal(RelativeInternalFile("/projects"))
            val collections = fs.listFiles(collectionRoot)
            collections.chunked(100).forEach { chunk ->
                val resolvedCollections = retrieveCollections(
                    providerGenerated = true,
                    collections.map { PathConverter.COLLECTION_PROJECT_PREFIX + it }
                ) ?: return@forEach

                val sizes = chunk.map { filename ->
                    val thisCollection = pathConverter.relativeToInternal(RelativeInternalFile("/projects/${filename}"))
                    fastDirectoryStats.getRecursiveSize(thisCollection) ?: 0L
                }

                val mappedChunk = chunk.map { filename ->
                    resolvedCollections
                        .find { it.providerGeneratedId == PathConverter.COLLECTION_PROJECT_PREFIX + it }
                        ?.id
                        ?.toLongOrNull()
                }

                processChunk(mappedChunk, sizes, resolvedCollections)
            }
        }

        println("Received the following data points: ${dataPoints}")

        for (chunk in dataPoints.values.chunked(100)) {
            val allRequests = chunk.mapNotNull { dataPoint ->
                val chargeId = when (val owner = dataPoint.key.owner) {
                    is WalletOwner.Project -> owner.projectId
                    is WalletOwner.User -> owner.username
                }

                val units = kotlin.math.ceil(dataPoint.usageInBytes / 1.GiB.toDouble()).toLong()
                if (units <= 0) return@mapNotNull null

                ResourceChargeCredits(
                    dataPoint.initialResourceId,
                    "$chargeId-$dateString",
                    units,
                    description = "Daily storage charge"
                )
            }

            charge(allRequests)
        }
    }

    private fun processChunk(
        chunk: List<Long?>,
        sizes: List<Long>,
        resolvedCollections: List<FileCollection>
    ) {
        for (idx in chunk.indices) {
            val size = sizes[idx]
            val collectionId = chunk[idx] ?: continue
            val resolvedCollection = resolvedCollections.find { it.id == collectionId.toString() } ?: continue
            val (username, project) = resolvedCollection.owner
            val key = UsageDataPoint.Key(
                if (project != null) {
                    WalletOwner.Project(project)
                } else {
                    WalletOwner.User(username)
                },
                ProductCategoryId(
                    resolvedCollection.specification.product.category,
                    resolvedCollection.specification.product.provider,
                )
            )

            val entry = dataPoints[key] ?: UsageDataPoint(key, resolvedCollection.id, 0L)
            entry.usageInBytes += size
            dataPoints[key] = entry
        }
    }

    private suspend fun retrieveCollections(
        providerGenerated: Boolean, 
        collections: List<String>
    ): List<FileCollection>? {
        val includeFlags = if (providerGenerated) {
            FileCollectionIncludeFlags(filterProviderIds = collections.joinToString(","))
        } else {
            FileCollectionIncludeFlags(filterIds = collections.joinToString(","))
        }

        try {
            return retrySection {
                FileCollectionsControl.browse.call(
                    ResourceBrowseRequest(includeFlags, itemsPerPage = 250),
                    serviceClient
                ).orThrow().items
            }
        } catch (ex: Throwable) {
            log.warn("Failed to retrieve information about collections: $collections")
            globalRequestCounter.getAndAdd(collections.size)
            globalErrorCounter.getAndAdd(collections.size)
            checkIfWeShouldTerminate()
            return null
        }
    }

    // NOTE(Dan): We use the following procedure for charging. The procedure is intended to be more roboust against
    // various error scnearios we have encountered in production.
    // 
    //  1. Attempt to bulk charge the entire chunk (retry up to 5 times with a fixed delay)
    //  2. If this fails, attempt to charge the individual requests. All requests are retried using the same algorithm.
    //  3. If a request still fails, we skip the entry and log a warning message that we failed.
    //     a. We keep a global failure counter, we use this counter to determine if the entire script should fail.
    //     b. If more than 10% requests have failed AND at least 100 requests have been attempted, then the entire
    //        script will fail.
    //     c. This should trigger an automatic warning in the system.
    //
    // NOTE(Dan): Step 2 is intended to handle situations where a specific folder is triggering an edge-case in the
    // accounting system. This mitigates the risk that a single folder can cause accounting of all folders to fail
    // (See SDU-eScience/UCloud#2712)
    private suspend fun charge(requests: List<ResourceChargeCredits>) {
        if (requests.isEmpty()) return

        try {
            retrySection { sendCharge(requests) }
            return
        } catch (ex: Throwable) {
            log.warn("Unable to charge requests (bulk): ${requests}")
        }

        for (request in requests) {
            try {
                retrySection { sendCharge(listOf(request)) }
            } catch (ex: Throwable) {
                log.warn("Unable to charge request (single): $request")
                globalRequestCounter.getAndAdd(1)
                globalErrorCounter.getAndAdd(1)
                checkIfWeShouldTerminate()
            }
        }
    }

    private suspend fun checkIfWeShouldTerminate() {
        val errorCounter = globalErrorCounter.get()
        val requestCounter = globalRequestCounter.get()
        if (requestCounter > 100 && requestCounter / errorCounter.toDouble() >= 0.10) {
            throw IllegalStateException("Error threshold has been exceeded")
        }
    }

    private suspend fun sendCharge(request: List<ResourceChargeCredits>) {
        FileCollectionsControl.chargeCredits.call(BulkRequest(request), serviceClient).orThrow()
        globalRequestCounter.getAndAdd(request.size)
    }

    private inline fun <T> retrySection(attempts: Int = 5, delay: Long = 500, block: () -> T): T {
        for (i in 1..attempts) {
            @Suppress("TooGenericExceptionCaught")
            try {
                return block()
            } catch (ex: Throwable) {
                if (i == attempts) throw ex
                Thread.sleep(delay)
            }
        }
        throw IllegalStateException("retrySection impossible situation reached. This should not happen.")
    }

    companion object : Loggable {
        override val log = logger()
    }
}

