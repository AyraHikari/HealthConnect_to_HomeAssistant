package me.ayra.ha.healthconnect.utils

import android.content.Context
import androidx.health.connect.client.records.SleepSessionRecord
import me.ayra.ha.healthconnect.data.Settings.getSettings
import me.ayra.ha.healthconnect.data.Settings.getSyncDays
import me.ayra.ha.healthconnect.utils.FitUtils.toExerciseName
import me.ayra.ha.healthconnect.utils.SleepUtils.toSleepStageText
import me.ayra.ha.healthconnect.utils.TimeUtils.dayTimestamp
import me.ayra.ha.healthconnect.utils.TimeUtils.toTimeCount

class HealthData(
    val context: Context,
    private var isUnavailable: Boolean = false,
    private val unavailableReason: MutableList<String> = mutableListOf<String>()
) {
    suspend fun getHealthData(hc: HealthConnectManager): MutableMap<String, Any?> {
        var healthData = mutableMapOf<String, Any?>()
        val syncDays = context.getSyncDays()
        if (context.getSettings("sleep", true) == true) {
            val sleep = getSleepData(hc, syncDays)
            healthData["sleep"] = sleep
        }
        if (context.getSettings("heartRate", true) == true) {
            val heartRate = getHeartRateData(hc, syncDays)
            healthData["heart"] = heartRate
        }
        if (context.getSettings("steps", true) == true) {
            val steps = getStepsData(hc, syncDays)
            healthData["steps"] = steps
        }
        if (context.getSettings("weight", true) == true) {
            val weight = getWeightData(hc, syncDays)
            healthData["weight"] = weight
        }
        if (context.getSettings("exercise", true) == true) {
            val exercise = getExerciseData(hc, syncDays)
            healthData["exercise"] = exercise
        }
        if (context.getSettings("oxygen", true) == true) {
            val oxygen = getOxygenSaturation(hc, syncDays)
            healthData["oxygen"] = oxygen
        }
        if (context.getSettings("hydration", true) == true) {
            val hydration = getHydrationRecord(hc, syncDays)
            healthData["hydration"] = hydration
        }
        if (context.getSettings("calories", true) == true) {
            val calories = getTotalCaloriesBurned(hc, syncDays)
            healthData["calories"] = calories
        }
        return healthData
    }

    private suspend fun getExerciseData(hc: HealthConnectManager, days: Long): Map<String, Any?>? {
        val resultMap = mutableMapOf<String, MutableMap<String, Any>>()

        val exerciseSessions = hc.getExerciseSessions(days)
            ?: run {
                isUnavailable = true
                if (!unavailableReason.contains("exercise")) unavailableReason.add("exercise")
                return null
            }

        if (exerciseSessions.isEmpty()) {
            isUnavailable = true
            if (!unavailableReason.contains("exercise")) unavailableReason.add("exercise")
            return null
        }

        exerciseSessions.forEach { session ->
            val date = dayTimestamp(session.startTime.epochSecond) ?: "unknown"

            val sessionsList = resultMap.getOrPut(date) {
                mutableMapOf(
                    "totalSessions" to 0,
                    "totalDuration" to 0L,
                    "totalDurationFormatted" to 0L.toTimeCount(),
                    "sessions" to mutableListOf<Map<String, Any?>>()
                )
            }["sessions"] as MutableList<Map<String, Any?>>

            val duration = session.endTime.epochSecond - session.startTime.epochSecond

            sessionsList.add(
                mapOf(
                    "startTime" to session.startTime.epochSecond,
                    "endTime" to session.endTime.epochSecond,
                    "duration" to duration,
                    "durationFormatted" to duration.toTimeCount(),
                    "exerciseType" to session.exerciseType.toString(),
                    "exerciseName" to session.exerciseType.toExerciseName(),
                    "title" to session.title,
                    "notes" to session.notes,
                    "segments" to session.segments.map {
                        mapOf(
                            "startTime" to it.startTime.epochSecond,
                            "endTime" to it.endTime.epochSecond,
                            "repetitions" to it.repetitions,
                            "segmentType" to it.segmentType
                        )
                    }
                )
            )

            resultMap[date]?.let { data ->
                val currentSessions = data["totalSessions"] as Int
                val currentDuration = data["totalDuration"] as Long
                data["totalSessions"] = currentSessions + 1
                data["totalDuration"] = currentDuration + duration
                data["totalDurationFormatted"] = (currentDuration + duration).toTimeCount()
            }
        }

        return resultMap
    }

    private suspend fun getStepsData(hc: HealthConnectManager, days: Long): Map<String, Any?>? {
        val resultMap = mutableMapOf<String, MutableMap<String, Any>>()

        // Process steps data
        hc.getSteps(days)?.forEach { record ->
            val date = dayTimestamp(record.startTime.epochSecond) ?: "unknown"
            if (!resultMap.containsKey(date)) {
                resultMap[date] = mutableMapOf()
            }

            resultMap[date]?.put("startTime", record.startTime.epochSecond)
            resultMap[date]?.put("endTime", record.endTime.epochSecond)
            resultMap[date]?.put("count", record.count)
        }

        if (resultMap.isEmpty()) {
            return null
        }

        return resultMap
    }

    private suspend fun getWeightData(hc: HealthConnectManager, days: Long): Map<String, Any?>? {
        val resultMap = mutableMapOf<String, MutableMap<Long, Any>>()

        // Process weight data
        hc.getWeight(days)?.forEach { record ->
            val date = dayTimestamp(record.time.epochSecond) ?: "unknown"
            val entry = resultMap.getOrPut(date) { mutableMapOf() }
            entry[record.time.epochSecond] = record.weight.inKilograms
        }

        if (resultMap.isEmpty()) {
            return null
        }

        return resultMap
    }

    private suspend fun getSleepData(hc: HealthConnectManager, days: Long): Map<String, Any>? {
        val data = hc.getSleep(days) ?: run {
            isUnavailable = true
            if (!unavailableReason.contains("sleep")) unavailableReason.add("sleep")
            return null
        }

        val lastSleep = hc.getLastSleep()

        val sleepSessionData = mutableMapOf<String, Any>()
        var totalSleepTime = 0L
        var totalTimeInBed = 0L
        var deepSleepTime = 0L
        var remSleepTime = 0L
        var awakeTime = 0L
        var lightSleepTime = 0L

        data.forEach { session ->
            val date = dayTimestamp(session.startTime.epochSecond) ?: "unknown"
            val sleepStages = mutableMapOf<Int, MutableMap<String, Any>>()
            val totalSessionDuration = session.endTime.epochSecond - session.startTime.epochSecond
            totalTimeInBed += totalSessionDuration

            session.stages.forEach { stage ->
                val duration = stage.endTime.epochSecond - stage.startTime.epochSecond
                when (stage.stage) {
                    SleepSessionRecord.STAGE_TYPE_DEEP -> {
                        deepSleepTime += duration
                        totalSleepTime += duration
                    }
                    SleepSessionRecord.STAGE_TYPE_REM -> {
                        remSleepTime += duration
                        totalSleepTime += duration
                    }
                    SleepSessionRecord.STAGE_TYPE_LIGHT -> {
                        lightSleepTime += duration
                        totalSleepTime += duration
                    }
                    SleepSessionRecord.STAGE_TYPE_AWAKE -> awakeTime += duration
                }
                updateSleepStages(sleepStages, stage, totalSessionDuration)
            }

            sleepSessionData[date] = mapOf(
                "start" to session.startTime.epochSecond,
                "end" to session.endTime.epochSecond,
                "stage" to sleepStages.values.toList()
            )
        }

        totalSleepTime = 0L
        totalTimeInBed = 0L
        deepSleepTime = 0L
        remSleepTime = 0L
        awakeTime = 0L
        lightSleepTime = 0L

        lastSleep?.forEach { session ->
            val date = dayTimestamp(session.startTime.epochSecond) ?: "unknown"
            val sleepStages = mutableMapOf<Int, MutableMap<String, Any>>()
            val totalSessionDuration = session.endTime.epochSecond - session.startTime.epochSecond
            totalTimeInBed += totalSessionDuration

            session.stages.forEach { stage ->
                val duration = stage.endTime.epochSecond - stage.startTime.epochSecond
                when (stage.stage) {
                    SleepSessionRecord.STAGE_TYPE_DEEP -> {
                        deepSleepTime += duration
                        totalSleepTime += duration
                    }
                    SleepSessionRecord.STAGE_TYPE_REM -> {
                        remSleepTime += duration
                        totalSleepTime += duration
                    }
                    SleepSessionRecord.STAGE_TYPE_LIGHT -> {
                        lightSleepTime += duration
                        totalSleepTime += duration
                    }
                    SleepSessionRecord.STAGE_TYPE_AWAKE -> awakeTime += duration
                }
                updateSleepStages(sleepStages, stage, totalSessionDuration)
            }

            sleepSessionData["lastSleep"] = mapOf(
                "start" to session.startTime.epochSecond,
                "end" to session.endTime.epochSecond,
                "stage" to sleepStages.values.toList()
            )
        }

        return if (sleepSessionData.isEmpty()) {
            null
        } else {
            sleepSessionData
        }
    }

    private fun updateSleepStages(
        sleepStages: MutableMap<Int, MutableMap<String, Any>>,
        stage: SleepSessionRecord.Stage,
        totalSessionDuration: Long
    ) {
        val duration = stage.endTime.epochSecond - stage.startTime.epochSecond

        val stageData = sleepStages.getOrPut(stage.stage) {
            mutableMapOf(
                "stage" to stage.stage.toString(),
                "stageFormat" to stage.stage.toSleepStageText(),
                "percentage" to 0.0,
                "occurrences" to 0,
                "sessions" to mutableListOf<Map<String, Long>>()
            )
        }

        val currentTime = (stageData["time"] as? Long) ?: 0L
        val newTime = currentTime + duration
        val percentage = if (totalSessionDuration > 0) {
            (newTime.toDouble() / totalSessionDuration) * 100
        } else {
            0.0
        }

        val sessions = stageData["sessions"] as? MutableList<Map<String, Long>> ?: mutableListOf()
        sessions.add(
            mapOf(
                "duration" to duration,
                "startTime" to stage.startTime.epochSecond,
                "endTime" to stage.endTime.epochSecond
            )
        )

        stageData["totalTime"] = newTime
        stageData["totalTimeFormat"] = newTime.toTimeCount()
        stageData["percentage"] = percentage
        stageData["occurrences"] = (stageData["occurrences"] as? Int ?: 0) + 1
        stageData["sessions"] = sessions
    }

    private suspend fun getHeartRateData(hc: HealthConnectManager, days: Long): Map<String, Any?>? {
        val resultMap = mutableMapOf<String, MutableMap<String, Any>>()

        hc.getHeartRate(days)?.forEach { record ->
            record.samples.forEach { sample ->
                val date = dayTimestamp(sample.time.epochSecond)

                if (!resultMap.containsKey(date)) {
                    resultMap[date ?: "unknown"] = mutableMapOf()
                }

                resultMap[date]?.put(
                    sample.time.epochSecond.toString(),
                    mapOf(
                        "time" to sample.time.epochSecond,
                        "bpm" to sample.beatsPerMinute
                    )
                )
            }
        }

        if (resultMap.isEmpty()) {
            isUnavailable = true
            if (!unavailableReason.contains("heart rate")) unavailableReason.add("heart rate")
            return null
        }

        return resultMap
    }

    private suspend fun getOxygenSaturation(hc: HealthConnectManager, days: Long): Map<String, Any?>? {
        val resultMap = mutableMapOf<String, MutableMap<String, Any>>()

        hc.getOxygenSaturation(days)?.forEach { record ->
            val date = dayTimestamp(record.time.epochSecond) ?: "unknown"

            if (!resultMap.containsKey(date)) {
                resultMap[date] = mutableMapOf()
            }

            resultMap[date]?.put(
                record.time.epochSecond.toString(),
                record.percentage.value
            )
        }

        if (resultMap.isEmpty()) {
            isUnavailable = true
            if (!unavailableReason.contains("oxygen saturation")) unavailableReason.add("oxygen saturation")
            return null
        }

        return resultMap
    }

    private suspend fun getHydrationRecord(hc: HealthConnectManager, days: Long): Map<String, Any?>? {
        val resultMap = mutableMapOf<String, MutableMap<String, Any>>()

        hc.getHydrationRecord(days)?.forEach { record ->
            val date = dayTimestamp(record.startTime.epochSecond) ?: "unknown"

            val dayData = resultMap.getOrPut(date) { mutableMapOf() }

            dayData["startTime"] = record.startTime.epochSecond
            dayData["endTime"] = record.endTime.epochSecond
            dayData["volume"] = record.volume.inMilliliters
            dayData["format"] = "ml"
        }

        if (resultMap.isEmpty()) {
            isUnavailable = true
            if (!unavailableReason.contains("hydration record")) unavailableReason.add("hydration record")
            return null
        }

        return resultMap
    }

    private suspend fun getTotalCaloriesBurned(hc: HealthConnectManager, days: Long): Map<String, Any?>? {
        val resultMap = mutableMapOf<String, MutableMap<String, Any>>()

        hc.getTotalCaloriesBurned(days)?.forEach { record ->
            val date = dayTimestamp(record.startTime.epochSecond) ?: "unknown"

            val dayData = resultMap.getOrPut(date) { mutableMapOf() }

            dayData["startTime"] = record.startTime.epochSecond
            dayData["endTime"] = record.endTime.epochSecond
            dayData["energy"] = record.energy.inKilocalories
            dayData["format"] = "kcal"
        }

        if (resultMap.isEmpty()) {
            isUnavailable = true
            if (!unavailableReason.contains("total calories")) {
                unavailableReason.add("total calories")
            }
            return null
        }

        return resultMap
    }

}