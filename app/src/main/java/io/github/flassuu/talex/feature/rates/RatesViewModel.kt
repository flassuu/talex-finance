package io.github.flassuu.talex.feature.rates

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.flassuu.talex.R
import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.result.AppError
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.domain.model.Budget
import io.github.flassuu.talex.domain.model.Rate
import io.github.flassuu.talex.domain.repository.AccountRepository
import io.github.flassuu.talex.domain.repository.BudgetRepository
import io.github.flassuu.talex.domain.repository.GoalRepository
import io.github.flassuu.talex.domain.repository.RateRepository
import io.github.flassuu.talex.domain.repository.RateSource
import io.github.flassuu.talex.domain.repository.SettingsRepository
import io.github.flassuu.talex.domain.usecase.UpdateRatesUseCase
import io.github.flassuu.talex.domain.usecase.UpdateRatesResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class RatesViewModel @Inject constructor(
    private val rateRepository: RateRepository,
    private val settingsRepository: SettingsRepository,
    accountRepository: AccountRepository,
    goalRepository: GoalRepository,
    budgetRepository: BudgetRepository,
    private val updateRates: UpdateRatesUseCase,
) : ViewModel() {

    /** A rate normalized to the base → quote direction. */
    data class BestRate(
        val value: BigDecimal,
        val date: LocalDate,
        val source: String,
        val stored: Rate,
    )

    data class PairRow(
        val quote: CurrencyCode,
        val best: BestRate?,
        val used: Boolean,
    )

    data class HistoryRow(
        val rate: Rate,
        val value: BigDecimal,
    )

    data class FormState(
        val quote: CurrencyCode? = null,
        val rate: String = "",
        val date: String = LocalDate.now().toString(),
        val error: AppError? = null,
    )

    data class UiMessage(
        val id: Long = System.nanoTime(),
        @StringRes val res: Int? = null,
        val args: List<String> = emptyList(),
        val error: AppError? = null,
    )

    data class UiState(
        val base: CurrencyCode = CurrencyCode.EUR,
        val pairs: List<PairRow> = emptyList(),
        val selected: CurrencyCode? = null,
        val history: List<HistoryRow> = emptyList(),
        val form: FormState? = null,
        val unavailable: List<CurrencyCode> = emptyList(),
        val busy: Boolean = false,
        val message: UiMessage? = null,
    )

    private data class Env(
        val base: CurrencyCode,
        val rates: List<Rate>,
        val accounts: List<CurrencyCode>,
        val goals: List<CurrencyCode>,
        val budgets: List<Budget>,
    )

    private data class LocalState(
        val selected: CurrencyCode? = null,
        val form: FormState? = null,
        val unavailable: List<CurrencyCode> = emptyList(),
        val busy: Boolean = false,
        val message: UiMessage? = null,
    )

    private val local = MutableStateFlow(LocalState())

    private val env = combine(
        settingsRepository.observeBaseCurrency(),
        rateRepository.observeAll(),
        accountRepository.observeAccounts(),
        goalRepository.observeGoals(),
        budgetRepository.observeBudgets(),
    ) { base, rates, accounts, goals, budgets ->
        Env(
            base = base ?: CurrencyCode.EUR,
            rates = rates,
            accounts = accounts.map { it.currency },
            goals = goals.map { it.currency },
            budgets = budgets,
        )
    }

    val uiState: StateFlow<UiState> = combine(env, local) { environment, state ->
        val used = usedCurrencies(environment)
        val stored = storedQuotes(environment.base, environment.rates)
        val quotes = (used + stored).sortedBy { it.value }
        val pairs = quotes.map { quote ->
            PairRow(
                quote = quote,
                best = bestRate(environment.base, quote, environment.rates),
                used = quote in used,
            )
        }
        val selected = state.selected
        val history = if (selected == null) {
            emptyList()
        } else {
            environment.rates
                .filter {
                    (it.base == environment.base && it.quote == selected) ||
                        (it.base == selected && it.quote == environment.base)
                }
                .map { row ->
                    HistoryRow(
                        rate = row,
                        value = if (row.base == environment.base) {
                            row.rate
                        } else {
                            inverse(row.rate)
                        },
                    )
                }
                .sortedByDescending { it.rate.date }
        }
        UiState(
            base = environment.base,
            pairs = pairs,
            selected = selected,
            history = history,
            form = state.form,
            unavailable = state.unavailable,
            busy = state.busy,
            message = state.message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun refresh() {
        if (local.value.busy) return
        viewModelScope.launch {
            local.update { it.copy(busy = true, message = null) }
            when (val result = updateRates()) {
                is Outcome.Success -> local.update {
                    val summary = result.value
                    it.copy(
                        busy = false,
                        unavailable = summary.unavailable,
                        message = fetchMessage(summary),
                    )
                }
                is Outcome.Failure -> local.update {
                    it.copy(busy = false, message = UiMessage(res = R.string.rates_fetch_failed))
                }
            }
        }
    }

    fun selectPair(quote: CurrencyCode) = local.update { it.copy(selected = quote) }

    fun closeHistory() = local.update { it.copy(selected = null) }

    fun openAdd() {
        val state = uiState.value
        val quote = state.pairs.firstOrNull { it.used && it.best == null }?.quote
            ?: state.pairs.firstOrNull()?.quote
            ?: if (state.base == CurrencyCode.USD) CurrencyCode.EUR else CurrencyCode.USD
        local.update { it.copy(form = FormState(quote = quote)) }
    }

    fun openEdit(row: PairRow) {
        val best = row.best
        local.update {
            it.copy(
                form = FormState(
                    quote = row.quote,
                    rate = best?.value?.stripTrailingZeros()?.toPlainString().orEmpty(),
                    date = best?.date?.toString() ?: LocalDate.now().toString(),
                ),
            )
        }
    }

    fun closeForm() = local.update { it.copy(form = null) }

    fun onQuoteChange(quote: CurrencyCode) = updateForm { it.copy(quote = quote, error = null) }

    fun onRateChange(value: String) = updateForm { it.copy(rate = value, error = null) }

    fun onDateChange(value: String) = updateForm { it.copy(date = value, error = null) }

    fun save() {
        val form = local.value.form ?: return
        val quote = form.quote
        val rate = form.rate.trim().replace(',', '.').toBigDecimalOrNull()
        val date = runCatching { LocalDate.parse(form.date.trim()) }.getOrNull()
        val error = when {
            quote == null || quote == uiState.value.base -> AppError.RATE_INVALID
            rate == null || rate.signum() <= 0 -> AppError.RATE_INVALID
            date == null -> AppError.DATE_INVALID
            else -> null
        }
        if (error != null) {
            updateForm { it.copy(error = error) }
            return
        }
        viewModelScope.launch {
            local.update { it.copy(busy = true) }
            val base = uiState.value.base
            val result = rateRepository.upsert(base, quote!!, date!!, rate!!, RateSource.MANUAL)
            local.update {
                when (result) {
                    is Outcome.Success -> it.copy(
                        busy = false,
                        form = null,
                        message = UiMessage(res = R.string.rates_saved),
                    )
                    is Outcome.Failure -> it.copy(
                        busy = false,
                        form = it.form?.copy(error = result.error),
                    )
                }
            }
        }
    }

    fun delete(rate: Rate) {
        viewModelScope.launch {
            local.update { it.copy(busy = true) }
            val result = rateRepository.delete(rate.base, rate.quote, rate.date)
            local.update {
                when (result) {
                    is Outcome.Success -> it.copy(
                        busy = false,
                        message = UiMessage(res = R.string.rates_deleted),
                    )
                    is Outcome.Failure -> it.copy(
                        busy = false,
                        message = UiMessage(error = result.error),
                    )
                }
            }
        }
    }

    fun clearMessage() = local.update { it.copy(message = null) }

    private fun updateForm(block: (FormState) -> FormState) {
        local.update { state ->
            val form = state.form ?: return@update state
            state.copy(form = block(form))
        }
    }

    private fun fetchMessage(summary: UpdateRatesResult): UiMessage = when {
        summary.fetched.isNotEmpty() -> UiMessage(
            res = R.string.rates_fetched,
            args = listOf(summary.fetched.size.toString()),
        )
        summary.unavailable.isNotEmpty() -> UiMessage(res = R.string.rates_fetched_none)
        else -> UiMessage(res = R.string.rates_fetched_none)
    }

    private fun usedCurrencies(env: Env): Set<CurrencyCode> {
        val budgets = env.budgets.filter { it.closedAt == null }.map { it.amount.currency }
        return (env.accounts + env.goals + budgets).toSet() - env.base
    }

    private fun storedQuotes(base: CurrencyCode, rates: List<Rate>): Set<CurrencyCode> =
        rates.flatMap { row ->
            when {
                row.base == base -> listOf(row.quote)
                row.quote == base -> listOf(row.base)
                else -> emptyList()
            }
        }.toSet()

    private fun bestRate(base: CurrencyCode, quote: CurrencyCode, rates: List<Rate>): BestRate? {
        if (quote == base) return null
        val direct = rates
            .filter { it.base == base && it.quote == quote }
            .maxByOrNull { it.date }
        val inverseRow = rates
            .filter { it.base == quote && it.quote == base && it.rate.signum() != 0 }
            .maxByOrNull { it.date }
        val chosen = listOfNotNull(direct, inverseRow)
            .maxWithOrNull(compareBy<Rate> { it.date }.thenBy { it.base == base })
            ?: return null
        val value = if (chosen.base == base) chosen.rate else inverse(chosen.rate)
        return BestRate(value = value, date = chosen.date, source = chosen.source, stored = chosen)
    }

    private fun inverse(rate: BigDecimal): BigDecimal =
        BigDecimal.ONE.divide(rate, INVERSE_SCALE, RoundingMode.HALF_UP)

    companion object {
        private const val INVERSE_SCALE = 8
    }
}
