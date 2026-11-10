package io.github.flassuu.talex.feature.unlock

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.flassuu.talex.R
import io.github.flassuu.talex.core.result.AppError
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.data.session.AppSessionManager
import io.github.flassuu.talex.ui.component.errorMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class UnlockViewModel @Inject constructor(
    private val session: AppSessionManager,
) : ViewModel() {

    data class UiState(
        val passphrase: String = "",
        val busy: Boolean = false,
        val error: AppError? = null,
        val biometricAvailable: Boolean = false,
        val biometricEnrolled: Boolean = false,
    )

    private val _uiState = MutableStateFlow(
        UiState(
            biometricAvailable = session.isBiometricAvailable(),
            biometricEnrolled = session.hasBiometricKey(),
        ),
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun onPassphraseChange(value: String) = _uiState.update { it.copy(passphrase = value, error = null) }

    fun refreshBiometricState() {
        _uiState.update {
            it.copy(
                biometricAvailable = session.isBiometricAvailable(),
                biometricEnrolled = session.hasBiometricKey(),
            )
        }
    }

    fun unlock() {
        if (_uiState.value.busy) return
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, error = null) }
            when (val result = session.unlock(_uiState.value.passphrase.toCharArray())) {
                // Drop the passphrase from memory as soon as the database is open.
                is Outcome.Success -> _uiState.update {
                    it.copy(passphrase = "", busy = false, error = null)
                }

                is Outcome.Failure -> _uiState.update { it.copy(busy = false, error = result.error) }
            }
        }
    }

    fun onBiometricSuccess() {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, error = null) }
            val key = withContext(Dispatchers.Default) { session.decryptBiometricKey() }
            if (key == null) {
                session.disableBiometric()
                _uiState.update {
                    it.copy(busy = false, error = AppError.BIOMETRIC_FAILED, biometricEnrolled = false)
                }
                return@launch
            }
            if (session.unlockWithKey(key) is Outcome.Failure) {
                _uiState.update { it.copy(busy = false, error = AppError.BIOMETRIC_FAILED) }
            }
        }
    }

    fun onBiometricError() {
        _uiState.update { it.copy(busy = false, error = AppError.BIOMETRIC_FAILED) }
    }
}

@Composable
fun UnlockScreen(viewModel: UnlockViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refreshBiometricState() }
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val executor = remember(context) { ContextCompat.getMainExecutor(context) }

    val callback = remember(viewModel) {
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                viewModel.onBiometricSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                ) {
                    viewModel.onBiometricError()
                }
            }
        }
    }

    val prompt = remember(activity, executor, callback) {
        activity?.let { BiometricPrompt(it, executor, callback) }
    }
    val promptTitle = stringResource(R.string.unlock_title)
    val promptSubtitle = stringResource(R.string.unlock_biometric)
    val promptNegative = stringResource(R.string.cancel_action)
    val promptInfo = remember(promptTitle, promptSubtitle, promptNegative) {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(promptTitle)
            .setSubtitle(promptSubtitle)
            .setNegativeButtonText(promptNegative)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK,
            )
            .build()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.unlock_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.unlock_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = state.passphrase,
                onValueChange = viewModel::onPassphraseChange,
                label = { Text(stringResource(R.string.passphrase_label)) },
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
                onClick = viewModel::unlock,
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
            ) {
                Text(stringResource(R.string.unlock_action))
            }
            if (state.biometricAvailable && state.biometricEnrolled && prompt != null) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { prompt.authenticate(promptInfo) },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
                ) {
                    Text(stringResource(R.string.unlock_biometric))
                }
            }
        }
    }
}
