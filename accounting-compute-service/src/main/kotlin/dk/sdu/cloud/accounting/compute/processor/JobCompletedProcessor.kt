package dk.sdu.cloud.accounting.compute.processor

import dk.sdu.cloud.accounting.compute.api.AccountingJobCompletedEvent
import dk.sdu.cloud.accounting.compute.services.CompletedJobsService
import dk.sdu.cloud.accounting.compute.services.normalizeUsername
import dk.sdu.cloud.app.api.JobCompletedEvent
import dk.sdu.cloud.auth.api.LookupUsersRequest
import dk.sdu.cloud.auth.api.UserDescriptions
import dk.sdu.cloud.calls.RPCException
import dk.sdu.cloud.calls.client.AuthenticatedClient
import dk.sdu.cloud.calls.client.call
import dk.sdu.cloud.calls.client.orRethrowAs
import dk.sdu.cloud.service.EventConsumer
import dk.sdu.cloud.service.EventConsumerFactory
import dk.sdu.cloud.service.Loggable
import dk.sdu.cloud.service.batched
import dk.sdu.cloud.service.consumeBatchAndCommit
import kotlinx.coroutines.runBlocking
import dk.sdu.cloud.app.api.AccountingEvents as JobAccountingEvents

class JobCompletedProcessor<DBSession>(
    private val eventConsumerFactory: EventConsumerFactory,
    private val completedJobsService: CompletedJobsService<DBSession>,
    private val serviceCloud: AuthenticatedClient,
    private val parallelism: Int = 4
) {
    fun init(): List<EventConsumer<*>> = (0 until parallelism).map { _ ->
        eventConsumerFactory.createConsumer(JobAccountingEvents.jobCompleted).configure { root ->
            root
                .batched(
                    batchTimeout = 500,
                    maxBatchSize = 250
                )
                .consumeBatchAndCommit { batch ->
                    val events = runBlocking { normalizeEvents(batch.map { it.second }) }
                    val accountingEvents = events.map { event ->
                        AccountingJobCompletedEvent(
                            event.application,
                            event.nodes,
                            event.duration,
                            event.jobOwner,
                            event.jobId,
                            event.jobCompletedAt
                        )
                    }

                    completedJobsService.insertBatch(accountingEvents)
                }
        }
    }

    /**
     * Normalizes project users to their normalized user (without the suffix)
     */
    private suspend fun normalizeEvents(events: List<JobCompletedEvent>): List<JobCompletedEvent> {
        // Normalize project users to their normalized user (without the suffix)
        val users = events.map { it.jobOwner }.toSet().toList()

        val userTable = UserDescriptions.lookupUsers.call(
            LookupUsersRequest(users),
            serviceCloud
        ).orRethrowAs {
            log.warn("Caught an exception while attempting to normalize events!")
            log.warn("Got back status: ${it.error} ${it.statusCode}")
            throw RPCException.fromStatusCode(it.statusCode)
        }.results

        return events.mapNotNull { event ->
            val owner = event.jobOwner
            val role = userTable[owner]?.role ?: return@mapNotNull run {
                log.warn("Could not find user $owner. Discarding accounting event.")
                null
            }

            event.copy(jobOwner = normalizeUsername(owner, role))
        }
    }

    companion object : Loggable {
        override val log = logger()
    }
}
