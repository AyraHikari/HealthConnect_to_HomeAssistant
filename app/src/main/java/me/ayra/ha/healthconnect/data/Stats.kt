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

data class StatsData(
    val heartRate: List<HeartRateSample> = emptyList(),
)

fun Context.saveStats(data: StatsData) {
    setKey(STATS_FOLDER, STATS_PATH, data.toJson())
}

fun Context.getStats(): StatsData? {
    val json = getKey<String>(STATS_FOLDER, STATS_PATH) ?: return null
    return tryParseJson(json)
}
