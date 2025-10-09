package me.ayra.ha.healthconnect.utils

import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_BADMINTON
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_BIKING
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_RUNNING
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_YOGA
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FitUtilsTest {
    private val sampleHeartRates = listOf(55L, 60L, 65L, 70L)

    @Test
    fun `calculateHeartRateScore returns weighted score`() {
        val score = FitUtils.calculateHeartRateScore(sampleHeartRates)
        assertEquals(80.0, score, 0.01)
    }

    @Test
    fun `calculateHeartRateScore returns zero when no data`() {
        val score = FitUtils.calculateHeartRateScore(emptyList())
        assertEquals(0.0, score, 0.0)
    }

    @Test
    fun `calculateEnergyScore combines sleep and heart rate metrics`() {
        val score = FitUtils.calculateEnergyScore(80.0, sampleHeartRates)
        assertEquals(80.0, score, 0.01)
    }

    @Test
    fun `calculateEnergyScore returns zero when no heart rate data`() {
        val score = FitUtils.calculateEnergyScore(80.0, emptyList())
        assertEquals(0.0, score, 0.0)
    }

    @Test
    fun `calculateCalories multiplies steps by weight factor`() {
        val calories = FitUtils.calculateCalories(10_000, 70.0)
        assertEquals(350.0, calories, 0.0)
    }

    @Test
    fun `calculateDistanceInKm accounts for gender specific stride length`() {
        val maleDistance = FitUtils.calculateDistanceInKm(8_000, 180.0, true)
        val femaleDistance = FitUtils.calculateDistanceInKm(8_000, 180.0, false)

        assertEquals(5.976, maleDistance, 0.001)
        assertEquals(5.904, femaleDistance, 0.001)
    }

    @Test
    fun `getSleepQualityDescription returns descriptive text`() {
        val description = FitUtils.getSleepQualityDescription(85.0)
        assertTrue(description.startsWith("Your sleep quality was very good"))
    }

    @Test
    fun `getSleepQualityDescription handles invalid score`() {
        val description = FitUtils.getSleepQualityDescription(150.0)
        assertTrue(description.contains("Invalid score"))
    }

    @Test
    fun `getRhrScoreDescription returns message for empty data`() {
        val description = FitUtils.getRhrScoreDescription(emptyList())
        assertEquals("No heart rate data", description)
    }

    @Test
    fun `getRhrScoreDescription categorises excellent`() {
        val description = FitUtils.getRhrScoreDescription(listOf(55L, 58L))
        assertTrue(description.startsWith("Your resting heart rate is excellent"))
    }

    @Test
    fun `getEnergyScoreDescription summarises energy level`() {
        val description = FitUtils.getEnergyScoreDescription(75.0)
        assertTrue(description.startsWith("Your energy levels are good"))
    }

    @Test
    fun `getEnergyScoreDescription handles invalid score`() {
        val description = FitUtils.getEnergyScoreDescription(-5.0)
        assertTrue(description.contains("Invalid score"))
    }

    @Test
    fun `getEnergyScoreSummaryTitle selects matching title`() {
        assertEquals("Maintain Excellent Habits", FitUtils.getEnergyScoreSummaryTitle(95.0))
        assertEquals("Invalid Score", FitUtils.getEnergyScoreSummaryTitle(150.0))
    }

    @Test
    fun `getScoreInfo returns label`() {
        assertEquals("Very Low", FitUtils.getScoreInfo(10.0))
        assertEquals("Needs Attention", FitUtils.getScoreInfo(35.0))
        assertEquals("Fair", FitUtils.getScoreInfo(55.0))
        assertEquals("Good", FitUtils.getScoreInfo(75.0))
        assertEquals("Very Good", FitUtils.getScoreInfo(85.0))
        assertEquals("Excellent", FitUtils.getScoreInfo(95.0))
        assertEquals("Invalid Score", FitUtils.getScoreInfo(150.0))
    }

    @Test
    fun `getScoreColor maps range to color`() {
        assertEquals("#5E7893", FitUtils.getScoreColor(95.0))
        assertEquals("#A05556", FitUtils.getScoreColor(85.0))
        assertEquals("#7A6BA3", FitUtils.getScoreColor(70.0))
        assertEquals("#BFA65A", FitUtils.getScoreColor(50.0))
        assertEquals("#5CA28F", FitUtils.getScoreColor(30.0))
        assertEquals("#6D9B65", FitUtils.getScoreColor(10.0))
        assertEquals("#B0B0B0", FitUtils.getScoreColor(-5.0))
    }

    @Test
    fun `toExerciseName returns readable name`() {
        assertEquals("Badminton", with(FitUtils) { EXERCISE_TYPE_BADMINTON.toExerciseName() })
        assertEquals("Biking", with(FitUtils) { EXERCISE_TYPE_BIKING.toExerciseName() })
        assertEquals("Running", with(FitUtils) { EXERCISE_TYPE_RUNNING.toExerciseName() })
        assertEquals("Yoga", with(FitUtils) { EXERCISE_TYPE_YOGA.toExerciseName() })
        assertEquals("Unknown", with(FitUtils) { (-1).toExerciseName() })
    }
}
