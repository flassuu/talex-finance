package io.github.flassuu.talex.feature.analytics

import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.money.Money
import io.github.flassuu.talex.domain.model.Account
import io.github.flassuu.talex.domain.model.AccountType
import io.github.flassuu.talex.domain.model.Category
import io.github.flassuu.talex.domain.model.CategoryKind
import io.github.flassuu.talex.domain.model.Entry
import io.github.flassuu.talex.domain.model.EntryLine
import io.github.flassuu.talex.domain.model.EntryType
import io.github.flassuu.talex.domain.model.Rate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class AnalyticsMathTest {

    private val eur = CurrencyCode.EUR
    private val usd = CurrencyCode.USD
    private val march = AnalyticsPeriod.Month(YearMonth.of(2026, 3))

    private val food = Category("food", CategoryKind.EXPENSE, "Food", null, archived = false)
    private val groceries = Category("groceries", CategoryKind.EXPENSE, "Groceries", "food", archived = false)
    private val salary = Category("salary", CategoryKind.INCOME, "Salary", null, archived = false)
    private val checking = Account("acc", "Checking", AccountType.CHECKING, eur, archived = false, balance = Money.zero(eur))

    @Test
    fun sumsIncomeAndExpensesAndFillsEmptyDays() {
        val entries = listOf(
            entry("i1", EntryType.INCOME, 300_000, LocalDate.of(2026, 3, 1), categoryId = "salary"),
            entry("e1", EntryType.EXPENSE, -100_000, LocalDate.of(2026, 3, 2), categoryId = "food"),
        )
        val data = compute(entries)
        assertEquals(31, data.points.size)
        assertEquals(300_000L, data.totalIncome.amountMinor)
        assertEquals(100_000L, data.totalExpenses.amountMinor)
        assertEquals(200_000L, data.totalCashFlow.amountMinor)
        assertEquals(0L, data.points.first { it.income.amountMinor == 0L }.income.amountMinor)
    }

    @Test
    fun excludesTransfersAndOpeningEntries() {
        val entries = listOf(
            entry("t1", EntryType.TRANSFER, -50_000, LocalDate.of(2026, 3, 2)),
            entry("t2", EntryType.TRANSFER, 50_000, LocalDate.of(2026, 3, 2)),
            entry("o1", EntryType.OPENING, 100_000, LocalDate.of(2026, 3, 1)),
        )
        val data = compute(entries)
        assertEquals(0L, data.totalIncome.amountMinor)
        assertEquals(0L, data.totalExpenses.amountMinor)
    }

    @Test
    fun rollsSplitLinesIntoTheTopCategory() {
        val split = Entry(
            id = "s1",
            accountId = "acc",
            type = EntryType.EXPENSE,
            amount = Money(-10_000, eur),
            date = LocalDate.of(2026, 3, 5),
            description = "Receipt",
            lines = listOf(
                EntryLine("l1", "food", Money(-7_000, eur)),
                EntryLine("l2", "groceries", Money(-3_000, eur)),
            ),
        )
        val data = compute(listOf(split))
        assertEquals(1, data.categories.size)
        assertEquals("food", data.categories.first().category.id)
        assertEquals(10_000L, data.categories.first().amount.amountMinor)
    }

    @Test
    fun netWorthIsTheBalanceAtTheEndOfTheBucket() {
        val entries = listOf(
            entry("o", EntryType.OPENING, 50_000, LocalDate.of(2026, 3, 1)),
            entry("i", EntryType.INCOME, 300_000, LocalDate.of(2026, 3, 5)),
            entry("e", EntryType.EXPENSE, -100_000, LocalDate.of(2026, 3, 6), categoryId = "food"),
        )
        val data = compute(entries)
        assertEquals(250_000L, data.netWorth.last().amountMinor)
        assertEquals(31, data.netWorth.size)
    }

    @Test
    fun missingRateMarksThePeriodIncomplete() {
        val usdIncome = entry("i1", EntryType.INCOME, 10_000, LocalDate.of(2026, 3, 1), categoryId = "salary", currency = usd)
        val data = compute(listOf(usdIncome))
        assertTrue(data.incomplete)
        assertEquals(0L, data.totalIncome.amountMinor)
    }

    @Test
    fun convertsAtTheTransactionDate() {
        val usdIncome = entry("i1", EntryType.INCOME, 10_000, LocalDate.of(2026, 3, 1), categoryId = "salary", currency = usd)
        val rates = listOf(Rate(usd, eur, LocalDate.of(2026, 3, 1), BigDecimal("1.2"), "manual"))
        val data = compute(listOf(usdIncome), rates = rates)
        assertFalse(data.incomplete)
        assertEquals(12_000L, data.totalIncome.amountMinor)
    }

    private fun compute(
        entries: List<Entry>,
        rates: List<Rate> = emptyList(),
        valuations: List<io.github.flassuu.talex.domain.model.Valuation> = emptyList(),
    ) = computeAnalytics(
        accounts = listOf(checking),
        entries = entries,
        categories = listOf(food, groceries, salary),
        valuations = valuations,
        rates = rates,
        base = eur,
        period = march,
    )

    private fun entry(
        id: String,
        type: EntryType,
        minor: Long,
        date: LocalDate,
        categoryId: String? = null,
        currency: CurrencyCode = eur,
    ) = Entry(
        id = id,
        accountId = "acc",
        type = type,
        amount = Money(minor, currency),
        date = date,
        description = "e",
        categoryId = categoryId,
    )
}
