package dk.sdu.cloud.app.orchestrator.services

import dk.sdu.cloud.*
import dk.sdu.cloud.accounting.api.Product
import dk.sdu.cloud.accounting.api.ProductArea
import dk.sdu.cloud.accounting.util.*
import dk.sdu.cloud.accounting.util.Providers
import dk.sdu.cloud.app.orchestrator.api.*
import dk.sdu.cloud.app.orchestrator.api.Job
import dk.sdu.cloud.app.store.api.ToolBackend
import dk.sdu.cloud.calls.BulkRequest
import dk.sdu.cloud.calls.BulkResponse
import dk.sdu.cloud.calls.RPCException
import dk.sdu.cloud.calls.client.*
import dk.sdu.cloud.calls.server.*
import dk.sdu.cloud.provider.api.FEATURE_NOT_SUPPORTED_BY_PROVIDER
import dk.sdu.cloud.provider.api.Permission
import dk.sdu.cloud.provider.api.ResourceUpdateAndId
import dk.sdu.cloud.service.*
import dk.sdu.cloud.service.db.async.AsyncDBConnection
import dk.sdu.cloud.service.db.async.AsyncDBSessionFactory
import dk.sdu.cloud.service.db.async.DBContext
import dk.sdu.cloud.service.db.async.sendPreparedStatement
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.selects.select
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.serializer

interface JobListener {
    suspend fun onVerified(ctx: DBContext, job: Job) {}
    suspend fun onCreate(ctx: DBContext, job: Job) {}
    suspend fun onTermination(ctx: DBContext, job: Job) {}
}

/**
 * The job orchestrator is responsible for the orchestration of computation providers.
 *
 * The orchestrator receives life-time events via its methods. These life-time events can be
 * related to user action (e.g. starting an application) or it could be related to computation
 * updates (e.g. state change). In reaction to life-time events the orchestrator will update
 * its internal state and potentially send new updates to the relevant computation providers.
 */
class JobOrchestrator(
    db: AsyncDBSessionFactory,
    providers: Providers<ComputeCommunication>,
    support: ProviderSupport<ComputeCommunication, Product.Compute, ComputeSupport>,
    serviceClient: AuthenticatedClient,
    private val appService: AppStoreCache,
) : ResourceService<Job, JobSpecification, JobUpdate, JobIncludeFlags, JobStatus,
    Product.Compute, ComputeSupport, ComputeCommunication>(db, providers, support, serviceClient) {
    private val verificationService = JobVerificationService(appService, this)
    private val listeners = ArrayList<JobListener>()

    fun addListener(listener: JobListener) {
        listeners.add(listener)
    }

    override val productArea: ProductArea = ProductArea.COMPUTE
    override val serializer: KSerializer<Job> = serializer()
    override val table: SqlObject.Table = SqlObject.Table("app_orchestrator.jobs")
    override val sortColumn: SqlObject.Column = SqlObject.Column(table, "resource")
    override val updateSerializer: KSerializer<JobUpdate> = serializer()

    override fun controlApi() = JobsControl
    override fun userApi() = Jobs
    override fun providerApi(comms: ProviderComms) = JobsProvider(comms.provider.id)

    override suspend fun verifyProviderSupportsCreate(
        spec: JobSpecification,
        res: ProductRefOrResource<Job>,
        support: ComputeSupport
    ) {
        val application = appService.resolveApplication(spec.application)
            ?: throw JobException.VerificationError("Application does not exist")

        when (application.invocation.tool.tool!!.description.backend) {
            ToolBackend.SINGULARITY -> {
                throw JobException.VerificationError("Unsupported application")
            }

            ToolBackend.DOCKER -> {
                if (support.docker.enabled != true) {
                    throw JobException.VerificationError("Application is not supported by provider")
                }
            }

            ToolBackend.VIRTUAL_MACHINE -> {
                if (support.virtualMachine.enabled != null) {
                    throw JobException.VerificationError("Application is not supported by provider")
                }
            }
        }
    }

    override suspend fun createSpecifications(
        actorAndProject: ActorAndProject,
        idWithSpec: List<Pair<Long, JobSpecification>>,
        session: AsyncDBConnection,
        allowDuplicates: Boolean
    ) {
        idWithSpec.forEach { (id, spec) ->
            verificationService.verifyOrThrow(actorAndProject, spec)

            val preliminaryJob = Job.fromSpecification(id.toString(), actorAndProject, spec)
            listeners.forEach { it.onVerified(db, preliminaryJob) }
        }

        session.sendPreparedStatement(
            {
                val applicationNames = ArrayList<String>().also { setParameter("application_names", it) }
                val applicationVersions = ArrayList<String>().also { setParameter("application_versions", it) }
                val timeAllocationMillis = ArrayList<Long?>().also { setParameter("time_allocation", it) }
                val names = ArrayList<String?>().also { setParameter("names", it) }
                val outputFolders = ArrayList<String>().also { setParameter("output_folders", it) }
                val resources = ArrayList<Long>().also { setParameter("resources", it) }

                for ((id, spec) in idWithSpec) {
                    applicationNames.add(spec.application.name)
                    applicationVersions.add(spec.application.version)
                    timeAllocationMillis.add(spec.timeAllocation?.toMillis())
                    names.add(spec.name)
                    outputFolders.add("/TODO")
                    resources.add(id)
                }
            },
            """
                with bulk_data as (
                    select unnest(:application_names::text[]) app_name, unnest(:application_versions::text[]) app_ver,
                           unnest(:time_allocation::bigint[]) time_alloc, unnest(:names::text[]) n, 
                           unnest(:output_folders::text[]) output, unnest(:resources::bigint[]) resource
                )
                insert into app_orchestrator.jobs
                    (application_name, application_version, time_allocation_millis, name, 
                     output_folder, current_state, started_at, resource) 
                select app_name, app_ver, time_alloc, n, output, 'IN_QUEUE', null, resource
                from bulk_data
            """
        )

        session.sendPreparedStatement(
            {
                val jobIds = ArrayList<Long>().also { setParameter("job_ids", it) }
                val resources = ArrayList<String>().also { setParameter("resources", it) }

                for ((id, spec) in idWithSpec) {
                    for (resource in (spec.resources ?: emptyList())) {
                        jobIds.add(id)
                        resources.add(defaultMapper.encodeToString(resource))
                    }
                }
            },
            """
                insert into app_orchestrator.job_resources (resource, job_id) 
                select unnest(:resources::jsonb[]), unnest(:job_ids::bigint[])
            """
        )

        session.sendPreparedStatement(
            {
                val jobIds = ArrayList<Long>().also { setParameter("job_ids", it) }
                val values = ArrayList<String>().also { setParameter("values", it) }
                val names = ArrayList<String>().also { setParameter("names", it) }

                for ((id, spec) in idWithSpec) {
                    for ((name, value) in (spec.parameters ?: emptyMap())) {
                        jobIds.add(id)
                        values.add(defaultMapper.encodeToString(value))
                        names.add(name)
                    }
                }
            },
            """
                insert into app_orchestrator.job_input_parameters (name, value, job_id)  
                select unnest(:names::text[]), unnest(:values::jsonb[]), unnest(:job_ids::bigint[])
            """
        )
    }

    override suspend fun onUpdate(
        resources: List<Job>,
        updates: List<ResourceUpdateAndId<JobUpdate>>,
        session: AsyncDBConnection
    ) {
        val loadedJobs = resources.associateBy { it.id }

        for ((jobId, updatesForJob) in updates.groupBy { it.id }) {
            val job = loadedJobs[jobId] ?: error("Job not found, this should have failed earlier")
            var currentState = job.status.state
            for ((_, update) in updatesForJob) {
                if (update.expectedState != null && update.expectedState != currentState) continue
                if (update.expectedDifferentState == true && update.state == currentState) continue

                val newState = update.state
                val newStatus = update.status

                if (newState != null && !currentState.isFinal()) {
                    @Suppress("SqlResolve")
                    currentState = newState

                    if (newState.isFinal()) {
                        listeners.forEach { it.onTermination(session, job) }
                    }

                    session.sendPreparedStatement(
                        {
                            setParameter("new_state", newState.name)
                            setParameter("job_id", jobId)
                        },
                        """
                            update app_orchestrator.jobs
                            set
                                current_state = :new_state,
                                started_at = case
                                    when :new_state = 'RUNNING' then coalesce(started_at, now())
                                    else started_at
                                end
                            where resource = :job_id
                        """
                    )
                } else if (newState != null && currentState.isFinal()) {
                    log.info(
                        "Ignoring job update for $jobId by ${job.specification.product.provider} " +
                            "(bad state transition)"
                    )
                    continue
                }
            }
        }
    }

    override suspend fun browseQuery(flags: JobIncludeFlags?): PartialQuery {
        @Suppress("SqlResolve")
        return PartialQuery(
            {
                setParameter("filter_state", flags?.filterState?.name)
            },
            """
                select
                    job.resource, job,
                    array_remove(array_agg(distinct res), null),
                    array_remove(array_agg(distinct param), null),
                    app, t

                from
                    app_orchestrator.jobs job join
                    app_store.applications app on
                        job.application_name = app.name and job.application_version = app.version join
                    app_store.tools t on app.tool_name = t.name and app.tool_version = t.version left join
                    app_orchestrator.job_resources res on job.resource = res.job_id left join
                    app_orchestrator.job_input_parameters param on job.resource = param.job_id

                where
                    (:filter_state::text is null or current_state = :filter_state::text)

                group by job.resource, job.*, app.*, t.*
            """
        )
    }

    suspend fun extendDuration(
        actorAndProject: ActorAndProject,
        request: BulkRequest<JobsExtendRequestItem>
    ): BulkResponse<Unit?> {
        return proxy.bulkProxy(
            actorAndProject,
            request,
            BulkProxyInstructions.pureProcedure(
                service = this,
                retrieveCall = { providerApi(it).extend },
                requestToId = { it.jobId },
                resourceToRequest = { req, res -> JobsProviderExtendRequestItem(res, req.requestedTime) },
                verifyRequest = { req, res, support ->
                    val job = (res as ProductRefOrResource.SomeResource).resource
                    val application = appService.resolveApplication(job.specification.application)
                        ?: throw RPCException("Unknown application", HttpStatusCode.BadRequest)
                    val appBackend = application.invocation.tool.tool!!.description.backend

                    val isSupported =
                        (appBackend == ToolBackend.DOCKER && support.docker.timeExtension == true) ||
                            (appBackend == ToolBackend.VIRTUAL_MACHINE && support.virtualMachine.timeExtension == true)

                    if (!isSupported) {
                        throw RPCException(
                            "Extension of deadline is not supported by the provider",
                            HttpStatusCode.BadRequest,
                            FEATURE_NOT_SUPPORTED_BY_PROVIDER
                        )
                    }
                }
            )
        )
    }

    suspend fun terminate(
        actorAndProject: ActorAndProject,
        request: BulkRequest<FindByStringId>,
    ): BulkResponse<Unit?> {
        return proxy.bulkProxy(
            actorAndProject,
            request,
            BulkProxyInstructions.pureProcedure(this, { providerApi(it).terminate }, { it.id }, { _, res -> res })
        )
    }

    suspend fun follow(
        callHandler: CallHandler<JobsFollowRequest, JobsFollowResponse, *>,
    ): Unit = with(callHandler) {
        withContext<WSCall> {
            val initialJob = retrieveBulk(
                callHandler.actorAndProject, listOf(request.id), listOf(Permission.Read),
                flags = JobIncludeFlags(includeUpdates = true, includeSupport = true)
            ).single()

            val support = initialJob.status.resolvedSupport!!.support
            val app = appService.resolveApplication(initialJob.specification.application)!!
            val backend = app.invocation.tool.tool!!.description.backend
            val logsSupported =
                (backend == ToolBackend.DOCKER && support.docker.logs == true) ||
                    (backend == ToolBackend.VIRTUAL_MACHINE && support.virtualMachine.logs == true)

            // NOTE(Dan): We do _not_ send the initial list of updates, instead we assume that clients will
            // retrieve them by themselves.
            sendWSMessage(JobsFollowResponse(emptyList(), emptyList(), initialJob.status))

            var lastUpdate = initialJob.updates.maxByOrNull { it.timestamp }?.timestamp ?: 0L
            var streamId: String? = null
            val states = JobState.values()
            val currentState = AtomicInteger(JobState.IN_QUEUE.ordinal)

            coroutineScope {
                val logJob = if (!logsSupported) null else launch {
                    while (isActive) {
                        if (currentState.get() == JobState.RUNNING.ordinal) {
                            proxy.proxySubscription(
                                actorAndProject,
                                Unit,
                                object : SubscriptionProxyInstructions<ComputeCommunication, ComputeSupport, Job,
                                    Unit, JobsProviderFollowRequest, JobsProviderFollowResponse>() {
                                    override val isUserRequest: Boolean = true
                                    override fun retrieveCall(comms: ComputeCommunication) = providerApi(comms).follow

                                    override suspend fun verifyAndFetchResources(
                                        actorAndProject: ActorAndProject,
                                        request: Unit
                                    ): RequestWithRefOrResource<Unit, Job> {
                                        return Unit to ProductRefOrResource.SomeResource(initialJob)
                                    }

                                    override suspend fun beforeCall(
                                        provider: String,
                                        resource: RequestWithRefOrResource<Unit, Job>
                                    ): JobsProviderFollowRequest {
                                        return JobsProviderFollowRequest.Init(initialJob)
                                    }

                                    override suspend fun onMessage(
                                        provider: String,
                                        resource: RequestWithRefOrResource<Unit, Job>,
                                        message: JobsProviderFollowResponse
                                    ) {
                                        if (streamId == null) streamId = message.streamId
                                        sendWSMessage(
                                            JobsFollowResponse(
                                                emptyList(),
                                                listOf(JobsLog(message.rank, message.stdout, message.stderr))
                                            )
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                val updateJob = launch {
                    try {
                        var lastStatus: JobStatus? = null
                        while (isActive && !states[currentState.get()].isFinal()) {
                            val job = retrieveBulk(
                                actorAndProject, listOf(request.id), listOf(Permission.Read),
                                JobIncludeFlags(includeUpdates = true)
                            ).single()

                            currentState.set(job.status.state.ordinal)

                            // TODO not ideal for chatty providers
                            val updates = job.updates.filter { it.timestamp > lastUpdate }
                            if (updates.isNotEmpty()) {
                                sendWSMessage(
                                    JobsFollowResponse(
                                        updates,
                                        emptyList(),
                                        null
                                    )
                                )
                                lastUpdate = job.updates.maxByOrNull { it.timestamp }?.timestamp ?: 0L
                            }

                            if (lastStatus != job.status) {
                                sendWSMessage(JobsFollowResponse(emptyList(), emptyList(), job.status))
                            }

                            lastStatus = job.status
                            delay(1000)
                        }
                    } catch (ex: Throwable) {
                        if (ex !is CancellationException) {
                            log.warn(ex.stackTraceToString())
                        }
                    }
                }

                try {
                    select<Unit> {
                        (logJob ?: Job()).onJoin {}
                        updateJob.onJoin {}
                    }
                } finally {
                    val capturedId = streamId
                    if (capturedId != null) {
                        proxy.proxy(
                            actorAndProject,
                            Unit,
                            object : ProxyInstructions<ComputeCommunication, ComputeSupport, Job, Unit,
                                JobsProviderFollowRequest, JobsProviderFollowResponse>() {
                                override val isUserRequest: Boolean = true
                                override fun retrieveCall(comms: ComputeCommunication) = providerApi(comms).follow

                                override suspend fun verifyAndFetchResources(
                                    actorAndProject: ActorAndProject,
                                    request: Unit
                                ): RequestWithRefOrResource<Unit, Job> =
                                    Unit to ProductRefOrResource.SomeResource(initialJob)

                                override suspend fun beforeCall(
                                    provider: String,
                                    resource: RequestWithRefOrResource<Unit, Job>
                                ): JobsProviderFollowRequest = JobsProviderFollowRequest.CancelStream(capturedId)

                            }
                        )
                    }

                    runCatching { logJob?.cancel("No longer following or EOF") }
                    runCatching { updateJob.cancel("No longer following or EOF") }
                }
            }
        }
    }

    suspend fun openInteractiveSession(
        actorAndProject: ActorAndProject,
        request: BulkRequest<JobsOpenInteractiveSessionRequestItem>,
    ): JobsOpenInteractiveSessionResponse {
        val jobIdToProvider = HashMap<String, String>()
        val responses = proxy.bulkProxy(
            actorAndProject,
            request,
            BulkProxyInstructions.pureProcedure(
                service = this,
                retrieveCall = { providerApi(it).openInteractiveSession },
                requestToId = { it.id },
                resourceToRequest = { req, res ->
                    jobIdToProvider[req.id] = res.specification.product.provider
                    JobsProviderOpenInteractiveSessionRequestItem(res, req.rank, req.sessionType)
                }
            )
        )

        return BulkResponse(
            request.items.zip(responses.responses).map { (request, response) ->
                val providerSpec = providers.prepareCommunication(jobIdToProvider.getValue(request.id)).provider
                if (response == null) null
                else OpenSessionWithProvider(
                    with(providerSpec) {
                        buildString {
                            if (https) append("https://") else append("http://")
                            append(domain)
                            if (port != null) append(":$port")
                        }
                    },
                    providerSpec.id,
                    response
                )
            }
        )
    }

    suspend fun retrieveUtilization(
        actorAndProject: ActorAndProject,
        request: JobsRetrieveUtilizationRequest
    ): JobsRetrieveUtilizationResponse {
        JobsRetrieveUtilizationResponse
        return proxy.proxy(
            actorAndProject,
            request,
            object : ProxyInstructions<ComputeCommunication, ComputeSupport, Job,
                JobsRetrieveUtilizationRequest, JobsProviderUtilizationRequest, JobsProviderUtilizationResponse>() {
                override val isUserRequest: Boolean = false
                override fun retrieveCall(comms: ComputeCommunication) = providerApi(comms).retrieveUtilization

                override suspend fun verifyAndFetchResources(
                    actorAndProject: ActorAndProject,
                    request: JobsRetrieveUtilizationRequest
                ): RequestWithRefOrResource<JobsRetrieveUtilizationRequest, Job> {
                    val job = retrieveBulk(actorAndProject, listOf(request.jobId), listOf(Permission.Read)).single()
                    return RequestWithRefOrResource(request, ProductRefOrResource.SomeResource(job))
                }

                override suspend fun verifyRequest(
                    request: JobsRetrieveUtilizationRequest,
                    res: ProductRefOrResource<Job>,
                    support: ComputeSupport
                ) {
                    val job = (res as ProductRefOrResource.SomeResource).resource
                    val app = appService.resolveApplication(job.specification.application)!!
                    val backend = app.invocation.tool.tool!!.description.backend
                    if (backend == ToolBackend.DOCKER) {
                        if (support.docker.utilization != true) {
                            throw RPCException(
                                "Not supported",
                                HttpStatusCode.BadRequest,
                                FEATURE_NOT_SUPPORTED_BY_PROVIDER
                            )
                        }
                    } else if (backend == ToolBackend.VIRTUAL_MACHINE) {
                        if (support.virtualMachine.utilization != true) {
                            throw RPCException(
                                "Not supported",
                                HttpStatusCode.BadRequest,
                                FEATURE_NOT_SUPPORTED_BY_PROVIDER
                            )
                        }
                    }
                }

                override suspend fun beforeCall(
                    provider: String,
                    resource: RequestWithRefOrResource<JobsRetrieveUtilizationRequest, Job>
                ) {
                }
            }
        )
    }

    companion object : Loggable {
        override val log = logger()
    }
}
