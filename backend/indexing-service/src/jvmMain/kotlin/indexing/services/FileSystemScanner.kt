package dk.sdu.cloud.indexing.services

import dk.sdu.cloud.*
import dk.sdu.cloud.file.api.*
import dk.sdu.cloud.indexing.api.*
import dk.sdu.cloud.indexing.util.*
import dk.sdu.cloud.service.*
import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.elasticsearch.*
import org.elasticsearch.action.bulk.*
import org.elasticsearch.action.delete.*
import org.elasticsearch.action.get.*
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.update.*
import org.elasticsearch.client.*
import org.elasticsearch.common.xcontent.*
import org.elasticsearch.index.query.*
import org.elasticsearch.index.reindex.DeleteByQueryRequest
import org.elasticsearch.search.builder.SearchSourceBuilder
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.*
import kotlin.math.*

const val DOC_TYPE = "_doc"

@Suppress("BlockingMethodInNonBlockingContext")
class FileSystemScanner(
    private val elastic: RestHighLevelClient,
    private val query: ElasticQueryService,
    private val cephFsRoot: String,
    private val stats: FastDirectoryStats?
) {
    private val pool = Executors.newFixedThreadPool(16).asCoroutineDispatcher()

    private fun updateDocWithNewFile(file: ElasticIndexedFile): UpdateRequest {
        return UpdateRequest(FILES_INDEX, DOC_TYPE, file.path).apply {
            val writeValueAsBytes = defaultMapper.encodeToString(file).encodeToByteArray()
            doc(writeValueAsBytes, XContentType.JSON)
            docAsUpsert(true)
        }
    }

    private fun deleteDocWithFile(cloudPath: String): DeleteRequest {
        return DeleteRequest(FILES_INDEX, DOC_TYPE, cloudPath)
    }

    suspend fun runScan() {
        withContext(pool) {
            launch {
                submitScan(File(cephFsRoot, "home").absoluteFile)
            }.join()
            launch {
                submitScan(File(cephFsRoot, "projects").absoluteFile)
            }.join()
        }
    }

    inner class BulkRequestBuilder {
        private var bulkCount = 0
        private var bulk = BulkRequest()

        fun flush() {
            if (bulkCount > 0) {
                elastic.bulk(bulk, RequestOptions.DEFAULT)
                bulkCount = 0
                bulk = BulkRequest()
            }
        }

        private fun flushIfNeeded() {
            if (bulkCount >= 100) {
                flush()
            }
        }

        fun add(updateRequest: UpdateRequest) {
            bulkCount++
            bulk.add(updateRequest)
            flushIfNeeded()
        }

        fun add(deleteRequest: DeleteRequest) {
            bulkCount++
            bulk.add(deleteRequest)
            flushIfNeeded()
        }

    }

    private suspend fun submitScan(path: File, upperLimitOfEntries: Long = Long.MAX_VALUE) {
        log.debug("Scanning: ${path.toCloudPath()} (${path})")

        val thisFileInIndex = run {
            val source =
                elastic.get(GetRequest(FILES_INDEX, DOC_TYPE, path.toCloudPath()), RequestOptions.DEFAULT)?.sourceAsString
            if (source != null) {
                defaultMapper.decodeFromString<ElasticIndexedFile>(source)
            } else {
                null
            }
        }

        var newUpperLimitOfEntries = 1L
        if (path.isDirectory) {
            val rctime = runCatching { stats?.getRecursiveTime(path.absolutePath) }.getOrNull()
            val (shouldContinue, limit) = shouldContinue(path, upperLimitOfEntries)
            newUpperLimitOfEntries = limit

            // We must continue if rctime does not match ceph
            if (rctime != null && thisFileInIndex != null && thisFileInIndex.rctime == rctime && !shouldContinue) {
                log.debug("${path.toCloudPath()} already up-to-date")
                return
            }
        }

        val fileList = (path.listFiles() ?: emptyArray()).filter { !Files.isSymbolicLink(Path.of(it.path)) }
        val files = fileList.map { it.toElasticIndexedFile() }.associateBy { it.path }
        val filesInIndex = query.query(
            FileQuery(
                listOf(path.toCloudPath()),
                fileDepth = AnyOf.with(
                    Comparison(path.toCloudPath().depth() + 1, ComparisonOperator.EQUALS)
                )
            )
        ).items.associateBy { it.path }

        val bulk = BulkRequestBuilder()
        filesInIndex.values.asSequence()
            .filter { it.path !in files }
            .forEach {
                bulk.add(deleteDocWithFile(it.path))
                val searchRequest = SearchRequest(FILES_INDEX)
                val query = SearchSourceBuilder().query(
                    QueryBuilders.wildcardQuery(
                        "_id",
                        "${it.path}/*"
                    )
                ).size(100)
                searchRequest.source(query)
                val queryDeleteRequest = DeleteByQueryRequest(searchRequest)
                queryDeleteRequest.setConflicts("proceed")
                try {
                    //We only delete 100 at a time to reduce stress. Redo until all matching search is deleted
                    var moreToDelete = true
                    while (moreToDelete) {
                        val response = elastic.deleteByQuery(queryDeleteRequest, RequestOptions.DEFAULT)
                        if (response.deleted == 0L) moreToDelete = false
                    }
                } catch (ex: ElasticsearchException) {
                    log.warn("Deletion of ${it.path}/* , failed")
                }
            }

        files.values.asSequence()
            .filter { it.path !in filesInIndex }
            .forEach { bulk.add(updateDocWithNewFile(it)) }

        if (thisFileInIndex == null) {
            bulk.add(updateDocWithNewFile(path.toElasticIndexedFile()))
        }

        bulk.flush()

        fileList.mapNotNull { file ->
            withContext(pool) {
                if (file.isDirectory) {
                    launch {
                        submitScan(file, newUpperLimitOfEntries)
                    }
                } else {
                    null
                }
            }
        }.joinAll()
    }

    private fun File.toElasticIndexedFile(): ElasticIndexedFile {
        return ElasticIndexedFile(
            toCloudPath(),
            length(),
            if (isDirectory) FileType.DIRECTORY else FileType.FILE,
            runCatching { stats?.getRecursiveTime(absolutePath) }.getOrNull()
        )
    }

    data class ShouldContinue(val shouldContinue: Boolean, val newUpperLimitOfEntries: Long)

    private fun shouldContinue(path: File, upperLimitOfEntries: Long): ShouldContinue {
        if (path.isFile) return ShouldContinue(true, 1)
        if (stats == null) return ShouldContinue(true, upperLimitOfEntries)
        if (upperLimitOfEntries < 100) return ShouldContinue(true, upperLimitOfEntries)
        val recursiveEntryCount = stats.getRecursiveEntryCount(path.absolutePath)
        if (recursiveEntryCount > upperLimitOfEntries) return ShouldContinue(true, recursiveEntryCount)

        val fileStats = query.statisticsQuery(
            StatisticsRequest(
                FileQuery(listOf(path.toCloudPath())),
                size = NumericStatisticsRequest(calculateSum = true)
            )
        )

        val fileCount = query.statisticsQuery(
            StatisticsRequest(
                FileQuery(
                    listOf(path.toCloudPath()),
                    fileTypes = AnyOf.with(FileType.FILE)
                )
            )
        )

        val dirCount = query.statisticsQuery(
            StatisticsRequest(
                FileQuery(
                    listOf(path.toCloudPath()),
                    fileTypes = AnyOf.with(FileType.DIRECTORY)
                )
            )
        )

        val size = fileStats.size!!
        val recursiveFiles = fileCount.count
        val recursiveSubDirs = dirCount.count

        if (recursiveEntryCount != recursiveFiles + recursiveSubDirs) {
            log.info("Entry count is different ($recursiveEntryCount != $recursiveFiles + $recursiveSubDirs)")
            return ShouldContinue(true, recursiveEntryCount)
        }

        val actualRecursiveSize = stats.getRecursiveSize(path.absolutePath)
        val sum = size.sum
        if (sum!!.toLong() != actualRecursiveSize) {
            val percentage = if (sum.toLong() == 0L) {
                1.0
            } else {
                1 - (actualRecursiveSize / sum)
            }

            if (percentage >= abs(0.05)) {
                log.info("Size is different $actualRecursiveSize != $sum")
                return ShouldContinue(true, recursiveEntryCount)
            }
        }

        val actualRecursiveFiles = stats.getRecursiveFileCount(path.absolutePath)
        val actualRecursiveSubDirs = stats.getRecursiveDirectoryCount(path.absolutePath)
        if (recursiveSubDirs != actualRecursiveSubDirs) {
            log.info("Sub dirs is different ${recursiveSubDirs} ${actualRecursiveSubDirs}")
            return ShouldContinue(true, recursiveEntryCount)
        }

        if (recursiveFiles != actualRecursiveFiles) {
            log.info("Recursive files is different $recursiveFiles $actualRecursiveFiles")
            return ShouldContinue(true, recursiveEntryCount)
        }

        log.info("Skipping $path ($recursiveEntryCount entries has been skipped)")
        return ShouldContinue(false, recursiveEntryCount)
    }

    private fun File.toCloudPath(): String {
        return "/" + absolutePath.normalize().removePrefix(cephFsRoot).removePrefix("/")
    }

    companion object : Loggable {
        override val log = logger()
        internal const val FILES_INDEX = "files"
    }
}
