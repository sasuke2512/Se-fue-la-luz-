package com.example.domain

import com.example.data.local.OutageEntity
import java.util.Calendar
import java.util.TimeZone

data class DayKey(
    val year: Int,
    val month: Int, // 1..12
    val dayOfMonth: Int // 1..31
) : Comparable<DayKey> {

    override fun compareTo(other: DayKey): Int {
        return compareValuesBy(this, other, DayKey::year, DayKey::month, DayKey::dayOfMonth)
    }

    fun startOfDayMillis(timeZone: TimeZone = TimeZone.getDefault()): Long {
        val cal = Calendar.getInstance(timeZone).apply {
            clear()
            set(year, month - 1, dayOfMonth, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun nextDay(timeZone: TimeZone = TimeZone.getDefault()): DayKey {
        val cal = Calendar.getInstance(timeZone).apply {
            clear()
            set(year, month - 1, dayOfMonth, 12, 0, 0)
            add(Calendar.DAY_OF_YEAR, 1)
        }
        return fromCalendar(cal)
    }

    fun addDays(days: Int, timeZone: TimeZone = TimeZone.getDefault()): DayKey {
        val cal = Calendar.getInstance(timeZone).apply {
            clear()
            set(year, month - 1, dayOfMonth, 12, 0, 0)
            add(Calendar.DAY_OF_YEAR, days)
        }
        return fromCalendar(cal)
    }

    companion object {
        fun fromMillis(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): DayKey {
            val cal = Calendar.getInstance(timeZone).apply {
                timeInMillis = millis
            }
            return fromCalendar(cal)
        }

        fun fromCalendar(cal: Calendar): DayKey {
            return DayKey(
                year = cal.get(Calendar.YEAR),
                month = cal.get(Calendar.MONTH) + 1,
                dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
            )
        }
    }
}

data class PeriodStats(
    val withoutPowerMillis: Long,
    val withPowerMillis: Long,
    val outageCount: Int,
    val longestOutageMillis: Long
)

data class DayOutageTotal(
    val dayKey: DayKey,
    val withoutPowerMillis: Long,
    val outageCount: Int
)

data class LongestOutageRecord(
    val outage: OutageEntity,
    val durationMillis: Long
)

data class OutageStatsSnapshot(
    val today: PeriodStats,
    val thisWeek: PeriodStats,
    val thisMonth: PeriodStats,
    val avgDailyLast7DaysMillis: Long,
    val avgDailyLast30DaysMillis: Long,
    val longestRecordedOutage: LongestOutageRecord?,
    val worstDay: DayOutageTotal?,
    val last7Days: List<DayOutageTotal>
)

object OutageStatsCalculator {

    fun calculate(
        outages: List<OutageEntity>,
        nowMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): OutageStatsSnapshot {
        val todayKey = DayKey.fromMillis(nowMillis, timeZone)
        val todayStart = todayKey.startOfDayMillis(timeZone)
        val weekStart = startOfWeekMillis(nowMillis, timeZone)
        val monthStart = startOfMonthMillis(nowMillis, timeZone)

        val todayStats = calculatePeriodStats(
            outages = outages,
            windowStartMillis = todayStart,
            windowEndMillis = nowMillis
        )
        val weekStats = calculatePeriodStats(
            outages = outages,
            windowStartMillis = weekStart,
            windowEndMillis = nowMillis
        )
        val monthStats = calculatePeriodStats(
            outages = outages,
            windowStartMillis = monthStart,
            windowEndMillis = nowMillis
        )

        val dailyBuckets = buildDailyBuckets(
            outages = outages,
            nowMillis = nowMillis,
            timeZone = timeZone
        )

        val last7Days = (-6..0).map { offset ->
            val day = todayKey.addDays(offset, timeZone)
            dailyBuckets[day] ?: DayOutageTotal(dayKey = day, withoutPowerMillis = 0L, outageCount = 0)
        }

        val last30DaysTotal = (-29..0).sumOf { offset ->
            val day = todayKey.addDays(offset, timeZone)
            dailyBuckets[day]?.withoutPowerMillis ?: 0L
        }

        val avg7 = last7Days.sumOf { it.withoutPowerMillis } / 7L
        val avg30 = last30DaysTotal / 30L

        val longestRecorded = outages
            .mapNotNull { outage ->
                val end = (outage.endTimeMillis ?: nowMillis).coerceAtMost(nowMillis)
                val duration = end - outage.startTimeMillis
                if (duration > 0L) LongestOutageRecord(outage = outage, durationMillis = duration) else null
            }
            .maxWithOrNull(
                compareBy<LongestOutageRecord> { it.durationMillis }
                    .thenBy { it.outage.startTimeMillis }
            )

        val worstDay = dailyBuckets.values
            .filter { it.withoutPowerMillis > 0L }
            .maxWithOrNull(
                compareBy<DayOutageTotal> { it.withoutPowerMillis }
                    .thenBy { it.dayKey }
            )

        return OutageStatsSnapshot(
            today = todayStats,
            thisWeek = weekStats,
            thisMonth = monthStats,
            avgDailyLast7DaysMillis = avg7,
            avgDailyLast30DaysMillis = avg30,
            longestRecordedOutage = longestRecorded,
            worstDay = worstDay,
            last7Days = last7Days
        )
    }

    fun calculatePeriodStats(
        outages: List<OutageEntity>,
        windowStartMillis: Long,
        windowEndMillis: Long
    ): PeriodStats {
        if (windowEndMillis <= windowStartMillis) {
            return PeriodStats(0L, 0L, 0, 0L)
        }

        val clippedIntervals = mutableListOf<Pair<Long, Long>>()
        var count = 0
        var longestInWindow = 0L

        for (outage in outages) {
            val effectiveEnd = (outage.endTimeMillis ?: windowEndMillis).coerceAtMost(windowEndMillis)
            val start = outage.startTimeMillis
            if (effectiveEnd < start) continue

            val overlaps = if (effectiveEnd == start) {
                start in windowStartMillis until windowEndMillis
            } else {
                effectiveEnd > windowStartMillis && start < windowEndMillis
            }

            if (overlaps) {
                count++
                val clippedStart = maxOf(start, windowStartMillis)
                val clippedEnd = minOf(effectiveEnd, windowEndMillis)
                val duration = (clippedEnd - clippedStart).coerceAtLeast(0L)
                if (duration > 0L) {
                    clippedIntervals.add(clippedStart to clippedEnd)
                    if (duration > longestInWindow) {
                        longestInWindow = duration
                    }
                }
            }
        }

        val withoutPower = sumMergedIntervals(clippedIntervals)
        val totalElapsed = (windowEndMillis - windowStartMillis).coerceAtLeast(0L)
        val withPower = (totalElapsed - withoutPower).coerceAtLeast(0L)

        return PeriodStats(
            withoutPowerMillis = withoutPower,
            withPowerMillis = withPower,
            outageCount = count,
            longestOutageMillis = longestInWindow
        )
    }

    fun buildDailyBuckets(
        outages: List<OutageEntity>,
        nowMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Map<DayKey, DayOutageTotal> {
        val intervalsByDay = mutableMapOf<DayKey, MutableList<Pair<Long, Long>>>()
        val countByDay = mutableMapOf<DayKey, Int>()

        for (outage in outages) {
            val start = outage.startTimeMillis
            val effectiveEnd = (outage.endTimeMillis ?: nowMillis).coerceAtMost(nowMillis)
            if (effectiveEnd < start) continue

            if (effectiveEnd == start) {
                val day = DayKey.fromMillis(start, timeZone)
                countByDay[day] = (countByDay[day] ?: 0) + 1
                intervalsByDay.getOrPut(day) { mutableListOf() }
                continue
            }

            var currentDay = DayKey.fromMillis(start, timeZone)
            var currentDayStart = currentDay.startOfDayMillis(timeZone)

            while (currentDayStart < effectiveEnd) {
                val nextDay = currentDay.nextDay(timeZone)
                val nextDayStart = nextDay.startOfDayMillis(timeZone)

                val sliceStart = maxOf(start, currentDayStart)
                val sliceEnd = minOf(effectiveEnd, nextDayStart)

                if (sliceEnd > sliceStart) {
                    intervalsByDay.getOrPut(currentDay) { mutableListOf() }
                        .add(sliceStart to sliceEnd)
                    countByDay[currentDay] = (countByDay[currentDay] ?: 0) + 1
                }

                currentDay = nextDay
                currentDayStart = nextDayStart
            }
        }

        return intervalsByDay.mapValues { (day, intervals) ->
            DayOutageTotal(
                dayKey = day,
                withoutPowerMillis = sumMergedIntervals(intervals),
                outageCount = countByDay[day] ?: 0
            )
        }
    }

    fun sumMergedIntervals(intervals: List<Pair<Long, Long>>): Long {
        if (intervals.isEmpty()) return 0L
        val sorted = intervals.sortedWith(compareBy<Pair<Long, Long>> { it.first }.thenBy { it.second })
        var total = 0L
        var currentStart = sorted[0].first
        var currentEnd = sorted[0].second

        for (i in 1 until sorted.size) {
            val (nextStart, nextEnd) = sorted[i]
            if (nextStart <= currentEnd) {
                currentEnd = maxOf(currentEnd, nextEnd)
            } else {
                total += (currentEnd - currentStart).coerceAtLeast(0L)
                currentStart = nextStart
                currentEnd = nextEnd
            }
        }
        total += (currentEnd - currentStart).coerceAtLeast(0L)
        return total
    }

    fun startOfWeekMillis(nowMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): Long {
        val cal = Calendar.getInstance(timeZone).apply {
            firstDayOfWeek = Calendar.MONDAY
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysSinceMonday = when (dayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
        cal.add(Calendar.DAY_OF_YEAR, -daysSinceMonday)
        return cal.timeInMillis
    }

    fun startOfMonthMillis(nowMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): Long {
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
