package io.github.flassuu.talex.feature.budgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.flassuu.talex.R
import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.money.Money
import io.github.flassuu.talex.core.money.MoneyFormat
import io.github.flassuu.talex.core.result.AppError
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.domain.model.Category
import io.github.flassuu.talex.domain.model.CategoryKind
import io.github.flassuu.talex.domain.model.Rate
import io.github.flassuu.talex.domain.repository.BudgetRepository
import io.github.flassuu.talex.domain.repository.CategoryRepository
import io.github.flassuu.talex.domain.repository.EntryRepository
import io.github.flassuu.talex.domain.repository.RateRepository
import io.github.flassuu.talex.domain.repository.SettingsRepository
import io.github.flassuu.talex.feature.analytics.AnalyticsPeriod
import io.github.flassuu.talex.ui.component.PeriodSelector
import io.github.flassuu.talex.ui.component.TalexScreen
import io.github.flassuu.talex.ui.component.categoryDisplayName
import io.github.flassuu.talex.ui.component.errorMessage
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
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    entryRepository: EntryRepository,
    settingsRepository: SettingsRepository,
    rateRepository: RateRepository,
) : ViewModel() {

    data class FormState(
        val categoryId: String? = null,
        val amount: String = "",
        val editing: Boolean = false,
        val error: AppError? = null,
    )

    data class UiState(
        val statuses: List<BudgetStatus> = emptyList(),
        val categories: List<Category> = emptyList(),
        val baseCurrency: CurrencyCode = CurrencyCode.EUR,
        val month: YearMonth = YearMonth.now(),
        val form: FormState? = null,
    )

    private data class Computed(
        val statuses: List<BudgetStatus>,
        val categories: List<Category>,
        val baseCurrency: CurrencyCode,
        val month: YearMonth,
    )

    private data class Env(
        val baseCurrency: CurrencyCode,
        val rates: List<Rate>,
        val month: YearMonth,
    )

    private val form = MutableStateFlow<FormState?>(null)
    private val selectedMonth = MutableStateFlow(YearMonth.now())

    private val data = combine(
        budgetRepository.observeBudgets(),
        categoryRepository.observeCategories(),
        entryRepository.observeAllWithLines(),
    ) { budgets, categories, entries -> Triple(budgets, categories, entries) }

    private val env = combine(
        settingsRepository.observeBaseCurrency(),
        rateRepository.observeAll(),
        selectedMonth,
    ) { base, rates, month -> Env(base ?: CurrencyCode.EUR, rates, month) }

    private val computed = combine(data, env) { (budgets, categories, entries), environment ->
        Computed(
            statuses = computeBudgetStatuses(
                budgets = budgets,
                entries = entries,
                categories = categories,
                base = environment.baseCurrency,
                rates = environment.rates,
                month = environment.month,
            ),
            categories = categories,
            baseCurrency = environment.baseCurrency,
            month = environment.month,
        )
    }

    val uiState: StateFlow<UiState> = combine(computed, form) { c, f ->
        UiState(c.statuses, c.categories, c.baseCurrency, c.month, f)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun setMonth(month: YearMonth) {
        selectedMonth.update { month }
    }

    fun openCreate() {
        val current = uiState.value
        val used = current.statuses.map { it.budget.categoryId }.toSet()
        val available = current.categories.filter {
            it.kind == CategoryKind.EXPENSE && !it.archived && it.id !in used
        }
        form.value = FormState(categoryId = available.firstOrNull()?.id)
    }

    fun openEdit(status: BudgetStatus) {
        form.value = FormState(
            categoryId = status.budget.categoryId,
            amount = amountText(status.budget.amount),
            editing = true,
        )
    }

    fun closeForm() {
        form.value = null
    }

    fun onCategoryChange(id: String) = form.update { it?.copy(categoryId = id, error = null) }

    fun onAmountChange(value: String) = form.update { it?.copy(amount = value, error = null) }

    fun save() {
        val state = form.value ?: return
        val categoryId = state.categoryId ?: return
        val amount = MoneyFormat.parse(state.amount, uiState.value.baseCurrency)
        if (amount == null || amount.isZero || amount.isNegative) {
            form.update { it?.copy(error = AppError.AMOUNT_INVALID) }
            return
        }
        viewModelScope.launch {
            when (val result = budgetRepository.setLimit(categoryId, amount, LocalDate.now())) {
                is Outcome.Success -> form.value = null
                is Outcome.Failure -> form.update { it?.copy(error = result.error) }
            }
        }
    }

    fun remove(status: BudgetStatus) {
        viewModelScope.launch {
            budgetRepository.removeBudget(status.budget.categoryId, LocalDate.now())
        }
    }

    private fun amountText(money: Money): String =
        BigDecimal(money.amountMinor)
            .movePointLeft(money.currency.fractionDigits)
            .setScale(money.currency.fractionDigits, RoundingMode.UNNECESSARY)
            .toPlainString()
}

@Composable
fun BudgetsScreen(viewModel: BudgetsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val categoryById = state.categories.associateBy { it.id }
    val form = state.form
    val editable = state.month == YearMonth.now()

    TalexScreen(
        title = stringResource(R.string.budgets_title),
        actions = {
            if (editable) {
                IconButton(onClick = viewModel::openCreate) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.add_budget),
                    )
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            PeriodSelector(
                period = AnalyticsPeriod.Month(state.month),
                onPeriodChange = { period ->
                    if (period is AnalyticsPeriod.Month) viewModel.setMonth(period.yearMonth)
                },
                allowYear = false,
                allowRange = false,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            if (state.statuses.isEmpty()) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.budgets_empty),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.budgets_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.statuses, key = { it.budget.id }) { status ->
                        BudgetCard(
                            status = status,
                            category = categoryById[status.budget.categoryId],
                            editable = editable,
                            onEdit = { viewModel.openEdit(status) },
                            onRemove = { viewModel.remove(status) },
                        )
                    }
                }
            }
        }
    }

    if (form != null) {
        BudgetDialog(
            form = form,
            categories = state.categories.filter { !it.archived },
            statuses = state.statuses,
            onDismiss = viewModel::closeForm,
            onCategoryChange = viewModel::onCategoryChange,
            onAmountChange = viewModel::onAmountChange,
            onConfirm = viewModel::save,
        )
    }
}

@Composable
private fun BudgetCard(
    status: BudgetStatus,
    category: Category?,
    editable: Boolean,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val title = category?.let { categoryDisplayName(it) } ?: status.budget.categoryId
    val fraction = (status.percent.toFloat()).coerceIn(0f, 1f)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (editable) {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = stringResource(R.string.more_actions),
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit_budget)) },
                            onClick = {
                                menuOpen = false
                                onEdit()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.remove_budget)) },
                            onClick = {
                                menuOpen = false
                                onRemove()
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(
                        R.string.budget_spent_of,
                        MoneyFormat.format(status.spent),
                        MoneyFormat.format(status.budget.amount),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (status.overBudget) {
                        stringResource(R.string.budget_over, MoneyFormat.format(status.remaining.abs()))
                    } else {
                        stringResource(R.string.budget_remaining, MoneyFormat.format(status.remaining))
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (status.overBudget) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            if (status.incomplete) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.budget_incomplete),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun BudgetDialog(
    form: BudgetsViewModel.FormState,
    categories: List<Category>,
    statuses: List<BudgetStatus>,
    onDismiss: () -> Unit,
    onCategoryChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    val used = statuses.map { it.budget.categoryId }.toSet()
    val options = categories.filter {
        it.kind == CategoryKind.EXPENSE && (form.editing || it.id !in used)
    }
    val selected = options.firstOrNull { it.id == form.categoryId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (form.editing) R.string.edit_budget else R.string.add_budget))
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CategoryField(
                    label = stringResource(R.string.entry_category_label),
                    selected = selected,
                    options = options,
                    enabled = !form.editing,
                    onSelect = onCategoryChange,
                )
                OutlinedTextField(
                    value = form.amount,
                    onValueChange = onAmountChange,
                    label = { Text(stringResource(R.string.budget_limit_label)) },
                    singleLine = true,
                    isError = form.error == AppError.AMOUNT_INVALID,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                form.error?.let { error ->
                    Text(
                        text = errorMessage(error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.save_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_action))
            }
        },
    )
}

@Composable
private fun CategoryField(
    label: String,
    selected: Category?,
    options: List<Category>,
    enabled: Boolean,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        Box {
            OutlinedButton(
                onClick = { if (enabled) expanded = true },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = selected?.let { categoryDisplayName(it) }
                        ?: stringResource(R.string.category_none),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (options.isNotEmpty()) {
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(categoryDisplayName(category)) },
                            onClick = {
                                onSelect(category.id)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}
