package io.github.flassuu.talex.feature.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.flassuu.talex.R
import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.money.MoneyFormat
import io.github.flassuu.talex.core.result.AppError
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.domain.model.Account
import io.github.flassuu.talex.domain.model.AccountType
import io.github.flassuu.talex.domain.repository.AccountRepository
import io.github.flassuu.talex.domain.repository.SettingsRepository
import io.github.flassuu.talex.domain.usecase.CreateAccountInput
import io.github.flassuu.talex.domain.usecase.CreateAccountUseCase
import io.github.flassuu.talex.ui.component.TalexScreen
import io.github.flassuu.talex.ui.component.accountTypeLabel
import io.github.flassuu.talex.ui.component.errorMessage
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
class AccountsViewModel @Inject constructor(
    accountRepository: AccountRepository,
    private val settingsRepository: SettingsRepository,
    private val createAccountUseCase: CreateAccountUseCase,
) : ViewModel() {

    data class FormState(
        val name: String = "",
        val type: AccountType = AccountType.CHECKING,
        val currency: CurrencyCode = CurrencyCode.EUR,
        val startingBalance: String = "",
        val error: AppError? = null,
    )

    data class UiState(
        val accounts: List<Account> = emptyList(),
        val createOpen: Boolean = false,
        val showArchived: Boolean = false,
        val form: FormState = FormState(),
        val saving: Boolean = false,
    )

    private val createOpen = MutableStateFlow(false)
    private val showArchived = MutableStateFlow(false)
    private val form = MutableStateFlow(FormState())
    private val saving = MutableStateFlow(false)

    val uiState: StateFlow<UiState> = combine(
        accountRepository.observeAccounts(),
        createOpen,
        showArchived,
        form,
        saving,
    ) { accounts, open, archivedVisible, formState, isSaving ->
        UiState(accounts, open, archivedVisible, formState, isSaving)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun openCreate() {
        viewModelScope.launch {
            val base = settingsRepository.getBaseCurrency() ?: CurrencyCode.EUR
            form.value = FormState(currency = base)
            createOpen.value = true
        }
    }

    fun closeCreate() {
        createOpen.value = false
    }

    fun toggleShowArchived() {
        showArchived.value = !showArchived.value
    }

    fun onNameChange(value: String) = form.update { it.copy(name = value, error = null) }

    fun onTypeChange(value: AccountType) = form.update { it.copy(type = value) }

    fun onCurrencyChange(value: CurrencyCode) = form.update { it.copy(currency = value) }

    fun onStartingBalanceChange(value: String) = form.update { it.copy(startingBalance = value, error = null) }

    fun createAccount() {
        val state = form.value
        if (saving.value) return

        val money = if (state.startingBalance.isBlank()) {
            null
        } else {
            val parsed = MoneyFormat.parse(state.startingBalance, state.currency)
            if (parsed == null) {
                form.update { it.copy(error = AppError.AMOUNT_INVALID) }
                return
            }
            parsed
        }

        viewModelScope.launch {
            saving.value = true
            val result = createAccountUseCase(
                CreateAccountInput(
                    name = state.name,
                    type = state.type,
                    currency = state.currency,
                    startingBalance = money,
                    date = LocalDate.now(),
                ),
            )
            saving.value = false
            when (result) {
                is Outcome.Success -> {
                    createOpen.value = false
                    form.value = FormState()
                }

                is Outcome.Failure -> form.update { it.copy(error = result.error) }
            }
        }
    }
}

@Composable
fun AccountsScreen(
    onOpenAccount: (String) -> Unit,
    viewModel: AccountsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val visibleAccounts = if (state.showArchived) state.accounts else state.accounts.filter { !it.archived }
    val hasArchived = state.accounts.any { it.archived }

    TalexScreen(
        title = stringResource(R.string.accounts_title),
        actions = {
            // The toggle is only shown when there is something to reveal, so it is
            // never a no-op button.
            if (hasArchived || state.showArchived) {
                IconButton(onClick = viewModel::toggleShowArchived) {
                    Icon(
                        imageVector = if (state.showArchived) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        contentDescription = stringResource(
                            if (state.showArchived) R.string.hide_archived else R.string.show_archived,
                        ),
                    )
                }
            }
            IconButton(onClick = viewModel::openCreate) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.add_account),
                )
            }
        },
    ) { padding ->
        if (visibleAccounts.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.accounts_empty),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.accounts_empty_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(visibleAccounts, key = { it.id }) { account ->
                    AccountCard(account = account, onClick = { onOpenAccount(account.id) })
                }
            }
        }
    }

    if (state.createOpen) {
        CreateAccountDialog(
            form = state.form,
            saving = state.saving,
            onDismiss = viewModel::closeCreate,
            onNameChange = viewModel::onNameChange,
            onTypeChange = viewModel::onTypeChange,
            onCurrencyChange = viewModel::onCurrencyChange,
            onStartingBalanceChange = viewModel::onStartingBalanceChange,
            onConfirm = viewModel::createAccount,
        )
    }
}

@Composable
private fun AccountCard(account: Account, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(account.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = accountTypeLabel(account.type) +
                        if (account.archived) " · " + stringResource(R.string.account_archived_label) else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = MoneyFormat.format(account.balance),
                style = MaterialTheme.typography.titleMedium,
                color = if (account.balance.isNegative) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

@Composable
private fun CreateAccountDialog(
    form: AccountsViewModel.FormState,
    saving: Boolean,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onTypeChange: (AccountType) -> Unit,
    onCurrencyChange: (CurrencyCode) -> Unit,
    onStartingBalanceChange: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_account)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = form.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.account_name_label)) },
                    singleLine = true,
                    isError = form.error == AppError.NAME_REQUIRED || form.error == AppError.NAME_TOO_LONG,
                    modifier = Modifier.fillMaxWidth(),
                )
                AccountTypeSelector(selected = form.type, onSelect = onTypeChange)
                CurrencySelector(selected = form.currency, onSelect = onCurrencyChange)
                OutlinedTextField(
                    value = form.startingBalance,
                    onValueChange = onStartingBalanceChange,
                    label = { Text(stringResource(R.string.account_starting_balance_label)) },
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
            TextButton(onClick = onConfirm, enabled = !saving) {
                Text(stringResource(R.string.create_action))
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
private fun AccountTypeSelector(selected: AccountType, onSelect: (AccountType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.account_type_label) + ": " + accountTypeLabel(selected),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AccountType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(accountTypeLabel(type)) },
                    onClick = {
                        onSelect(type)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun CurrencySelector(selected: CurrencyCode, onSelect: (CurrencyCode) -> Unit) {
    Column {
        Text(
            text = stringResource(R.string.account_currency_label),
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CurrencyCode.common.forEach { code ->
                FilterChip(
                    selected = code == selected,
                    onClick = { onSelect(code) },
                    label = { Text(code.value) },
                )
            }
        }
    }
}
