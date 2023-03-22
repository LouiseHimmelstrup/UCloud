package dk.sdu.cloud.integration.backend

import dk.sdu.cloud.accounting.api.Accounting
import dk.sdu.cloud.accounting.api.ChargeWalletRequestItem
import dk.sdu.cloud.accounting.api.UpdateAllocationRequestItem
import dk.sdu.cloud.calls.bulkRequestOf
import dk.sdu.cloud.calls.client.call
import dk.sdu.cloud.calls.client.orThrow
import dk.sdu.cloud.integration.IntegrationTest
import dk.sdu.cloud.integration.UCloudLauncher
import java.util.*
import kotlin.test.assertTrue

class AccountingDifferentialTest : IntegrationTest() {
    override fun defineTests() {

        run {
            class In(
                val chargeAmount: Long
            )

            class Out(
            )
            test<In, Out>("Differential root update test") {
                execute {
                    createSampleProducts()
                    val serviceClient = UCloudLauncher.serviceClient
                    val projectAllocations = prepareProjectChain(
                        10000,
                        listOf(
                            Allocation(true, 5000),
                            Allocation(true, 2000),
                            Allocation(true, 1000)
                        ),
                        sampleStorageDifferential.category,
                        serviceClient = serviceClient
                    )

                    val root = projectAllocations.first()
                    val mid = projectAllocations[1]
                    val leaf = projectAllocations.last()

                    var rootAllocations = retrieveAllocationsInternal(root.owner, sampleStorageDifferential.category, serviceClient)
                    var midAllocations = retrieveAllocationsInternal(mid.owner, sampleStorageDifferential.category, serviceClient)
                    var leafAllocations = retrieveAllocationsInternal(leaf.owner, sampleStorageDifferential.category, serviceClient)

                    require(
                        leafAllocations.single().maxUsableBalance == leafAllocations.single().initialBalance
                    ) { "MaxBalance in leaf is not correct initially" }
                    require(
                        midAllocations.single().maxUsableBalance == midAllocations.single().initialBalance
                    ) { "MaxBalance in mid is not correct initially" }
                    require(
                        rootAllocations.single().maxUsableBalance == rootAllocations.single().initialBalance
                    ) { "MaxBalance in root is not correct initially" }

                    run {
                        val response = Accounting.charge.call(
                            bulkRequestOf(
                                ChargeWalletRequestItem(
                                    leaf.owner,
                                    input.chargeAmount,
                                    1,
                                    sampleStorageDifferential.toReference(),
                                    "SYSTEM",
                                    "daily charge",
                                )
                            ),
                            serviceClient
                        ).orThrow()

                        require(response.responses.first())

                        rootAllocations =
                            retrieveAllocationsInternal(root.owner, sampleStorageDifferential.category, serviceClient)
                        midAllocations =
                            retrieveAllocationsInternal(mid.owner, sampleStorageDifferential.category, serviceClient)
                        leafAllocations =
                            retrieveAllocationsInternal(leaf.owner, sampleStorageDifferential.category, serviceClient)

                        require(
                            rootAllocations.single().balance == rootAllocations.single().initialBalance - input.chargeAmount
                        ) { "Balance in root is not correct after charge" }
                        require(
                            midAllocations.single().balance == midAllocations.single().initialBalance - input.chargeAmount
                        ) { "Balance in mid is not correct after charge" }
                        require(
                            leafAllocations.single().balance == leafAllocations.single().initialBalance - input.chargeAmount
                        ) { "Balance in leaf is not correct after charge" }

                    }
                    run {
                        Accounting.updateAllocation.call(
                            bulkRequestOf(
                                UpdateAllocationRequestItem(
                                    midAllocations.single().id,
                                    10,
                                    midAllocations.single().startDate,
                                    reason = "Gave to many"
                                )
                            ),
                            root.client
                        ).orThrow()

                        rootAllocations =
                            retrieveAllocationsInternal(root.owner, sampleStorageDifferential.category, serviceClient)
                        midAllocations =
                            retrieveAllocationsInternal(mid.owner, sampleStorageDifferential.category, serviceClient)
                        leafAllocations =
                            retrieveAllocationsInternal(leaf.owner, sampleStorageDifferential.category, serviceClient)

                        require(
                            rootAllocations.single().balance == rootAllocations.single().initialBalance - input.chargeAmount
                        ) { "Balance in root is not correct after update" }
                        require(
                            midAllocations.single().balance == midAllocations.single().initialBalance - input.chargeAmount
                        ) { "Balance in mid is not correct after charge" }
                        require(
                            leafAllocations.single().balance == leafAllocations.single().initialBalance - input.chargeAmount
                        ) { "Balance in leaf is not correct after update" }

                        require(midAllocations.single().balance < 0) { "Balance is not negative. It is: ${midAllocations.single().balance}" }
                    }
                    run {
                        val response = Accounting.charge.call(
                            bulkRequestOf(
                                ChargeWalletRequestItem(
                                    leaf.owner,
                                    0,
                                    1,
                                    sampleStorageDifferential.toReference(),
                                    "SYSTEM",
                                    "daily charge",
                                )
                            ),
                            serviceClient
                        ).orThrow()

                        require(response.responses.first())

                        rootAllocations =
                            retrieveAllocationsInternal(root.owner, sampleStorageDifferential.category, serviceClient)
                        midAllocations =
                            retrieveAllocationsInternal(mid.owner, sampleStorageDifferential.category, serviceClient)
                        leafAllocations =
                            retrieveAllocationsInternal(leaf.owner, sampleStorageDifferential.category, serviceClient)

                        require(
                            rootAllocations.single().balance == rootAllocations.single().initialBalance
                        ) { "Balance in root is not correct after update" }
                        require(
                            midAllocations.single().balance == midAllocations.single().initialBalance
                        ) { "Balance in mid is not correct after charge" }
                        require(
                            leafAllocations.single().balance == leafAllocations.single().initialBalance
                        ) { "Balance in leaf is not correct after update" }

                        require(
                            leafAllocations.single().maxUsableBalance == midAllocations.single().balance
                        ) { "maxBalance in leaf is not correct after update" }

                    }
                    run {
                        val response = Accounting.charge.call(
                            bulkRequestOf(
                                ChargeWalletRequestItem(
                                    leaf.owner,
                                    40,
                                    1,
                                    sampleStorageDifferential.toReference(),
                                    "SYSTEM",
                                    "daily charge",
                                )
                            ),
                            serviceClient
                        ).orThrow()

                        require(!response.responses.first())

                    }

                    Out()

                }

                case("initial test") {
                    input(
                        In(
                            chargeAmount = 50L
                        )
                    )
                    check {
                        assertTrue(true)
                    }
                }
            }
        }
    }
}
