package io.github.flassuu.talex.feature.goals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import io.github.flassuu.talex.domain.model.Goal
import io.github.flassuu.talex.domain.model.GoalInput
import io.github.flassuu.talex.domain.repository.GoalRepository
import io.github.flassuu.talex.domain.repository.SettingsRepository
import io.github.flassuu.talex.ui.component.TalexScreen
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
class GoalsViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    data class FormState(
        val id: String? = null,
        val name: String = "",
        val target: String = "",
        val currency: CurrencyCode = CurrencyCode.EUR,
        val targetDate: LocalDate? = null,
        val error: AppError? = null,
    )

    data class UiState(
        val goals: List<Goal> = emptyList(),
        val form: FormState? = null,
    )

    private val form = MutableStateFlow<FormState?>(null)

    val uiState: StateFlow<UiState> = combine(goalRepository.observeGoals(), form) { goals, f ->
        UiState(goals, f)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun openCreate() {
        viewModelScope.launch {
            val base = settingsRepository.getBaseCurrency() ?: CurrencyCode.EUR
            form.value = FormState(currency = base)
        }
    }

    fun openEdit(goal: Goal) {
        form.value = FormState(
            id = goal.id,
            name = goal.name,
            target = amountText(goal.targetAmount),
            currency = goal.currency,
            targetDate = goal.targetDate,
        )
    }

    fun closeForm() = form.update { null }

    fun onNameChange(value: String) = form.update { it?.copy(name = value, error = null) }

    fun onTargetChange(value: String) = form.update { it?.copy(target = value, error = null) }

    fun onCurrencyChange(value: CurrencyCode) = form.update { it?.copy(currency = value) }

    fun onDateChange(date: LocalDate?) = form.update { it?.copy(targetDate = date) }

    fun save() {
        val state = form.value ?: return
        val name = state.name.trim()
        if (name.isEmpty()) {
            form.update { it?.copy(error = AppError.NAME_REQUIRED) }
            return
        }
        val target = MoneyFormat.parse(state.target, state.currency)
        if (target == null || target.isZero || target.isNegative) {
            form.update { it?.copy(error = AppError.AMOUNT_INVALID) }
            return
        }
        val input = GoalInput(name = name, targetAmount = target, targetDate = state.targetDate)
        viewModelScope.launch {
            val result = if (state.id == null) {
                goalRepository.create(input)
            } else {
                goalRepository.update(state.id, input)
            }
            when (result) {
                is Outcome.Success -> form.value = null
                is Outcome.Failure -> form.update { it?.copy(error = result.error) }
            }
        }
    }

    fun delete(goal: Goal) {
        viewModelScope.launch {
            goalRepository.delete(goal.id)
        }
    }

    private fun amountText(money: Money): String =
        BigDecimal(money.amountMinor)
            .movePointLeft(money.currency.fractionDigits)
            .setScale(money.currency.fractionDigits, RoundingMode.UNNECESSARY)
            .toPlainString()
}

@Composable
fun GoalsScreen(
    onOpenGoal: (String) -> Unit,
    viewModel: GoalsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    TalexScreen(
        title = stringResource(R.string.goals_title),
        actions = {
            IconButton(onClick = viewModel::openCreate) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.add_goal),
                )
            }
        },
    ) { padding ->
        if (state.goals.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.goals_empty),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.goals_empty_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.goals, key = { it.id }) { goal ->
                    GoalCard(
                        goal = goal,
                        onOpen = { onOpenGoal(goal.id) },
                        onEdit = { viewModel.openEdit(goal) },
                        onDelete = { viewModel.delete(goal) },
                    )
                }
            }
        }
    }

    state.form?.let { form ->
        GoalDialog(
            form = form,
            onDismiss = viewModel::closeForm,
            onNameChange = viewModel::onNameChange,
            onTargetChange = viewModel::onTargetChange,
            onCurrencyChange = viewModel::onCurrencyChange,
            onDateChange = viewModel::onDateChange,
            onConfirm = viewModel::save,
        )
    }
}

@Composable
private fun GoalCard(
    goal: Goal,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val target = goal.targetAmount.amountMinor
    val fraction = if (target > 0) {
        (goal.currentAmount.amountMinor.toFloat() / target).coerceIn(0f, 1f)
    } else {
        0f
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = goal.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.more_actions),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit_action)) },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete_action)) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.goal_progress,
                    MoneyFormat.format(goal.currentAmount),
                    MoneyFormat.format(goal.targetAmount),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            goal.targetDate?.let { date ->
                Text(
                    text = stringResource(R.string.goal_target_date, date.toString()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalDialog(
    form: GoalsViewModel.FormState,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onTargetChange: (String) -> Unit,
    onCurrencyChange: (CurrencyCode) -> Unit,
    onDateChange: (LocalDate?) -> Unit,
    onConfirm: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (form.id == null) R.string.add_goal else R.string.edit_goal))
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = form.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.goal_name_label)) },
                    singleLine = true,
                    isError = form.error == AppError.NAME_REQUIRED || form.error == AppError.NAME_TOO_LONG,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = form.target,
                    onValueChange = onTargetChange,
                    label = { Text(stringResource(R.string.goal_target_label)) },
                    singleLine = true,
                    isError = form.error == AppError.AMOUNT_INVALID,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.goal_currency_label),
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CurrencyCode.common.forEach { code ->
                        FilterChip(
                            selected = code == form.currency,
                            onClick = { onCurrencyChange(code) },
                            label = { Text(code.value) },
                            enabled = form.id == null,
                        )
                    }
                }
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = form.targetDate?.let {
                            stringResource(R.string.goal_target_date, it.toString())
                        } ?: stringResource(R.string.goal_no_target_date),
                    )
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
            initialSelectedDateMillis = (form.targetDate ?: LocalDate.now())
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
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
