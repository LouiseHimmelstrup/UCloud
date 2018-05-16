package dk.sdu.cloud.metadata.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.util.*

data class Project(
    val id: String?,
    val fsRoot: String,
    val description: String
)

sealed class ProjectException : RuntimeException() {
    class Duplicate : ProjectException()
}

interface ProjectDAO {
    fun findByFSRoot(path: String): Project?
    fun findById(id: String): Project?
    fun createProject(project: Project): String
    fun findBestMatchingProjectByPath(path: String): Project?
}

class InMemoryProjectDAO : ProjectDAO {
    private val lock = Any()
    private val mapper = jacksonObjectMapper()
    private val diskFile = File("project_dao.json")

    private val dbById: MutableMap<String, Project> = run {
        try {
            mapper.readValue<Map<String, Project>>(diskFile).toMutableMap()
        } catch (_: Exception) {
            HashMap()
        }
    }

    private val dbByRoot = HashMap<String, Project>().apply {
        dbById.forEach { _, v ->
            put(v.fsRoot, v)
        }
    }

    override fun findByFSRoot(path: String): Project? {
        synchronized(lock) {
            return dbByRoot[path]
        }
    }

    override fun findById(id: String): Project? {
        synchronized(lock) {
            return dbById[id]
        }
    }

    override fun createProject(project: Project): String {
        synchronized(lock) {
            val id = UUID.randomUUID().toString()
            if (dbByRoot[project.fsRoot] != null) throw ProjectException.Duplicate()

            val projectWithId = project.copy(id = id)
            dbByRoot[project.fsRoot] = projectWithId
            dbById[id] = projectWithId
            return id
        }
    }

    override fun findBestMatchingProjectByPath(path: String): Project? {
        return synchronized(lock) {
            dbByRoot.values.filter { path.startsWith(it.fsRoot) }.maxBy { it.fsRoot.length }
        }
    }
}

// Maybe this would make sense?
class ProjectService(private val dao: ProjectDAO) : ProjectDAO by dao {

}