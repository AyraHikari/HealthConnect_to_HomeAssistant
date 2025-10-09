package me.ayra.ha.healthconnect.utils

import androidx.health.connect.client.records.SleepSessionRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class SleepUtilsTest {
    @Test
    fun `toSleepStageText returns expected values for known stages`() {
        val expectations = mapOf(
            SleepSessionRecord.STAGE_TYPE_AWAKE to "Awake",
            SleepSessionRecord.STAGE_TYPE_SLEEPING to "Sleep",
            SleepSessionRecord.STAGE_TYPE_DEEP to "Deep Sleep",
            SleepSessionRecord.STAGE_TYPE_LIGHT to "Light Sleep",
            SleepSessionRecord.STAGE_TYPE_REM to "REM",
            SleepSessionRecord.STAGE_TYPE_OUT_OF_BED to "Out of bed",
            SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED to "Awake in bed",
        )

        expectations.forEach { (stage, expected) ->
            val actual = with(SleepUtils) { stage.toSleepStageText() }
            assertEquals("Unexpected text for stage $stage", expected, actual)
        }
    }

    @Test
    fun `toSleepStageText returns Unknown for unknown stage`() {
        val actual = with(SleepUtils) { (-1).toSleepStageText() }
        assertEquals("Unknown", actual)
    }
}
