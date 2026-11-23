package com.example

import com.example.data.local.OutageEntity
import com.example.domain.DayKey
import com.example.domain.OutageStatsCalculator
import com.example.domain.SpanishFormatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class OutageStatsCalculatorTest {

    private val havanaTz: TimeZone = TimeZone.getTimeZone("America/Havana")

    private fun millisAt(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        second: Int = 0
    ): Long {
        return Calendar.getInstance(havanaTz).apply {
            clear()
            set(year, month - 1, day, hour, minute, second)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun `cross midnight outage splits accurately and reconciles today week and month totals`() {
        // Nov 23, 2026 is a Monday. So Nov 22 (Sunday) was previous week!
        // Use Wednesday Nov 25, 2026 at 14:00 as "now" so Tuesday Nov 24 and Wednesday Nov 25 are both in the same week & month.
        val now = millisAt(2026, 11, 25, 14, 0)

        // Outage 1 (crosses midnight Tue Nov 24 22:00 -> Wed Nov 25 03:30 = 5h 30m total: 2h on Nov 24, 3h 30m on Nov 25)
        val crossMidnight = OutageEntity(
            id = 1L,
            startTimeMillis = millisAt(2026, 11, 24, 22, 0),
            endTimeMillis = millisAt(2026, 11, 25, 3, 30)
        )

        // Outage 2 (today Wed Nov 25 09:00 -> 10:15 = 1h 15m)
        val todayMorning = OutageEntity(
            id = 2L,
            startTimeMillis = millisAt(2026, 11, 25, 9, 0),
            endTimeMillis = millisAt(2026, 11, 25, 10, 15)
        )

        // Outage 3 (earlier this month Mon Nov 2 06:32 -> 10:15 = 3h 43m, NOT in current week)
        val earlierMonth = OutageEntity(
            id = 3L,
            startTimeMillis = millisAt(2026, 11, 2, 6, 32),
            endTimeMillis = millisAt(2026, 11, 2, 10, 15)
        )

        val snapshot = OutageStatsCalculator.calculate(
            outages = listOf(crossMidnight, todayMorning, earlierMonth),
            nowMillis = now,
            timeZone = havanaTz
        )

        val hourMs = 3_600_000L
        val minMs = 60_000L

        // Today (Nov 25 00:00 -> 14:00 = 14h elapsed): 3h 30m + 1h 15m = 4h 45m without power; 9h 15m with power; 2 outages
        assertEquals(4 * hourMs + 45 * minMs, snapshot.today.withoutPowerMillis)
        assertEquals(9 * hourMs + 15 * minMs, snapshot.today.withPowerMillis)
        assertEquals(2, snapshot.today.outageCount)
        assertEquals(3 * hourMs + 30 * minMs, snapshot.today.longestOutageMillis)

        // This week (Mon Nov 23 00:00 -> Wed Nov 25 14:00 = 62h elapsed):
        // Outage 1 (5h 30m) + Outage 2 (1h 15m) = 6h 45m without power; 2 distinct outages (cross-midnight not double counted!)
        assertEquals(6 * hourMs + 45 * minMs, snapshot.thisWeek.withoutPowerMillis)
        assertEquals(62 * hourMs - (6 * hourMs + 45 * minMs), snapshot.thisWeek.withPowerMillis)
        assertEquals(2, snapshot.thisWeek.outageCount)

        // This month (Nov 1 00:00 -> Nov 25 14:00 = 24*24h + 14h = 590h elapsed):
        // 5h 30m + 1h 15m + 3h 43m = 10h 28m without power; 3 distinct outages
        val expectedMonthWithout = 10 * hourMs + 28 * minMs
        assertEquals(expectedMonthWithout, snapshot.thisMonth.withoutPowerMillis)
        assertEquals(590 * hourMs - expectedMonthWithout, snapshot.thisMonth.withPowerMillis)
        assertEquals(3, snapshot.thisMonth.outageCount)

        // Last 7 days bar chart check: Nov 24 = 2h, Nov 25 = 4h 45m
        val nov24Bucket = snapshot.last7Days.first { it.dayKey == DayKey(2026, 11, 24) }
        val nov25Bucket = snapshot.last7Days.first { it.dayKey == DayKey(2026, 11, 25) }
        assertEquals(2 * hourMs, nov24Bucket.withoutPowerMillis)
        assertEquals(4 * hourMs + 45 * minMs, nov25Bucket.withoutPowerMillis)

        // Last 7 days average = (6h 45m) / 7
        assertEquals((6 * hourMs + 45 * minMs) / 7L, snapshot.avgDailyLast7DaysMillis)
        // Last 30 days average = (10h 28m) / 30 (since Nov 2 is within last 30 days of Nov 25)
        assertEquals(expectedMonthWithout / 30L, snapshot.avgDailyLast30DaysMillis)

        // Longest single outage is the 5h 30m cross-midnight one
        val longest = snapshot.longestRecordedOutage
        assertNotNull(longest)
        assertEquals(1L, longest!!.outage.id)
        assertEquals(5 * hourMs + 30 * minMs, longest.durationMillis)

        // Worst day is Nov 25 with 4h 45m (after midnight splitting!)
        val worst = snapshot.worstDay
        assertNotNull(worst)
        assertEquals(DayKey(2026, 11, 25), worst!!.dayKey)
        assertEquals(4 * hourMs + 45 * minMs, worst.withoutPowerMillis)
    }

    @Test
    fun `spanish 12h formatting and clipboard summary match expected pattern`() {
        val start = millisAt(2026, 11, 23, 6, 32)
        val end = millisAt(2026, 11, 23, 10, 15)
        assertEquals("6:32 a. m. – 10:15 a. m.", SpanishFormatters.formatTimeRange12h(start, end, havanaTz))
        assertEquals("3 h 43 min", SpanishFormatters.formatDurationReadable(end - start))
        assertEquals("00:05:09", SpanishFormatters.formatChronometer(309_000L))

        val now = millisAt(2026, 11, 23, 12, 0)
        val todayStats = OutageStatsCalculator.calculatePeriodStats(
            outages = listOf(OutageEntity(id = 1, startTimeMillis = start, endTimeMillis = end)),
            windowStartMillis = DayKey(2026, 11, 23).startOfDayMillis(havanaTz),
            windowEndMillis = now
        )
        val summary = SpanishFormatters.buildTodayClipboardSummary(now, todayStats, havanaTz)
        assertEquals(
            "Se fue la luz! – 23/11/2026: hoy hubo 1 apagón, 3 h 43 min sin luz y 8 h 17 min con luz. Apagón más largo: 3 h 43 min.",
            summary
        )
    }
}
