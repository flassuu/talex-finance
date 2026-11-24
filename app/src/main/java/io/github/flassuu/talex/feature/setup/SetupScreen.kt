package io.github.flassuu.talex.feature.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import io.github.flassuu.talex.core.security.PassphrasePolicy
import io.github.flassuu.talex.data.session.AppSessionManager
import io.github.flassuu.talex.domain.model.AccountType
import io.github.flassuu.talex.domain.repository.AccountRepository
import io.github.flassuu.talex.domain.repository.SettingsRepository
import io.github.flassuu.talex.domain.usecase.SeedCategoriesUseCase
import io.github.flassuu.talex.domain.usecase.SeedDemoDataUseCase
import io.github.flassuu.talex.ui.component.AccountTypeSelector
import io.github.flassuu.talex.ui.component.errorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val session: AppSessionManager,
    private val settingsRepository: SettingsRepository,
    private val seedCategories: SeedCategoriesUseCase,
    private val accounts: AccountRepository,
    private val seedDemoData: SeedDemoDataUseCase,
) : ViewModel() {

    enum class Step { PASSPHRASE, CURRENCY, ACCOUNT, CATEGORIES }

    data class UiState(
        val step: Step = Step.PASSPHRASE,
        val passphrase: String = "",
        val confirm: String = "",
        val currency: CurrencyCode = CurrencyCode.EUR,
        val accountName: String = "",
        val accountType: AccountType = AccountType.CHECKING,
        val accountBalance: String = "",
        val error: AppError? = null,
        val busy: Boolean = false,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun onPassphraseChange(value: String) = _uiState.update { it.copy(passphrase = value, error = null) }

    fun onConfirmChange(value: String) = _uiState.update { it.copy(confirm = value, error = null) }

    fun onCurrencyChange(value: CurrencyCode) = _uiState.update { it.copy(currency = value) }

    fun onAccountNameChange(value: String) = _uiState.update { it.copy(accountName = value, error = null) }

    fun onAccountTypeChange(value: AccountType) = _uiState.update { it.copy(accountType = value) }

    fun onAccountBalanceChange(value: String) = _uiState.update { it.copy(accountBalance = value, error = null) }

    fun continueToCurrency() {
        val state = _uiState.value
        val error = when {
            !PassphrasePolicy.isLongEnough(state.passphrase) -> AppError.PASSPHRASE_TOO_SHORT
            state.passphrase != state.confirm -> AppError.PASSPHRASE_MISMATCH
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(error = error) }
            return
        }
        _uiState.update { it.copy(step = Step.CURRENCY, error = null) }
    }

    fun continueToAccount() {
        _uiState.update { it.copy(step = Step.ACCOUNT, error = null) }
    }

    fun continueToCategories() {
        val error = validateAccount(_uiState.value)
        if (error != null) {
            _uiState.update { it.copy(error = error) }
            return
        }
        _uiState.update { it.copy(step = Step.CATEGORIES, error = null) }
    }

    fun finish() {
        if (_uiState.value.busy) return
        val state = _uiState.value
        val error = validateAccount(state)
        if (error != null) {
            _uiState.update { it.copy(error = error) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, error = null) }
            when (val result = session.setup(state.passphrase.toCharArray())) {
                is Outcome.Success -> {
                    settingsRepository.setBaseCurrency(state.currency)
                    seedCategories()
                    createFirstAccount(state)
                    // Drop the passphrase from memory before leaving the wizard.
                    _uiState.update { it.copy(passphrase = "", confirm = "", busy = false) }
                    session.markUnlocked()
                }

                is Outcome.Failure -> _uiState.update { it.copy(busy = false, error = result.error) }
            }
        }
    }

    fun startDemo() {
        if (_uiState.value.busy) return
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, error = null) }
            when (val result = session.setupDemo()) {
                is Outcome.Success -> {
                    settingsRepository.setBaseCurrency(CurrencyCode.EUR)
                    seedCategories()
                    seedDemoData()
                    _uiState.update { it.copy(busy = false, passphrase = "", confirm = "") }
                    session.markUnlocked()
                }

                is Outcome.Failure -> _uiState.update { it.copy(busy = false, error = result.error) }
            }
        }
    }

    private suspend fun createFirstAccount(state: UiState) {
        val name = state.accountName.trim()
        if (name.isEmpty()) return
        val balance = state.accountBalance.trim()
            .takeIf { it.isNotEmpty() }
            ?.let { MoneyFormat.parse(it, state.currency) }
        accounts.createAccount(name, state.accountType, state.currency, balance, LocalDate.now())
    }

    private fun validateAccount(state: UiState): AppError? = when {
        state.accountName.trim().length > MAX_ACCOUNT_NAME_LENGTH -> AppError.NAME_TOO_LONG
        state.accountBalance.isNotBlank() &&
            MoneyFormat.parse(state.accountBalance, state.currency) == null -> AppError.AMOUNT_INVALID
        else -> null
    }

    companion object {
        const val MAX_ACCOUNT_NAME_LENGTH = 60
    }
}

@Composable
fun SetupScreen(viewModel: SetupViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (state.step) {
                SetupViewModel.Step.PASSPHRASE -> PassphraseStep(state, viewModel)
                SetupViewModel.Step.CURRENCY -> CurrencyStep(state, viewModel)
                SetupViewModel.Step.ACCOUNT -> AccountStep(state, viewModel)
                SetupViewModel.Step.CATEGORIES -> CategoriesStep(state, viewModel)
            }
        }
    }
}

@Composable
private fun PassphraseStep(state: SetupViewModel.UiState, viewModel: SetupViewModel) {
    Text(
        text = stringResource(R.string.setup_title),
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.setup_passphrase_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(24.dp))
    OutlinedTextField(
        value = state.passphrase,
        onValueChange = viewModel::onPassphraseChange,
        label = { Text(stringResource(R.string.passphrase_label)) },
        supportingText = { Text(stringResource(R.string.passphrase_hint)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next,
        ),
        modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = state.confirm,
        onValueChange = viewModel::onConfirmChange,
        label = { Text(stringResource(R.string.passphrase_confirm_label)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
        ),
        modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
    )
    state.error?.let { error ->
        Spacer(Modifier.height(8.dp))
        Text(
            text = errorMessage(error),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    Spacer(Modifier.height(24.dp))
    Button(
        onClick = viewModel::continueToCurrency,
        enabled = !state.busy,
        modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
    ) {
        Text(stringResource(R.string.continue_action))
    }
    Spacer(Modifier.height(16.dp))
    Text(
        text = stringResource(R.string.setup_demo_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    OutlinedButton(
        onClick = viewModel::startDemo,
        enabled = !state.busy,
        modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
    ) {
        Text(stringResource(R.string.setup_demo_action))
    }
}

@Composable
private fun CurrencyStep(state: SetupViewModel.UiState, viewModel: SetupViewModel) {
    Text(
        text = stringResource(R.string.base_currency_title),
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.setup_currency_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(24.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CurrencyCode.common.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { code ->
                    FilterChip(
                        selected = code == state.currency,
                        onClick = { viewModel.onCurrencyChange(code) },
                        label = { Text(code.value) },
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(24.dp))
    Button(
        onClick = viewModel::continueToAccount,
        enabled = !state.busy,
        modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
    ) {
        Text(stringResource(R.string.continue_action))
    }
}

@Composable
private fun AccountStep(state: SetupViewModel.UiState, viewModel: SetupViewModel) {
    Text(
        text = stringResource(R.string.setup_account_title),
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.setup_account_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(24.dp))
    OutlinedTextField(
        value = state.accountName,
        onValueChange = viewModel::onAccountNameChange,
        label = { Text(stringResource(R.string.account_name_label)) },
        supportingText = { Text(stringResource(R.string.setup_account_skip_hint)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
    )
    Spacer(Modifier.height(12.dp))
    AccountTypeSelector(
        selected = state.accountType,
        onSelect = viewModel::onAccountTypeChange,
        modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = state.accountBalance,
        onValueChange = viewModel::onAccountBalanceChange,
        label = { Text(stringResource(R.string.account_starting_balance_label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
    )
    state.error?.let { error ->
        Spacer(Modifier.height(8.dp))
        Text(
            text = errorMessage(error),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    Spacer(Modifier.height(24.dp))
    Button(
        onClick = viewModel::continueToCategories,
        enabled = !state.busy,
        modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
    ) {
        Text(stringResource(R.string.continue_action))
    }
}

@Composable
private fun CategoriesStep(state: SetupViewModel.UiState, viewModel: SetupViewModel) {
    Text(
        text = stringResource(R.string.setup_categories_title),
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.setup_categories_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(24.dp))
    val defaults = listOf(
        R.string.category_food,
        R.string.category_transport,
        R.string.category_housing,
        R.string.category_health,
        R.string.category_shopping,
        R.string.category_entertainment,
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        defaults.forEach { res ->
            Text(
                text = stringResource(res),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    state.error?.let { error ->
        Spacer(Modifier.height(8.dp))
        Text(
            text = errorMessage(error),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    Spacer(Modifier.height(24.dp))
    Button(
        onClick = viewModel::finish,
        enabled = !state.busy,
        modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
    ) {
        Text(stringResource(R.string.finish_action))
    }
}
