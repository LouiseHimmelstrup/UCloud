package dk.sdu.cloud.activity.services

import dk.sdu.cloud.activity.api.ActivityEvent
import dk.sdu.cloud.activity.api.ActivityFilter
import dk.sdu.cloud.activity.api.ActivityForFrontend
import dk.sdu.cloud.activity.api.type
import dk.sdu.cloud.file.api.path
import dk.sdu.cloud.service.Loggable
import dk.sdu.cloud.service.NormalizedPaginationRequest
import dk.sdu.cloud.service.NormalizedScrollRequest
import dk.sdu.cloud.service.Page
import dk.sdu.cloud.service.ScrollResult

class ActivityService(
    private val activityEventElasticDao: ActivityEventElasticDao,
    private val fileLookupService: FileLookupService
) {

    suspend fun findEventsForPath(
        pagination: NormalizedPaginationRequest,
        path: String,
        userAccessToken: String,
        user: String,
        causedBy: String? = null
    ): Page<ActivityForFrontend> {
        val fileStat = fileLookupService.lookupFile(path, userAccessToken, user, causedBy)
        return activityEventElasticDao.findByFilePath(pagination, fileStat.path)
    }

    fun browseForUser(
        scroll: NormalizedScrollRequest<Int>,
        user: String,
        userFilter: ActivityFilter? = null
    ): ScrollResult<ActivityForFrontend, Int> {
        val filter = ActivityEventFilter(
            offset = scroll.offset,
            user = user,
            minTimestamp = userFilter?.minTimestamp,
            maxTimestamp = userFilter?.maxTimestamp,
            type = userFilter?.type
        )

        val allEvents = activityEventElasticDao.findEvents(
            scroll.scrollSize,
            filter
        )

        val results = allEvents.map { ActivityForFrontend(it.type, it.timestamp, it) }

        val nextOffset = allEvents.size + (scroll.offset ?: 0)

        return ScrollResult(results, nextOffset, endOfScroll = allEvents.size < scroll.scrollSize)
    }

    companion object : Loggable {
        override val log = logger()
    }
}

