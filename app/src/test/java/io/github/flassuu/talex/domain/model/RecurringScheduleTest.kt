package io.github.flassuu.talex.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RecurringScheduleTest {

    @Test
    fun weeklyAddsSevenDays() {
        val anchor = LocalDate.of(2026, 1, 5)
        assertEquals(LocalDate.of(2026, 1, 12), RecurringSchedule.occurrence(anchor, Frequency.WEEKLY, 1))
        assertEquals(LocalDate.of(2026, 1, 26), RecurringSchedule.occurrence(anchor, Frequency.WEEKLY, 3))
    }

    @Test
    fun monthlyClampsToTheLastDay() {
        val anchor = LocalDate.of(2026, 1, 31)
        assertEquals(LocalDate.of(2026, 2, 28), RecurringSchedule.occurrence(anchor, Frequency.MONTHLY, 1))
        assertEquals(LocalDate.of(2026, 3, 31), RecurringSchedule.occurrence(anchor, Frequency.MONTHLY, 2))
    }

    @Test
    fun leapYearClampsToFebruary29() {
        val anchor = LocalDate.of(2024, 1, 31)
        assertEquals(LocalDate.of(2024, 2, 29), RecurringSchedule.occurrence(anchor, Frequency.MONTHLY, 1))
    }

    @Test
    fun firstOnOrAfterSkipsPastOccurrences() {
        val anchor = LocalDate.of(2026, 1, 31)
        assertEquals(
            LocalDate.of(2026, 2, 28),
            RecurringSchedule.firstOnOrAfter(anchor, Frequency.MONTHLY, LocalDate.of(2026, 2, 15)),
        )
        assertEquals(
            LocalDate.of(2026, 3, 31),
            RecurringSchedule.firstOnOrAfter(anchor, Frequency.MONTHLY, LocalDate.of(2026, 3, 1)),
        )
        assertEquals(
            anchor,
            RecurringSchedule.firstOnOrAfter(anchor, Frequency.MONTHLY, LocalDate.of(2026, 1, 1)),
        )
    }

    @Test
    fun yearlyAddsYears() {
        val anchor = LocalDate.of(2024, 2, 29)
        assertEquals(LocalDate.of(2025, 2, 28), RecurringSchedule.occurrence(anchor, Frequency.YEARLY, 1))
        assertEquals(LocalDate.of(2028, 2, 29), RecurringSchedule.occurrence(anchor, Frequency.YEARLY, 4))
    }
}
