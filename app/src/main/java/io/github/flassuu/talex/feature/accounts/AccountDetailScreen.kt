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
import io.github.flassuu.talex.domain.repository.AccountRepository
import io.github.flassuu.talex.domain.repository.EntryRepository
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
import javax.inject.Inject

@HiltViewModel
class AccountDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository,
    private val entryRepository: EntryRepository,
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
    )

    data class UiState(
        val account: Account? = null,
        val entries: List<Entry> = emptyList(),
        val editing: Boolean = false,
        val form: FormState = FormState(),
        val message: AppError? = null,
        val saving: Boolean = false,
        val deleted: Boolean = false,
    )

    private val local = MutableStateFlow(LocalState())

    val uiState: StateFlow<UiState> = combine(
        accountRepository.observeAccount(accountId),
        entryRepository.observeForAccount(accountId),
        local,
    ) { account, entries, l ->
        UiState(account, entries, l.editing, l.form, l.message, l.saving, l.deleted)
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
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete_account)) },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                        enabled = state.entries.isEmpty(),
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
private fun EntryRow(entry: Entry) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (entry.type == EntryType.OPENING) {
                        stringResource(R.string.entry_opening_description)
                    } else {
                        entry.description
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
