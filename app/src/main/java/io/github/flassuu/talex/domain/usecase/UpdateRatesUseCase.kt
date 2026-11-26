package io.github.flassuu.talex.domain.usecase

import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.result.AppError
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.domain.money.roundRate
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

/** Outcome of a rate refresh (ADR-0032, ADR-0037). */
data class UpdateRatesResult(
    val base: CurrencyCode,
    val fetched: List<CurrencyCode>,
    /** Currencies the provider could not serve; they stay manual. */
    val unavailable: List<CurrencyCode>,
    /** Currencies the user switched to manual-only. */
    val disabled: List<CurrencyCode> = emptyList(),
)

/**
 * Fetches rates for the pairs the data currently uses and stores them with
 * `source = "fetched"` at the user's precision. Disabled currencies are skipped.
 * Called by an explicit action or by the scheduled refresh (ADR-0037).
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
        val sync = settings.getRateSync()
        val allUsed = usedCurrencies(base)
        val used = allUsed.filter { sync.isCurrencyEnabled(it.value) }.toSet()
        val disabled = (allUsed - used).sortedBy { it.value }
        if (used.isEmpty()) {
            return Outcome.Success(
                UpdateRatesResult(base, emptyList(), emptyList(), disabled),
            )
        }
        val response = provider.fetch(base, used)
            .getOrElse { return Outcome.Failure(AppError.RATE_FETCH_FAILED) }
        response.forEach { (quote, rate) ->
            val stored = rates.upsert(
                base = base,
                quote = quote,
                date = today,
                rate = roundRate(rate, sync.precision),
                source = RateSource.FETCHED,
            )
            if (stored is Outcome.Failure) return Outcome.Failure(stored.error)
        }
        settings.saveRateSync(sync.copy(lastFetchAt = System.currentTimeMillis()))
        return Outcome.Success(
            UpdateRatesResult(
                base = base,
                fetched = response.keys.sortedBy { it.value },
                unavailable = (used - response.keys).sortedBy { it.value },
                disabled = disabled,
            ),
        )
    }

    private suspend fun usedCurrencies(base: CurrencyCode): Set<CurrencyCode> {
        val accountCurrencies = accounts.observeAccounts().first().map { it.currency }
        val goalCurrencies = goals.observeGoals().first().map { it.currency }
        val budgetCurrencies = budgets.observeBudgets().first()
            .filter { it.closedAt == null }
            .map { it.amount.currency }
        // A pair that already has a stored rate is kept fresh even when no account
        // currently uses the currency; the per-currency switch decides.
        val storedCurrencies = rates.observeAll().first().flatMap { row ->
            when {
                row.base == base -> listOf(row.quote)
                row.quote == base -> listOf(row.base)
                else -> emptyList()
            }
        }
        return (accountCurrencies + goalCurrencies + budgetCurrencies + storedCurrencies)
            .toSet() - base
    }
}
