package io.github.flassuu.talex.domain.insights

import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.money.Money
import io.github.flassuu.talex.domain.model.Account
import io.github.flassuu.talex.domain.model.AccountType
import io.github.flassuu.talex.domain.model.Budget
import io.github.flassuu.talex.domain.model.Category
import io.github.flassuu.talex.domain.model.CategoryKind
import io.github.flassuu.talex.domain.model.Entry
import io.github.flassuu.talex.domain.model.EntryType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class InsightsTest {

    private val eur = CurrencyCode.EUR
    private val today = LocalDate.of(2026, 6, 15)

    @Test
    fun twoNegativeCompletedMonthsRaiseTheCashFlowAlert() {
        val insights = compute(
            entries = listOf(
                expense("a", 10_000, LocalDate.of(2026, 4, 10)),
                expense("b", 10_000, LocalDate.of(2026, 5, 10)),
            ),
        )
        assertTrue(
            insights.alerts.any {
                it.kind == AlertKind.NEGATIVE_CASH_FLOW && it.months == 2
            },
        )
    }

    @Test
    fun oneNegativeMonthDoesNotRaiseTheCashFlowAlert() {
        val insights = compute(
            entries = listOf(expense("a", 10_000, LocalDate.of(2026, 5, 10))),
        )
        assertFalse(insights.alerts.any { it.kind == AlertKind.NEGATIVE_CASH_FLOW })
    }

    @Test
    fun aDisabledSignalIsRemoved() {
        val entries = listOf(
            expense("a", 10_000, LocalDate.of(2026, 4, 10)),
            expense("b", 10_000, LocalDate.of(2026, 5, 10)),
        )
        val insights = compute(
            entries = entries,
            settings = InsightSettings(disabledAlerts = setOf("NEGATIVE_CASH_FLOW")),
        )
        assertFalse(insights.alerts.any { it.kind == AlertKind.NEGATIVE_CASH_FLOW })
    }

    @Test
    fun anOverBudgetCurrentMonthRaisesTheBudgetAlert() {
        val category = Category(
            id = "c1",
            kind = CategoryKind.EXPENSE,
            name = "Food",
            parentId = null,
            archived = false,
        )
        val budget = Budget(
            id = "b1",
            categoryId = "c1",
            amount = Money(10_000, eur),
            effectiveFrom = LocalDate.of(2026, 1, 1),
        )
        val insights = compute(
            entries = listOf(expense("a", 20_000, LocalDate.of(2026, 6, 10), categoryId = "c1")),
            categories = listOf(category),
            budgets = listOf(budget),
        )
        assertTrue(
            insights.alerts.any {
                it.kind == AlertKind.BUDGET_EXCEEDED && it.budgetCount == 1
            },
        )
    }

    @Test
    fun riskyCapitalAboveTheLimitRaisesTheAllocationAlert() {
        val insights = compute(
            accounts = listOf(
                account("inv", AccountType.INVESTMENT, 30_000),
                account("cash", AccountType.CHECKING, 70_000),
            ),
        )
        assertTrue(insights.alerts.any { it.kind == AlertKind.RISKY_ALLOCATION })
    }

    @Test
    fun aCategoryUpOnItsAverageRaisesRisingAdvice() {
        val category = Category(
            id = "c1",
            kind = CategoryKind.EXPENSE,
            name = "Food",
            parentId = null,
            archived = false,
        )
        val insights = compute(
            entries = listOf(
                expense("m1", 10_000, LocalDate.of(2026, 3, 10), categoryId = "c1"),
                expense("m2", 10_000, LocalDate.of(2026, 4, 10), categoryId = "c1"),
                expense("m3", 10_000, LocalDate.of(2026, 5, 10), categoryId = "c1"),
                expense("m4", 30_000, LocalDate.of(2026, 6, 10), categoryId = "c1"),
            ),
            categories = listOf(category),
        )
        assertTrue(insights.advice.any { it.kind == AdviceKind.RISING_CATEGORY })
    }

    private fun compute(
        entries: List<Entry> = emptyList(),
        accounts: List<Account> = emptyList(),
        categories: List<Category> = emptyList(),
        budgets: List<Budget> = emptyList(),
        settings: InsightSettings = InsightSettings(),
    ): Insights = computeInsights(
        accounts = accounts,
        entries = entries,
        categories = categories,
        budgets = budgets,
        valuations = emptyList(),
        base = eur,
        rates = emptyMap(),
        today = today,
        settings = settings,
    )

    private fun expense(
        id: String,
        amount: Long,
        date: LocalDate,
        categoryId: String? = null,
    ) = Entry(
        id = id,
        accountId = "a",
        type = EntryType.EXPENSE,
        amount = Money(-amount, eur),
        date = date,
        description = "expense",
        categoryId = categoryId,
    )

    private fun account(id: String, type: AccountType, balance: Long) = Account(
        id = id,
        name = id,
        type = type,
        currency = eur,
        archived = false,
        balance = Money(balance, eur),
    )
}
