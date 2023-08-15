package dk.sdu.cloud.app.store.services

import dk.sdu.cloud.calls.HttpStatusCode
import dk.sdu.cloud.calls.RPCException
import dk.sdu.cloud.service.db.async.DBContext
import dk.sdu.cloud.service.db.async.sendPreparedStatement
import dk.sdu.cloud.service.db.async.withSession

class ApplicationTagsService (
    private val db: DBContext,
    private val elasticDao: ElasticDao?
) {
    suspend fun createTags(tags: List<String>, applicationName: String) {
        db.withSession { session ->
            findOwnerOfApplication(session, applicationName) ?: throw RPCException.fromStatusCode(
                HttpStatusCode.NotFound
            )

            session.sendPreparedStatement(
                {
                    setParameter("tags", tags)
                },
                """
                    insert into tags (tag)
                        select unnest(:tags::text[])
                        on conflict do nothing
                """
            )

            session.sendPreparedStatement(
                {
                    setParameter("application_name", applicationName)
                    setParameter("tags", tags)
                },
                """
                    insert into application_tags (application_name, tag_id)
                        select :application_name, id
                        from tags where
                            lower(tag) in (select lower(unnest(:tags::text[])))
                        on conflict do nothing 
                """
            )
        }

        elasticDao?.addTagToElastic(applicationName, tags)

    }

    suspend fun deleteTags(tags: List<String>, applicationName: String) {
        db.withSession { session ->
            findOwnerOfApplication(session, applicationName) ?: throw RPCException.fromStatusCode(
                HttpStatusCode.NotFound
            )

            session.sendPreparedStatement(
                {
                    setParameter("app_name", applicationName)
                    setParameter("tags", tags)
                },
                """
                delete from application_tags
                    where application_name = :app_name and
                    tag_id in (
                        select id from tags where lower(tag) in (select lower(unnest(:tags::text[])))
                    )
            """
            )
        }
        elasticDao?.removeTagFromElastic(applicationName, tags)
    }

    suspend fun listTags(): List<String> {
        return db.withSession { session ->
            session.sendPreparedStatement(
                """
                    select tag from tags
                """
            ).rows.mapNotNull {
                it.getString(0)
            }
        }
    }

    suspend fun findTagsForApp(
        ctx: DBContext,
        applicationName: String
    ): List<String> {
        return ctx.withSession { session ->
            session.sendPreparedStatement(
                {
                    setParameter("app_name", applicationName)
                },
                """
                    select t.tag
                    from application_tags
                    inner join tags t on t.id = application_tags.tag_id
                    where application_name = :app_name and tag_id 
                    
                """
            ).rows.mapNotNull {
                it.getString(0)
            }
        }
    }
}
