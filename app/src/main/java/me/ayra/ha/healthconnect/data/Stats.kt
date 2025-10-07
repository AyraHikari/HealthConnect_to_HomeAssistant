package me.ayra.ha.healthconnect.data

import android.content.Context
import me.ayra.ha.healthconnect.utils.DataStore.getKey
import me.ayra.ha.healthconnect.utils.DataStore.setKey
import me.ayra.ha.healthconnect.utils.DataStore.toJson
import me.ayra.ha.healthconnect.utils.DataStore.tryParseJson

private const val STATS_FOLDER = "STATS"
private const val STATS_PATH = "stats_cache"

data class HeartRateSample(
    val timestamp: Long,
    val beatsPerMinute: Long,
)

data class SleepStageDuration(
    val stageType: Int,
    val durationSeconds: Long,
)

data class SleepStats(
    val totalDurationSeconds: Long,
    val sleepDurationSeconds: Long,
    val stageDurations: List<SleepStageDuration> = emptyList(),
)

data class StepsStats(
    val totalSteps: Long = 0L,
    val distanceKilometers: Double = 0.0,
    val caloriesBurned: Double = 0.0,
    val goal: Int = 5_000,
    val timeline: List<StepsTimelineEntry> = emptyList(),
)

data class StepsTimelineEntry(
    val endTimeEpochSecond: Long,
    val cumulativeSteps: Long,
)

data class StatsData(
    val heartRate: List<HeartRateSample> = emptyList(),
    val sleep: SleepStats? = null,
    val steps: StepsStats? = null,
)

fun Context.saveStats(data: StatsData) {
    setKey(STATS_FOLDER, STATS_PATH, data.toJson())
}

fun Context.getStats(): StatsData? {
    val json = getKey<String>(STATS_FOLDER, STATS_PATH) ?: return null
    return tryParseJson(json)
}
