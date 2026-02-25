package com.onepercent.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
}
