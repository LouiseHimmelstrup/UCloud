package dk.sdu.cloud.accounting.services.grants

import dk.sdu.cloud.ActorAndProject
import dk.sdu.cloud.calls.RPCException
import dk.sdu.cloud.grant.api.ReadTemplatesResponse
import dk.sdu.cloud.grant.api.UploadTemplatesRequest
import dk.sdu.cloud.safeUsername
import dk.sdu.cloud.service.db.async.*
import io.ktor.http.HttpStatusCode

class GrantTemplateService(
    private val db: DBContext,
) {
    suspend fun uploadTemplates(
        actorAndProject: ActorAndProject,
        templates: UploadTemplatesRequest
    ) {
        db.withSession(remapExceptions = true) { session ->
            session.sendPreparedStatement(
                {
                    setParameter("username", actorAndProject.actor.safeUsername())
                    setParameter("projectId", actorAndProject.project)
                    setParameter("personalProject", templates.personalProject)
                    setParameter("existingProject", templates.existingProject)
                    setParameter("newProject", templates.newProject)
                },

                """
                    insert into "grant".templates (project_id, personal_project, existing_project, new_project) 
                    select :projectId, :personalProject, :existingProject, :newProject
                    from project.project_members pm
                    where
                        pm.username = :username and
                        pm.project_id = :projectId and
                        (pm.role = 'ADMIN' or pm.role = 'PI')
                    on conflict (project_id) do update set 
                        personal_project = excluded.personal_project,
                        existing_project = excluded.existing_project,
                        new_project = excluded.new_project
                """
            )
        }
    }

    suspend fun fetchTemplates(
       actorAndProject: ActorAndProject,
       projectId: String
    ): ReadTemplatesResponse {
        return db.withSession(remapExceptions = true) { session ->
            session.sendPreparedStatement(
                {
                    setParameter("username", actorAndProject.actor.safeUsername())
                    setParameter("active_project", actorAndProject.project)
                    setParameter("project_id", projectId)
                },
                """
                    select
                        t.personal_project,
                        t.new_project,
                        t.existing_project
                    from
                        "grant".templates t
                    where
                        t.project_id = :project_id and
                        "grant".can_submit_application(
                            :username,
                            :project_id,
                            case
                                when :active_project::text is null then :username
                                else :active_project::text
                            end,
                            case
                                when :active_project::text is null then 'personal'
                                else 'existing_project'
                            end
                        )
                """
            ).rows.map { ReadTemplatesResponse(it.getString(0)!!, it.getString(1)!!, it.getString(2)!!) }.singleOrNull()
                ?: throw RPCException("Could not find any templates", HttpStatusCode.NotFound)
        }
    }
}
