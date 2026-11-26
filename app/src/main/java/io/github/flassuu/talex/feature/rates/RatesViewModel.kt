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
import io.github.flassuu.talex.domain.model.RateRefreshSchedule
import io.github.flassuu.talex.domain.model.RateSyncSettings
import io.github.flassuu.talex.domain.money.formatRate
import io.github.flassuu.talex.domain.money.roundRate
import io.github.flassuu.talex.domain.repository.AccountRepository
import io.github.flassuu.talex.domain.repository.BudgetRepository
import io.github.flassuu.talex.domain.repository.GoalRepository
import io.github.flassuu.talex.domain.repository.RateRepository
import io.github.flassuu.talex.domain.repository.RateSource
import io.github.flassuu.talex.domain.repository.SettingsRepository
import io.github.flassuu.talex.domain.usecase.FetchRateUseCase
import io.github.flassuu.talex.domain.usecase.UpdateRatesResult
import io.github.flassuu.talex.domain.usecase.UpdateRatesUseCase
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
    private val fetchRate: FetchRateUseCase,
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
        val syncEnabled: Boolean,
    )

    data class HistoryRow(
        val rate: Rate,
        val value: BigDecimal,
    )

    data class FormState(
        val quote: CurrencyCode? = null,
        val rate: String = "",
        val date: String = LocalDate.now().toString(),
        val fetching: Boolean = false,
        val error: AppError? = null,
    )

    data class UiMessage(
        val id: Long = System.nanoTime(),
        @StringRes val res: Int? = null,
        val args: List<String> = emptyList(),
        val error: AppError? = null,
    )

    /** A sync change waiting for the first-run explanation (ADR-0037). */
    sealed interface PendingSync {
        data class Schedule(
            val schedule: RateRefreshSchedule,
            val customDays: Int,
        ) : PendingSync

        data class Currency(
            val quote: CurrencyCode,
            val enabled: Boolean,
        ) : PendingSync
    }

    data class UiState(
        val base: CurrencyCode = CurrencyCode.EUR,
        val sync: RateSyncSettings = RateSyncSettings(),
        val pairs: List<PairRow> = emptyList(),
        val selected: CurrencyCode? = null,
        val history: List<HistoryRow> = emptyList(),
        val form: FormState? = null,
        val unavailable: List<CurrencyCode> = emptyList(),
        val pendingIntro: PendingSync? = null,
        val customDaysInput: String = "",
        val precisionInput: String = "",
        val busy: Boolean = false,
        val message: UiMessage? = null,
    ) {
        val customDaysInvalid: Boolean
            get() = sync.schedule == RateRefreshSchedule.CUSTOM &&
                (customDaysInput.toIntOrNull() ?: 0) !in RateSyncSettings.MIN_CUSTOM_DAYS..
                RateSyncSettings.MAX_CUSTOM_DAYS

        val precisionInvalid: Boolean
            get() = (precisionInput.toIntOrNull() ?: 0) !in
                RateSyncSettings.MIN_PRECISION..RateSyncSettings.MAX_PRECISION
    }

    private data class Env(
        val base: CurrencyCode,
        val sync: RateSyncSettings,
        val rates: List<Rate>,
        val accounts: List<CurrencyCode>,
        val goals: List<CurrencyCode>,
        val budgets: List<Budget>,
    )

    private data class LocalState(
        val selected: CurrencyCode? = null,
        val form: FormState? = null,
        val unavailable: List<CurrencyCode> = emptyList(),
        val pendingIntro: PendingSync? = null,
        val customDaysInput: String = "",
        val precisionInput: String = "",
        val busy: Boolean = false,
        val message: UiMessage? = null,
    )

    private val local = MutableStateFlow(LocalState())

    init {
        // Seed the numeric fields once; user edits are not overwritten.
        viewModelScope.launch {
            settingsRepository.observeRateSync().collect { sync ->
                local.update { state ->
                    state.copy(
                        customDaysInput = state.customDaysInput.ifEmpty { sync.customDays.toString() },
                        precisionInput = state.precisionInput.ifEmpty { sync.precision.toString() },
                    )
                }
            }
        }
    }

    private val core = combine(
        settingsRepository.observeBaseCurrency(),
        settingsRepository.observeRateSync(),
        rateRepository.observeAll(),
    ) { base, sync, rates -> Triple(base ?: CurrencyCode.EUR, sync, rates) }

    private val extras = combine(
        accountRepository.observeAccounts(),
        goalRepository.observeGoals(),
        budgetRepository.observeBudgets(),
    ) { accounts, goals, budgets ->
        Triple(accounts.map { it.currency }, goals.map { it.currency }, budgets)
    }

    private val env = combine(core, extras) { (base, sync, rates), (accounts, goals, budgets) ->
        Env(
            base = base,
            sync = sync,
            rates = rates,
            accounts = accounts,
            goals = goals,
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
                syncEnabled = environment.sync.isCurrencyEnabled(quote.value),
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
                        value = if (row.base == environment.base) row.rate else inverse(row.rate),
                    )
                }
                .sortedByDescending { it.rate.date }
        }
        UiState(
            base = environment.base,
            sync = environment.sync,
            pairs = pairs,
            selected = selected,
            history = history,
            form = state.form,
            unavailable = state.unavailable,
            pendingIntro = state.pendingIntro,
            customDaysInput = state.customDaysInput,
            precisionInput = state.precisionInput,
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
                    it.copy(
                        busy = false,
                        unavailable = result.value.unavailable,
                        message = fetchMessage(result.value),
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
        val precision = uiState.value.sync.precision
        local.update {
            it.copy(
                form = FormState(
                    quote = row.quote,
                    rate = best?.let { formatRate(it.value, precision) }.orEmpty(),
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
            val precision = uiState.value.sync.precision
            val result = rateRepository.upsert(
                base = base,
                quote = quote!!,
                date = date!!,
                rate = roundRate(rate!!, precision),
                source = RateSource.MANUAL,
            )
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

    fun requestSchedule(schedule: RateRefreshSchedule) {
        val sync = uiState.value.sync
        if (schedule == RateRefreshSchedule.MANUAL || sync.introSeen) {
            saveSync(sync.copy(schedule = schedule))
        } else {
            local.update {
                it.copy(
                    pendingIntro = PendingSync.Schedule(schedule, sync.customDays),
                )
            }
        }
    }

    fun onCustomDaysChange(value: String) {
        local.update { it.copy(customDaysInput = value) }
        val days = value.toIntOrNull() ?: return
        if (days !in RateSyncSettings.MIN_CUSTOM_DAYS..RateSyncSettings.MAX_CUSTOM_DAYS) return
        val sync = uiState.value.sync
        if (sync.schedule == RateRefreshSchedule.CUSTOM && sync.customDays != days) {
            saveSync(sync.copy(customDays = days))
        }
    }

    fun requestCurrencySync(quote: CurrencyCode, enabled: Boolean) {
        val sync = uiState.value.sync
        if (!enabled || sync.introSeen) {
            saveCurrencySync(quote, enabled, sync)
        } else {
            local.update {
                it.copy(pendingIntro = PendingSync.Currency(quote, enabled))
            }
        }
    }

    fun onPrecisionChange(value: String) {
        local.update { it.copy(precisionInput = value) }
        val precision = value.toIntOrNull() ?: return
        if (precision !in RateSyncSettings.MIN_PRECISION..RateSyncSettings.MAX_PRECISION) return
        val sync = uiState.value.sync
        if (sync.precision != precision) saveSync(sync.copy(precision = precision))
    }

    /** Fetches the selected pair for the add/edit form, so the user can review it. */
    fun fetchForForm() {
        val form = local.value.form ?: return
        val quote = form.quote ?: return
        if (form.fetching) return
        val base = uiState.value.base
        if (quote == base) {
            updateForm { it.copy(error = AppError.RATE_INVALID) }
            return
        }
        viewModelScope.launch {
            updateForm { it.copy(fetching = true, error = null) }
            val precision = uiState.value.sync.precision
            when (val result = fetchRate(base, quote)) {
                is Outcome.Success -> local.update { state ->
                    val current = state.form ?: return@update state
                    state.copy(
                        form = current.copy(
                            fetching = false,
                            rate = formatRate(result.value, precision),
                            date = LocalDate.now().toString(),
                            error = null,
                        ),
                        message = UiMessage(res = R.string.rates_fetched_single),
                    )
                }
                is Outcome.Failure -> updateForm {
                    it.copy(fetching = false, error = result.error)
                }
            }
        }
    }

    fun confirmIntro() {
        val pending = local.value.pendingIntro ?: return
        val sync = uiState.value.sync
        val next = when (pending) {
            is PendingSync.Schedule -> sync.copy(
                schedule = pending.schedule,
                customDays = pending.customDays,
                introSeen = true,
            )
            is PendingSync.Currency -> sync.copy(
                disabledCurrencies = withCurrency(sync.disabledCurrencies, pending.quote, pending.enabled),
                introSeen = true,
            )
        }
        saveSync(next)
        local.update { it.copy(pendingIntro = null) }
    }

    fun cancelIntro() = local.update { it.copy(pendingIntro = null) }

    fun clearMessage() = local.update { it.copy(message = null) }

    private fun saveCurrencySync(
        quote: CurrencyCode,
        enabled: Boolean,
        sync: RateSyncSettings,
    ) {
        saveSync(sync.copy(disabledCurrencies = withCurrency(sync.disabledCurrencies, quote, enabled)))
    }

    private fun saveSync(next: RateSyncSettings) {
        viewModelScope.launch { settingsRepository.saveRateSync(next) }
    }

    private fun withCurrency(
        disabled: Set<String>,
        quote: CurrencyCode,
        enabled: Boolean,
    ): Set<String> = if (enabled) disabled - quote.value else disabled + quote.value

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
        summary.disabled.isNotEmpty() && summary.unavailable.isEmpty() ->
            UiMessage(res = R.string.rates_all_manual)
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
