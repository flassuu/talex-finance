package io.github.flassuu.talex.domain.usecase

import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.money.Money
import io.github.flassuu.talex.core.result.AppError
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.domain.model.Account
import io.github.flassuu.talex.domain.model.AccountType
import io.github.flassuu.talex.domain.model.Budget
import io.github.flassuu.talex.domain.model.Goal
import io.github.flassuu.talex.domain.model.GoalContribution
import io.github.flassuu.talex.domain.model.GoalContributionInput
import io.github.flassuu.talex.domain.model.GoalInput
import io.github.flassuu.talex.domain.model.Rate
import io.github.flassuu.talex.domain.repository.AccountRepository
import io.github.flassuu.talex.domain.repository.BudgetRepository
import io.github.flassuu.talex.domain.repository.GoalRepository
import io.github.flassuu.talex.domain.repository.RateProvider
import io.github.flassuu.talex.domain.repository.RateRepository
import io.github.flassuu.talex.domain.repository.RateSource
import io.github.flassuu.talex.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class UpdateRatesUseCaseTest {

    private val eur = CurrencyCode.EUR
    private val usd = CurrencyCode.USD
    private val uah = CurrencyCode.UAH
    private val today = LocalDate.of(2026, 3, 15)

    @Test
    fun fetchesOnlyTheCurrenciesInUseAndSkipsTheBase() = runTest {
        val rates = FakeRateRepository()
        val provider = FakeRateProvider { _, _ ->
            Result.success(mapOf(usd to BigDecimal("1.10")))
        }
        val useCase = UpdateRatesUseCase(
            rates = rates,
            provider = provider,
            settings = FakeSettings(eur),
            accounts = FakeAccounts(listOf(account(usd), account(eur))),
            goals = FakeGoals(listOf(goal(uah))),
            budgets = FakeBudgets(listOf(openBudget(usd))),
        )

        val result = useCase(today)

        assertTrue(result is Outcome.Success)
        val summary = (result as Outcome.Success).value
        assertEquals(eur, summary.base)
        assertEquals(listOf(usd), summary.fetched)
        assertEquals(listOf(uah), summary.unavailable)
        assertEquals(setOf(eur, usd, uah), provider.lastTargets + eur)
        assertEquals(1, rates.upserts.size)
        val upsert = rates.upserts.single()
        assertEquals(usd, upsert.quote)
        assertEquals(RateSource.FETCHED, upsert.source)
        assertEquals(today, upsert.date)
    }

    @Test
    fun doesNothingWhenTheBaseIsTheOnlyCurrency() = runTest {
        val provider = FakeRateProvider { _, _ -> error("should not be called") }
        val useCase = UpdateRatesUseCase(
            rates = FakeRateRepository(),
            provider = provider,
            settings = FakeSettings(eur),
            accounts = FakeAccounts(listOf(account(eur))),
            goals = FakeGoals(emptyList()),
            budgets = FakeBudgets(emptyList()),
        )

        val result = useCase(today)

        assertTrue(result is Outcome.Success)
        assertEquals(emptyList<CurrencyCode>(), (result as Outcome.Success).value.fetched)
    }

    @Test
    fun providerFailureIsReported() = runTest {
        val useCase = UpdateRatesUseCase(
            rates = FakeRateRepository(),
            provider = FakeRateProvider { _, _ -> Result.failure(IllegalStateException("offline")) },
            settings = FakeSettings(eur),
            accounts = FakeAccounts(listOf(account(usd))),
            goals = FakeGoals(emptyList()),
            budgets = FakeBudgets(emptyList()),
        )

        assertEquals(Outcome.Failure(AppError.RATE_FETCH_FAILED), useCase(today))
    }

    private fun account(currency: CurrencyCode) = Account(
        id = "a-" + currency.value,
        name = "Account",
        type = AccountType.CHECKING,
        currency = currency,
        archived = false,
        balance = Money.zero(currency),
    )

    private fun goal(currency: CurrencyCode) = Goal(
        id = "g-" + currency.value,
        name = "Goal",
        targetAmount = Money.zero(currency),
        targetDate = null,
        currency = currency,
        currentAmount = Money.zero(currency),
    )

    private fun openBudget(currency: CurrencyCode) = Budget(
        id = "b-" + currency.value,
        categoryId = "food",
        amount = Money(10_000, currency),
        effectiveFrom = LocalDate.of(2026, 1, 1),
        closedAt = null,
    )

    private class FakeRateProvider(
        private val block: (CurrencyCode, Set<CurrencyCode>) -> Result<Map<CurrencyCode, BigDecimal>>,
    ) : RateProvider {
        var lastTargets: Set<CurrencyCode> = emptySet()
        override suspend fun fetch(base: CurrencyCode, targets: Set<CurrencyCode>): Result<Map<CurrencyCode, BigDecimal>> {
            lastTargets = targets
            return block(base, targets)
        }
    }

    private class FakeRateRepository : RateRepository {
        data class Upsert(
            val base: CurrencyCode,
            val quote: CurrencyCode,
            val date: LocalDate,
            val rate: BigDecimal,
            val source: String,
        )

        val upserts = mutableListOf<Upsert>()

        override fun observeLatest(): Flow<Map<Pair<String, String>, String>> = flowOf(emptyMap())
        override fun observeAll(): Flow<List<Rate>> = flowOf(emptyList())
        override suspend fun latest(base: String, quote: String): String? = null
        override suspend fun upsert(
            base: CurrencyCode,
            quote: CurrencyCode,
            date: LocalDate,
            rate: BigDecimal,
            source: String,
        ): Outcome<Unit> {
            upserts += Upsert(base, quote, date, rate, source)
            return Outcome.Success(Unit)
        }

        override suspend fun delete(base: CurrencyCode, quote: CurrencyCode, date: LocalDate): Outcome<Unit> =
            Outcome.Success(Unit)
    }

    private class FakeSettings(private val base: CurrencyCode) : SettingsRepository {
        override fun observeBaseCurrency(): Flow<CurrencyCode?> = flowOf(base)
        override suspend fun getBaseCurrency(): CurrencyCode = base
        override suspend fun setBaseCurrency(code: CurrencyCode) = Unit
        override suspend fun changeBaseCurrency(newBase: CurrencyCode, today: LocalDate): Outcome<Unit> =
            Outcome.Success(Unit)
        override fun observeHideAmounts(): Flow<Boolean> = flowOf(false)
        override suspend fun setHideAmounts(hidden: Boolean) = Unit
    }

    private class FakeAccounts(private val accounts: List<Account>) : AccountRepository {
        override fun observeAccounts(): Flow<List<Account>> = flowOf(accounts)
        override fun observeAccount(id: String): Flow<Account?> = flowOf(accounts.find { it.id == id })
        override suspend fun get(id: String): Account? = accounts.find { it.id == id }
        override suspend fun createAccount(
            name: String,
            type: AccountType,
            currency: CurrencyCode,
            startingBalance: Money?,
            openingDate: LocalDate,
        ): Outcome<Account> = Outcome.Failure(AppError.UNKNOWN)

        override suspend fun updateAccount(id: String, name: String, type: AccountType): Outcome<Unit> =
            Outcome.Success(Unit)

        override suspend fun setArchived(id: String, archived: Boolean): Outcome<Unit> = Outcome.Success(Unit)
        override suspend fun deleteAccount(id: String): Outcome<Unit> = Outcome.Success(Unit)
    }

    private class FakeGoals(private val goals: List<Goal>) : GoalRepository {
        override fun observeGoals(): Flow<List<Goal>> = flowOf(goals)
        override suspend fun get(id: String): Goal? = goals.find { it.id == id }
        override suspend fun create(input: GoalInput): Outcome<Goal> = Outcome.Failure(AppError.UNKNOWN)
        override suspend fun update(id: String, input: GoalInput): Outcome<Goal> = Outcome.Failure(AppError.UNKNOWN)
        override suspend fun delete(id: String): Outcome<Unit> = Outcome.Success(Unit)
        override suspend fun addContribution(goalId: String, input: GoalContributionInput): Outcome<Unit> =
            Outcome.Success(Unit)

        override suspend fun removeContribution(contributionId: String): Outcome<Unit> = Outcome.Success(Unit)
        override fun observeContributions(goalId: String): Flow<List<GoalContribution>> = flowOf(emptyList())
    }

    private class FakeBudgets(private val budgets: List<Budget>) : BudgetRepository {
        override fun observeBudgets(): Flow<List<Budget>> = flowOf(budgets)
        override suspend fun setLimit(categoryId: String, amount: Money, today: LocalDate): Outcome<Unit> =
            Outcome.Success(Unit)

        override suspend fun removeBudget(categoryId: String, today: LocalDate): Outcome<Unit> = Outcome.Success(Unit)
    }
}
