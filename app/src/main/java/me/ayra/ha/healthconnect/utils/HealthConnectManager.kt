package me.ayra.ha.healthconnect.utils

import android.content.Context
import android.util.Log
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.CervicalMucusRecord
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.MenstruationPeriodRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import me.ayra.ha.healthconnect.data.DEFAULT_SYNC_DAYS
import me.ayra.ha.healthconnect.data.MAX_SYNC_DAYS
import me.ayra.ha.healthconnect.data.MIN_SYNC_DAYS
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

val healthConnectPermissions = setOf(
    HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND,
    HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
    HealthPermission.getReadPermission(BasalBodyTemperatureRecord::class),
    HealthPermission.getReadPermission(BloodGlucoseRecord::class),
    HealthPermission.getReadPermission(BloodPressureRecord::class),
    HealthPermission.getReadPermission(BasalMetabolicRateRecord::class),
    HealthPermission.getReadPermission(BodyFatRecord::class),
    HealthPermission.getReadPermission(BodyTemperatureRecord::class),
    HealthPermission.getReadPermission(BoneMassRecord::class),
    HealthPermission.getReadPermission(CyclingPedalingCadenceRecord::class),
    HealthPermission.getReadPermission(CervicalMucusRecord::class),
    HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    HealthPermission.getReadPermission(DistanceRecord::class),
    HealthPermission.getReadPermission(ElevationGainedRecord::class),
    HealthPermission.getReadPermission(FloorsClimbedRecord::class),
    HealthPermission.getReadPermission(HeartRateRecord::class),
    HealthPermission.getReadPermission(HeightRecord::class),
    HealthPermission.getReadPermission(HydrationRecord::class),
    HealthPermission.getReadPermission(LeanBodyMassRecord::class),
    HealthPermission.getReadPermission(MenstruationFlowRecord::class),
    HealthPermission.getReadPermission(MenstruationPeriodRecord::class),
    HealthPermission.getReadPermission(NutritionRecord::class),
    HealthPermission.getReadPermission(OvulationTestRecord::class),
    HealthPermission.getReadPermission(OxygenSaturationRecord::class),
    HealthPermission.getReadPermission(PowerRecord::class),
    HealthPermission.getReadPermission(RespiratoryRateRecord::class),
    HealthPermission.getReadPermission(RestingHeartRateRecord::class),
    HealthPermission.getReadPermission(SleepSessionRecord::class),
    HealthPermission.getReadPermission(SpeedRecord::class),
    HealthPermission.getReadPermission(StepsRecord::class),
    HealthPermission.getReadPermission(StepsCadenceRecord::class),
    HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
    HealthPermission.getReadPermission(Vo2MaxRecord::class),
    HealthPermission.getReadPermission(WeightRecord::class),
    HealthPermission.getReadPermission(WheelchairPushesRecord::class)
)

class HealthConnectManager(private val context: Context) {
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    fun getPermissions(): Set<String> {
        return healthConnectPermissions
    }

    suspend fun hasAllPermissions(): Boolean {
        return healthConnectClient.permissionController.getGrantedPermissions().containsAll(
            healthConnectPermissions
        )
    }

    fun requestPermissionsActivityContract(): ActivityResultContract<Set<String>, Set<String>> {
        return PermissionController.createRequestPermissionResultContract()
    }

    suspend fun getHeartRate(days: Long = DEFAULT_SYNC_DAYS): List<HeartRateRecord>? {
        val currentZoneId = ZoneId.systemDefault()
        val endDateTime = ZonedDateTime.ofInstant(Instant.now(), currentZoneId)
        val startDateTime = endDateTime.minusDays(sanitizeDays(days))

        val timeRange = TimeRangeFilter.between(startDateTime.toInstant(), endDateTime.toInstant())
        val fetchedData = try {
            val request = ReadRecordsRequest(recordType = HeartRateRecord::class,
                timeRangeFilter = timeRange)
            healthConnectClient.readRecords(request)
        } catch (e: Exception) {
            return null
        }
        return fetchedData.records
    }

    suspend fun getSleep(days: Long = DEFAULT_SYNC_DAYS): List<SleepSessionRecord>? {
        val currentZoneId = ZoneId.systemDefault()
        val endDateTime = ZonedDateTime.ofInstant(Instant.now(), currentZoneId)
        val startDateTime = endDateTime.minusDays(sanitizeDays(days))

        val timeRange = TimeRangeFilter.between(startDateTime.toInstant(), endDateTime.toInstant())
        val fetchedData = try {
            val request = ReadRecordsRequest(recordType = SleepSessionRecord::class,
                timeRangeFilter = timeRange)
            healthConnectClient.readRecords(request)
        } catch (e: Exception) {
            return null
        }
        return fetchedData.records
    }

    suspend fun getLastSleep(): List<SleepSessionRecord>? {
        val now = Instant.now()
        val currentTime = LocalDateTime.now()

        // Setting the start of the "last night" period at 20:00
        val lastNight20 = currentTime
            .withHour(20)
            .withMinute(0)
            .withSecond(0)
            .minusDays(if (currentTime.hour < 20) 1 else 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()

        // Creating the time range between last night at 20:00 and now
        val timeRange = TimeRangeFilter.between(lastNight20, now)

        // Fetch sleep records between lastNight20 and now
        val fetchedData = try {
            val request = ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = timeRange
            )
            healthConnectClient.readRecords(request)
        } catch (e: Exception) {
            return null
        }

        // If data is found, return it
        if (fetchedData.records.isNotEmpty()) {
            return fetchedData.records
        }

        // If no data found, fallback to check for today until tomorrow at 00:00
        val todayStart = currentTime.withHour(0).withMinute(0).withSecond(0)
            .atZone(ZoneId.systemDefault()).toInstant()
        val tomorrowStart = todayStart.plusSeconds(86400) // Adding 24 hours to get tomorrow start

        val fallbackTimeRange = TimeRangeFilter.between(todayStart, tomorrowStart)

        val fallbackData = try {
            val fallbackRequest = ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = fallbackTimeRange
            )
            healthConnectClient.readRecords(fallbackRequest)
        } catch (e: Exception) {
            return null
        }

        return fallbackData.records
    }

    suspend fun getSteps(days: Long = DEFAULT_SYNC_DAYS): List<StepsRecord>? {
        val currentZoneId = ZoneId.systemDefault()
        val endDateTime = ZonedDateTime.ofInstant(Instant.now(), currentZoneId)
        val startDateTime = endDateTime.minusDays(sanitizeDays(days))

        val timeRange = TimeRangeFilter.between(startDateTime.toInstant(), endDateTime.toInstant())
        val fetchedData = try {
            val request = ReadRecordsRequest(recordType = StepsRecord::class,
                timeRangeFilter = timeRange)
            healthConnectClient.readRecords(request)
        } catch (e: Exception) {
            return null
        }
        return fetchedData.records
    }

    suspend fun getWeight(days: Long = DEFAULT_SYNC_DAYS): List<WeightRecord>? {
        val currentZoneId = ZoneId.systemDefault()
        val endDateTime = ZonedDateTime.ofInstant(Instant.now(), currentZoneId)
        val startDateTime = endDateTime.minusDays(sanitizeDays(days))

        val timeRange = TimeRangeFilter.between(startDateTime.toInstant(), endDateTime.toInstant())
        val fetchedData = try {
            val request = ReadRecordsRequest(recordType = WeightRecord::class,
                timeRangeFilter = timeRange)
            healthConnectClient.readRecords(request)
        } catch (e: Exception) {
            Log.e("HealthConnect", e.message.toString())
            return null
        }
        return fetchedData.records
    }

    suspend fun getExerciseSessions(days: Long = DEFAULT_SYNC_DAYS): List<ExerciseSessionRecord>? {
        val currentZoneId = ZoneId.systemDefault()
        val endDateTime = ZonedDateTime.ofInstant(Instant.now(), currentZoneId)
        val startDateTime = endDateTime.minusDays(sanitizeDays(days))

        val timeRange = TimeRangeFilter.between(startDateTime.toInstant(), endDateTime.toInstant())
        val fetchedData = try {
            val request = ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = timeRange
            )
            healthConnectClient.readRecords(request)
        } catch (e: Exception) {
            return null
        }
        return fetchedData.records
    }

    suspend fun getOxygenSaturation(days: Long = DEFAULT_SYNC_DAYS): List<OxygenSaturationRecord>? {
        val currentZoneId = ZoneId.systemDefault()
        val endDateTime = ZonedDateTime.ofInstant(Instant.now(), currentZoneId)
        val startDateTime = endDateTime.minusDays(sanitizeDays(days))

        val timeRange = TimeRangeFilter.between(startDateTime.toInstant(), endDateTime.toInstant())
        val fetchedData = try {
            val request = ReadRecordsRequest(
                recordType = OxygenSaturationRecord::class,
                timeRangeFilter = timeRange
            )
            healthConnectClient.readRecords(request)
        } catch (e: Exception) {
            return null
        }
        return fetchedData.records
    }

    suspend fun getHydrationRecord(days: Long = DEFAULT_SYNC_DAYS): List<HydrationRecord>? {
        val currentZoneId = ZoneId.systemDefault()
        val endDateTime = ZonedDateTime.ofInstant(Instant.now(), currentZoneId)
        val startDateTime = endDateTime.minusDays(sanitizeDays(days))

        val timeRange = TimeRangeFilter.between(startDateTime.toInstant(), endDateTime.toInstant())
        val fetchedData = try {
            val request = ReadRecordsRequest(
                recordType = HydrationRecord::class,
                timeRangeFilter = timeRange
            )
            healthConnectClient.readRecords(request)
        } catch (e: Exception) {
            return null
        }
        return fetchedData.records
    }

    suspend fun getBodyTemperature(days: Long = DEFAULT_SYNC_DAYS): List<BodyTemperatureRecord>? {
        val currentZoneId = ZoneId.systemDefault()
        val endDateTime = ZonedDateTime.ofInstant(Instant.now(), currentZoneId)
        val startDateTime = endDateTime.minusDays(sanitizeDays(days))

        val timeRange = TimeRangeFilter.between(startDateTime.toInstant(), endDateTime.toInstant())
        val fetchedData = try {
            val request = ReadRecordsRequest(
                recordType = BodyTemperatureRecord::class,
                timeRangeFilter = timeRange
            )
            healthConnectClient.readRecords(request)
        } catch (e: Exception) {
            return null
        }
        return fetchedData.records
    }

    suspend fun getTotalCaloriesBurned(days: Long = DEFAULT_SYNC_DAYS): List<TotalCaloriesBurnedRecord>? {
        val currentZoneId = ZoneId.systemDefault()
        val endDateTime = ZonedDateTime.ofInstant(Instant.now(), currentZoneId)
        val startDateTime = endDateTime.minusDays(sanitizeDays(days))

        val timeRange = TimeRangeFilter.between(startDateTime.toInstant(), endDateTime.toInstant())
        return try {
            val request = ReadRecordsRequest(
                recordType = TotalCaloriesBurnedRecord::class,
                timeRangeFilter = timeRange
            )
            healthConnectClient.readRecords(request).records
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getAll(days: Long = DEFAULT_SYNC_DAYS): Map<String, List<Record>?> {
        val currentZoneId = ZoneId.systemDefault()
        val endDateTime = ZonedDateTime.ofInstant(Instant.now(), currentZoneId)
        val startDateTime = endDateTime.minusDays(sanitizeDays(days))

        val timeRange = TimeRangeFilter.between(startDateTime.toInstant(), endDateTime.toInstant())

        // Map of record type names to their corresponding record classes
        val recordTypes = mapOf(
            "ActiveCaloriesBurned" to ActiveCaloriesBurnedRecord::class,
            "BasalBodyTemperature" to BasalBodyTemperatureRecord::class,
            "BloodGlucose" to BloodGlucoseRecord::class,
            "BloodPressure" to BloodPressureRecord::class,
            "BasalMetabolicRate" to BasalMetabolicRateRecord::class,
            "BodyFat" to BodyFatRecord::class,
            "BodyTemperature" to BodyTemperatureRecord::class,
            "BoneMass" to BoneMassRecord::class,
            "CyclingPedalingCadence" to CyclingPedalingCadenceRecord::class,
            "CervicalMucus" to CervicalMucusRecord::class,
            "ExerciseSession" to ExerciseSessionRecord::class,
            "Distance" to DistanceRecord::class,
            "ElevationGained" to ElevationGainedRecord::class,
            "FloorsClimbed" to FloorsClimbedRecord::class,
            "HeartRate" to HeartRateRecord::class,
            "Height" to HeightRecord::class,
            "Hydration" to HydrationRecord::class,
            "LeanBodyMass" to LeanBodyMassRecord::class,
            "MenstruationFlow" to MenstruationFlowRecord::class,
            "MenstruationPeriod" to MenstruationPeriodRecord::class,
            "Nutrition" to NutritionRecord::class,
            "OvulationTest" to OvulationTestRecord::class,
            "OxygenSaturation" to OxygenSaturationRecord::class,
            "Power" to PowerRecord::class,
            "RespiratoryRate" to RespiratoryRateRecord::class,
            "RestingHeartRate" to RestingHeartRateRecord::class,
            "SleepSession" to SleepSessionRecord::class,
            "Speed" to SpeedRecord::class,
            "Steps" to StepsRecord::class,
            "StepsCadence" to StepsCadenceRecord::class,
            "TotalCaloriesBurned" to TotalCaloriesBurnedRecord::class,
            "Vo2Max" to Vo2MaxRecord::class,
            "Weight" to WeightRecord::class,
            "WheelchairPushes" to WheelchairPushesRecord::class
        )

        return recordTypes.mapValues { (_, recordClass) ->
            try {
                val request = ReadRecordsRequest(
                    recordType = recordClass,
                    timeRangeFilter = timeRange
                )
                healthConnectClient.readRecords(request).records
            } catch (e: Exception) {
                null
            }
        }
    }
}

private fun sanitizeDays(days: Long): Long {
    return days.coerceIn(MIN_SYNC_DAYS, MAX_SYNC_DAYS)
}
