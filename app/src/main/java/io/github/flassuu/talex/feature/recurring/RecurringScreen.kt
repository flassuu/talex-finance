package io.github.flassuu.talex.feature.recurring

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import io.github.flassuu.talex.core.money.Money
import io.github.flassuu.talex.core.money.MoneyFormat
import io.github.flassuu.talex.core.result.AppError
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.domain.model.Account
import io.github.flassuu.talex.domain.model.Category
import io.github.flassuu.talex.domain.model.CategoryKind
import io.github.flassuu.talex.domain.model.EntryType
import io.github.flassuu.talex.domain.model.Frequency
import io.github.flassuu.talex.domain.model.RecurringInput
import io.github.flassuu.talex.domain.model.RecurringRule
import io.github.flassuu.talex.domain.repository.AccountRepository
import io.github.flassuu.talex.domain.repository.CategoryRepository
import io.github.flassuu.talex.domain.repository.RecurringRepository
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

@HiltViewModel
class RecurringViewModel @Inject constructor(
    private val recurringRepository: RecurringRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    data class FormState(
        val id: String? = null,
        val accountId: String? = null,
        val type: EntryType = EntryType.EXPENSE,
        val amount: String = "",
        val description: String = "",
        val categoryId: String? = null,
        val frequency: Frequency = Frequency.MONTHLY,
        val anchorDate: LocalDate = LocalDate.now(),
        val active: Boolean = true,
        val error: AppError? = null,
    )

    private data class Local(
        val form: FormState? = null,
        val message: AppError? = null,
    )

    data class UiState(
        val rules: List<RecurringRule> = emptyList(),
        val accounts: Map<String, Account> = emptyMap(),
        val categories: Map<String, Category> = emptyMap(),
        val form: FormState? = null,
        val message: AppError? = null,
    )

    private val local = MutableStateFlow(Local())

    val uiState: StateFlow<UiState> = combine(
        recurringRepository.observeRules(),
        accountRepository.observeAccounts(),
        categoryRepository.observeCategories(),
        local,
    ) { rules, accounts, categories, l ->
        UiState(
            rules = rules,
            accounts = accounts.filter { !it.type.isValuationBased }.associateBy { it.id },
            categories = categories.associateBy { it.id },
            form = l.form,
            message = l.message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun openCreate() {
        val firstAccount = uiState.value.accounts.values.firstOrNull { !it.archived }
        local.value = Local(FormState(accountId = firstAccount?.id))
    }

    fun openEdit(rule: RecurringRule) {
        local.value = Local(
            FormState(
                id = rule.id,
                accountId = rule.accountId,
                type = rule.type,
                amount = amountText(rule.amount),
                description = rule.description,
                categoryId = rule.categoryId,
                frequency = rule.frequency,
                anchorDate = rule.anchorDate,
                active = rule.active,
            ),
        )
    }

    fun closeForm() = local.update { it.copy(form = null) }

    fun onAccountChange(id: String) = updateForm { it.copy(accountId = id, error = null) }

    fun onTypeChange(type: EntryType) = updateForm { it.copy(type = type, error = null) }

    fun onAmountChange(value: String) = updateForm { it.copy(amount = value, error = null) }

    fun onDescriptionChange(value: String) = updateForm { it.copy(description = value, error = null) }

    fun onCategoryChange(id: String?) = updateForm { it.copy(categoryId = id) }

    fun onFrequencyChange(frequency: Frequency) = updateForm { it.copy(frequency = frequency) }

    fun onDateChange(date: LocalDate) = updateForm { it.copy(anchorDate = date) }

    fun onActiveChange(active: Boolean) = updateForm { it.copy(active = active) }

    fun save() {
        val form = local.value.form ?: return
        val account = uiState.value.accounts[form.accountId]
        if (account == null) {
            updateForm { it.copy(error = AppError.ACCOUNT_NOT_FOUND) }
            return
        }
        val amount = MoneyFormat.parse(form.amount, account.currency)
        if (amount == null || amount.isZero) {
            updateForm { it.copy(error = AppError.AMOUNT_INVALID) }
            return
        }
        val description = form.description.trim()
        if (description.isEmpty()) {
            updateForm { it.copy(error = AppError.DESCRIPTION_REQUIRED) }
            return
        }
        val signed = if (form.type == EntryType.INCOME) amount.abs() else -amount.abs()
        val input = RecurringInput(
            accountId = account.id,
            type = form.type,
            amount = signed,
            description = description,
            categoryId = form.categoryId,
            frequency = form.frequency,
            anchorDate = form.anchorDate,
            active = form.active,
        )
        viewModelScope.launch {
            val result = if (form.id == null) {
                recurringRepository.create(input)
            } else {
                recurringRepository.update(form.id, input)
            }
            when (result) {
                is Outcome.Success -> local.update { it.copy(form = null, message = null) }
                is Outcome.Failure -> local.update { l ->
                    l.form?.let { l.copy(form = it.copy(error = result.error)) } ?: l
                }
            }
        }
    }

    fun delete(rule: RecurringRule) {
        viewModelScope.launch {
            when (val result = recurringRepository.delete(rule.id)) {
                is Outcome.Success -> local.update { it.copy(message = null) }
                is Outcome.Failure -> local.update { it.copy(message = result.error) }
            }
        }
    }

    fun setActive(rule: RecurringRule, active: Boolean) {
        viewModelScope.launch {
            when (val result = recurringRepository.setActive(rule.id, active, LocalDate.now())) {
                is Outcome.Success -> local.update { it.copy(message = null) }
                is Outcome.Failure -> local.update { it.copy(message = result.error) }
            }
        }
    }

    fun clearMessage() = local.update { it.copy(message = null) }

    private fun updateForm(block: (FormState) -> FormState) {
        local.update { l -> l.form?.let { l.copy(form = block(it)) } ?: l }
    }

    private fun amountText(money: Money): String =
        BigDecimal(money.amountMinor).abs()
            .movePointLeft(money.currency.fractionDigits)
            .setScale(money.currency.fractionDigits, RoundingMode.UNNECESSARY)
            .toPlainString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(viewModel: RecurringViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    TalexScreen(
        title = stringResource(R.string.recurring_title),
        actions = {
            IconButton(onClick = viewModel::openCreate) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.add_recurring),
                )
            }
        },
    ) { padding ->
        if (state.rules.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.recurring_empty),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.recurring_empty_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.rules, key = { it.id }) { rule ->
                    RecurringRow(
                        rule = rule,
                        accountName = state.accounts[rule.accountId]?.name,
                        category = rule.categoryId?.let { state.categories[it] },
                        onEdit = { viewModel.openEdit(rule) },
                        onDelete = { viewModel.delete(rule) },
                        onActiveChange = { viewModel.setActive(rule, it) },
                    )
                }
                state.message?.let { error ->
                    item {
                        Text(
                            text = errorMessage(error),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }

    state.form?.let { form ->
        RecurringDialog(
            form = form,
            accounts = state.accounts.values.filter { !it.archived },
            categories = state.categories.values.filter { !it.archived },
            onDismiss = viewModel::closeForm,
            onAccountChange = viewModel::onAccountChange,
            onTypeChange = viewModel::onTypeChange,
            onAmountChange = viewModel::onAmountChange,
            onDescriptionChange = viewModel::onDescriptionChange,
            onCategoryChange = viewModel::onCategoryChange,
            onFrequencyChange = viewModel::onFrequencyChange,
            onDateChange = viewModel::onDateChange,
            onActiveChange = viewModel::onActiveChange,
            onConfirm = viewModel::save,
        )
    }
}

@Composable
private fun RecurringRow(
    rule: RecurringRule,
    accountName: String?,
    category: Category?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onActiveChange: (Boolean) -> Unit,
) {
    val categoryLabel = category?.let { categoryDisplayName(it) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f).clickable(onClick = onEdit),
            ) {
                Text(rule.description, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = buildString {
                        append(MoneyFormat.format(rule.amount))
                        accountName?.let { append(" · ").append(it) }
                        categoryLabel?.let { append(" · ").append(it) }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = frequencyLabel(rule.frequency) + " · " + rule.nextDueDate.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = rule.active, onCheckedChange = onActiveChange)
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.delete_action),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringDialog(
    form: RecurringViewModel.FormState,
    accounts: List<Account>,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onAccountChange: (String) -> Unit,
    onTypeChange: (EntryType) -> Unit,
    onAmountChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCategoryChange: (String?) -> Unit,
    onFrequencyChange: (Frequency) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onActiveChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val selectedAccount = accounts.firstOrNull { it.id == form.accountId }
    val selectedCategory = categories.firstOrNull { it.id == form.categoryId }
    val kind = if (form.type == EntryType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (form.id == null) R.string.add_recurring else R.string.edit_recurring,
                ),
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = form.type == EntryType.EXPENSE,
                        onClick = { onTypeChange(EntryType.EXPENSE) },
                        label = { Text(stringResource(R.string.entry_type_expense)) },
                    )
                    FilterChip(
                        selected = form.type == EntryType.INCOME,
                        onClick = { onTypeChange(EntryType.INCOME) },
                        label = { Text(stringResource(R.string.entry_type_income)) },
                    )
                }
                OutlinedTextField(
                    value = form.amount,
                    onValueChange = onAmountChange,
                    label = { Text(stringResource(R.string.entry_amount_label)) },
                    singleLine = true,
                    isError = form.error == AppError.AMOUNT_INVALID,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                SelectorField(
                    label = stringResource(R.string.entry_account_label),
                    value = selectedAccount?.let { it.name + " (" + it.currency.value + ")" }
                        ?: stringResource(R.string.entry_account_label),
                    options = accounts.map { it.id to (it.name + " (" + it.currency.value + ")") },
                    onSelect = { id -> id?.let(onAccountChange) },
                )
                OutlinedTextField(
                    value = form.description,
                    onValueChange = onDescriptionChange,
                    label = { Text(stringResource(R.string.entry_description_label)) },
                    singleLine = true,
                    isError = form.error == AppError.DESCRIPTION_REQUIRED,
                    modifier = Modifier.fillMaxWidth(),
                )
                SelectorField(
                    label = stringResource(R.string.entry_category_label),
                    value = selectedCategory?.let { categoryDisplayName(it) }
                        ?: stringResource(R.string.category_none),
                    options = listOf<Pair<String?, String>>(null to stringResource(R.string.category_none)) +
                        categories.filter { it.kind == kind }
                            .map { it.id to categoryDisplayName(it) },
                    onSelect = onCategoryChange,
                )
                Text(
                    text = stringResource(R.string.recurring_frequency_label),
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Frequency.entries.forEach { frequency ->
                        FilterChip(
                            selected = form.frequency == frequency,
                            onClick = { onFrequencyChange(frequency) },
                            label = { Text(frequencyLabel(frequency)) },
                        )
                    }
                }
                SelectorField(
                    label = stringResource(R.string.recurring_anchor_label),
                    value = form.anchorDate.toString(),
                    options = emptyList(),
                    onClick = { showDatePicker = true },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.recurring_active_label),
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = form.active, onCheckedChange = onActiveChange)
                }
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

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = form.anchorDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            onDateChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                        }
                        showDatePicker = false
                    },
                ) {
                    Text(stringResource(R.string.ok_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel_action))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun SelectorField(
    label: String,
    value: String,
    options: List<Pair<String?, String>>,
    onSelect: (String?) -> Unit = {},
    onClick: (() -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        Box {
            OutlinedButton(
                onClick = {
                    if (onClick != null) onClick() else expanded = true
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = value, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (options.isNotEmpty()) {
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { (id, labelText) ->
                        DropdownMenuItem(
                            text = { Text(labelText) },
                            onClick = {
                                onSelect(id)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun frequencyLabel(frequency: Frequency): String = stringResource(
    when (frequency) {
        Frequency.WEEKLY -> R.string.frequency_weekly
        Frequency.MONTHLY -> R.string.frequency_monthly
        Frequency.YEARLY -> R.string.frequency_yearly
    },
)
