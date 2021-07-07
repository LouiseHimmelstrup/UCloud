package dk.sdu.cloud.app.orchestrator.services

import dk.sdu.cloud.ActorAndProject
import dk.sdu.cloud.accounting.api.PaymentModel
import dk.sdu.cloud.accounting.api.Product
import dk.sdu.cloud.accounting.api.ProductArea
import dk.sdu.cloud.accounting.api.providers.ResourceApi
import dk.sdu.cloud.accounting.api.providers.ResourceControlApi
import dk.sdu.cloud.accounting.api.providers.ResourceProviderApi
import dk.sdu.cloud.accounting.util.ProviderComms
import dk.sdu.cloud.accounting.util.ProviderSupport
import dk.sdu.cloud.app.orchestrator.api.*
import dk.sdu.cloud.app.store.api.AppParameterValue
import dk.sdu.cloud.calls.client.AuthenticatedClient
import dk.sdu.cloud.service.db.async.AsyncDBSessionFactory
import dk.sdu.cloud.accounting.util.Providers
import dk.sdu.cloud.accounting.util.SqlObject
import dk.sdu.cloud.service.db.async.AsyncDBConnection
import dk.sdu.cloud.service.db.async.parameterList
import dk.sdu.cloud.service.db.async.sendPreparedStatement
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

class LicenseService(
    db: AsyncDBSessionFactory,
    providers: Providers<ComputeCommunication>,
    support: ProviderSupport<ComputeCommunication, Product.License, LicenseSupport>,
    serviceClient: AuthenticatedClient,
    orchestrator: JobOrchestrator,
) : JobBoundResource<License, LicenseSpecification, LicenseUpdate, LicenseIncludeFlags, LicenseStatus, Product.License,
        LicenseSupport, ComputeCommunication,
        AppParameterValue.License>(db, providers, support, serviceClient, orchestrator) {
    override val table = SqlObject.Table("app_orchestrator.licenses")
    override val sqlJsonConverter = SqlObject.Function("license_to_json")
    override val resourceType: String = "license"

    override val sortColumns: Map<String, SqlObject.Column> = mapOf("resource" to SqlObject.Column(table, "resource"))
    override val defaultSortColumn: SqlObject.Column = SqlObject.Column(table, "resource")
    override val currentStateColumn: SqlObject.Column = SqlObject.Column(table, "current_state")
    override val statusBoundToColumn: SqlObject.Column = SqlObject.Column(table, "status_bound_to")
    override val serializer = serializer<License>()
    override val updateSerializer = serializer<LicenseUpdate>()
    override val productArea = ProductArea.LICENSE

    override fun bindsExclusively(): Boolean = false
    override fun requireCreditCheck(res: License, product: Product.License): Boolean =
        product.paymentModel != PaymentModel.FREE_BUT_REQUIRE_BALANCE

    override fun userApi() = Licenses
    override fun controlApi() = LicenseControl
    override fun providerApi(comms: ProviderComms) = LicenseProvider(comms.provider.id)

    override suspend fun createSpecifications(
        actorAndProject: ActorAndProject,
        idWithSpec: List<Pair<Long, LicenseSpecification>>,
        session: AsyncDBConnection,
        allowDuplicates: Boolean
    ) {
        session
            .sendPreparedStatement(
                {
                    val ids by parameterList<Long>()
                    for ((id, spec) in idWithSpec) ids.add(id)
                },
                """
                    insert into app_orchestrator.licenses (resource)
                    select unnest(:ids::bigint[])
                    on conflict (resource) do nothing
                """
            )
    }

    override fun resourcesFromJob(job: Job): List<AppParameterValue.License> =
        job.specification.parameters?.values?.filterIsInstance<AppParameterValue.License>() ?: emptyList()

    override fun isReady(res: License): Boolean = res.status.state == LicenseState.READY
    override fun boundUpdate(binding: JobBinding): LicenseUpdate = LicenseUpdate(binding = binding)
}
