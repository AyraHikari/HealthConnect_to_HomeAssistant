package me.ayra.ha.healthconnect.utils

import androidx.health.connect.client.records.SleepSessionRecord
import me.ayra.ha.healthconnect.utils.FitUtils.toExerciseName
import me.ayra.ha.healthconnect.utils.SleepUtils.toSleepStageText
import me.ayra.ha.healthconnect.utils.TimeUtils.dayTimestamp
import me.ayra.ha.healthconnect.utils.TimeUtils.toTimeCount

class HealthData(
    private var isUnavailable: Boolean = false,
    private val unavailableReason: MutableList<String> = mutableListOf<String>()
) {
    suspend fun getHealthData(hc: HealthConnectManager): MutableMap<String, Any?> {
        val sleep = getSleepData(hc)
        val heartRate = getHeartRateData(hc)
        val steps = getStepsData(hc)
        val weight = getWeightData(hc)
        val exercise = getExerciseData(hc)
        val oxygen = getOxygenSaturation(hc)
        val calories = getTotalCaloriesBurned(hc)

        return mutableMapOf(
            "sleep" to sleep,
            "heart" to heartRate,
            "steps" to steps,
            "weight" to weight,
            "exercise" to exercise,
            "oxygen" to oxygen,
            "calories" to calories
        )
    }

    private suspend fun getExerciseData(hc: HealthConnectManager): Map<String, Any?>? {
        val resultMap = mutableMapOf<String, MutableMap<String, Any>>()

        val exerciseSessions = hc.getExerciseSessions()
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

    private suspend fun getStepsData(hc: HealthConnectManager): Map<String, Any?>? {
        val resultMap = mutableMapOf<String, MutableMap<String, Any>>()

        // Process steps data
        hc.getSteps()?.forEach { record ->
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

    private suspend fun getWeightData(hc: HealthConnectManager): Map<String, Any?>? {
        val resultMap = mutableMapOf<String, MutableMap<Long, Any>>()

        // Process weight data
        hc.getWeight()?.forEach { record ->
            val date = dayTimestamp(record.time.epochSecond) ?: "unknown"
            val entry = resultMap.getOrPut(date) { mutableMapOf() }
            entry[record.time.epochSecond] = record.weight.inKilograms
        }

        if (resultMap.isEmpty()) {
            return null
        }

        return resultMap
    }

    private suspend fun getSleepData(hc: HealthConnectManager): Map<String, Any>? {
        val data = hc.getSleep() ?: run {
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

    private suspend fun getHeartRateData(hc: HealthConnectManager): Map<String, Any?>? {
        val resultMap = mutableMapOf<String, MutableMap<String, Any>>()

        hc.getHeartRate()?.forEach { record ->
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

    private suspend fun getOxygenSaturation(hc: HealthConnectManager): Map<String, Any?>? {
        val resultMap = mutableMapOf<String, MutableMap<String, Any>>()

        hc.getOxygenSaturation()?.forEach { record ->
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

    private suspend fun getTotalCaloriesBurned(hc: HealthConnectManager): Map<String, Any?>? {
        val resultMap = mutableMapOf<String, MutableMap<String, Any>>()

        hc.getTotalCaloriesBurned()?.forEach { record ->
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