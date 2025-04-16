package me.ayra.ha.healthconnect.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TimeUtils {
    val unixTime: Long
        get() = System.currentTimeMillis() / 1000L
    val unixTimeMs: Long
        get() = System.currentTimeMillis()

    fun convertTime(timestamp: Long): String? {
        val currentZoneId = ZoneId.systemDefault()
        val instant = Instant.ofEpochSecond(timestamp)

        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy")
            .withZone(currentZoneId)

        return formatter.format(instant)
    }

    fun convertTimeClock(timestamp: Long): String? {
        val currentZoneId = ZoneId.systemDefault()
        val instant = Instant.ofEpochSecond(timestamp)

        val formatter = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(currentZoneId)

        return formatter.format(instant)
    }

    fun dayTimestamp(timestamp: Long): String? {
        val currentZoneId = ZoneId.systemDefault()
        val instant = Instant.ofEpochSecond(timestamp)

        val formatter = DateTimeFormatter.ofPattern("dd")
            .withZone(currentZoneId)

        return formatter.format(instant)
    }

    fun Long.toTimeCount(): String {
        val hours = this / 3600
        val minutes = (this % 3600) / 60

        return buildString {
            if (hours > 0) {
                append("${hours}h ")
            }
            append("${minutes}m")
        }.trim()
    }

    fun Long.toDate(format: String): String {
        if (this == 0L) return "Never"
        val instant = Instant.ofEpochMilli(this)
        val formatter = DateTimeFormatter.ofPattern(format)
            .withZone(ZoneId.systemDefault())
        return formatter.format(instant)
    }
}