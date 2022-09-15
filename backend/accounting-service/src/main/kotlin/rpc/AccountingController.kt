package dk.sdu.cloud.accounting.rpc

import dk.sdu.cloud.Role
import dk.sdu.cloud.accounting.api.*
import dk.sdu.cloud.accounting.services.wallets.AccountingProcessor
import dk.sdu.cloud.accounting.services.wallets.AccountingService
import dk.sdu.cloud.accounting.services.wallets.DepositNotificationService
import dk.sdu.cloud.calls.CallDescription
import dk.sdu.cloud.calls.client.*
import dk.sdu.cloud.calls.server.CallHandler
import dk.sdu.cloud.calls.server.RpcServer
import dk.sdu.cloud.calls.server.project
import dk.sdu.cloud.calls.server.securityPrincipal
import dk.sdu.cloud.service.Controller
import dk.sdu.cloud.service.Time
import dk.sdu.cloud.service.actorAndProject

class AccountingController(
    private val accounting: AccountingService,
    private val notifications: DepositNotificationService,
    private val client: AuthenticatedClient,
) : Controller {
    private fun <R : Any, S : Any, E : Any> RpcServer.implementOrDispatch(
        call: CallDescription<R, S, E>,
        handler: suspend CallHandler<R, S, E>.() -> Unit,
    ) {
        implement(call) {
            val activeProcessor = accounting.retriveActiveProcessorAddress()
            if (activeProcessor == null) {
                handler()
            } else {
                ok(
                    call.call(
                        request,
                        client.withFixedHost(HostInfo(activeProcessor, "http", 8080))
                    ).orThrow()
                )
            }
        }
    }

    override fun configure(rpcServer: RpcServer) = with(rpcServer) {
        implementOrDispatch(Accounting.charge) {
            ok(accounting.charge(actorAndProject, request))
        }

        implementOrDispatch(Accounting.deposit) {
            val user = ctx.securityPrincipal.username
            request.items.forEach { req ->
                req.transactionId = "${user}-${req.transactionId}"
                req.startDate = req.startDate ?: Time.now()
            }
            ok(accounting.deposit(actorAndProject, request))
        }

        implementOrDispatch(Accounting.check) {
            ok(accounting.check(actorAndProject, request))
        }

        implementOrDispatch(Accounting.updateAllocation) {
            val user = ctx.securityPrincipal.username
            request.items.forEach { req ->
                req.transactionId = "${user}-${req.transactionId}"
            }
            ok(accounting.updateAllocation(actorAndProject, request))
        }

        implementOrDispatch(Accounting.rootDeposit) {
            val user = ctx.securityPrincipal.username
            request.items.forEach { req ->
                req.transactionId = "${user}-${req.transactionId}"
                req.startDate = req.startDate ?: Time.now()
            }
            ok(accounting.rootDeposit(actorAndProject, request))
        }

        implement(Wallets.browse) {
            ok(accounting.browseWallets(actorAndProject, request))
        }

        implementOrDispatch(Wallets.retrieveWalletsInternal) {
            val walletOwner = request.owner

            ok(WalletsInternalRetrieveResponse(accounting.retrieveWalletsInternal(actorAndProject, walletOwner)))
        }

        implementOrDispatch(Wallets.retrieveAllocationsInternal) {
            val walletOwner = request.owner
            ok(
                WalletAllocationsInternalRetrieveResponse(
                    accounting.retrieveAllocationsInternal(
                        actorAndProject,
                        walletOwner,
                        request.categoryId
                    )
                )
            )
        }

        implement(Wallets.searchSubAllocations) {
            //TODO()
            ok(accounting.browseSubAllocations(actorAndProject, request, request.query))
        }

        implement(Wallets.browseSubAllocations) {
            //TODO() Fix update 30 sek delay?
            ok(accounting.browseSubAllocations(actorAndProject, request))
        }

        implement(Wallets.retrieveRecipient) {
            ok(accounting.retrieveRecipient(actorAndProject, request))
        }

        implement(Wallets.register) {
            ok(accounting.register(actorAndProject, request))
        }

        implement(Wallets.retrieveProviderSummary) {
            ok(accounting.retrieveProviderSummary(actorAndProject, request))
        }

        implement(Visualization.retrieveUsage) {
            ok(accounting.retrieveUsage(actorAndProject, request))
        }

        implement(Visualization.retrieveBreakdown) {
            ok(accounting.retrieveBreakdown(actorAndProject, request))
        }

        implement(Transactions.browse) {
            ok(accounting.browseTransactions(actorAndProject, request))
        }

        implement(DepositNotifications.retrieve) {
            ok(notifications.retrieveNotifications(actorAndProject))
        }

        implement(DepositNotifications.markAsRead) {
            notifications.markAsRead(actorAndProject, request)
            ok(Unit)
        }

        return@with
    }
}
