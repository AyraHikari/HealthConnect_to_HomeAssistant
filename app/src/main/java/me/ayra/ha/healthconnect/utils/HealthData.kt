package me.ayra.ha.healthconnect.utils

import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import me.ayra.ha.healthconnect.utils.SleepUtils.toSleepStageText
import me.ayra.ha.healthconnect.utils.TimeUtils.dayTimestamp
import me.ayra.ha.healthconnect.utils.TimeUtils.toTimeCount
import me.ayra.ha.healthconnect.utils.TimeUtils.unixTime
import me.ayra.healthconnectsync.utils.FitUtils.calculateCalories
import me.ayra.healthconnectsync.utils.FitUtils.calculateDistanceInKm

class HealthData(
    private var totalSleep: Long = 0L,
    private val heartRates: MutableList<Long> = mutableListOf(),
    private var isUnavailable: Boolean = false,
    private val unavailableReason: MutableList<String> = mutableListOf<String>()
) {
    suspend fun updateHealthData(hc: HealthConnectManager): MutableMap<String, Any?> {
        val sleep = updateSleepData(hc)
        val heartRate = updateHeartRateData(hc)
        val stepsWeight = updateStepsData(hc)
        val exercise = updateExerciseData(hc)

        return mutableMapOf(
            "sleep" to sleep,
            "heart" to heartRate,
            "steps" to stepsWeight?.get("steps"),
            "weight" to stepsWeight?.get("weight"),
            "exercise" to exercise
        )
    }

    private suspend fun updateExerciseData(hc: HealthConnectManager): Map<String, Any>? {
        val today = dayTimestamp(unixTime)
        val exerciseSessions = hc.getExerciseSessions()
            ?.filter { dayTimestamp(it.startTime.epochSecond) == today }
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

        val exerciseData = mutableListOf<Map<String, Any?>>()
        var totalDuration = 0L

        exerciseSessions.forEach { session ->
            session.segments
            val duration = session.endTime.epochSecond - session.startTime.epochSecond
            totalDuration += duration

            exerciseData.add(mapOf(
                "startTime" to session.startTime.epochSecond,
                "endTime" to session.endTime.epochSecond,
                "duration" to duration,
                "durationFormatted" to duration.toTimeCount(),
                "exerciseType" to session.exerciseType.toString(),
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
            ))
        }

        return mapOf(
            "totalSessions" to exerciseSessions.size,
            "totalDuration" to totalDuration,
            "totalDurationFormatted" to totalDuration.toTimeCount(),
            "sessions" to exerciseData
        )
    }

    private suspend fun updateStepsData(hc: HealthConnectManager): MutableMap<String, Any>? {
        val data = mutableMapOf<String, Any>()
        val today = dayTimestamp(unixTime)

        // Weight data
        val weightKg = hc.getWeight()
            ?.firstOrNull { dayTimestamp(it.time.epochSecond) == today }
            ?.weight?.inKilograms ?: 0.0

        data["weight"] = weightKg

        // Steps data
        val stepData = hc.getSteps()
            ?.firstOrNull { dayTimestamp(it.startTime.epochSecond) == today }

        return if (stepData != null) {
            val calories = calculateCalories(stepData.count, weightKg).toInt()
            val distance = calculateDistanceInKm(stepData.count, 155.0, true)

            data["steps"] = stepData.count
            data
        } else {
            null
        }
    }

    private suspend fun updateSleepData(hc: HealthConnectManager): Map<String, Any>? {
        val data = hc.getLastSleep() ?: run {
            isUnavailable = true
            if (!unavailableReason.contains("sleep")) unavailableReason.add("sleep")
            return null
        }

        val sleepSessionData = mutableMapOf<Int, Any>()
        var totalSleepTime = 0L
        var totalTimeInBed = 0L
        var deepSleepTime = 0L
        var remSleepTime = 0L
        var awakeTime = 0L
        var lightSleepTime = 0L

        data.forEachIndexed { index, session ->
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

            sleepSessionData[index + 1] = mapOf(
                "start" to session.startTime.epochSecond,
                "end" to session.endTime.epochSecond,
                "stage" to sleepStages.values.toList()
            )
        }

        totalSleep = totalSleepTime

        return if (totalSleep == 0L && sleepSessionData.isEmpty()) {
            null
        } else {
            mapOf(
                "totalSleep" to totalSleep,
                "data" to sleepSessionData
            )
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
                "time" to 0L,
                "timeFormat" to "",
                "percentage" to 0.0,
                "occurrences" to 0,
                "durations" to mutableListOf<Long>(),
                "startTimes" to mutableListOf<String>(),
                "endTimes" to mutableListOf<String>()
            )
        }

        val newTime = (stageData["time"] as Long) + duration
        val percentage = (newTime.toDouble() / totalSessionDuration.toDouble()) * 100

        (stageData["durations"] as MutableList<Long>).add(duration)
        (stageData["startTimes"] as MutableList<String>).add(stage.startTime.toString())
        (stageData["endTimes"] as MutableList<String>).add(stage.endTime.toString())

        stageData.apply {
            put("time", newTime)
            put("timeFormat", newTime.toTimeCount())
            put("percentage", percentage)
            put("occurrences", (get("occurrences") as Int) + 1)
        }
    }

    private suspend fun updateHeartRateData(hc: HealthConnectManager): Map<String, Any?>? {
        heartRates.clear()
        val today = dayTimestamp(unixTime)

        val heartRateSamples = mutableListOf<Map<String, Any>>() // New: detailed samples list

        hc.getHeartRate()
            ?.filter { dayTimestamp(it.startTime.epochSecond) == today }
            ?.forEach { record ->
                record.samples.forEach { sample ->
                    heartRates.add(sample.beatsPerMinute)
                    heartRateSamples.add(
                        mapOf(
                            "time" to sample.time.epochSecond,
                            "bpm" to sample.beatsPerMinute
                        )
                    )
                }
            }

        if (heartRates.isEmpty()) {
            isUnavailable = true
            if (!unavailableReason.contains("heart rate")) unavailableReason.add("heart rate")
            return null
        }

        val averageHeartRate = heartRates.average()
        val minHeartRate = heartRates.minOrNull()
        val maxHeartRate = heartRates.maxOrNull()

        return mapOf(
            "averageHeartRate" to averageHeartRate,
            "minHeartRate" to minHeartRate,
            "maxHeartRate" to maxHeartRate,
            "heartRates" to heartRates, // list of integers (bpm only)
            "heartRateSamples" to heartRateSamples // new: list of timestamped samples
        )
    }
}