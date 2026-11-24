package io.github.flassuu.talex.domain.usecase

import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.result.AppError
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.domain.repository.AccountRepository
import io.github.flassuu.talex.domain.repository.BudgetRepository
import io.github.flassuu.talex.domain.repository.GoalRepository
import io.github.flassuu.talex.domain.repository.RateProvider
import io.github.flassuu.talex.domain.repository.RateRepository
import io.github.flassuu.talex.domain.repository.RateSource
import io.github.flassuu.talex.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/** Outcome of an explicit rate refresh (ADR-0032). */
data class UpdateRatesResult(
    val base: CurrencyCode,
    val fetched: List<CurrencyCode>,
    /** Currencies the provider could not serve; they stay manual. */
    val unavailable: List<CurrencyCode>,
)

/**
 * Fetches rates for the pairs the data currently uses and stores them with
 * `source = "fetched"`. Only called from an explicit user action; never scheduled.
 */
class UpdateRatesUseCase @Inject constructor(
    private val rates: RateRepository,
    private val provider: RateProvider,
    private val settings: SettingsRepository,
    private val accounts: AccountRepository,
    private val goals: GoalRepository,
    private val budgets: BudgetRepository,
) {
    suspend operator fun invoke(today: LocalDate = LocalDate.now()): Outcome<UpdateRatesResult> {
        val base = settings.getBaseCurrency() ?: CurrencyCode.EUR
        val used = usedCurrencies() - base
        if (used.isEmpty()) {
            return Outcome.Success(UpdateRatesResult(base, emptyList(), emptyList()))
        }
        val response = provider.fetch(base, used)
            .getOrElse { return Outcome.Failure(AppError.RATE_FETCH_FAILED) }
        response.forEach { (quote, rate) ->
            val stored = rates.upsert(base, quote, today, rate, RateSource.FETCHED)
            if (stored is Outcome.Failure) return Outcome.Failure(stored.error)
        }
        return Outcome.Success(
            UpdateRatesResult(
                base = base,
                fetched = response.keys.sortedBy { it.value },
                unavailable = (used - response.keys).sortedBy { it.value },
            ),
        )
    }

    private suspend fun usedCurrencies(): Set<CurrencyCode> {
        val accountCurrencies = accounts.observeAccounts().first().map { it.currency }
        val goalCurrencies = goals.observeGoals().first().map { it.currency }
        val budgetCurrencies = budgets.observeBudgets().first()
            .filter { it.closedAt == null }
            .map { it.amount.currency }
        return (accountCurrencies + goalCurrencies + budgetCurrencies).toSet()
    }
}
