package io.github.flassuu.talex.feature.settings

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.flassuu.talex.R
import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.money.Money
import io.github.flassuu.talex.core.result.AppError
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.domain.model.Budget
import io.github.flassuu.talex.domain.model.Category
import io.github.flassuu.talex.domain.model.Rate
import io.github.flassuu.talex.domain.money.hasRateAtDate
import io.github.flassuu.talex.domain.money.planBaseCurrencyConversion
import io.github.flassuu.talex.domain.repository.AccountRepository
import io.github.flassuu.talex.domain.repository.BudgetRepository
import io.github.flassuu.talex.domain.repository.CategoryRepository
import io.github.flassuu.talex.domain.repository.GoalRepository
import io.github.flassuu.talex.domain.repository.RateRepository
import io.github.flassuu.talex.domain.repository.SettingsRepository
import io.github.flassuu.talex.domain.usecase.ChangeBaseCurrencyUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ChangeBaseCurrencyViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    budgetRepository: BudgetRepository,
    rateRepository: RateRepository,
    accountRepository: AccountRepository,
    goalRepository: GoalRepository,
    categoryRepository: CategoryRepository,
    private val changeBaseCurrency: ChangeBaseCurrencyUseCase,
) : ViewModel() {

    data class BudgetPreview(
        val categoryId: String,
        val from: Money,
        val to: Money?,
    )

    data class UiMessage(
        val id: Long = System.nanoTime(),
        @StringRes val res: Int? = null,
        val error: AppError? = null,
    )

    data class UiState(
        val currentBase: CurrencyCode = CurrencyCode.EUR,
        val newBase: CurrencyCode = CurrencyCode.EUR,
        val budgets: List<BudgetPreview> = emptyList(),
        val categories: List<Category> = emptyList(),
        val missingBudgetCurrencies: List<CurrencyCode> = emptyList(),
        val usedWithoutRate: List<CurrencyCode> = emptyList(),
        val busy: Boolean = false,
        val message: UiMessage? = null,
        val applied: Boolean = false,
    ) {
        val changed: Boolean get() = newBase != currentBase
        val blocked: Boolean get() = missingBudgetCurrencies.isNotEmpty()
        val canApply: Boolean get() = changed && !blocked && !busy
    }

    private data class Env(
        val currentBase: CurrencyCode,
        val budgets: List<Budget>,
        val rates: List<Rate>,
        val accountCurrencies: List<CurrencyCode>,
        val goalCurrencies: List<CurrencyCode>,
        val categories: List<Category>,
    )

    private data class LocalState(
        val newBase: CurrencyCode? = null,
        val busy: Boolean = false,
        val message: UiMessage? = null,
        val applied: Boolean = false,
    )

    private val local = MutableStateFlow(LocalState())

    private val core = combine(
        settingsRepository.observeBaseCurrency(),
        budgetRepository.observeBudgets(),
        rateRepository.observeAll(),
    ) { base, budgets, rates -> Triple(base ?: CurrencyCode.EUR, budgets, rates) }

    private val extras = combine(
        accountRepository.observeAccounts(),
        goalRepository.observeGoals(),
        categoryRepository.observeCategories(),
    ) { accounts, goals, categories ->
        Triple(accounts.map { it.currency }, goals.map { it.currency }, categories)
    }

    private val env = combine(core, extras) { (base, budgets, rates), (accounts, goals, categories) ->
        Env(
            currentBase = base,
            budgets = budgets,
            rates = rates,
            accountCurrencies = accounts,
            goalCurrencies = goals,
            categories = categories,
        )
    }

    val uiState: StateFlow<UiState> = combine(env, local) { environment, state ->
        val today = LocalDate.now()
        val newBase = state.newBase ?: environment.currentBase
        val openBudgets = environment.budgets.filter { it.closedAt == null }
        val plan = planBaseCurrencyConversion(openBudgets, newBase, environment.rates, today)
        val used = (environment.accountCurrencies + environment.goalCurrencies +
            openBudgets.map { it.amount.currency })
            .filter { it != newBase }
            .distinct()
        val usedWithoutRate = used
            .filterNot { hasRateAtDate(it, newBase, environment.rates, today) }
            .sortedBy { it.value }
        val missingBudgetCurrencies = plan.budgets
            .filter { it.converted == null }
            .map { it.budget.amount.currency }
            .filter { it != newBase }
            .distinct()
            .sortedBy { it.value }
        UiState(
            currentBase = environment.currentBase,
            newBase = newBase,
            budgets = plan.budgets.map {
                BudgetPreview(it.budget.categoryId, it.budget.amount, it.converted)
            },
            categories = environment.categories,
            missingBudgetCurrencies = missingBudgetCurrencies,
            usedWithoutRate = usedWithoutRate,
            busy = state.busy,
            message = state.message,
            applied = state.applied,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun onNewBase(code: CurrencyCode) = local.update { it.copy(newBase = code, message = null) }

    fun apply() {
        val state = uiState.value
        if (state.busy || state.applied) return
        if (!state.changed) {
            local.update { it.copy(message = UiMessage(res = R.string.base_change_same)) }
            return
        }
        if (state.blocked) {
            local.update { it.copy(message = UiMessage(error = AppError.RATE_REQUIRED)) }
            return
        }
        viewModelScope.launch {
            local.update { it.copy(busy = true, message = null) }
            when (val result = changeBaseCurrency(state.newBase)) {
                is Outcome.Success -> local.update {
                    it.copy(busy = false, applied = true, message = UiMessage(res = R.string.base_change_done))
                }
                is Outcome.Failure -> local.update {
                    it.copy(busy = false, message = UiMessage(error = result.error))
                }
            }
        }
    }

    fun clearMessage() = local.update { it.copy(message = null) }
}
