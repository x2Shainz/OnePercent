package com.onepercent.app.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/** A Sun–Sat week range (both ends inclusive). */
data class WeekRange(
    val sunday: LocalDate,
    val saturday: LocalDate
)

/**
 * Pure utility for Sun–Sat week range computation and formatting.
 * Contains no Android dependencies and is fully testable on the JVM.
 */
object WeekCalculator {

    private val weekLabelFormatter = DateTimeFormatter.ofPattern("EEE. M/d")
    private val dayTitleFormatter  = DateTimeFormatter.ofPattern("EEEE M/d")

    /**
     * Returns the [WeekRange] whose Sunday–Saturday span contains [date].
     * If [date] is already a Sunday, it is the start of the returned range.
     */
    fun currentWeekRange(date: LocalDate): WeekRange {
        val sunday   = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val saturday = sunday.plusDays(6)
        return WeekRange(sunday, saturday)
    }

    /**
     * Returns exactly 4 consecutive [WeekRange]s starting with the week containing [date],
     * followed by the 3 upcoming weeks.
     */
    fun fourWeekRanges(date: LocalDate): List<WeekRange> {
        val first = currentWeekRange(date)
        return List(4) { i ->
            val sunday = first.sunday.plusWeeks(i.toLong())
            WeekRange(sunday, sunday.plusDays(6))
        }
    }

    /**
     * Formats a [WeekRange] as a sidebar label, e.g. "Sun. 2/22 - Sat. 2/28".
     */
    fun formatWeekLabel(range: WeekRange): String =
        "${range.sunday.format(weekLabelFormatter)} - ${range.saturday.format(weekLabelFormatter)}"

    /**
     * Formats a single [date] as a pager page title, e.g. "Sunday 2/22".
     */
    fun formatDayTitle(date: LocalDate): String =
        date.format(dayTitleFormatter)

    /**
     * Returns the epoch day (via [LocalDate.toEpochDay]) for [date]'s week Sunday.
     * Used as the route argument for the weekly pager screen.
     */
    fun weekStartEpochDay(date: LocalDate): Long =
        currentWeekRange(date).sunday.toEpochDay()

    /**
     * Reconstructs the [WeekRange] from a Sunday's epoch day.
     * Inverse of [weekStartEpochDay].
     */
    fun weekRangeFromEpochDay(epochDay: Long): WeekRange {
        val sunday = LocalDate.ofEpochDay(epochDay)
        return WeekRange(sunday, sunday.plusDays(6))
    }

    /**
     * Returns the 7 [LocalDate]s in [range], ordered Sunday through Saturday.
     */
    fun daysInWeek(range: WeekRange): List<LocalDate> =
        List(7) { i -> range.sunday.plusDays(i.toLong()) }

    /**
     * Returns all [WeekRange]s from the week containing [earliest] up to but not including
     * the week starting on [exclusiveEnd] (the Sunday of the current 4-week window).
     *
     * Returns an empty list if [earliest] is on or after [exclusiveEnd]. When [earliest]
     * falls mid-week, the first returned range starts on that week's Sunday.
     */
    fun pastWeekRanges(earliest: LocalDate, exclusiveEnd: LocalDate): List<WeekRange> {
        val firstSunday = earliest.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        if (!firstSunday.isBefore(exclusiveEnd)) return emptyList()
        val ranges = mutableListOf<WeekRange>()
        var sunday = firstSunday
        while (sunday.isBefore(exclusiveEnd)) {
            ranges.add(WeekRange(sunday, sunday.plusDays(6)))
            sunday = sunday.plusWeeks(1)
        }
        return ranges
    }
}
