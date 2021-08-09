package dk.sdu.cloud.integration.backend

import dk.sdu.cloud.accounting.api.*
import dk.sdu.cloud.auth.api.UserDescriptions
import dk.sdu.cloud.calls.bulkRequestOf
import dk.sdu.cloud.calls.client.AuthenticatedClient
import dk.sdu.cloud.calls.client.call
import dk.sdu.cloud.calls.client.orThrow
import dk.sdu.cloud.calls.client.withProject
import dk.sdu.cloud.grant.api.DKK
import dk.sdu.cloud.integration.IntegrationTest
import dk.sdu.cloud.integration.UCloudLauncher.serviceClient
import dk.sdu.cloud.project.api.CreateProjectRequest
import dk.sdu.cloud.project.api.Projects
import dk.sdu.cloud.service.Time
import dk.sdu.cloud.service.test.assertThatInstance
import dk.sdu.cloud.service.test.assertThatPropertyEquals
import io.ktor.http.*
import java.util.*
import kotlin.math.max
import kotlin.test.assertEquals
import kotlin.test.assertTrue

suspend fun findWallet(
    client: AuthenticatedClient,
    product: ProductCategoryId
): Wallet? {
    return Wallets.browse.call(
        WalletBrowseRequest(),
        client
    ).orThrow().items.find { it.paysFor == product }
}

suspend fun findPersonalWallet(
    username: String,
    client: AuthenticatedClient,
    product: ProductCategoryId
): Wallet? {
    return Wallets.browse.call(
        WalletBrowseRequest(),
        client
    ).orThrow().items.find { it.paysFor == product }
}

suspend fun findProjectWallet(
    projectId: String,
    client: AuthenticatedClient,
    product: ProductCategoryId
): Wallet? {
    return Wallets.browse.call(
        WalletBrowseRequest(),
        client.withProject(projectId)
    ).orThrow().items.find { it.paysFor == product }
}

class AccountingTest : IntegrationTest() {
    override fun defineTests() {

        testFilter = {title, subtitle ->
            title == "Transfers"
        }

        class Allocation(
            val isProject: Boolean,
            val amount: Long,
            val startDate: Long? = null,
            val endDate: Long? = null
        )

        data class AllocationResult(
            val username: String,
            val projectId: String?,
            val client: AuthenticatedClient,
            val balance: Long
        ) {
            val owner: WalletOwner = if (projectId == null) {
                WalletOwner.User(username)
            } else {
                WalletOwner.Project(projectId)
            }
        }

        suspend fun prepareProjectChain(
            rootBalance: Long,
            chainFromRoot: List<Allocation>,
            productCategory: ProductCategoryId,
            skipCreationOfLeaf: Boolean = false,
        ): List<AllocationResult> {
            val rootPi = createUser()
            val rootProject = initializeRootProject(rootPi.username, amount = rootBalance)
            val irrelevantUser = createUser()

            val leaves = ArrayList<AllocationResult>()

            for ((index, allocOwner) in chainFromRoot.withIndex()) {
                if (allocOwner.isProject) {
                    val user = createUser()
                    val project = Projects.create.call(
                        CreateProjectRequest("P$index", principalInvestigator = user.username),
                        serviceClient
                    ).orThrow()

                    leaves.add(
                        AllocationResult(
                            user.username,
                            project.id,
                            user.client.withProject(project.id),
                            allocOwner.amount
                        )
                    )
                } else {
                    val user = createUser()
                    leaves.add(AllocationResult(user.username, null, user.client, allocOwner.amount))
                }
            }

            var previousAllocation: String =
                findProjectWallet(rootProject, rootPi.client, productCategory)!!.allocations.single().id
            var previousPi: AuthenticatedClient = rootPi.client
            val expectedAllocationPath = arrayListOf(previousAllocation)
            for ((index, leaf) in leaves.withIndex()) {
                if (index == leaves.lastIndex && skipCreationOfLeaf) break

                val request = bulkRequestOf(
                    DepositToWalletRequestItem(
                        leaf.owner,
                        previousAllocation,
                        leaf.balance,
                        "Transfer",
                        chainFromRoot[index].startDate,
                        chainFromRoot[index].endDate
                    )
                )
                Accounting.deposit.call(request, previousPi).orThrow()

                assertThatInstance(
                    Accounting.deposit.call(request, irrelevantUser.client),
                    "Should fail because they are not a part of the project"
                ) { it.statusCode.value in 400..499 }

                val alloc = findWallet(leaf.client, productCategory)!!.allocations.single()
                previousAllocation = alloc.id
                previousPi = leaf.client

                expectedAllocationPath.add(alloc.id)
                assertEquals(alloc.allocationPath, expectedAllocationPath)
                assertEquals(alloc.balance, leaf.balance)
                assertEquals(alloc.initialBalance, leaf.balance)
            }
            return leaves
        }

        run {
            class In(
                val request: TransferToWalletRequestItem
            )

            test<In, Unit>("Transfers") {
                execute {
                    createSampleProducts()
                    val leaves = prepareProjectChain(
                        10000.DKK,
                        (0 until 3).map { Allocation(true, 1000.DKK) },
                        sampleCompute.category
                    )

                    Accounting.transfer.call(
                        bulkRequestOf(
                            input.request
                        ),
                        leaves.last().client
                    )


                }

                case("empty Test") {
                    input(
                        In(
                            TransferToWalletRequestItem(
                                sampleCompute.category,
                                WalletOwner.User("username"),
                                WalletOwner.User("user2"),
                                20L,
                                performedBy = "user"
                            )
                        )
                    )
                    check {
                        assertTrue { true }
                    }
                }

            }
        }

        run {
            class In(
                val owner: WalletOwner,
                val createProducts: Boolean = true,
                val createUser: String? = null
            )

            test<In, Unit>("Bad uses of rootDeposit") {
                execute {
                    if (input.createProducts) {
                        createSampleProducts()
                    }

                    val username = input.createUser
                    if (username != null) createUser(username)

                    Accounting.rootDeposit.call(
                        bulkRequestOf(
                            RootDepositRequestItem(sampleCompute.category, input.owner, 100.DKK, "Initial balance")
                        ),
                        serviceClient
                    ).orThrow()
                }

                case("Bad user") {
                    input(In(WalletOwner.User("BADUSER"), createProducts = true))
                    expectStatusCode(HttpStatusCode.BadRequest)
                }

                case("Bad project") {
                    input(In(WalletOwner.Project("BADPROJECT"), createProducts = true))
                    expectStatusCode(HttpStatusCode.BadRequest)
                }

                case("Bad product") {
                    val id = UUID.randomUUID().toString()
                    input(In(WalletOwner.User(id), createProducts = false, createUser = id))
                    expectStatusCode(HttpStatusCode.BadRequest)
                }
            }
        }

        run {
            class In(
                val walletBelongsToProject: Boolean,
                val initialBalance: Long,
                val units: Long,
                val numberOfProducts: Long = 1,
                val product: Product = sampleCompute
            )

            class Out(
                val newBalance: Long
            )

            test<In, Out>("Root deposit and charge") {
                execute {
                    createSampleProducts()

                    val owner: WalletOwner
                    val client: AuthenticatedClient
                    val createdUser = createUser("${title}_$testId")

                    if (input.walletBelongsToProject) {
                        val id = Projects.create.call(
                            CreateProjectRequest(
                                "${title}_$testId",
                                null,
                                principalInvestigator = createdUser.username
                            ),
                            serviceClient
                        ).orThrow().id

                        owner = WalletOwner.Project(id)
                        client = createdUser.client.withProject(id)
                    } else {
                        client = createdUser.client
                        owner = WalletOwner.User(createdUser.username)
                    }

                    Accounting.rootDeposit.call(
                        bulkRequestOf(
                            RootDepositRequestItem(
                                input.product.category,
                                owner,
                                input.initialBalance,
                                "Initial deposit"
                            )
                        ),
                        serviceClient
                    ).orThrow()

                    val initialWallets = Wallets.browse.call(WalletBrowseRequest(), client).orThrow().items

                    assertThatInstance(initialWallets, "has a single wallet with correct parameters") {
                        it.size == 1 && it.single().paysFor == input.product.category &&
                                it.single().allocations.size == 1
                    }

                    assertThatInstance(initialWallets.single().allocations.single(), "has a valid allocation") {
                        it.balance == input.initialBalance &&
                                it.initialBalance == input.initialBalance &&
                                it.endDate == null
                    }

                    Accounting.charge.call(
                        bulkRequestOf(
                            ChargeWalletRequestItem(
                                owner,
                                input.units,
                                input.numberOfProducts,
                                input.product.toReference(),
                                createdUser.username,
                                "Test charge"
                            )
                        ),
                        serviceClient
                    ).orThrow()

                    val walletsAfterCharge = Wallets.browse.call(WalletBrowseRequest(), client).orThrow().items
                    val newAllocation = walletsAfterCharge.singleOrNull()?.allocations?.singleOrNull()
                        ?: error("newAllocation is null")

                    Out(newAllocation.balance)
                }

                listOf(true, false).forEach { isProject ->
                    val name = if (isProject) "Project" else "User"

                    fun balanceWasDeducted(input: In, output: Out) {
                        assertEquals(
                            input.initialBalance - (input.product.pricePerUnit * input.units * input.numberOfProducts),
                            output.newBalance
                        )
                    }

                    case("$name with enough credits") {
                        input(In(isProject, 1000.DKK, 1))
                        check { balanceWasDeducted(input, output) }
                    }

                    case("$name with over-charge") {
                        input(In(isProject, 10.DKK, 20.DKK))
                        check {
                            assertEquals(0, output.newBalance)
                        }
                    }

                    case("$name with negative units") {
                        input(In(isProject, 100.DKK, -100))
                        expectStatusCode(HttpStatusCode.BadRequest)
                    }

                    case("$name with negative number of products") {
                        input(In(isProject, 100.DKK, 1, -1))
                        expectStatusCode(HttpStatusCode.BadRequest)
                    }

                    case("$name with no products involved") {
                        input(In(isProject, 100.DKK, 0))
                        expectStatusCode(HttpStatusCode.BadRequest)
                    }
                }
            }
        }

        run {
            class In(
                val rootBalance: Long,
                val chainFromRoot: List<Allocation>,
                val units: Long,
                val numberOfProducts: Long = 1,
                val product: Product = sampleCompute,
                val skipCreationOfLeaf: Boolean = false
            )

            class ChargeOutput(
                val balancesFromRoot: List<Long>,
                val transactionsFromRoot: List<List<Transaction>>
            )

            class CheckOutput(
                val hasEnoughCredits: Boolean
            )

            suspend fun prepare(input: In): List<AllocationResult> {
                return prepareProjectChain(
                    input.rootBalance, input.chainFromRoot, input.product.category,
                    input.skipCreationOfLeaf
                )
            }

            test<In, ChargeOutput>("Deposit and charge") {
                execute {
                    val leaves = prepare(input)

                    Accounting.charge.call(
                        bulkRequestOf(
                            ChargeWalletRequestItem(
                                leaves.last().owner,
                                input.units,
                                input.numberOfProducts,
                                input.product.toReference(),
                                leaves.last().username,
                                "Test charge"
                            )
                        ),
                        serviceClient
                    ).orThrow()

                    ChargeOutput(
                        leaves.map {
                            findWallet(it.client, input.product.category)?.allocations?.singleOrNull()?.balance ?: 0L
                        },
                        leaves.map {
                            Transactions.browse.call(
                                TransactionsBrowseRequest(input.product.category.name, input.product.category.provider),
                                it.client
                            ).orThrow().items
                        }
                    )
                }

                fun balanceWasDeducted(input: In, output: ChargeOutput) {
                    val paymentRequired = input.product.pricePerUnit * input.units * input.numberOfProducts

                    output.balancesFromRoot.forEachIndexed { index, balance ->
                        assertEquals(
                            max(0, input.chainFromRoot[index].amount - paymentRequired),
                            balance,
                            "New balance of $index should match expected value"
                        )
                    }

                    output.transactionsFromRoot.forEachIndexed { index, transactions ->
                        val deposits = transactions.filterIsInstance<Transaction.Deposit>()
                        assertThatInstance(deposits, "Should only contain a single deposit ($index)") {
                            it.size == 1
                        }

                        val deposit = deposits.single()
                        assertThatPropertyEquals(
                            deposit,
                            { it.resolvedCategory },
                            input.product.category,
                            "resolvedCategory[$index]"
                        )
                        assertThatPropertyEquals(
                            deposit,
                            { it.change },
                            input.chainFromRoot[index].amount,
                            "change[$index]"
                        )
                        assertThatInstance(deposit, "startDate[$index]") { it.startDate != null }
                        assertThatPropertyEquals(deposit, { it.endDate }, null, "endDate[$index]")
                    }

                    output.transactionsFromRoot.forEachIndexed { index, transactions ->
                        val charges = transactions.filterIsInstance<Transaction.Charge>()
                        assertThatInstance(charges, "Should only contain a single charge ($index)") {
                            it.size == 1
                        }

                        val expectedNewBalance = max(0, input.chainFromRoot[index].amount - paymentRequired)
                        val expectedChange = expectedNewBalance - input.chainFromRoot[index].amount
                        val charge = charges.single()
                        assertThatPropertyEquals(charge, { it.units }, input.units, "units[$index]")
                        assertThatPropertyEquals(
                            charge,
                            { it.numberOfProducts },
                            input.numberOfProducts,
                            "numberOfProducts[$index]"
                        )
                        assertThatPropertyEquals(charge, { it.productId }, input.product.name, "productId[$index]")
                        assertThatPropertyEquals(
                            charge,
                            { it.resolvedCategory },
                            input.product.category,
                            "resolvedCategory[$index]"
                        )
                        assertThatPropertyEquals(charge, { it.change }, expectedChange, "change[$index]")
                    }
                }

                listOf(true, false).forEach { isProject ->
                    val name = if (isProject) "project" else "user"

                    (1..3).forEach { nlevels ->
                        case("$nlevels-level(s) of $name") {
                            input(
                                In(
                                    rootBalance = 1000.DKK,
                                    chainFromRoot = (0 until nlevels).map { Allocation(isProject, 1000.DKK) },
                                    units = 1L
                                )
                            )

                            check { balanceWasDeducted(input, output) }
                        }
                    }
                }

                (3..6).forEach { nlevels ->
                    case("$nlevels-levels of mixed users/projects") {
                        input(
                            In(
                                rootBalance = 1000.DKK,
                                chainFromRoot = (0 until nlevels).map { Allocation(it % 2 == 0, 1000.DKK) },
                                units = 1L
                            )
                        )

                        check { balanceWasDeducted(input, output) }
                    }
                }

                case("negative units") {
                    input(
                        In(
                            rootBalance = 1000.DKK,
                            chainFromRoot = listOf(Allocation(true, 1000.DKK)),
                            units = -1L
                        )
                    )

                    expectStatusCode(HttpStatusCode.BadRequest)
                }

                case("negative numberOfProducts") {
                    input(
                        In(
                            rootBalance = 1000.DKK,
                            chainFromRoot = listOf(Allocation(true, 1000.DKK)),
                            units = 1L,
                            numberOfProducts = -1L
                        )
                    )

                    expectStatusCode(HttpStatusCode.BadRequest)
                }

                case("very large charge") {
                    input(
                        In(
                            rootBalance = 1000.DKK,
                            chainFromRoot = listOf(Allocation(true, 1000.DKK)),
                            units = Int.MAX_VALUE.toLong() * 2
                        )
                    )

                    check { balanceWasDeducted(input, output) }
                }

                case("Charge missing payer") {
                    input(
                        In(
                            rootBalance = 1000.DKK,
                            chainFromRoot = (0 until 3).map { Allocation(true, 1000.DKK) },
                            units = 100L,
                            skipCreationOfLeaf = true
                        )
                    )

                    expectStatusCode(HttpStatusCode.BadRequest)
                }
            }

            test<In, CheckOutput>("Deposit and check") {
                execute {
                    val leaves = prepare(input)
                    CheckOutput(
                        Accounting.check.call(
                            bulkRequestOf(
                                ChargeWalletRequestItem(
                                    leaves.last().owner,
                                    input.units,
                                    input.numberOfProducts,
                                    input.product.toReference(),
                                    leaves.last().username,
                                    "Test charge"
                                )
                            ),
                            serviceClient
                        ).orThrow().responses.single()
                    )
                }

                listOf(true, false).forEach { isProject ->
                    val name = if (isProject) "project" else "user"

                    (1..3).forEach { nlevels ->
                        case("$nlevels-level(s) of $name") {
                            input(
                                In(
                                    rootBalance = 1000.DKK,
                                    chainFromRoot = (0 until nlevels).map { Allocation(isProject, 1000.DKK) },
                                    units = 1L
                                )
                            )

                            check { assertEquals(true, output.hasEnoughCredits) }
                        }
                    }
                }

                (3..6).forEach { nlevels ->
                    case("$nlevels-levels of mixed users/projects") {
                        input(
                            In(
                                rootBalance = 1000.DKK,
                                chainFromRoot = (0 until nlevels).map { Allocation(it % 2 == 0, 1000.DKK) },
                                units = 1L
                            )
                        )

                        check { assertEquals(true, output.hasEnoughCredits) }
                    }
                }

                case("negative units") {
                    input(
                        In(
                            rootBalance = 1000.DKK,
                            chainFromRoot = listOf(Allocation(true, 1000.DKK)),
                            units = -1L
                        )
                    )

                    expectStatusCode(HttpStatusCode.BadRequest)
                }

                case("negative numberOfProducts") {
                    input(
                        In(
                            rootBalance = 1000.DKK,
                            chainFromRoot = listOf(Allocation(true, 1000.DKK)),
                            units = 1L,
                            numberOfProducts = -1L
                        )
                    )

                    expectStatusCode(HttpStatusCode.BadRequest)
                }

                case("very large charge") {
                    input(
                        In(
                            rootBalance = 1000.DKK,
                            chainFromRoot = listOf(Allocation(true, 1000.DKK)),
                            units = Int.MAX_VALUE.toLong() * 2
                        )
                    )

                    check { assertEquals(false, output.hasEnoughCredits) }
                }

                case("Charge missing payer") {
                    input(
                        In(
                            rootBalance = 1000.DKK,
                            chainFromRoot = (0 until 3).map { Allocation(true, 1000.DKK) },
                            units = 100L,
                            skipCreationOfLeaf = true
                        )
                    )

                    expectStatusCode(HttpStatusCode.BadRequest)
                }

                case("Missing payment in a non-leaf") {
                    input(
                        In(
                            rootBalance = 1000.DKK,
                            chainFromRoot = listOf(
                                Allocation(true, 1000.DKK),
                                Allocation(true, 0.DKK),
                                Allocation(true, 1000.DKK)
                            ),
                            units = 100L
                        )
                    )

                    check { assertEquals(false, output.hasEnoughCredits) }
                }
            }
        }

        run {
            class In(
                val chainFromRoot: List<Allocation>,
                val updateIndex: Int,
                val newBalance: Long,
                val newStartDate: Long,
                val newEndDate: Long?,
                val product: Product = sampleCompute,
            )

            class Out(
                val allocationsFromRoot: List<WalletAllocation>,
                val transactionsFromRoot: List<List<Transaction>>,
            )

            test<In, Out>("Update allocations") {
                execute {
                    val leaves = prepareProjectChain(10_000.DKK, input.chainFromRoot, input.product.category)
                    val alloc =
                        findWallet(leaves[input.updateIndex].client, input.product.category)!!.allocations.single()

                    val request = bulkRequestOf(
                        UpdateAllocationRequestItem(
                            alloc.id,
                            input.newBalance,
                            input.newStartDate,
                            input.newEndDate,
                            "Change"
                        )
                    )
                    Accounting.updateAllocation.call(request, leaves[max(0, input.updateIndex - 1)].client).orThrow()
                    assertThatInstance(
                        Accounting.updateAllocation.call(request, leaves.last().client),
                        "Should not be able to update this allocation"
                    ) { it.statusCode.value in 400..499 }

                    Out(
                        leaves.map {
                            findWallet(it.client, input.product.category)!!.allocations.single()
                        },
                        leaves.map {
                            Transactions.browse.call(
                                TransactionsBrowseRequest(
                                    input.product.category.name,
                                    input.product.category.provider
                                ),
                                it.client
                            ).orThrow().items
                        }
                    )
                }

                // NOTE(Dan): We don't store millisecond precision hence this weird calculation
                val initialStartDate = ((Time.now() + (1000 * 60 * 60 * 24 * 7)) / 1000) * 1000

                listOf(true, false).forEach { isProject ->
                    val name = if (isProject) "project" else "user"
                    (2..4).forEach { nlevels ->
                        (1 until nlevels).forEach { updateIdx ->
                            case("${nlevels}-levels of $name (updateIdx = $updateIdx)") {
                                val initialBalance = 1000.DKK
                                val newBalance = 500.DKK

                                input(
                                    In(
                                        (0 until nlevels).map { Allocation(isProject, initialBalance, initialStartDate) },
                                        updateIdx,
                                        newBalance,
                                        initialStartDate,
                                        null
                                    )
                                )

                                check {
                                    output.allocationsFromRoot.forEachIndexed { idx, alloc ->
                                        if (idx == updateIdx) {
                                            assertThatPropertyEquals(alloc, { it.balance }, newBalance, "balance")
                                            assertThatPropertyEquals(alloc, { it.initialBalance }, newBalance,
                                                "initialBalance")
                                        } else {
                                            assertThatPropertyEquals(alloc, { it.balance }, initialBalance, "balance")
                                            assertThatPropertyEquals(alloc, { it.initialBalance }, initialBalance,
                                                "initialBalance")
                                        }
                                    }
                                }

                                check {
                                    output.transactionsFromRoot.forEachIndexed { index, transactions ->
                                        val updates = transactions.filterIsInstance<Transaction.AllocationUpdate>()
                                        if (index != updateIdx) {
                                            assertThatInstance(updates, "should be empty") { it.isEmpty() }
                                        } else {
                                            assertThatInstance(updates, "has only one update") { it.size == 1 }
                                            val update = updates.single()
                                            assertThatPropertyEquals(update, { it.change }, -(initialBalance - newBalance))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                case("update self should fail") {
                    input(
                        In(
                            listOf(Allocation(true, 1000.DKK, initialStartDate)),
                            0,
                            1_000_000.DKK,
                            initialStartDate,
                            null
                        )
                    )

                    expectStatusCode(HttpStatusCode.BadRequest)
                }

                case("update affects children allocation period") {
                    val newEndDate = initialStartDate + (1000 * 60 * 60 * 24)

                    input(
                        In(
                            listOf(
                                Allocation(true, 1000.DKK, initialStartDate),
                                Allocation(true, 1000.DKK, initialStartDate),
                                Allocation(true, 1000.DKK, initialStartDate),
                                Allocation(true, 1000.DKK, initialStartDate),
                            ),
                            1,
                            1000.DKK,
                            initialStartDate,
                            newEndDate
                        )
                    )

                    check {
                        output.allocationsFromRoot.drop(1).forEach { alloc ->
                            assertThatPropertyEquals(alloc, { it.startDate }, initialStartDate, "startDate")
                            assertThatPropertyEquals(alloc, { it.endDate }, newEndDate, "endDate")
                        }

                        output.transactionsFromRoot.forEachIndexed { index, transactions ->
                            if (index >= 1) {
                                val updates = transactions.filterIsInstance<Transaction.AllocationUpdate>()
                                assertThatInstance(updates, "has only one update") { it.size == 1 }
                                val update = updates.single()
                                assertThatPropertyEquals(update, { it.change }, 0)
                                assertThatPropertyEquals(update, { it.startDate }, initialStartDate)
                                assertThatPropertyEquals(update, { it.endDate }, newEndDate)
                            }
                        }
                    }
                }
            }
        }
    }
}
