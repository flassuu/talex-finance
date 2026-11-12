package io.github.flassuu.talex.feature.budgets

import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.money.Money
import io.github.flassuu.talex.domain.model.Budget
import io.github.flassuu.talex.domain.model.Category
import io.github.flassuu.talex.domain.model.CategoryKind
import io.github.flassuu.talex.domain.model.Entry
import io.github.flassuu.talex.domain.model.EntryLine
import io.github.flassuu.talex.domain.model.EntryType
import io.github.flassuu.talex.domain.model.Rate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class BudgetMathTest {

    private val eur = CurrencyCode.EUR
    private val usd = CurrencyCode.USD
    private val march = YearMonth.of(2026, 3)

    private val food = Category("food", CategoryKind.EXPENSE, "Food", null, archived = false)
    private val groceries = Category("groceries", CategoryKind.EXPENSE, "Groceries", "food", archived = false)
    private val transport = Category("transport", CategoryKind.EXPENSE, "Transport", null, archived = false)

    @Test
    fun parentBudgetIncludesDirectAndChildSpending() {
        val budgets = listOf(budget("b1", "food", 10_000, LocalDate.of(2026, 3, 1)))
        val entries = listOf(
            expense("e1", -4_000, LocalDate.of(2026, 3, 2), categoryId = "food"),
            expense("e2", -1_000, LocalDate.of(2026, 3, 3), categoryId = "groceries"),
            expense("e3", -2_000, LocalDate.of(2026, 3, 4), categoryId = "transport"),
        )
        val status = singleStatus(budgets, entries)
        assertEquals(5_000L, status.spent.amountMinor)
        assertEquals(5_000L, status.remaining.amountMinor)
        assertEquals(0.5, status.percent, 0.0001)
        assertFalse(status.overBudget)
    }

    @Test
    fun childBudgetCountsOnlyItself() {
        val budgets = listOf(budget("b1", "groceries", 2_000, LocalDate.of(2026, 3, 1)))
        val entries = listOf(
            expense("e1", -4_000, LocalDate.of(2026, 3, 2), categoryId = "food"),
            expense("e2", -1_500, LocalDate.of(2026, 3, 3), categoryId = "groceries"),
        )
        val status = singleStatus(budgets, entries)
        assertEquals(1_500L, status.spent.amountMinor)
    }

    @Test
    fun splitLinesContributeToTheirCategories() {
        val budgets = listOf(budget("b1", "food", 10_000, LocalDate.of(2026, 3, 1)))
        val entries = listOf(
            Entry(
                id = "e1",
                accountId = "acc",
                type = EntryType.EXPENSE,
                amount = Money(-3_000, eur),
                date = LocalDate.of(2026, 3, 5),
                description = "receipt",
                lines = listOf(
                    EntryLine("l1", "food", Money(-2_000, eur)),
                    EntryLine("l2", "groceries", Money(-1_000, eur)),
                ),
            ),
        )
        val status = singleStatus(budgets, entries)
        assertEquals(3_000L, status.spent.amountMinor)
    }

    @Test
    fun overBudgetIsStrictlyGreaterThanTheLimit() {
        val budgets = listOf(budget("b1", "food", 1_000, LocalDate.of(2026, 3, 1)))
        val entries = listOf(expense("e1", -1_000, LocalDate.of(2026, 3, 2), categoryId = "food"))
        assertFalse(singleStatus(budgets, entries).overBudget)

        val over = listOf(expense("e1", -1_001, LocalDate.of(2026, 3, 2), categoryId = "food"))
        assertTrue(singleStatus(budgets, over).overBudget)
    }

    @Test
    fun missingRateMarksTheStatusIncomplete() {
        val budgets = listOf(budget("b1", "food", 10_000, LocalDate.of(2026, 3, 1)))
        val entries = listOf(
            Entry(
                id = "e1",
                accountId = "acc",
                type = EntryType.EXPENSE,
                amount = Money(-1_000, usd),
                date = LocalDate.of(2026, 3, 5),
                description = "usd",
                categoryId = "food",
            ),
        )
        val status = singleStatus(budgets, entries)
        assertTrue(status.incomplete)
        assertEquals(0L, status.spent.amountMinor)
    }

    @Test
    fun limitForAMonthUsesTheLatestEffectiveRow() {
        val budgets = listOf(
            budget("b1", "food", 30_000, LocalDate.of(2026, 1, 1)),
            budget("b2", "food", 40_000, LocalDate.of(2026, 3, 1)),
        )
        assertEquals(30_000L, budgetInForce(budgets, "food", YearMonth.of(2026, 2))?.amount?.amountMinor)
        assertEquals(40_000L, budgetInForce(budgets, "food", YearMonth.of(2026, 3))?.amount?.amountMinor)
    }

    @Test
    fun removingABudgetClosesTheCurrentPeriodButKeepsHistory() {
        val closed = budget(
            "b1",
            "food",
            30_000,
            LocalDate.of(2026, 1, 1),
            closedAt = LocalDate.of(2026, 3, 10),
        )
        assertEquals(30_000L, budgetInForce(listOf(closed), "food", YearMonth.of(2026, 2))?.amount?.amountMinor)
        assertNull(budgetInForce(listOf(closed), "food", march))
        assertTrue(computeBudgetStatuses(listOf(closed), emptyList(), listOf(food), eur, emptyList(), march).isEmpty())
    }

    private fun singleStatus(budgets: List<Budget>, entries: List<Entry>): BudgetStatus =
        computeBudgetStatuses(budgets, entries, listOf(food, groceries, transport), eur, emptyList(), march)
            .single()

    private fun budget(
        id: String,
        categoryId: String,
        minor: Long,
        effectiveFrom: LocalDate,
        closedAt: LocalDate? = null,
    ) = Budget(id, categoryId, Money(minor, eur), effectiveFrom, closedAt)

    private fun expense(
        id: String,
        minor: Long,
        date: LocalDate,
        categoryId: String?,
    ) = Entry(
        id = id,
        accountId = "acc",
        type = EntryType.EXPENSE,
        amount = Money(minor, eur),
        date = date,
        description = "e",
        categoryId = categoryId,
    )

    @Suppress("unused")
    private fun rate() = Rate(eur, usd, LocalDate.of(2026, 3, 1), BigDecimal("1.1"), "manual")
}
