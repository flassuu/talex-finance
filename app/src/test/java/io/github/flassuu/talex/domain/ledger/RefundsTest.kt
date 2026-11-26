package io.github.flassuu.talex.domain.ledger

import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.money.Money
import io.github.flassuu.talex.domain.model.Entry
import io.github.flassuu.talex.domain.model.EntryType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RefundsTest {

    private val eur = CurrencyCode.EUR
    private val expense = Entry(
        id = "expense",
        accountId = "a",
        type = EntryType.EXPENSE,
        amount = Money(-6000, eur),
        date = LocalDate.of(2026, 1, 1),
        description = "Amazon",
    )

    @Test
    fun sumsRefundsPerOriginal() {
        val refunds = listOf(
            expense,
            refundEntry("r1", 2000),
            refundEntry("r2", 1000),
        )
        assertEquals(mapOf("expense" to 3000L), refundsByOriginal(refunds))
    }

    @Test
    fun aPartialRefundScalesTheExpense() {
        val refunds = refundsByOriginal(listOf(expense, refundEntry("r1", 3000)))
        assertEquals(0.5, refundFactor(expense, refunds), 0.0001)
    }

    @Test
    fun aFullRefundDropsTheExpense() {
        val refunds = refundsByOriginal(listOf(expense, refundEntry("r1", 6000)))
        assertEquals(0.0, refundFactor(expense, refunds), 0.0001)
    }

    @Test
    fun anUnrefundedExpenseKeepsItsFullValue() {
        assertEquals(1.0, refundFactor(expense, emptyMap()), 0.0001)
    }

    private fun refundEntry(id: String, amount: Long) = Entry(
        id = id,
        accountId = "a",
        type = EntryType.REFUND,
        amount = Money(amount, eur),
        date = LocalDate.of(2026, 1, 3),
        description = "Refund",
        refundedEntryId = "expense",
    )
}
