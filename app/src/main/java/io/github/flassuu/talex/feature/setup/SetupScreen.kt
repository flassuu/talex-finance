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
import io.github.flassuu.talex.core.result.AppError
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.data.session.AppSessionManager
import io.github.flassuu.talex.domain.repository.SettingsRepository
import io.github.flassuu.talex.ui.component.errorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val session: AppSessionManager,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    enum class Step { PASSPHRASE, CURRENCY }

    data class UiState(
        val step: Step = Step.PASSPHRASE,
        val passphrase: String = "",
        val confirm: String = "",
        val currency: CurrencyCode = CurrencyCode.EUR,
        val error: AppError? = null,
        val busy: Boolean = false,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun onPassphraseChange(value: String) = _uiState.update { it.copy(passphrase = value, error = null) }

    fun onConfirmChange(value: String) = _uiState.update { it.copy(confirm = value, error = null) }

    fun onCurrencyChange(value: CurrencyCode) = _uiState.update { it.copy(currency = value) }

    fun continueToCurrency() {
        val state = _uiState.value
        val error = when {
            state.passphrase.length < MIN_PASSPHRASE_LENGTH -> AppError.PASSPHRASE_TOO_SHORT
            state.passphrase != state.confirm -> AppError.PASSPHRASE_MISMATCH
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(error = error) }
            return
        }
        _uiState.update { it.copy(step = Step.CURRENCY, error = null) }
    }

    fun finish() {
        if (_uiState.value.busy) return
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, error = null) }
            val currency = _uiState.value.currency
            when (val result = session.setup(_uiState.value.passphrase.toCharArray())) {
                is Outcome.Success -> {
                    settingsRepository.setBaseCurrency(currency)
                    // Drop the passphrase from memory before leaving the wizard.
                    _uiState.update { it.copy(passphrase = "", confirm = "", busy = false) }
                    session.markUnlocked()
                }

                is Outcome.Failure -> _uiState.update { it.copy(busy = false, error = result.error) }
            }
        }
    }

    companion object {
        const val MIN_PASSPHRASE_LENGTH = 8
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
                SetupViewModel.Step.PASSPHRASE -> {
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
                }

                SetupViewModel.Step.CURRENCY -> {
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
                        onClick = viewModel::finish,
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
                    ) {
                        Text(stringResource(R.string.finish_action))
                    }
                }
            }
        }
    }
}
