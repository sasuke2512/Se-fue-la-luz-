package com.example.domain

import java.text.Normalizer
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object SpanishFormatters {

    private val shortMonths = arrayOf(
        "ene", "feb", "mar", "abr", "may", "jun",
        "jul", "ago", "sep", "oct", "nov", "dic"
    )

    private val fullMonths = arrayOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
    )

    private val shortWeekdays = arrayOf(
        "", "Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"
    )

    private val compactWeekdayLetters = arrayOf(
        "", "D", "L", "M", "X", "J", "V", "S"
    )

    fun formatChronometer(elapsedMillis: Long): String {
        val totalSeconds = (elapsedMillis / 1000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun formatDurationReadable(durationMillis: Long): String {
        val safeMillis = durationMillis.coerceAtLeast(0L)
        if (safeMillis == 0L) return "0 min"
        val totalMinutes = safeMillis / 60_000L
        if (totalMinutes == 0L) return "< 1 min"
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return when {
            hours > 0L && minutes > 0L -> "$hours h $minutes min"
            hours > 0L -> "$hours h"
            else -> "$minutes min"
        }
    }

    fun formatDurationHoursDecimal(durationMillis: Long): String {
        val hours = durationMillis.coerceAtLeast(0L) / 3_600_000.0
        return if (hours == 0.0) "0 h" else String.format(Locale("es", "CU"), "%.1f h", hours)
    }

    fun formatTime12h(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = millis
        }
        val hour12 = cal.get(Calendar.HOUR).let { if (it == 0) 12 else it }
        val minute = cal.get(Calendar.MINUTE)
        val period = if (cal.get(Calendar.AM_PM) == Calendar.AM) "a. m." else "p. m."
        return String.format(Locale.US, "%d:%02d %s", hour12, minute, period)
    }

    fun formatTimeRange12h(
        startMillis: Long,
        endMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val startDay = DayKey.fromMillis(startMillis, timeZone)
        val endDay = DayKey.fromMillis(endMillis, timeZone)
        val startStr = formatTime12h(startMillis, timeZone)
        val endStr = formatTime12h(endMillis, timeZone)
        return if (startDay == endDay) {
            "$startStr – $endStr"
        } else {
            "$startStr – $endStr (${formatShortDayMonth(endDay)})"
        }
    }

    fun formatShortDayMonth(dayKey: DayKey): String {
        val monthIdx = (dayKey.month - 1).coerceIn(0, 11)
        return "${dayKey.dayOfMonth} ${shortMonths[monthIdx]}"
    }

    fun formatFullDate(dayKey: DayKey): String {
        val monthIdx = (dayKey.month - 1).coerceIn(0, 11)
        return "${dayKey.dayOfMonth} ${shortMonths[monthIdx]} ${dayKey.year}"
    }

    fun formatDateTime12h(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
        val dayKey = DayKey.fromMillis(millis, timeZone)
        return "${formatFullDate(dayKey)}, ${formatTime12h(millis, timeZone)}"
    }

    fun formatNumericDate(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
        val day = DayKey.fromMillis(millis, timeZone)
        return String.format(Locale.US, "%02d/%02d/%04d", day.dayOfMonth, day.month, day.year)
    }

    fun formatNumericDate(dayKey: DayKey): String {
        return String.format(Locale.US, "%02d/%02d/%04d", dayKey.dayOfMonth, dayKey.month, dayKey.year)
    }

    fun formatDayHeader(
        dayKey: DayKey,
        todayKey: DayKey,
        yesterdayKey: DayKey,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val dateText = formatFullDate(dayKey)
        return when (dayKey) {
            todayKey -> "Hoy · $dateText"
            yesterdayKey -> "Ayer · $dateText"
            else -> {
                val cal = Calendar.getInstance(timeZone).apply {
                    clear()
                    set(dayKey.year, dayKey.month - 1, dayKey.dayOfMonth, 12, 0, 0)
                }
                val weekday = shortWeekdays[cal.get(Calendar.DAY_OF_WEEK)]
                "$weekday · $dateText"
            }
        }
    }

    fun formatChartDayLabel(dayKey: DayKey, timeZone: TimeZone = TimeZone.getDefault()): String {
        val cal = Calendar.getInstance(timeZone).apply {
            clear()
            set(dayKey.year, dayKey.month - 1, dayKey.dayOfMonth, 12, 0, 0)
        }
        val letter = compactWeekdayLetters[cal.get(Calendar.DAY_OF_WEEK)]
        return "$letter\n${dayKey.dayOfMonth}"
    }

    fun formatMonthYear(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
        val cal = Calendar.getInstance(timeZone).apply { timeInMillis = millis }
        val monthIdx = cal.get(Calendar.MONTH).coerceIn(0, 11)
        val year = cal.get(Calendar.YEAR)
        return "${fullMonths[monthIdx]} $year"
    }

    fun formatOutageCountPhrase(count: Int): String {
        return if (count == 1) "1 apagón" else "$count apagones"
    }

    fun buildTodayClipboardSummary(
        nowMillis: Long,
        todayStats: PeriodStats,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val dateStr = formatNumericDate(nowMillis, timeZone)
        val countPhrase = formatOutageCountPhrase(todayStats.outageCount)
        val withoutStr = formatDurationReadable(todayStats.withoutPowerMillis)
        val withStr = formatDurationReadable(todayStats.withPowerMillis)
        val longestStr = formatDurationReadable(todayStats.longestOutageMillis)
        return "Se fue la luz! – $dateStr: hoy hubo $countPhrase, $withoutStr sin luz y $withStr con luz. Apagón más largo: $longestStr."
    }

    fun buildMonthClipboardSummary(
        nowMillis: Long,
        monthStats: PeriodStats,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val monthStr = formatMonthYear(nowMillis, timeZone)
        val countPhrase = formatOutageCountPhrase(monthStats.outageCount)
        val withoutStr = formatDurationReadable(monthStats.withoutPowerMillis)
        val withStr = formatDurationReadable(monthStats.withPowerMillis)
        val longestStr = formatDurationReadable(monthStats.longestOutageMillis)
        return "Se fue la luz! – $monthStr: este mes hubo $countPhrase, $withoutStr sin luz y $withStr con luz. Apagón más largo: $longestStr."
    }

    fun normalizeForSearch(text: String): String {
        val normalized = Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }
}
