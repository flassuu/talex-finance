package io.github.flassuu.talex.feature.goals

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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.flassuu.talex.R
import io.github.flassuu.talex.core.money.MoneyFormat
import io.github.flassuu.talex.core.result.AppError
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.domain.model.Goal
import io.github.flassuu.talex.domain.model.GoalContribution
import io.github.flassuu.talex.domain.model.GoalContributionInput
import io.github.flassuu.talex.domain.repository.GoalRepository
import io.github.flassuu.talex.ui.component.TalexScreen
import io.github.flassuu.talex.ui.component.errorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

@HiltViewModel
class GoalDetailViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val goalId: String = checkNotNull(savedStateHandle["goalId"])

    data class FormState(
        val amount: String = "",
        val date: LocalDate = LocalDate.now(),
        val note: String = "",
        val error: AppError? = null,
    )

    data class UiState(
        val goal: Goal? = null,
        val form: FormState? = null,
    )

    private val form = MutableStateFlow<FormState?>(null)

    val uiState: StateFlow<UiState> = combine(
        goalRepository.observeGoals(),
        goalRepository.observeContributions(goalId),
        form,
    ) { goals, contributions, f ->
        UiState(
            goal = goals.firstOrNull { it.id == goalId }?.copy(contributions = contributions),
            form = f,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun openContribution() {
        form.value = FormState(date = LocalDate.now())
    }

    fun closeForm() = form.update { null }

    fun onAmountChange(value: String) = form.update { it?.copy(amount = value, error = null) }

    fun onNoteChange(value: String) = form.update { it?.copy(note = value) }

    fun onDateChange(date: LocalDate) = form.update { it?.copy(date = date) }

    fun addContribution() {
        val state = form.value ?: return
        val goal = uiState.value.goal ?: return
        val amount = MoneyFormat.parse(state.amount, goal.currency)
        if (amount == null || amount.isZero) {
            form.update { it?.copy(error = AppError.AMOUNT_INVALID) }
            return
        }
        viewModelScope.launch {
            val result = goalRepository.addContribution(
                goal.id,
                GoalContributionInput(amount = amount, date = state.date, note = state.note),
            )
            when (result) {
                is Outcome.Success -> form.value = null
                is Outcome.Failure -> form.update { it?.copy(error = result.error) }
            }
        }
    }

    fun deleteContribution(contribution: GoalContribution) {
        viewModelScope.launch {
            goalRepository.removeContribution(contribution.id)
        }
    }
}

@Composable
fun GoalDetailScreen(
    onBack: () -> Unit,
    viewModel: GoalDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val goal = state.goal

    TalexScreen(
        title = goal?.name ?: stringResource(R.string.goals_title),
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.back_action),
                )
            }
        },
        actions = {
            IconButton(onClick = viewModel::openContribution) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.add_contribution),
                )
            }
        },
    ) { padding ->
        if (goal == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(stringResource(R.string.goal_not_found), style = MaterialTheme.typography.titleMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { GoalSummary(goal) }
                if (goal.contributions.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.contributions_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    items(goal.contributions, key = { it.id }) { contribution ->
                        ContributionRow(
                            contribution = contribution,
                            onDelete = { viewModel.deleteContribution(contribution) },
                        )
                    }
                }
            }
        }
    }

    state.form?.let { form ->
        ContributionDialog(
            form = form,
            onDismiss = viewModel::closeForm,
            onAmountChange = viewModel::onAmountChange,
            onNoteChange = viewModel::onNoteChange,
            onDateChange = viewModel::onDateChange,
            onConfirm = viewModel::addContribution,
        )
    }
}

@Composable
private fun GoalSummary(goal: Goal) {
    val target = goal.targetAmount.amountMinor
    val fraction = if (target > 0) {
        (goal.currentAmount.amountMinor.toFloat() / target).coerceIn(0f, 1f)
    } else {
        0f
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(
                    R.string.goal_progress,
                    MoneyFormat.format(goal.currentAmount),
                    MoneyFormat.format(goal.targetAmount),
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
            goal.targetDate?.let { date ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.goal_target_date, date.toString()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ContributionRow(contribution: GoalContribution, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = MoneyFormat.format(contribution.amount),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (contribution.amount.isNegative) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                Text(
                    text = contribution.date.toString() +
                        (contribution.note?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
private fun ContributionDialog(
    form: GoalDetailViewModel.FormState,
    onDismiss: () -> Unit,
    onAmountChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onConfirm: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_contribution)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = form.amount,
                    onValueChange = onAmountChange,
                    label = { Text(stringResource(R.string.contribution_amount_label)) },
                    singleLine = true,
                    isError = form.error == AppError.AMOUNT_INVALID,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.contribution_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(form.date.toString())
                }
                OutlinedTextField(
                    value = form.note,
                    onValueChange = onNoteChange,
                    label = { Text(stringResource(R.string.contribution_note_label)) },
                    singleLine = true,
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

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = form.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
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
