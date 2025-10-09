package me.ayra.ha.healthconnect.utils

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.TimeZone
import kotlin.math.abs

class TimeUtilsTest {
    private lateinit var originalTimeZone: TimeZone

    @Before
    fun setup() {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun `unixTime matches current epoch seconds`() {
        val expected = System.currentTimeMillis() / 1000L
        val actual = TimeUtils.unixTime
        assertTrue(abs(actual - expected) <= 1)
    }

    @Test
    fun `unixTimeMs matches current epoch milliseconds`() {
        val expected = System.currentTimeMillis()
        val actual = TimeUtils.unixTimeMs
        assertTrue(abs(actual - expected) < 100)
    }

    @Test
    fun `convertTime formats epoch seconds`() {
        val formatted = TimeUtils.convertTime(0L)
        assertEquals("00:00:00 01/01/1970", formatted)
    }

    @Test
    fun `convertTimeClock formats to hour and minute`() {
        val formatted = TimeUtils.convertTimeClock(0L)
        assertEquals("00:00", formatted)
    }

    @Test
    fun `dayTimestamp formats to date`() {
        val formatted = TimeUtils.dayTimestamp(0L)
        assertEquals("1970-01-01", formatted)
    }

    @Test
    fun `toTimeCount formats seconds into hours and minutes`() {
        assertEquals("1h 1m", 3660L.toTimeCount())
        assertEquals("0m", 0L.toTimeCount())
        assertEquals("59m", 3540L.toTimeCount())
    }

    @Test
    fun `toDate returns Never for zero`() {
        assertEquals("Never", 0L.toDate("yyyy-MM-dd"))
    }

    @Test
    fun `toDate formats epoch milliseconds`() {
        val millis = 1_700_000_000_000L
        val formatted = millis.toDate("yyyy-MM-dd HH:mm")
        assertEquals("2023-11-14 22:13", formatted)
    }
}
