package dk.sdu.cloud.notification.services

import dk.sdu.cloud.calls.client.HostInfo
import dk.sdu.cloud.service.db.async.DBContext
import dk.sdu.cloud.service.db.async.SQLTable
import dk.sdu.cloud.service.db.async.allocateId
import dk.sdu.cloud.service.db.async.getField
import dk.sdu.cloud.service.db.async.insert
import dk.sdu.cloud.service.db.async.int
import dk.sdu.cloud.service.db.async.long
import dk.sdu.cloud.service.db.async.sendPreparedStatement
import dk.sdu.cloud.service.db.async.text
import dk.sdu.cloud.service.db.async.timestamp
import dk.sdu.cloud.service.db.async.withSession
import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime
import java.util.*

object SubscriptionsTable : SQLTable("subscriptions") {
    val hostname = text("hostname", notNull = true)
    val port = int("port", notNull = true)
    val username = text("username", notNull = true)
    val lastPing = timestamp("last_ping", notNull = true)
    val id = long("id")
}

class SubscriptionHibernateDao : SubscriptionDao {
    override suspend fun open(ctx: DBContext, username: String, hostname: String, port: Int): Long {
        val id = ctx.withSession { it.allocateId() }
        ctx.withSession { session ->
            session.insert(SubscriptionsTable) {
                set(SubscriptionsTable.hostname, hostname)
                set(SubscriptionsTable.port, port)
                set(SubscriptionsTable.username, username)
                set(SubscriptionsTable.lastPing, LocalDateTime.now(DateTimeZone.UTC))
            }
        }
        return id
    }

    override suspend fun close(ctx: DBContext, id: Long) {
        ctx.withSession { session ->
            session.sendPreparedStatement(
                {
                    setParameter("id", id)
                },
                """
                    DELETE FROM subscriptions
                    WHERE id = ?id
                """.trimIndent()
            )
        }
    }

    override suspend fun findConnections(ctx: DBContext, username: String): List<Subscription> {
        val earliestAllowedPing = Date(System.currentTimeMillis() - SubscriptionService.MAX_MS_SINCE_LAST_PING).time
        return ctx.withSession { session ->
            session.sendPreparedStatement(
                {
                    setParameter("username", username)
                    setParameter("earliest", earliestAllowedPing)
                },
                """
                    FROM subscriptions
                    WHERE (username = ?username) AND (last_ping >= to_timestamp(?earliest))
                """.trimIndent()
            ).rows.map {
                Subscription(
                    HostInfo(
                        host = it.getField(SubscriptionsTable.hostname),
                        port = it.getField(SubscriptionsTable.port)
                    ),
                    it.getField(SubscriptionsTable.username),
                    it.getField(SubscriptionsTable.id
                    )
                ) }
        }

    }

    override suspend fun refreshSessions(ctx: DBContext, hostname: String, port: Int) {
        ctx.withSession { session ->
            session.sendPreparedStatement(
                {
                    setParameter("newPing", Date().time)
                    setParameter("hostname", hostname)
                    setParameter("port", port)
                },
                """
                    UPDATE subscriptions
                    SET last_ping = to_timestamp(?newPing)
                    WHERE (hostname = ?hostname) AND (port = ?port)
                """.trimIndent()
            )
        }
    }
}
