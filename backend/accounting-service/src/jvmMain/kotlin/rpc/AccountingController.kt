package dk.sdu.cloud.accounting.rpc

import dk.sdu.cloud.accounting.api.Accounting
import dk.sdu.cloud.accounting.api.DepositNotifications
import dk.sdu.cloud.accounting.api.Transactions
import dk.sdu.cloud.accounting.api.Visualization
import dk.sdu.cloud.accounting.api.Wallets
import dk.sdu.cloud.accounting.services.wallets.AccountingService
import dk.sdu.cloud.accounting.services.wallets.DepositNotificationService
import dk.sdu.cloud.calls.server.RpcServer
import dk.sdu.cloud.calls.server.securityPrincipal
import dk.sdu.cloud.service.Controller
import dk.sdu.cloud.service.actorAndProject
import org.elasticsearch.action.bulk.BulkRequest

class AccountingController(
    private val accounting: AccountingService,
    private val notifications: DepositNotificationService,
) : Controller {
    override fun configure(rpcServer: RpcServer) = with(rpcServer) {
        implement(Accounting.charge) {
            ok(accounting.charge(actorAndProject, request))
        }

        implement(Accounting.deposit) {
            val user = ctx.securityPrincipal.username
            request.items.forEach { req ->
                req.transactionId = "${user}-${req.transactionId}"
            }
            ok(accounting.deposit(actorAndProject, request))
        }

        implement(Accounting.check) {
            ok(accounting.check(actorAndProject, request))
        }

        implement(Accounting.transfer) {
            val user = ctx.securityPrincipal.username
            request.items.forEach { req ->
                req.transactionId = "${user}-${req.transactionId}"
            }
            ok(accounting.transfer(actorAndProject, request))
        }

        implement(Accounting.updateAllocation) {
            val user = ctx.securityPrincipal.username
            request.items.forEach { req ->
                req.transactionId = "${user}-${req.transactionId}"
            }
            ok(accounting.updateAllocation(actorAndProject, request))
        }

        implement(Accounting.rootDeposit) {
            val user = ctx.securityPrincipal.username
            request.items.forEach { req ->
                req.transactionId = "${user}-${req.transactionId}"
            }
            ok(accounting.rootDeposit(actorAndProject, request))
        }

        implement(Wallets.browse) {
            ok(accounting.browseWallets(actorAndProject, request))
        }

        implement(Wallets.browseSubAllocations) {
            ok(accounting.browseSubAllocations(actorAndProject, request))
        }

        implement(Wallets.retrieveRecipient) {
            ok(accounting.retrieveRecipient(actorAndProject, request))
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
