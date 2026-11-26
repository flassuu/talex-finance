package io.github.flassuu.talex.domain.ledger

import io.github.flassuu.talex.domain.model.Entry
import io.github.flassuu.talex.domain.model.EntryType

/**
 * Refunds linked to each original expense, summed per original (Q-042). A refund
 * is never income and never a category total; it only reduces the original's
 * contribution to spending. Amounts stay in the original account's currency.
 */
fun refundsByOriginal(entries: List<Entry>): Map<String, Long> =
    entries
        .asSequence()
        .filter { it.type == EntryType.REFUND && it.deletedAt == null }
        .mapNotNull { entry -> entry.refundedEntryId?.let { it to entry.amount.amountMinor } }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, values) -> values.sum() }

/**
 * How much of an expense remains after its refunds, as a 0..1 factor. A fully
 * refunded expense yields 0 and drops out of every spending report.
 */
fun refundFactor(entry: Entry, refunds: Map<String, Long>): Double {
    val magnitude = -entry.amount.amountMinor
    if (magnitude <= 0L) return 1.0
    val refunded = refunds[entry.id] ?: 0L
    return ((magnitude - refunded).coerceAtLeast(0L)).toDouble() / magnitude
}
