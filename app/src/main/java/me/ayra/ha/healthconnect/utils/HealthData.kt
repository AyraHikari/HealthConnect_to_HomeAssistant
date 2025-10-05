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
    private val unavailableReason: MutableList<String> = mutableListOf<String>(),
) {
    private data class SleepAccumulator(
        var start: Long? = null,
        var end: Long? = null,
        var totalDuration: Long = 0L,
        val stages: MutableMap<Int, MutableMap<String, Any>> = mutableMapOf(),
    )

    suspend fun getHealthData(hc: HealthConnectManager): MutableMap<String, Any?> {
        val healthData = mutableMapOf<String, Any?>()
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
        if (context.getSettings("bodyTemperature", true) == true) {
            val bodyTemperature = getBodyTemperatureData(hc, syncDays)
            healthData["bodyTemperature"] = bodyTemperature
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

    private suspend fun getExerciseData(
        hc: HealthConnectManager,
        days: Long,
    ): Map<String, Any?>? {
        val resultMap = mutableMapOf<String, MutableMap<String, Any>>()

        val exerciseSessions =
            hc.getExerciseSessions(days)
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

            val sessionsList =
                resultMap.getOrPut(date) {
                    mutableMapOf(
                        "totalSessions" to 0,
                        "totalDuration" to 0L,
                        "totalDurationFormatted" to 0L.toTimeCount(),
                        "sessions" to mutableListOf<Map<String, Any?>>(),
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
                    "segments" to
                        session.segments.map {
                            mapOf(
                                "startTime" to it.startTime.epochSecond,
                                "endTime" to it.endTime.epochSecond,
                                "repetitions" to it.repetitions,
                                "segmentType" to it.segmentType,
                            )
                        },
                ),
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

    private suspend fun getStepsData(
        hc: HealthConnectManager,
        days: Long,
    ): Map<String, Any?>? {
        val resultMap = mutableMapOf<String, MutableMap<String, Any>>()

        // Process steps data
        hc.getSteps(days)?.forEach { record ->
            val date = dayTimestamp(record.startTime.epochSecond) ?: "unknown"
            val dateData =
                resultMap.getOrPut(date) {
                    mutableMapOf(
                        "startTime" to record.startTime.epochSecond,
                        "endTime" to record.endTime.epochSecond,
                        "count" to 0L,
                    )
                }

            val currentStartTime = dateData["startTime"] as Long
            val currentEndTime = dateData["endTime"] as Long
            val currentCount = dateData["count"] as Long

            dateData["startTime"] = minOf(currentStartTime, record.startTime.epochSecond)
            dateData["endTime"] = maxOf(currentEndTime, record.endTime.epochSecond)
            dateData["count"] = currentCount + record.count
        }

        if (resultMap.isEmpty()) {
            return null
        }

        return resultMap
    }

    private suspend fun getWeightData(
        hc: HealthConnectManager,
        days: Long,
    ): Map<String, Any?>? {
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

    private suspend fun getBodyTemperatureData(
        hc: HealthConnectManager,
        days: Long,
    ): Map<String, Any?>? {
        val resultMap = mutableMapOf<String, MutableMap<String, Any>>()

        hc.getBodyTemperature(days)?.forEach { record ->
            val date = dayTimestamp(record.time.epochSecond) ?: "unknown"
            val dayData = resultMap.getOrPut(date) { mutableMapOf() }

            dayData[record.time.epochSecond.toString()] =
                mapOf(
                    "time" to record.time.epochSecond,
                    "temperatureCelsius" to record.temperature.inCelsius,
                    "temperatureFahrenheit" to record.temperature.inFahrenheit,
                    "measurementLocation" to record.measurementLocation,
                )
        }

        if (resultMap.isEmpty()) {
            isUnavailable = true
            if (!unavailableReason.contains("body temperature")) {
                unavailableReason.add("body temperature")
            }
            return null
        }

        return resultMap
    }

    private suspend fun getSleepData(
        hc: HealthConnectManager,
        days: Long,
    ): Map<String, Any>? {
        val data =
            hc.getSleep(days) ?: run {
                isUnavailable = true
                if (!unavailableReason.contains("sleep")) unavailableReason.add("sleep")
                return null
            }

        val lastSleep = hc.getLastSleep()

        val sleepSessionData = mutableMapOf<String, Any>()
        val sleepAccumulators = mutableMapOf<String, SleepAccumulator>()

        data.forEach { session ->
            val date = dayTimestamp(session.startTime.epochSecond) ?: "unknown"
            val accumulator = sleepAccumulators.getOrPut(date) { SleepAccumulator() }

            val sessionStart = session.startTime.epochSecond
            val sessionEnd = session.endTime.epochSecond

            accumulator.start =
                accumulator.start?.let { minOf(it, sessionStart) } ?: sessionStart
            accumulator.end = accumulator.end?.let { maxOf(it, sessionEnd) } ?: sessionEnd
            accumulator.totalDuration += sessionEnd - sessionStart

            session.stages.forEach { stage ->
                updateSleepStages(accumulator.stages, stage)
            }
        }

        sleepAccumulators.forEach { (date, accumulator) ->
            val start = accumulator.start
            val end = accumulator.end
            if (start != null && end != null) {
                finalizeSleepStages(accumulator.stages, accumulator.totalDuration)
                sleepSessionData[date] =
                    mapOf(
                        "start" to start,
                        "end" to end,
                        "stage" to accumulator.stages.values.toList(),
                    )
            }
        }

        lastSleep?.let { sessions ->
            val accumulator = SleepAccumulator()

            sessions.forEach { session ->
                val sessionStart = session.startTime.epochSecond
                val sessionEnd = session.endTime.epochSecond

                accumulator.start =
                    accumulator.start?.let { minOf(it, sessionStart) } ?: sessionStart
                accumulator.end =
                    accumulator.end?.let { maxOf(it, sessionEnd) } ?: sessionEnd
                accumulator.totalDuration += sessionEnd - sessionStart

                session.stages.forEach { stage ->
                    updateSleepStages(accumulator.stages, stage)
                }
            }

            val start = accumulator.start
            val end = accumulator.end
            if (start != null && end != null) {
                finalizeSleepStages(accumulator.stages, accumulator.totalDuration)
                sleepSessionData["lastSleep"] =
                    mapOf(
                        "start" to start,
                        "end" to end,
                        "stage" to accumulator.stages.values.toList(),
                    )
            }
        }

        return sleepSessionData.ifEmpty {
            null
        }
    }

    private fun updateSleepStages(
        sleepStages: MutableMap<Int, MutableMap<String, Any>>,
        stage: SleepSessionRecord.Stage,
    ) {
        val duration = stage.endTime.epochSecond - stage.startTime.epochSecond

        val stageData =
            sleepStages.getOrPut(stage.stage) {
                mutableMapOf(
                    "stage" to stage.stage.toString(),
                    "stageFormat" to stage.stage.toSleepStageText(),
                    "percentage" to 0.0,
                    "occurrences" to 0,
                    "sessions" to mutableListOf<Map<String, Long>>(),
                    "totalTime" to 0L,
                    "totalTimeFormat" to 0L.toTimeCount(),
                )
            }

        val sessions = stageData["sessions"] as MutableList<Map<String, Long>>
        sessions.add(
            mapOf(
                "duration" to duration,
                "startTime" to stage.startTime.epochSecond,
                "endTime" to stage.endTime.epochSecond,
            ),
        )

        val newTime = (stageData["totalTime"] as Long) + duration
        stageData["totalTime"] = newTime
        stageData["totalTimeFormat"] = newTime.toTimeCount()
        stageData["occurrences"] = (stageData["occurrences"] as Int) + 1
    }

    private fun finalizeSleepStages(
        sleepStages: MutableMap<Int, MutableMap<String, Any>>,
        totalDuration: Long,
    ) {
        sleepStages.values.forEach { stageData ->
            val totalTime = stageData["totalTime"] as? Long ?: 0L
            val percentage =
                if (totalDuration > 0) {
                    (totalTime.toDouble() / totalDuration) * 100
                } else {
                    0.0
                }

            stageData["percentage"] = percentage
        }
    }

    private suspend fun getHeartRateData(
        hc: HealthConnectManager,
        days: Long,
    ): Map<String, Any?>? {
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
                        "bpm" to sample.beatsPerMinute,
                    ),
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

    private suspend fun getOxygenSaturation(
        hc: HealthConnectManager,
        days: Long,
    ): Map<String, Any?>? {
        val resultMap = mutableMapOf<String, MutableMap<String, Any>>()

        hc.getOxygenSaturation(days)?.forEach { record ->
            val date = dayTimestamp(record.time.epochSecond) ?: "unknown"

            if (!resultMap.containsKey(date)) {
                resultMap[date] = mutableMapOf()
            }

            resultMap[date]?.put(
                record.time.epochSecond.toString(),
                record.percentage.value,
            )
        }

        if (resultMap.isEmpty()) {
            isUnavailable = true
            if (!unavailableReason.contains("oxygen saturation")) unavailableReason.add("oxygen saturation")
            return null
        }

        return resultMap
    }

    private suspend fun getHydrationRecord(
        hc: HealthConnectManager,
        days: Long,
    ): Map<String, Any?>? {
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

    private suspend fun getTotalCaloriesBurned(
        hc: HealthConnectManager,
        days: Long,
    ): Map<String, Any?>? {
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
