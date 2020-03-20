package dk.sdu.cloud.project.services

import com.github.jasync.sql.db.RowData
import dk.sdu.cloud.project.api.Project
import dk.sdu.cloud.project.api.ProjectMember
import dk.sdu.cloud.project.api.ProjectRole
import dk.sdu.cloud.project.api.UserProjectSummary
import dk.sdu.cloud.service.NormalizedPaginationRequest
import dk.sdu.cloud.service.Page
import dk.sdu.cloud.service.db.async.AsyncDBConnection
import dk.sdu.cloud.service.db.async.SQLTable
import dk.sdu.cloud.service.db.async.getField
import dk.sdu.cloud.service.db.async.insert
import dk.sdu.cloud.service.db.async.long
import dk.sdu.cloud.service.db.async.sendPreparedStatement
import dk.sdu.cloud.service.db.async.text
import dk.sdu.cloud.service.db.async.timestamp
import org.joda.time.LocalDateTime

class ProjectAsyncDao : ProjectDao<AsyncDBConnection> {
    override suspend fun create(session: AsyncDBConnection, id: String, title: String, principalInvestigator: String) {
        session.insert(ProjectTable) {
            set(ProjectTable.id, id)
            set(ProjectTable.title, title)
            set(ProjectTable.createdAt, LocalDateTime.now())
            set(ProjectTable.modifiedAt, LocalDateTime.now())
        }

        session.insert(ProjectMemberTable) {
            set(ProjectMemberTable.username, principalInvestigator)
            set(ProjectMemberTable.role, ProjectRole.PI.name)
            set(ProjectMemberTable.project, id)
            set(ProjectMemberTable.createdAt, LocalDateTime.now())
            set(ProjectMemberTable.modifiedAt, LocalDateTime.now())
        }
    }

    override suspend fun delete(session: AsyncDBConnection, id: String) {
        session
            .sendPreparedStatement(
                {
                    setParameter("project", id)
                },
                """
                    delete from project_members
                    where project = ?project
                """
            )

        session
            .sendPreparedStatement(
                {
                    setParameter("project", id)
                },
                """
                    delete from projects  
                    where id = ?project
                """
            )
    }

    override suspend fun findById(session: AsyncDBConnection, projectId: String): Project {
        val members = session
            .sendPreparedStatement(
                { setParameter("project", projectId) },
                "select * from project_members where project_id = ?project"
            )
            .rows
            .map { it.toProjectMember() }

        return session
            .sendPreparedStatement(
                { setParameter("project", projectId) },
                "select * from projects where id = ?project"
            )
            .rows
            .singleOrNull()
            ?.toProject(members)
            ?: throw ProjectException.NotFound()
    }

    override suspend fun findByIdPrefix(session: AsyncDBConnection, prefix: String): List<String> {
        return session
            .sendPreparedStatement(
                { setParameter("project", "$prefix%") },
                "select id from projects where id like ?project"
            )
            .rows
            .map { it.getString(0)!! }
    }

    override suspend fun addMember(session: AsyncDBConnection, projectId: String, member: ProjectMember) {
        session.insert(ProjectMemberTable) {
            set(ProjectMemberTable.username, member.username)
            set(ProjectMemberTable.role, member.role.name)
            set(ProjectMemberTable.project, projectId)
            set(ProjectMemberTable.createdAt, LocalDateTime.now())
            set(ProjectMemberTable.modifiedAt, LocalDateTime.now())
        }
    }

    override suspend fun deleteMember(session: AsyncDBConnection, projectId: String, member: String) {
        session
            .sendPreparedStatement(
                {
                    setParameter("project", projectId)
                    setParameter("member", member)
                },
                """
                    delete from project_members
                    where project_id = ?project and username = ?member
                """
            )
    }

    override suspend fun changeMemberRole(
        session: AsyncDBConnection,
        projectId: String,
        member: String,
        newRole: ProjectRole
    ) {
        session
            .sendPreparedStatement(
                {
                    setParameter("role", newRole.name)
                    setParameter("username", member)
                    setParameter("project", projectId)
                },
                """
                    update project_members  
                    set
                        modified_at = now(),
                        role = ?role
                    where
                        username = ?username and
                        project_id = ?project
                """
            )
    }

    override suspend fun findRoleOfMember(session: AsyncDBConnection, projectId: String, member: String): ProjectRole? {
        return session
            .sendPreparedStatement(
                {
                    setParameter("username", member)
                    setParameter("project", projectId)
                },
                """
                    select role
                    from project_members
                    where 
                        username = ?username and
                        project_id = ?project
                """
            )
            .rows
            .map { ProjectRole.valueOf(it.getString(0)!!) }
            .singleOrNull()
    }

    override suspend fun listProjectsForUser(
        session: AsyncDBConnection,
        pagination: NormalizedPaginationRequest?,
        user: String
    ): Page<UserProjectSummary> {
        val items = session
            .sendPreparedStatement(
                {
                    setParameter("username", user)
                    setParameter("offset", if (pagination == null) 0 else pagination.page * pagination.itemsPerPage)
                    setParameter("limit", pagination?.itemsPerPage ?: Int.MAX_VALUE)
                },
                """
                    select mem.role, p.id, p.title  
                    from 
                        project_members mem inner join projects p on mem.project_id = p.id
                    where mem.username = ?username
                    order by p.id
                    offset ?offset
                    limit ?limit
                """
            )
            .rows
            .map {
                val role = ProjectRole.valueOf(it.getString(0)!!)
                val id = it.getString(1)!!
                val title = it.getString(2)!!

                UserProjectSummary(id, title, ProjectMember(user, role))
            }

        val count = if (pagination == null) {
            items.size
        } else {
            session
                .sendPreparedStatement(
                    { setParameter("username", user) },
                    """
                        select count(*)
                        from project_members
                        where username = ?username
                    """
                )
                .rows
                .map { it.getLong(0)!!.toInt() }
                .singleOrNull() ?: items.size
        }

        return Page(count, pagination?.itemsPerPage ?: count, pagination?.page ?: 0, items)
    }

    private object ProjectTable : SQLTable("projects") {
        val id = text("id")
        val title = text("title")
        val createdAt = timestamp("created_at")
        val modifiedAt = timestamp("modified_at")
    }

    private fun RowData.toProject(members: List<ProjectMember>): Project = Project(
        getField(ProjectTable.id),
        getField(ProjectTable.title),
        members
    )

    private object ProjectMemberTable : SQLTable("project_members") {
        val username = text("username")
        val role = text("role")
        val project = text("project_id")
        val id = long("id")
        val createdAt = timestamp("created_at")
        val modifiedAt = timestamp("modified_at")
    }

    private fun RowData.toProjectMember(): ProjectMember = ProjectMember(
        getField(ProjectMemberTable.username),
        ProjectRole.valueOf(getField(ProjectMemberTable.role))
    )
}
