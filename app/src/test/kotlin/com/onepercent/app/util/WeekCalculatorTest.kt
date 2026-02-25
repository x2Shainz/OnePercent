package com.onepercent.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class WeekCalculatorTest {

    // Fixed reference date: Wednesday 2026-02-25
    private val wednesday = LocalDate.of(2026, 2, 25)
    private val sunday    = LocalDate.of(2026, 2, 22)
    private val saturday  = LocalDate.of(2026, 2, 28)

    // --- currentWeekRange ---

    @Test
    fun currentWeekRange_whenWednesday_returnsCorrectSundayAndSaturday() {
        val range = WeekCalculator.currentWeekRange(wednesday)
        assertEquals(sunday, range.sunday)
        assertEquals(saturday, range.saturday)
    }

    @Test
    fun currentWeekRange_whenSunday_returnsSameDayAsSunday() {
        val range = WeekCalculator.currentWeekRange(sunday)
        assertEquals(sunday, range.sunday)
    }

    @Test
    fun currentWeekRange_whenSaturday_returnsSameDayAsSaturday() {
        val range = WeekCalculator.currentWeekRange(saturday)
        assertEquals(saturday, range.saturday)
    }

    @Test
    fun currentWeekRange_sundayIsAlwaysSunday() {
        val range = WeekCalculator.currentWeekRange(wednesday)
        assertEquals(DayOfWeek.SUNDAY, range.sunday.dayOfWeek)
    }

    @Test
    fun currentWeekRange_saturdayIsAlwaysSaturday() {
        val range = WeekCalculator.currentWeekRange(wednesday)
        assertEquals(DayOfWeek.SATURDAY, range.saturday.dayOfWeek)
    }

    // --- fourWeekRanges ---

    @Test
    fun fourWeekRanges_returnsExactlyFourItems() {
        val ranges = WeekCalculator.fourWeekRanges(wednesday)
        assertEquals(4, ranges.size)
    }

    @Test
    fun fourWeekRanges_firstRangeIsCurrentWeek() {
        val ranges = WeekCalculator.fourWeekRanges(wednesday)
        assertEquals(WeekCalculator.currentWeekRange(wednesday), ranges[0])
    }

    @Test
    fun fourWeekRanges_weeksAreConsecutive() {
        val ranges = WeekCalculator.fourWeekRanges(wednesday)
        for (i in 0 until ranges.size - 1) {
            assertEquals(
                "Week ${i + 1} saturday + 1 should equal week ${i + 2} sunday",
                ranges[i].saturday.plusDays(1),
                ranges[i + 1].sunday
            )
        }
    }

    @Test
    fun fourWeekRanges_noDuplicateWeeks() {
        val ranges = WeekCalculator.fourWeekRanges(wednesday)
        val sundays = ranges.map { it.sunday }
        assertEquals(sundays.size, sundays.distinct().size)
    }

    @Test
    fun fourWeekRanges_eachWeekIsSevenDays() {
        val ranges = WeekCalculator.fourWeekRanges(wednesday)
        ranges.forEach { range ->
            assertEquals(6, range.saturday.toEpochDay() - range.sunday.toEpochDay())
        }
    }

    // --- formatWeekLabel ---

    @Test
    fun formatWeekLabel_correctFormat() {
        val range = WeekRange(sunday, saturday)
        val label = WeekCalculator.formatWeekLabel(range)
        assertEquals("Sun. 2/22 - Sat. 2/28", label)
    }

    @Test
    fun formatWeekLabel_singleDigitMonthAndDay() {
        val sun = LocalDate.of(2026, 3, 1)
        val sat = LocalDate.of(2026, 3, 7)
        val label = WeekCalculator.formatWeekLabel(WeekRange(sun, sat))
        assertEquals("Sun. 3/1 - Sat. 3/7", label)
    }

    // --- formatDayTitle ---

    @Test
    fun formatDayTitle_correctFormat() {
        val title = WeekCalculator.formatDayTitle(wednesday)
        assertEquals("Wednesday 2/25", title)
    }

    @Test
    fun formatDayTitle_sunday() {
        val title = WeekCalculator.formatDayTitle(sunday)
        assertEquals("Sunday 2/22", title)
    }

    // --- weekStartEpochDay / weekRangeFromEpochDay round-trip ---

    @Test
    fun weekStartEpochDay_roundtrip() {
        val epochDay = WeekCalculator.weekStartEpochDay(wednesday)
        val reconstructed = WeekCalculator.weekRangeFromEpochDay(epochDay)
        assertEquals(WeekCalculator.currentWeekRange(wednesday), reconstructed)
    }

    @Test
    fun weekRangeFromEpochDay_sundayIsCorrect() {
        val epochDay = WeekCalculator.weekStartEpochDay(wednesday)
        val range = WeekCalculator.weekRangeFromEpochDay(epochDay)
        assertEquals(DayOfWeek.SUNDAY, range.sunday.dayOfWeek)
        assertEquals(sunday, range.sunday)
    }

    // --- daysInWeek ---

    @Test
    fun daysInWeek_returnsSevenDays() {
        val days = WeekCalculator.daysInWeek(WeekRange(sunday, saturday))
        assertEquals(7, days.size)
    }

    @Test
    fun daysInWeek_startsOnSunday() {
        val days = WeekCalculator.daysInWeek(WeekRange(sunday, saturday))
        assertEquals(DayOfWeek.SUNDAY, days.first().dayOfWeek)
    }

    @Test
    fun daysInWeek_endsOnSaturday() {
        val days = WeekCalculator.daysInWeek(WeekRange(sunday, saturday))
        assertEquals(DayOfWeek.SATURDAY, days.last().dayOfWeek)
    }

    @Test
    fun daysInWeek_areConsecutive() {
        val days = WeekCalculator.daysInWeek(WeekRange(sunday, saturday))
        for (i in 0 until days.size - 1) {
            assertEquals(days[i].plusDays(1), days[i + 1])
        }
    }

    @Test
    fun daysInWeek_firstDayMatchesSunday() {
        val range = WeekCalculator.currentWeekRange(wednesday)
        val days = WeekCalculator.daysInWeek(range)
        assertEquals(range.sunday, days[0])
    }

    @Test
    fun daysInWeek_lastDayMatchesSaturday() {
        val range = WeekCalculator.currentWeekRange(wednesday)
        val days = WeekCalculator.daysInWeek(range)
        assertEquals(range.saturday, days[6])
    }

    // --- pastWeekRanges ---
    // pastWeekRanges(earliest, exclusiveEnd) should return all Sun–Sat week ranges from
    // the week containing 'earliest' up to (but not including) the week that starts on
    // 'exclusiveEnd'. In production, exclusiveEnd = the Sunday of the current week.
    //
    // All tests use exclusiveEnd = 2026-02-22 (the "current" reference Sunday).

    @Test
    fun pastWeekRanges_whenEarliestEqualsExclusiveEnd_returnsEmpty() {
        // If the earliest task is on the same Sunday that starts the current window,
        // there are zero prior weeks to show — the list must be empty.
        val result = WeekCalculator.pastWeekRanges(sunday, sunday)
        assertTrue(result.isEmpty())
    }

    @Test
    fun pastWeekRanges_whenEarliestAfterExclusiveEnd_returnsEmpty() {
        // If the earliest task is already inside the current (or future) window,
        // there are no past weeks — the list must be empty.
        val result = WeekCalculator.pastWeekRanges(sunday.plusDays(3), sunday)
        assertTrue(result.isEmpty())
    }

    @Test
    fun pastWeekRanges_whenEarliestInPreviousWeek_returnsOneRange() {
        // One week before the current window → exactly one past-week range.
        val previousSunday = sunday.minusWeeks(1)
        val result = WeekCalculator.pastWeekRanges(previousSunday, sunday)
        assertEquals(1, result.size)
    }

    @Test
    fun pastWeekRanges_oneRange_hasCorrectBounds() {
        // The single range must span exactly the previous Sun–Sat
        // (i.e., starts on the Sunday one week back, ends the day before exclusiveEnd).
        val previousSunday = sunday.minusWeeks(1)
        val result = WeekCalculator.pastWeekRanges(previousSunday, sunday)
        assertEquals(previousSunday, result[0].sunday)
        assertEquals(sunday.minusDays(1), result[0].saturday)
    }

    @Test
    fun pastWeekRanges_whenEarliestTwoWeeksBefore_returnsTwoRanges() {
        // Two weeks before the current window → exactly two past-week ranges.
        val earliest = sunday.minusWeeks(2)
        val result = WeekCalculator.pastWeekRanges(earliest, sunday)
        assertEquals(2, result.size)
    }

    @Test
    fun pastWeekRanges_resultsAreConsecutive() {
        // Every range's Saturday + 1 day must equal the next range's Sunday.
        // Verifies there are no gaps or duplicate weeks in the result.
        val earliest = sunday.minusWeeks(3)
        val result = WeekCalculator.pastWeekRanges(earliest, sunday)
        for (i in 0 until result.size - 1) {
            assertEquals(result[i].saturday.plusDays(1), result[i + 1].sunday)
        }
    }

    @Test
    fun pastWeekRanges_lastRangeSaturdayIsDayBeforeExclusiveEnd() {
        // The final range must end on the Saturday immediately before exclusiveEnd's Sunday,
        // so there is no gap between past weeks and the current window.
        val earliest = sunday.minusWeeks(2)
        val result = WeekCalculator.pastWeekRanges(earliest, sunday)
        assertEquals(sunday.minusDays(1), result.last().saturday)
    }

    @Test
    fun pastWeekRanges_crossesMonthBoundary_isConsecutive() {
        // Verifies that week boundaries are correct even when they cross a month boundary
        // (Jan → Feb in this case), and that the last range still ends the day before exclusiveEnd.
        val earliest = LocalDate.of(2026, 1, 25)
        val exclusiveEnd = LocalDate.of(2026, 2, 22)
        val result = WeekCalculator.pastWeekRanges(earliest, exclusiveEnd)
        assertTrue(result.isNotEmpty())
        for (i in 0 until result.size - 1) {
            assertEquals(result[i].saturday.plusDays(1), result[i + 1].sunday)
        }
        assertEquals(exclusiveEnd.minusDays(1), result.last().saturday)
    }

    @Test
    fun pastWeekRanges_whenEarliestIsMidWeek_firstRangeStartsOnSunday() {
        // If the earliest task falls mid-week, the first returned range must still start
        // on that week's Sunday (not on the task date itself).
        val earliest = sunday.minusWeeks(3).plusDays(3) // a Wednesday
        val result = WeekCalculator.pastWeekRanges(earliest, sunday)
        assertEquals(DayOfWeek.SUNDAY, result[0].sunday.dayOfWeek)
        assertEquals(sunday.minusWeeks(3), result[0].sunday)
    }
}
