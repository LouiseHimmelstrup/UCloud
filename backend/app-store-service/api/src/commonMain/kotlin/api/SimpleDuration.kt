package dk.sdu.cloud.app.store.api

import kotlinx.serialization.Serializable

private const val MAX_SECONDS = 59

@Serializable
data class SimpleDuration(val hours: Int, val minutes: Int, val seconds: Int) {
    init {
        if (seconds !in 0..MAX_SECONDS) throw IllegalArgumentException("seconds must be in 0..59")
        if (minutes !in 0..MAX_SECONDS) throw IllegalArgumentException("minutes must be in 0..59")
    }

    override fun toString() = StringBuilder().apply {
        append(hours.toString().padStart(2, '0'))
        append(':')
        append(minutes.toString().padStart(2, '0'))
        append(':')
        append(seconds.toString().padStart(2, '0'))
    }.toString()

    fun toMillis(): Long {
        return (hours * 60L * 60 * 1000 + minutes * 60 * 1000 + seconds * 1000)
    }

    companion object {
        fun fromMillis(durationMs: Long): SimpleDuration {
            val hours = durationMs / (1000 * 60 * 60)
            val minutes = (durationMs % (1000 * 60 * 60)) / (1000 * 60)
            val seconds = ((durationMs % (1000 * 60 * 60)) % (1000 * 60)) / 1000

            return SimpleDuration(hours.toInt(), minutes.toInt(), seconds.toInt())
        }
    }
}
