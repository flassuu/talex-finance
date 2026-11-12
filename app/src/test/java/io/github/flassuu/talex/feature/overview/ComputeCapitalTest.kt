package io.github.flassuu.talex.feature.overview

import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.money.Money
import io.github.flassuu.talex.domain.model.Account
import io.github.flassuu.talex.domain.model.AccountType
import io.github.flassuu.talex.domain.model.Category
import io.github.flassuu.talex.domain.model.CategoryKind
import io.github.flassuu.talex.domain.model.Entry
import io.github.flassuu.talex.domain.model.EntryLine
import io.github.flassuu.talex.domain.model.EntryType
import io.github.flassuu.talex.domain.model.Valuation
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ComputeCapitalTest {

    private val eur = CurrencyCode.EUR
    private val usd = CurrencyCode.USD
    private val today = LocalDate.of(2026, 3, 15)

    @Test
    fun worksAndIdleFollowAccountTypes() {
        val accounts = listOf(
            account("inv", AccountType.INVESTMENT, eur, 10_000),
            account("chk", AccountType.CHECKING, eur, 5_000),
            account("sav", AccountType.SAVINGS, eur, 2_000),
            account("cash", AccountType.CASH, eur, 1_000),
            account("loan", AccountType.LOAN, eur, -3_000),
        )
        val state = computeCapital(accounts, emptyList(), emptyList(), emptyList(), eur, emptyMap(), today)
        assertEquals(10_000L, state.works.amountMinor)
        assertEquals(8_000L, state.idle.amountMinor)
    }

    @Test
    fun assetsCountAsWorksAndLiabilitiesReduceNetWorth() {
        val accounts = listOf(
            account("inv", AccountType.INVESTMENT, eur, 10_000),
            account("home", AccountType.PROPERTY, eur, 200_000),
            account("car", AccountType.VEHICLE, eur, 15_000),
            account("loan", AccountType.LOAN, eur, -50_000),
        )
        val state = computeCapital(accounts, emptyList(), emptyList(), emptyList(), eur, emptyMap(), today)
        assertEquals(225_000L, state.works.amountMinor)
        assertEquals(175_000L, state.netWorth.amountMinor)
    }

    @Test
    fun lostCountsRecurringAndUnnecessarySpending() {
        val leisure = Category("leisure", CategoryKind.EXPENSE, "Leisure", null, archived = false, unnecessary = true)
        val food = Category("food", CategoryKind.EXPENSE, "Food", null, archived = false)
        val accounts = listOf(account("chk", AccountType.CHECKING, eur, 0))
        val entries = listOf(
            entry("e1", "chk", EntryType.EXPENSE, -1_000, LocalDate.of(2026, 3, 2), sourceRuleId = "r1"),
            entry("e2", "chk", EntryType.EXPENSE, -500, LocalDate.of(2026, 3, 3), categoryId = "leisure"),
            entry("e3", "chk", EntryType.EXPENSE, -2_000, LocalDate.of(2026, 3, 4), categoryId = "food"),
            entry("e4", "chk", EntryType.EXPENSE, -2_000, LocalDate.of(2026, 2, 28), sourceRuleId = "r1"),
        )
        val state = computeCapital(
            accounts,
            entries,
            listOf(leisure, food),
            emptyList(),
            eur,
            emptyMap(),
            today,
        )
        assertEquals(1_500L, state.lost.amountMinor)
    }

    @Test
    fun savingsRateUsesThisMonthIncomeAndExpenses() {
        val accounts = listOf(account("chk", AccountType.CHECKING, eur, 0))
        val entries = listOf(
            entry("i1", "chk", EntryType.INCOME, 10_000, LocalDate.of(2026, 3, 1)),
            entry("e1", "chk", EntryType.EXPENSE, -4_000, LocalDate.of(2026, 3, 2)),
        )
        val state = computeCapital(accounts, entries, emptyList(), emptyList(), eur, emptyMap(), today)
        assertEquals(0.6, state.savingsRate ?: 0.0, 0.0001)
    }

    @Test
    fun convertsBalancesUsingTheLatestRate() {
        val accounts = listOf(account("usd", AccountType.CHECKING, usd, 10_000))
        val rates = mapOf((usd.value to eur.value) to "0.9")
        val state = computeCapital(accounts, emptyList(), emptyList(), emptyList(), eur, rates, today)
        assertEquals(9_000L, state.idle.amountMinor)
    }

    @Test
    fun categoryRollupIncludesChildrenAndSplits() {
        val food = Category("food", CategoryKind.EXPENSE, "Food", null, archived = false)
        val groceries = Category("groceries", CategoryKind.EXPENSE, "Groceries", "food", archived = false)
        val accounts = listOf(account("chk", AccountType.CHECKING, eur, 0))
        val entries = listOf(
            entry("e1", "chk", EntryType.EXPENSE, -1_000, LocalDate.of(2026, 3, 2), categoryId = "food"),
            splitEntry("e2", "chk", -1_000, LocalDate.of(2026, 3, 3), categoryId = "groceries"),
        )
        val state = computeCapital(
            accounts,
            entries,
            listOf(food, groceries),
            emptyList(),
            eur,
            emptyMap(),
            today,
        )
        assertEquals(1, state.topCategories.size)
        assertEquals("food", state.topCategories.single().category.id)
        assertEquals(2_000L, state.topCategories.single().amount.amountMinor)
    }

    @Test
    fun trendFillsEachDayFromEntriesUpToThatDay() {
        val accounts = listOf(account("chk", AccountType.CHECKING, eur, 0))
        val entries = listOf(
            entry("e1", "chk", EntryType.INCOME, 5_000, LocalDate.of(2026, 3, 1)),
            entry("e2", "chk", EntryType.EXPENSE, -2_000, LocalDate.of(2026, 3, 10)),
        )
        val state = computeCapital(accounts, entries, emptyList(), emptyList(), eur, emptyMap(), today)
        assertEquals(3_000L, state.trend.last())
        assertEquals(5_000L, state.trend[state.trend.size - 15])
    }

    @Test
    fun trendUsesTheLatestValuationOnOrBeforeEachDay() {
        val accounts = listOf(account("home", AccountType.PROPERTY, eur, 200_000))
        val valuations = listOf(
            Valuation("v1", "home", Money(100_000, eur), LocalDate.of(2026, 3, 1)),
            Valuation("v2", "home", Money(200_000, eur), LocalDate.of(2026, 3, 10)),
        )
        val state = computeCapital(accounts, emptyList(), emptyList(), valuations, eur, emptyMap(), today)
        assertEquals(200_000L, state.trend.last())
        assertEquals(100_000L, state.trend[state.trend.size - 10])
    }

    private fun account(id: String, type: AccountType, currency: CurrencyCode, minor: Long) =
        Account(id, id, type, currency, archived = false, balance = Money(minor, currency))

    private fun entry(
        id: String,
        accountId: String,
        type: EntryType,
        minor: Long,
        date: LocalDate,
        sourceRuleId: String? = null,
        categoryId: String? = null,
    ) = Entry(
        id = id,
        accountId = accountId,
        type = type,
        amount = Money(minor, eur),
        date = date,
        description = "e",
        categoryId = categoryId,
        sourceRuleId = sourceRuleId,
    )

    private fun splitEntry(
        id: String,
        accountId: String,
        minor: Long,
        date: LocalDate,
        categoryId: String,
    ) = Entry(
        id = id,
        accountId = accountId,
        type = EntryType.EXPENSE,
        amount = Money(minor, eur),
        date = date,
        description = "e",
        categoryId = null,
        lines = listOf(EntryLine(id = "l1", categoryId = categoryId, amount = Money(minor, eur))),
    )
}
