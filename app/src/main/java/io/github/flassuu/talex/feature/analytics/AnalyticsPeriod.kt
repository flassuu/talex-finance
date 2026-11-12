package io.github.flassuu.talex.feature.analytics

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * The reporting window used by analytics and the budget screen. It is deliberately
 * a small value model so the same selector drives every report.
 */
sealed interface AnalyticsPeriod {

    data class Month(val yearMonth: YearMonth) : AnalyticsPeriod

    data class Year(val year: Int) : AnalyticsPeriod

    data class Range(val start: LocalDate, val end: LocalDate) : AnalyticsPeriod

    val startDate: LocalDate
        get() = when (this) {
            is Month -> yearMonth.atDay(1)
            is Year -> LocalDate.of(year, 1, 1)
            is Range -> start
        }

    val endDate: LocalDate
        get() = when (this) {
            is Month -> yearMonth.atEndOfMonth()
            is Year -> LocalDate.of(year, 12, 31)
            is Range -> end
        }

    fun previous(): AnalyticsPeriod = when (this) {
        is Month -> Month(yearMonth.minusMonths(1))
        is Year -> Year(year - 1)
        is Range -> this
    }

    fun next(): AnalyticsPeriod = when (this) {
        is Month -> Month(yearMonth.plusMonths(1))
        is Year -> Year(year + 1)
        is Range -> this
    }

    fun label(locale: Locale = Locale.getDefault()): String = when (this) {
        is Month -> yearMonth.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
        is Year -> year.toString()
        is Range -> "${start.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))} – " +
            end.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    }

    companion object {
        fun current(now: LocalDate = LocalDate.now()): AnalyticsPeriod =
            Month(YearMonth.from(now))

        /** A range is valid only when both dates are present and ordered. */
        fun rangeOrNull(start: LocalDate?, end: LocalDate?): Range? =
            if (start != null && end != null && !end.isBefore(start)) Range(start, end) else null
    }
}

/** One bucket on the time axis. */
data class PeriodBucket(
    val start: LocalDate,
    val end: LocalDate,
    val label: String,
)

/** Splits [period] into buckets: days for a month or a short range, months otherwise. */
fun periodBuckets(period: AnalyticsPeriod, locale: Locale = Locale.getDefault()): List<PeriodBucket> =
    when (period) {
        is AnalyticsPeriod.Month -> daysOf(period.startDate, period.endDate, locale)
        is AnalyticsPeriod.Year -> monthsOf(period.startDate, period.endDate, locale)
        is AnalyticsPeriod.Range -> {
            val days = ChronoUnit.DAYS.between(period.startDate, period.endDate) + 1
            if (days <= SHORT_RANGE_DAYS) daysOf(period.startDate, period.endDate, locale)
            else monthsOf(period.startDate, period.endDate, locale)
        }
    }

private fun daysOf(start: LocalDate, end: LocalDate, locale: Locale): List<PeriodBucket> {
    val dayFormat = DateTimeFormatter.ofPattern("d", locale)
    return generateSequence(start) { it.plusDays(1) }
        .takeWhile { !it.isAfter(end) }
        .map { PeriodBucket(it, it, it.format(dayFormat)) }
        .toList()
}

private fun monthsOf(start: LocalDate, end: LocalDate, locale: Locale): List<PeriodBucket> {
    val monthFormat = DateTimeFormatter.ofPattern("MMM", locale)
    val first = YearMonth.from(start)
    val last = YearMonth.from(end)
    return generateSequence(first) { it.plusMonths(1) }
        .takeWhile { !it.isAfter(last) }
        .map {
            val firstDay = it.atDay(1)
            val lastDay = it.atEndOfMonth()
            val bucketStart = if (firstDay.isBefore(start)) start else firstDay
            val bucketEnd = if (lastDay.isAfter(end)) end else lastDay
            PeriodBucket(bucketStart, bucketEnd, bucketStart.format(monthFormat))
        }
        .toList()
}

private const val SHORT_RANGE_DAYS = 62L
