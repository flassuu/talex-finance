package io.github.flassuu.talex.feature.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class AnalyticsPeriodTest {

    @Test
    fun monthUsesDays() {
        val buckets = periodBuckets(AnalyticsPeriod.Month(YearMonth.of(2026, 3)))
        assertEquals(31, buckets.size)
        assertEquals(LocalDate.of(2026, 3, 1), buckets.first().start)
        assertEquals(LocalDate.of(2026, 3, 31), buckets.last().end)
    }

    @Test
    fun yearUsesMonths() {
        val buckets = periodBuckets(AnalyticsPeriod.Year(2026))
        assertEquals(12, buckets.size)
        assertEquals(LocalDate.of(2026, 1, 1), buckets.first().start)
        assertEquals(LocalDate.of(2026, 12, 31), buckets.last().end)
    }

    @Test
    fun shortRangeUsesDays() {
        val buckets = periodBuckets(
            AnalyticsPeriod.Range(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 10)),
        )
        assertEquals(10, buckets.size)
    }

    @Test
    fun longRangeUsesMonths() {
        val buckets = periodBuckets(
            AnalyticsPeriod.Range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30)),
        )
        assertEquals(6, buckets.size)
    }

    @Test
    fun navigatesMonthsAndYears() {
        val month = AnalyticsPeriod.Month(YearMonth.of(2026, 3))
        assertEquals(YearMonth.of(2026, 2), (month.previous() as AnalyticsPeriod.Month).yearMonth)
        assertEquals(YearMonth.of(2026, 4), (month.next() as AnalyticsPeriod.Month).yearMonth)

        val year = AnalyticsPeriod.Year(2026)
        assertEquals(2025, (year.previous() as AnalyticsPeriod.Year).year)
        assertEquals(2027, (year.next() as AnalyticsPeriod.Year).year)
    }

    @Test
    fun rangeRequiresOrderedDates() {
        assertNotNull(AnalyticsPeriod.rangeOrNull(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 5)))
        assertNull(AnalyticsPeriod.rangeOrNull(LocalDate.of(2026, 3, 5), LocalDate.of(2026, 3, 1)))
        assertNull(AnalyticsPeriod.rangeOrNull(null, LocalDate.of(2026, 3, 1)))
    }
}
