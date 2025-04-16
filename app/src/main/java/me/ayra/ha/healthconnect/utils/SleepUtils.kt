package me.ayra.ha.healthconnect.utils

import androidx.health.connect.client.records.SleepSessionRecord

object SleepUtils {
    fun Int.toSleepStageText(): String {
        return when (this) {
            SleepSessionRecord.STAGE_TYPE_AWAKE -> "Awake"
            SleepSessionRecord.STAGE_TYPE_SLEEPING -> "Sleep"
            SleepSessionRecord.STAGE_TYPE_DEEP -> "Deep Sleep"
            SleepSessionRecord.STAGE_TYPE_LIGHT -> "Light Sleep"
            SleepSessionRecord.STAGE_TYPE_REM -> "REM"
            SleepSessionRecord.STAGE_TYPE_OUT_OF_BED -> "Out of bed"
            SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED -> "Awake in bed"
            else -> "Unknown"
        }
    }
}