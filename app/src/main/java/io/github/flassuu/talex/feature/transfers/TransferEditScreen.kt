package io.github.flassuu.talex.feature.transfers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.SavedStateHandle
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
import io.github.flassuu.talex.domain.repository.AccountRepository
import io.github.flassuu.talex.domain.repository.TransferRepository
import io.github.flassuu.talex.domain.usecase.SaveTransferInput
import io.github.flassuu.talex.domain.usecase.SaveTransferUseCase
import io.github.flassuu.talex.ui.component.TalexScreen
import io.github.flassuu.talex.ui.component.errorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

@HiltViewModel
class TransferEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository,
    private val transferRepository: TransferRepository,
    private val saveTransfer: SaveTransferUseCase,
) : ViewModel() {

    private val transferId: String? = savedStateHandle.get<String>("transferId")

    data class FormState(
        val sourceAccountId: String? = null,
        val destinationAccountId: String? = null,
        val sourceAmount: String = "",
        val destinationAmount: String = "",
        val date: LocalDate = LocalDate.now(),
        val description: String = "",
        val error: AppError? = null,
    )

    data class UiState(
        val accounts: List<Account> = emptyList(),
        val form: FormState = FormState(),
        val editing: Boolean = false,
        val loading: Boolean = true,
        val saving: Boolean = false,
        val done: Boolean = false,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val accounts = accountRepository.observeAccounts().first()
            val transfer = transferId?.let { transferRepository.get(it) }
            val form = if (transfer != null) {
                FormState(
                    sourceAccountId = transfer.sourceAccountId,
                    destinationAccountId = transfer.destinationAccountId,
                    sourceAmount = amountText(transfer.sourceAmount),
                    destinationAmount = amountText(transfer.destinationAmount),
                    date = transfer.date,
                    description = transfer.description,
                )
            } else {
                FormState()
            }
            _uiState.value = UiState(accounts, form, editing = transfer != null, loading = false)
        }
    }

    fun onSourceAccountChange(id: String) = updateForm { form ->
        syncSameCurrency(form.copy(sourceAccountId = id, error = null))
    }

    fun onDestinationAccountChange(id: String) = updateForm { form ->
        syncSameCurrency(form.copy(destinationAccountId = id, error = null))
    }

    fun onSourceAmountChange(value: String) = updateForm { form ->
        val next = form.copy(sourceAmount = value, error = null)
        if (sameCurrency(next)) next.copy(destinationAmount = value) else next
    }

    fun onDestinationAmountChange(value: String) =
        updateForm { it.copy(destinationAmount = value, error = null) }

    fun onDateChange(date: LocalDate) = updateForm { it.copy(date = date) }

    fun onDescriptionChange(value: String) = updateForm { it.copy(description = value) }

    fun save() {
        val state = _uiState.value
        if (state.saving) return
        val form = state.form
        val source = state.accounts.firstOrNull { it.id == form.sourceAccountId }
        val destination = state.accounts.firstOrNull { it.id == form.destinationAccountId }
        if (source == null || destination == null) {
            updateForm { it.copy(error = AppError.ACCOUNT_NOT_FOUND) }
            return
        }
        val sourceAmount = MoneyFormat.parse(form.sourceAmount, source.currency)
        if (sourceAmount == null || sourceAmount.isZero) {
            updateForm { it.copy(error = AppError.AMOUNT_INVALID) }
            return
        }
        val destinationAmount = if (source.currency == destination.currency) {
            sourceAmount
        } else {
            MoneyFormat.parse(form.destinationAmount, destination.currency)
        }
        if (destinationAmount == null || destinationAmount.isZero) {
            updateForm { it.copy(error = AppError.AMOUNT_INVALID) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true) }
            val result = saveTransfer(
                SaveTransferInput(
                    id = transferId,
                    sourceAccountId = source.id,
                    destinationAccountId = destination.id,
                    sourceAmount = sourceAmount.abs(),
                    destinationAmount = destinationAmount.abs(),
                    date = form.date,
                    description = form.description,
                ),
            )
            when (result) {
                is Outcome.Success -> _uiState.update { it.copy(saving = false, done = true) }
                is Outcome.Failure -> _uiState.update {
                    it.copy(saving = false, form = it.form.copy(error = result.error))
                }
            }
        }
    }

    fun delete() {
        val id = transferId ?: return
        viewModelScope.launch {
            when (val result = transferRepository.delete(id)) {
                is Outcome.Success -> _uiState.update { it.copy(done = true) }
                is Outcome.Failure -> updateForm { it.copy(error = result.error) }
            }
        }
    }

    private fun updateForm(block: (FormState) -> FormState) {
        _uiState.update { it.copy(form = block(it.form)) }
    }

    private fun sameCurrency(form: FormState): Boolean {
        val accounts = _uiState.value.accounts
        val source = accounts.firstOrNull { it.id == form.sourceAccountId } ?: return false
        val destination = accounts.firstOrNull { it.id == form.destinationAccountId } ?: return false
        return source.currency == destination.currency
    }

    private fun syncSameCurrency(form: FormState): FormState =
        if (sameCurrency(form) && form.sourceAmount.isNotEmpty()) {
            form.copy(destinationAmount = form.sourceAmount)
        } else {
            form
        }

    private fun amountText(money: Money): String =
        BigDecimal(money.amountMinor).abs()
            .movePointLeft(money.currency.fractionDigits)
            .setScale(money.currency.fractionDigits, RoundingMode.UNNECESSARY)
            .toPlainString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferEditScreen(
    onDone: () -> Unit,
    viewModel: TransferEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showSourcePicker by remember { mutableStateOf(false) }
    var showDestinationPicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.done) {
        if (state.done) onDone()
    }

    val form = state.form
    val sourceAccount = state.accounts.firstOrNull { it.id == form.sourceAccountId }
    val destinationAccount = state.accounts.firstOrNull { it.id == form.destinationAccountId }
    val sameCurrency = sourceAccount != null &&
        destinationAccount != null &&
        sourceAccount.currency == destinationAccount.currency
    val rateText = rateText(sourceAccount, destinationAccount, form)

    TalexScreen(
        title = stringResource(
            if (state.editing) R.string.edit_transfer_title else R.string.new_transfer_title,
        ),
        navigationIcon = {
            IconButton(onClick = onDone) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.back_action),
                )
            }
        },
        actions = {
            if (state.editing) {
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.delete_transfer),
                    )
                }
            }
        },
    ) { padding ->
        if (state.loading) return@TalexScreen

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SelectorField(
                label = stringResource(R.string.transfer_source_account_label),
                value = sourceAccount?.let { it.name + " (" + it.currency.value + ")" }
                    ?: stringResource(R.string.transfer_source_account_label),
                onClick = { showSourcePicker = true },
            )
            OutlinedTextField(
                value = form.sourceAmount,
                onValueChange = viewModel::onSourceAmountChange,
                label = { Text(stringResource(R.string.transfer_source_amount_label)) },
                singleLine = true,
                isError = form.error == AppError.AMOUNT_INVALID,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            SelectorField(
                label = stringResource(R.string.transfer_destination_account_label),
                value = destinationAccount?.let { it.name + " (" + it.currency.value + ")" }
                    ?: stringResource(R.string.transfer_destination_account_label),
                onClick = { showDestinationPicker = true },
            )
            OutlinedTextField(
                value = form.destinationAmount,
                onValueChange = viewModel::onDestinationAmountChange,
                label = { Text(stringResource(R.string.transfer_destination_amount_label)) },
                singleLine = true,
                enabled = !sameCurrency,
                isError = form.error == AppError.AMOUNT_INVALID,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            if (rateText != null) {
                Text(
                    text = stringResource(R.string.transfer_rate_label) + ": " + rateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SelectorField(
                label = stringResource(R.string.entry_date_label),
                value = form.date.toString(),
                onClick = { showDatePicker = true },
            )
            OutlinedTextField(
                value = form.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text(stringResource(R.string.transfer_description_label)) },
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
            Button(
                onClick = viewModel::save,
                enabled = !state.saving && state.accounts.size >= 2,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.save_action))
            }
            if (state.accounts.size < 2) {
                Text(
                    text = stringResource(R.string.transfer_needs_two_accounts),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    if (showSourcePicker) {
        AccountPickerDialog(
            accounts = state.accounts.filter { !it.archived },
            selectedId = form.sourceAccountId,
            onSelect = {
                viewModel.onSourceAccountChange(it)
                showSourcePicker = false
            },
            onDismiss = { showSourcePicker = false },
        )
    }

    if (showDestinationPicker) {
        AccountPickerDialog(
            accounts = state.accounts.filter { !it.archived },
            selectedId = form.destinationAccountId,
            onSelect = {
                viewModel.onDestinationAccountChange(it)
                showDestinationPicker = false
            },
            onDismiss = { showDestinationPicker = false },
        )
    }

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
                            viewModel.onDateChange(
                                Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate(),
                            )
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

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_transfer)) },
            text = { Text(stringResource(R.string.delete_transfer_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.delete()
                    },
                ) {
                    Text(stringResource(R.string.delete_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel_action))
                }
            },
        )
    }
}

private fun rateText(
    sourceAccount: Account?,
    destinationAccount: Account?,
    form: TransferEditViewModel.FormState,
): String? {
    if (sourceAccount == null || destinationAccount == null) return null
    if (sourceAccount.currency == destinationAccount.currency) return "1"
    val sourceAmount = MoneyFormat.parse(form.sourceAmount, sourceAccount.currency) ?: return null
    val destinationAmount = MoneyFormat.parse(form.destinationAmount, destinationAccount.currency) ?: return null
    if (sourceAmount.isZero) return null
    val sourceMajor = BigDecimal(sourceAmount.amountMinor).movePointLeft(sourceAccount.currency.fractionDigits)
    val destinationMajor = BigDecimal(destinationAmount.amountMinor)
        .movePointLeft(destinationAccount.currency.fractionDigits)
    return destinationMajor.divide(sourceMajor, 8, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
}

@Composable
private fun SelectorField(label: String, value: String, onClick: () -> Unit) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = value, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AccountPickerDialog(
    accounts: List<Account>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.entry_account_label)) },
        text = {
            Column {
                accounts.forEach { account ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(account.id) }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = account.name + " (" + account.currency.value + ")",
                            modifier = Modifier.weight(1f),
                        )
                        if (account.id == selectedId) {
                            Icon(imageVector = Icons.Outlined.Check, contentDescription = null)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_action))
            }
        },
    )
}
