package io.github.flassuu.talex.feature.assets

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import io.github.flassuu.talex.core.money.convertMinor
import io.github.flassuu.talex.core.result.AppError
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.domain.model.Account
import io.github.flassuu.talex.domain.model.AccountType
import io.github.flassuu.talex.domain.model.ValuationInput
import io.github.flassuu.talex.domain.repository.AccountRepository
import io.github.flassuu.talex.domain.repository.RateRepository
import io.github.flassuu.talex.domain.repository.SettingsRepository
import io.github.flassuu.talex.domain.repository.ValuationRepository
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

@HiltViewModel
class AssetsViewModel @Inject constructor(
    accountRepository: AccountRepository,
    private val valuationRepository: ValuationRepository,
    settingsRepository: SettingsRepository,
    rateRepository: RateRepository,
) : ViewModel() {

    data class FormState(
        val accountId: String? = null,
        val amount: String = "",
        val date: LocalDate = LocalDate.now(),
        val error: AppError? = null,
    )

    data class UiState(
        val baseCurrency: CurrencyCode = CurrencyCode.EUR,
        val netWorth: Money = Money.zero(CurrencyCode.EUR),
        val assets: List<Account> = emptyList(),
        val liabilities: List<Account> = emptyList(),
        val valuationAccounts: List<Account> = emptyList(),
        val form: FormState? = null,
    )

    private data class Computed(
        val baseCurrency: CurrencyCode,
        val netWorth: Money,
        val assets: List<Account>,
        val liabilities: List<Account>,
        val valuationAccounts: List<Account>,
    )

    private val form = MutableStateFlow<FormState?>(null)

    private val computed = combine(
        accountRepository.observeAccounts(),
        settingsRepository.observeBaseCurrency(),
        rateRepository.observeLatest(),
    ) { accounts, base, rates ->
        val currency = base ?: CurrencyCode.EUR
        val active = accounts.filter { !it.archived }
        fun convert(money: Money): Long = convertMinor(money.amountMinor, money.currency, currency, rates)
        val netWorth = active.sumOf { convert(it.balance) }
        val assetAccounts = active.filter { it.type == AccountType.INVESTMENT || it.type.isValuationBased }
        val assetIds = assetAccounts.map { it.id }.toSet()
        val liabilityAccounts = active.filter {
            it.id !in assetIds &&
                (it.type == AccountType.LOAN || it.type == AccountType.CREDIT_CARD || it.balance.isNegative)
        }
        Computed(
            baseCurrency = currency,
            netWorth = Money(netWorth, currency),
            assets = assetAccounts,
            liabilities = liabilityAccounts,
            valuationAccounts = active.filter { it.type.isValuationBased },
        )
    }

    val uiState: StateFlow<UiState> = combine(computed, form) { c, f ->
        UiState(c.baseCurrency, c.netWorth, c.assets, c.liabilities, c.valuationAccounts, f)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun openValuation() {
        val first = uiState.value.valuationAccounts.firstOrNull()
        form.value = FormState(accountId = first?.id)
    }

    fun closeForm() = form.update { null }

    fun onAccountChange(id: String) = form.update { it?.copy(accountId = id, error = null) }

    fun onAmountChange(value: String) = form.update { it?.copy(amount = value, error = null) }

    fun onDateChange(date: LocalDate) = form.update { it?.copy(date = date) }

    fun save() {
        val state = form.value ?: return
        val account = uiState.value.valuationAccounts.firstOrNull { it.id == state.accountId }
        if (account == null) {
            form.update { it?.copy(error = AppError.ACCOUNT_NOT_FOUND) }
            return
        }
        val amount = MoneyFormat.parse(state.amount, account.currency)
        if (amount == null || amount.isNegative) {
            form.update { it?.copy(error = AppError.AMOUNT_INVALID) }
            return
        }
        viewModelScope.launch {
            val result = valuationRepository.add(account.id, ValuationInput(amount, state.date))
            when (result) {
                is Outcome.Success -> form.value = null
                is Outcome.Failure -> form.update { it?.copy(error = result.error) }
            }
        }
    }
}

@Composable
fun AssetsScreen(
    onOpenAccount: (String) -> Unit,
    viewModel: AssetsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val hasValuationAccount = state.valuationAccounts.isNotEmpty()

    TalexScreen(
        title = stringResource(R.string.assets_title),
        actions = {
            if (hasValuationAccount) {
                IconButton(onClick = viewModel::openValuation) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.add_valuation),
                    )
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.net_worth),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = MoneyFormat.format(state.netWorth),
                            style = MaterialTheme.typography.headlineSmall,
                            color = if (state.netWorth.isNegative) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }

            item { SectionTitle(stringResource(R.string.assets_section)) }
            if (state.assets.isEmpty()) {
                item { EmptyHint(stringResource(R.string.assets_empty_hint)) }
            } else {
                items(state.assets, key = { it.id }) { account ->
                    AccountRow(account = account, onClick = { onOpenAccount(account.id) })
                }
            }

            item { SectionTitle(stringResource(R.string.liabilities_section)) }
            if (state.liabilities.isEmpty()) {
                item { EmptyHint(stringResource(R.string.liabilities_empty_hint)) }
            } else {
                items(state.liabilities, key = { it.id }) { account ->
                    AccountRow(account = account, onClick = { onOpenAccount(account.id) })
                }
            }
        }
    }

    state.form?.let { form ->
        ValuationDialog(
            form = form,
            accounts = state.valuationAccounts,
            onDismiss = viewModel::closeForm,
            onAccountChange = viewModel::onAccountChange,
            onAmountChange = viewModel::onAmountChange,
            onDateChange = viewModel::onDateChange,
            onConfirm = viewModel::save,
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun AccountRow(account: Account, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(account.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = accountTypeLabel(account.type),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ValuationDialog(
    form: AssetsViewModel.FormState,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onAccountChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onConfirm: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val selected = accounts.firstOrNull { it.id == form.accountId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_valuation)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AccountField(
                    label = stringResource(R.string.entry_account_label),
                    selected = selected,
                    options = accounts,
                    onSelect = onAccountChange,
                )
                OutlinedTextField(
                    value = form.amount,
                    onValueChange = onAmountChange,
                    label = { Text(stringResource(R.string.valuation_amount_label)) },
                    singleLine = true,
                    isError = form.error == AppError.AMOUNT_INVALID,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(form.date.toString())
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

@Composable
private fun AccountField(
    label: String,
    selected: Account?,
    options: List<Account>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = selected?.let { it.name + " (" + it.currency.value + ")" }
                        ?: label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (options.isNotEmpty()) {
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(account.name + " (" + account.currency.value + ")") },
                            onClick = {
                                onSelect(account.id)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}
