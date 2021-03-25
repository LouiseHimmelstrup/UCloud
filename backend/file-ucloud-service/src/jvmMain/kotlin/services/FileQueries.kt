package dk.sdu.cloud.file.ucloud.services

import dk.sdu.cloud.Actor
import dk.sdu.cloud.PageV2
import dk.sdu.cloud.PaginationRequestV2
import dk.sdu.cloud.accounting.api.ProductReference
import dk.sdu.cloud.accounting.api.UCLOUD_PROVIDER
import dk.sdu.cloud.file.orchestrator.api.*
import dk.sdu.cloud.file.ucloud.services.acl.AclService

interface PathLike<T> {
    val path: String
    fun withNewPath(path: String): T
}

inline class InternalFile(override val path: String) : PathLike<InternalFile> {
    override fun withNewPath(path: String): InternalFile = InternalFile(path)
}

inline class UCloudFile(override val path: String) : PathLike<UCloudFile> {
    override fun withNewPath(path: String): UCloudFile = UCloudFile(path)
}

fun <T : PathLike<T>> T.parent(): T = withNewPath(path.parent())
fun <T : PathLike<T>> T.parents(): List<T> = path.parents().map { withNewPath(it) }
fun <T : PathLike<T>> T.normalize(): T = withNewPath(path.normalize())
fun <T : PathLike<T>> T.components(): List<String> = path.components()
fun <T : PathLike<T>> T.fileName(): String = path.fileName()

class FileQueries(
    private val aclService: AclService,
    private val rootDirectory: InternalFile,
) {
    fun convertUCloudPathToInternalFile(file: UCloudFile): InternalFile {
        // TODO Deal with backwards-compatible paths
        val withoutMetadata = file.normalize().components().drop(3)
        if (withoutMetadata.isEmpty()) {
            throw FSException.NotFound()
        }

        val collection = withoutMetadata[0]
        if (collection.startsWith(COLLECTION_HOME_PREFIX)) {
            return InternalFile(
                buildString {
                    append(rootDirectory.path)
                    append('/')
                    append(HOME_DIRECTORY)
                    append('/')
                    append(collection.removePrefix(COLLECTION_HOME_PREFIX))
                    for ((idx, component) in withoutMetadata.withIndex()) {
                        if (idx == 0) continue
                        append('/')
                        append(component)
                    }
                }
            )
        } else if (collection.startsWith(COLLECTION_PROJECT_PREFIX)) {
            val withoutPrefix = collection.removePrefix(COLLECTION_PROJECT_PREFIX)
            val splitterIdx = withoutPrefix.indexOfLast { it == '-' }
            if (splitterIdx == -1) throw FSException.NotFound()
            if (splitterIdx == withoutPrefix.length) throw FSException.NotFound()
            val projectId = withoutPrefix.substring(0, splitterIdx)
            val repository = withoutPrefix.substring(splitterIdx + 1)

            return InternalFile(
                buildString {
                    append(rootDirectory.path)
                    append('/')
                    append(PROJECT_DIRECTORY)
                    append('/')
                    append(projectId)
                    append('/')
                    append(repository)
                    append('/')
                    for ((idx, component) in withoutMetadata.withIndex()) {
                        if (idx == 0) continue
                        append('/')
                        append(component)
                    }
                }
            )
        } else {
            throw FSException.NotFound()
        }
    }

    fun convertInternalFileToUCloudPath(file: InternalFile): UCloudFile {
        val components = file.path.removePrefix(rootDirectory.path).normalize().components()
        if (components.size <= 1) throw FSException.CriticalException("Not a valid UCloud file")

        return UCloudFile(
            buildString {
                append('/')
                append(PRODUCT_REFERENCE.provider)
                append('/')
                append(PRODUCT_REFERENCE.category)
                append('/')
                append(PRODUCT_REFERENCE.id)
                append('/')

                val startIdx: Int
                when (components[0]) {
                    HOME_DIRECTORY -> {
                        append(COLLECTION_HOME_PREFIX)
                        append(components[1])
                        startIdx = 2
                    }

                    PROJECT_DIRECTORY -> {
                        if (components.size <= 2) throw FSException.CriticalException("Not a valid UCloud file")

                        append(COLLECTION_PROJECT_PREFIX)
                        append(components[1])
                        append(components[2])
                        startIdx = 3
                    }

                    else -> throw FSException.CriticalException("Not a valid UCloud file")
                }

                for ((idx, component) in components.withIndex()) {
                    if (idx < startIdx) continue
                    append('/')
                    append(component)
                }
            }
        )
    }

    fun retrieve(actor: Actor, file: UCloudFile, flags: FilesIncludeFlags): UFile {
        TODO()
    }

    fun retrieveTypeInternal(actor: Actor, file: InternalFile): FileType {
        TODO()
    }

    fun retrieveInternal(actor: Actor, file: InternalFile, flags: FilesIncludeFlags): UFile {
        TODO()
    }

    fun fileExists(actor: Actor, file: UCloudFile): Boolean {
        TODO()
    }

    fun fileExistsInternal(actor: Actor, file: InternalFile): Boolean {
        TODO()
    }

    fun listInternalFilesOrNull(actor: Actor, file: InternalFile): List<InternalFile>? {
        TODO()
    }

    fun browseFiles(
        actor: Actor,
        path: String,
        flags: FilesIncludeFlags,
        pagination: PaginationRequestV2,
    ): PageV2<UFile> {
        TODO()
    }

    fun renameAccordingToPolicy(desiredPath: String, policy: WriteConflictPolicy): String {
        TODO()
    }

    companion object {
        const val COLLECTION_HOME_PREFIX = "h-"
        const val COLLECTION_PROJECT_PREFIX = "p-"
        const val HOME_DIRECTORY = "home"
        const val PROJECT_DIRECTORY = "projects"

        val PRODUCT_REFERENCE = ProductReference("cephfs", "cephfs", UCLOUD_PROVIDER)
    }
}
