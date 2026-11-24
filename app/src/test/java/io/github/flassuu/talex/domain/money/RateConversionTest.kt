package io.github.flassuu.talex.domain.money

import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.money.Money
import io.github.flassuu.talex.domain.model.Budget
import io.github.flassuu.talex.domain.model.Rate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class RateConversionTest {

    private val eur = CurrencyCode.EUR
    private val usd = CurrencyCode.USD
    private val uah = CurrencyCode.UAH
    private val today = LocalDate.of(2026, 3, 15)

    @Test
    fun directRateUsesTheLatestOnOrBeforeTheDate() {
        val rates = listOf(
            rate(eur, usd, LocalDate.of(2026, 3, 1), "1.10"),
            rate(eur, usd, LocalDate.of(2026, 3, 20), "1.50"),
        )
        // 100.00 EUR at the 1 March rate becomes 110.00 USD.
        assertEquals(11_000L, convertAtDate(Money(10_000, eur), usd, rates, today))
    }

    @Test
    fun inverseRateIsAppliedWhenOnlyTheOppositePairExists() {
        val rates = listOf(rate(eur, usd, LocalDate.of(2026, 3, 1), "0.80"))
        // 80.00 USD / 0.80 = 100.00 EUR (the EUR → USD row is used inversely).
        assertEquals(10_000L, convertAtDate(Money(8_000, usd), eur, rates, today))
    }

    @Test
    fun missingRateReturnsNull() {
        assertNull(convertAtDate(Money(10_000, eur), uah, emptyList(), today))
    }

    @Test
    fun sameCurrencyNeedsNoRate() {
        assertEquals(10_000L, convertAtDate(Money(10_000, eur), eur, emptyList(), today))
    }

    @Test
    fun rateAfterTheDateIsIgnored() {
        val rates = listOf(rate(eur, usd, LocalDate.of(2026, 3, 20), "1.50"))
        assertNull(convertAtDate(Money(10_000, eur), usd, rates, today))
    }

    @Test
    fun hasRateAtDateAcceptsDirectAndInverse() {
        assertTrue(hasRateAtDate(eur, usd, listOf(rate(eur, usd, LocalDate.of(2026, 3, 1), "1.1")), today))
        assertTrue(hasRateAtDate(usd, eur, listOf(rate(eur, usd, LocalDate.of(2026, 3, 1), "1.1")), today))
        assertFalse(hasRateAtDate(eur, uah, emptyList(), today))
        assertTrue(hasRateAtDate(eur, eur, emptyList(), today))
    }

    @Test
    fun planConvertsOpenBudgetsAndLeavesClosedHistory() {
        val open = budget("b1", eur, 10_000)
        val closed = budget("b2", eur, 5_000, closedAt = LocalDate.of(2026, 2, 1))
        val rates = listOf(rate(eur, usd, LocalDate.of(2026, 3, 1), "1.10"))

        val plan = planBaseCurrencyConversion(listOf(open, closed), usd, rates, today)

        assertFalse(plan.blocked)
        assertEquals(1, plan.budgets.size)
        assertEquals(11_000L, plan.budgets.single().converted?.amountMinor)
        assertEquals(usd, plan.budgets.single().converted?.currency)
    }

    @Test
    fun planIsBlockedWhenAnOpenBudgetHasNoRate() {
        val plan = planBaseCurrencyConversion(
            listOf(budget("b1", eur, 10_000)),
            uah,
            emptyList(),
            today,
        )
        assertTrue(plan.blocked)
        assertNull(plan.budgets.single().converted)
        assertEquals(listOf(eur), plan.missingRate.map { it.amount.currency })
    }

    @Test
    fun planKeepsABudgetAlreadyInTheNewBase() {
        val budget = budget("b1", usd, 7_000)
        val plan = planBaseCurrencyConversion(listOf(budget), usd, emptyList(), today)
        assertFalse(plan.blocked)
        assertEquals(7_000L, plan.budgets.single().converted?.amountMinor)
    }

    private fun rate(base: CurrencyCode, quote: CurrencyCode, date: LocalDate, value: String) =
        Rate(base, quote, date, BigDecimal(value), "manual")

    private fun budget(
        id: String,
        currency: CurrencyCode,
        minor: Long,
        closedAt: LocalDate? = null,
    ) = Budget(id, "food", Money(minor, currency), LocalDate.of(2026, 3, 1), closedAt)
}
