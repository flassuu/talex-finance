package io.github.flassuu.talex.domain.model

import java.time.LocalDate

/**
 * Occurrence math for recurring rules. The n-th occurrence is always computed from
 * the anchor, so month-end clamping is stable (Jan 31 -> Feb 28 -> Mar 31).
 */
object RecurringSchedule {

    fun occurrence(anchor: LocalDate, frequency: Frequency, index: Int): LocalDate = when (frequency) {
        Frequency.WEEKLY -> anchor.plusWeeks(index.toLong())
        Frequency.MONTHLY -> anchor.plusMonths(index.toLong())
        Frequency.YEARLY -> anchor.plusYears(index.toLong())
    }

    fun firstOnOrAfter(anchor: LocalDate, frequency: Frequency, date: LocalDate): LocalDate {
        if (!anchor.isBefore(date)) return anchor
        var index = 0
        var next = anchor
        while (next.isBefore(date)) {
            index++
            next = occurrence(anchor, frequency, index)
        }
        return next
    }
}
