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
        val now = millisAt(2026, 11, 25, 14, 0)
        val trackingStart = millisAt(2026, 11, 1, 0, 0)

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
            trackingStartMillis = trackingStart,
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
        // Outage 1 (5h 30m) + Outage 2 (1h 15m) = 6h 45m without power; 2 distinct outages
        assertEquals(6 * hourMs + 45 * minMs, snapshot.thisWeek.withoutPowerMillis)
        assertEquals(62 * hourMs - (6 * hourMs + 45 * minMs), snapshot.thisWeek.withPowerMillis)
        assertEquals(2, snapshot.thisWeek.outageCount)

        // This month (Nov 1 00:00 -> Nov 25 14:00 = 590h elapsed):
        val expectedMonthWithout = 10 * hourMs + 28 * minMs
        assertEquals(expectedMonthWithout, snapshot.thisMonth.withoutPowerMillis)
        assertEquals(590 * hourMs - expectedMonthWithout, snapshot.thisMonth.withPowerMillis)
        assertEquals(3, snapshot.thisMonth.outageCount)

        // Last 7 days bar chart check: Nov 24 = 2h, Nov 25 = 4h 45m
        val nov24Bucket = snapshot.last7Days.first { it.dayKey == DayKey(2026, 11, 24) }
        val nov25Bucket = snapshot.last7Days.first { it.dayKey == DayKey(2026, 11, 25) }
        assertEquals(2 * hourMs, nov24Bucket.withoutPowerMillis)
        assertEquals(4 * hourMs + 45 * minMs, nov25Bucket.withoutPowerMillis)

        // Tracking started Nov 1 -> 7 active days in 7-day window, 25 active days in 30-day window (Nov 1..25)
        assertEquals((6 * hourMs + 45 * minMs) / 7L, snapshot.avgDailyLast7DaysMillis)
        assertEquals(expectedMonthWithout / 25L, snapshot.avgDailyLast30DaysMillis)

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
    fun `with power counts only from tracking start and excludes prior unmonitored hours`() {
        // First use today (Nov 23) at 18:00, queried today at 20:00 with a 30-minute outage (19:00 -> 19:30)
        val trackingStart = millisAt(2026, 11, 23, 18, 0)
        val now = millisAt(2026, 11, 23, 20, 0)
        val outage = OutageEntity(
            id = 10L,
            startTimeMillis = millisAt(2026, 11, 23, 19, 0),
            endTimeMillis = millisAt(2026, 11, 23, 19, 30)
        )

        val snapshot = OutageStatsCalculator.calculate(
            outages = listOf(outage),
            nowMillis = now,
            trackingStartMillis = trackingStart,
            timeZone = havanaTz
        )

        val hourMs = 3_600_000L
        val minMs = 60_000L

        assertEquals(trackingStart, snapshot.effectiveTrackingStartMillis)

        // Today: 2h tracked (18:00 -> 20:00) minus 30 min outage = 1h 30m with power
        assertEquals(30 * minMs, snapshot.today.withoutPowerMillis)
        assertEquals(1 * hourMs + 30 * minMs, snapshot.today.withPowerMillis)
        assertEquals("1 h 30 min", SpanishFormatters.formatDurationReadable(snapshot.today.withPowerMillis))

        // Month: MUST NOT include any hours before 18:00 -> also 1h 30m with power
        assertEquals(30 * minMs, snapshot.thisMonth.withoutPowerMillis)
        assertEquals(1 * hourMs + 30 * minMs, snapshot.thisMonth.withPowerMillis)

        // Week: also 1h 30m with power
        assertEquals(1 * hourMs + 30 * minMs, snapshot.thisWeek.withPowerMillis)

        // Daily averages divide by 1 day (started today), not 7 or 30
        assertEquals(30 * minMs, snapshot.avgDailyLast7DaysMillis)
        assertEquals(30 * minMs, snapshot.avgDailyLast30DaysMillis)
    }

    @Test
    fun `manual outage older than first launch shifts effective tracking start earlier`() {
        val firstLaunch = millisAt(2026, 11, 23, 18, 0)
        val now = millisAt(2026, 11, 23, 20, 0)

        // User manually records an older outage on Nov 21 from 10:00 to 12:00 (2h)
        val oldManual = OutageEntity(
            id = 20L,
            startTimeMillis = millisAt(2026, 11, 21, 10, 0),
            endTimeMillis = millisAt(2026, 11, 21, 12, 0)
        )

        val snapshot = OutageStatsCalculator.calculate(
            outages = listOf(oldManual),
            nowMillis = now,
            trackingStartMillis = firstLaunch,
            timeZone = havanaTz
        )

        val hourMs = 3_600_000L

        // Effective tracking start shifts to Nov 21 10:00
        assertEquals(oldManual.startTimeMillis, snapshot.effectiveTrackingStartMillis)

        // Today (Nov 23): full 20h elapsed since 00:00 because tracking started Nov 21
        assertEquals(0L, snapshot.today.withoutPowerMillis)
        assertEquals(20 * hourMs, snapshot.today.withPowerMillis)

        // Month: window starts at Nov 21 10:00 -> Nov 23 20:00 = 58h, minus 2h outage = 56h with power
        assertEquals(2 * hourMs, snapshot.thisMonth.withoutPowerMillis)
        assertEquals(56 * hourMs, snapshot.thisMonth.withPowerMillis)

        // Daily average: Nov 21, 22, 23 = 3 days elapsed -> 2h / 3 = 40 min/day
        assertEquals((2 * hourMs) / 3L, snapshot.avgDailyLast7DaysMillis)
        assertEquals((2 * hourMs) / 3L, snapshot.avgDailyLast30DaysMillis)
    }

    @Test
    fun `spanish 12h formatting and clipboard summary match expected pattern`() {
        val start = millisAt(2026, 11, 23, 6, 32)
        val end = millisAt(2026, 11, 23, 10, 15)
        assertEquals("6:32 a. m. – 10:15 a. m.", SpanishFormatters.formatTimeRange12h(start, end, havanaTz))
        assertEquals("3 h 43 min", SpanishFormatters.formatDurationReadable(end - start))
        assertEquals("00:05:09", SpanishFormatters.formatChronometer(309_000L))
        assertEquals("23 nov 2026, 6:32 a. m.", SpanishFormatters.formatDateTime12h(start, havanaTz))

        val now = millisAt(2026, 11, 23, 12, 0)
        val todayStats = OutageStatsCalculator.calculatePeriodStats(
            outages = listOf(OutageEntity(id = 1, startTimeMillis = start, endTimeMillis = end)),
            windowStartMillis = DayKey(2026, 11, 23).startOfDayMillis(havanaTz),
            windowEndMillis = now,
            effectiveTrackingStartMillis = DayKey(2026, 11, 23).startOfDayMillis(havanaTz)
        )
        val summary = SpanishFormatters.buildTodayClipboardSummary(now, todayStats, havanaTz)
        assertEquals(
            "Se fue la luz! – 23/11/2026: hoy hubo 1 apagón, 3 h 43 min sin luz y 8 h 17 min con luz. Apagón más largo: 3 h 43 min.",
            summary
        )
    }
}
