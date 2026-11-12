package io.github.flassuu.talex.feature.accounts

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
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
import io.github.flassuu.talex.domain.model.Account
import io.github.flassuu.talex.domain.model.AccountType
import io.github.flassuu.talex.domain.model.Entry
import io.github.flassuu.talex.domain.model.EntryType
import io.github.flassuu.talex.domain.model.Valuation
import io.github.flassuu.talex.domain.repository.AccountRepository
import io.github.flassuu.talex.domain.repository.EntryRepository
import io.github.flassuu.talex.domain.repository.ValuationRepository
import io.github.flassuu.talex.domain.usecase.UpdateAccountInput
import io.github.flassuu.talex.domain.usecase.UpdateAccountUseCase
import io.github.flassuu.talex.ui.component.AccountTypeSelector
import io.github.flassuu.talex.ui.component.TalexScreen
import io.github.flassuu.talex.ui.component.accountTypeLabel
import io.github.flassuu.talex.ui.component.entryTypeLabel
import io.github.flassuu.talex.ui.component.errorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AccountDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository,
    private val entryRepository: EntryRepository,
    private val valuationRepository: ValuationRepository,
    private val updateAccountUseCase: UpdateAccountUseCase,
) : ViewModel() {

    private val accountId: String = checkNotNull(savedStateHandle.get<String>("accountId"))

    data class FormState(
        val name: String = "",
        val type: AccountType = AccountType.CHECKING,
        val error: AppError? = null,
    )

    private data class LocalState(
        val editing: Boolean = false,
        val form: FormState = FormState(),
        val message: AppError? = null,
        val saving: Boolean = false,
        val deleted: Boolean = false,
        val adjustOpen: Boolean = false,
        val adjustText: String = "",
        val adjustError: AppError? = null,
    )

    data class UiState(
        val account: Account? = null,
        val entries: List<Entry> = emptyList(),
        val valuations: List<Valuation> = emptyList(),
        val editing: Boolean = false,
        val form: FormState = FormState(),
        val message: AppError? = null,
        val saving: Boolean = false,
        val deleted: Boolean = false,
        val adjustOpen: Boolean = false,
        val adjustText: String = "",
        val adjustError: AppError? = null,
    )

    private val local = MutableStateFlow(LocalState())

    val uiState: StateFlow<UiState> = combine(
        accountRepository.observeAccount(accountId),
        entryRepository.observeForAccount(accountId),
        valuationRepository.observeForAccount(accountId),
        local,
    ) { account, entries, valuations, l ->
        UiState(
            account = account,
            entries = entries,
            valuations = valuations,
            editing = l.editing,
            form = l.form,
            message = l.message,
            saving = l.saving,
            deleted = l.deleted,
            adjustOpen = l.adjustOpen,
            adjustText = l.adjustText,
            adjustError = l.adjustError,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun startEdit() {
        val account = uiState.value.account ?: return
        local.update {
            it.copy(editing = true, form = FormState(account.name, account.type), message = null)
        }
    }

    fun cancelEdit() = local.update { it.copy(editing = false, form = FormState(), message = null) }

    fun onNameChange(value: String) =
        local.update { it.copy(form = it.form.copy(name = value, error = null)) }

    fun onTypeChange(value: AccountType) =
        local.update { it.copy(form = it.form.copy(type = value)) }

    fun save() {
        if (local.value.saving) return
        val form = local.value.form
        viewModelScope.launch {
            local.update { it.copy(saving = true) }
            when (val result = updateAccountUseCase(UpdateAccountInput(accountId, form.name, form.type))) {
                is Outcome.Success -> local.update {
                    it.copy(editing = false, saving = false, form = FormState(), message = null)
                }

                is Outcome.Failure -> local.update {
                    it.copy(saving = false, form = it.form.copy(error = result.error))
                }
            }
        }
    }

    fun setArchived(archived: Boolean) {
        viewModelScope.launch {
            when (val result = accountRepository.setArchived(accountId, archived)) {
                is Outcome.Success -> local.update { it.copy(message = null) }
                is Outcome.Failure -> local.update { it.copy(message = result.error) }
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            when (val result = accountRepository.deleteAccount(accountId)) {
                is Outcome.Success -> local.update { it.copy(deleted = true, message = null) }
                is Outcome.Failure -> local.update { it.copy(message = result.error) }
            }
        }
    }

    fun openAdjust() {
        val account = uiState.value.account ?: return
        local.update {
            it.copy(adjustOpen = true, adjustText = amountText(account), adjustError = null)
        }
    }

    fun closeAdjust() = local.update { it.copy(adjustOpen = false, adjustError = null) }

    fun onAdjustChange(value: String) =
        local.update { it.copy(adjustText = value, adjustError = null) }

    fun saveAdjust() {
        val state = uiState.value
        val account = state.account ?: return
        val parsed = MoneyFormat.parse(state.adjustText, account.currency)
        if (parsed == null) {
            local.update { it.copy(adjustError = AppError.AMOUNT_INVALID) }
            return
        }
        viewModelScope.launch {
            when (val result = accountRepository.adjustBalance(account.id, parsed, LocalDate.now())) {
                is Outcome.Success -> local.update { it.copy(adjustOpen = false, adjustError = null) }
                is Outcome.Failure -> local.update { it.copy(adjustError = result.error) }
            }
        }
    }

    private fun amountText(account: Account): String =
        BigDecimal(account.balance.amountMinor)
            .movePointLeft(account.currency.fractionDigits)
            .toPlainString()

    fun clearMessage() = local.update { it.copy(message = null) }
}

@Composable
fun AccountDetailScreen(
    onBack: () -> Unit,
    viewModel: AccountDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(state.deleted) {
        if (state.deleted) onBack()
    }

    val account = state.account
    TalexScreen(
        title = account?.name ?: stringResource(R.string.account_title),
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.back_action),
                )
            }
        },
        actions = {
            if (account != null) {
                IconButton(onClick = viewModel::startEdit) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.edit_action),
                    )
                }
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.more_actions),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (account.archived) R.string.unarchive_account else R.string.archive_account,
                                ),
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (account.archived) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            menuOpen = false
                            viewModel.setArchived(!account.archived)
                        },
                    )
                    if (!account.type.isValuationBased) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.adjust_balance)) },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                viewModel.openAdjust()
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete_account)) },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            showDeleteConfirm = true
                        },
                    )
                }
            }
        },
    ) { padding ->
        if (account == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(stringResource(R.string.account_not_found), style = MaterialTheme.typography.bodyLarge)
            }
            return@TalexScreen
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.balance_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = MoneyFormat.format(account.balance),
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (account.balance.isNegative) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(
                                onClick = {},
                                label = { Text(accountTypeLabel(account.type)) },
                            )
                            AssistChip(
                                onClick = {},
                                label = { Text(account.currency.value) },
                            )
                            if (account.archived) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text(stringResource(R.string.account_archived_label)) },
                                )
                            }
                        }
                    }
                }
            }

            state.message?.let { error ->
                item {
                    Text(
                        text = errorMessage(error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (account.type.isValuationBased) {
                item {
                    Text(
                        text = stringResource(R.string.valuations_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                if (state.valuations.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.valuations_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        )
                    }
                } else {
                    items(state.valuations, key = { it.id }) { valuation ->
                        ValuationRow(valuation = valuation)
                    }
                }
            } else {
                item {
                    Text(
                        text = stringResource(R.string.transactions_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                if (state.entries.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.account_no_entries),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        )
                    }
                } else {
                    items(state.entries, key = { it.id }) { entry ->
                        EntryRow(entry = entry)
                    }
                }
            }
        }
    }

    if (state.editing) {
        EditAccountDialog(
            form = state.form,
            saving = state.saving,
            onDismiss = viewModel::cancelEdit,
            onNameChange = viewModel::onNameChange,
            onTypeChange = viewModel::onTypeChange,
            onConfirm = viewModel::save,
        )
    }

    if (state.adjustOpen) {
        AlertDialog(
            onDismissRequest = viewModel::closeAdjust,
            title = { Text(stringResource(R.string.adjust_balance)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = state.adjustText,
                        onValueChange = viewModel::onAdjustChange,
                        label = { Text(stringResource(R.string.new_balance_label)) },
                        singleLine = true,
                        isError = state.adjustError == AppError.AMOUNT_INVALID,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    state.adjustError?.let { error ->
                        Text(
                            text = errorMessage(error),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::saveAdjust) {
                    Text(stringResource(R.string.save_action))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::closeAdjust) {
                    Text(stringResource(R.string.cancel_action))
                }
            },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_account)) },
            text = { Text(stringResource(R.string.delete_account_confirm)) },
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

@Composable
private fun ValuationRow(valuation: Valuation) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = MoneyFormat.format(valuation.amount),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = valuation.date.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EntryRow(entry: Entry) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (entry.type) {
                        EntryType.OPENING -> stringResource(R.string.entry_opening_description)
                        EntryType.ADJUSTMENT -> stringResource(R.string.entry_adjustment_description)
                        else -> entry.description.ifBlank { stringResource(R.string.no_description) }
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = entry.date.toString() + " · " + entryTypeLabel(entry.type),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = MoneyFormat.format(entry.amount),
                style = MaterialTheme.typography.titleMedium,
                color = when {
                    entry.type == EntryType.INCOME -> MaterialTheme.colorScheme.primary
                    entry.type == EntryType.EXPENSE -> MaterialTheme.colorScheme.error
                    entry.amount.isNegative -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

@Composable
private fun EditAccountDialog(
    form: AccountDetailViewModel.FormState,
    saving: Boolean,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onTypeChange: (AccountType) -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_account)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = form.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.account_name_label)) },
                    singleLine = true,
                    isError = form.error == AppError.NAME_REQUIRED || form.error == AppError.NAME_TOO_LONG,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth(),
                )
                AccountTypeSelector(selected = form.type, onSelect = onTypeChange)
                Text(
                    text = stringResource(R.string.account_currency_locked),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            TextButton(onClick = onConfirm, enabled = !saving) {
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
