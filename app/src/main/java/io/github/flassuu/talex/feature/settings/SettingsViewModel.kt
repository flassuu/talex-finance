package io.github.flassuu.talex.feature.settings

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.flassuu.talex.R
import io.github.flassuu.talex.core.csv.CsvParseException
import io.github.flassuu.talex.core.csv.TransactionCsvCodec
import io.github.flassuu.talex.core.locale.AppLanguage
import io.github.flassuu.talex.core.locale.LanguagePreferences
import io.github.flassuu.talex.core.result.AppError
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.core.security.LockTimeout
import io.github.flassuu.talex.core.security.PassphrasePolicy
import io.github.flassuu.talex.data.preferences.NavigationPreferences
import io.github.flassuu.talex.data.security.SecurityPreferences
import io.github.flassuu.talex.data.session.AppSessionManager
import io.github.flassuu.talex.domain.repository.DataTransferRepository
import io.github.flassuu.talex.domain.repository.ImportReport
import io.github.flassuu.talex.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val session: AppSessionManager,
    private val dataTransfer: DataTransferRepository,
    private val settingsRepository: SettingsRepository,
    private val navigationPreferences: NavigationPreferences,
) : ViewModel() {

    data class UiMessage(
        val id: Long = System.nanoTime(),
        val error: AppError? = null,
        @StringRes val successRes: Int? = null,
    )

    data class RestoreState(
        val uri: Uri,
        val passphrase: String = "",
        val error: AppError? = null,
        val busy: Boolean = false,
    )

    data class ImportPreview(val rowCount: Int, val text: String, val mapping: Map<String, String> = emptyMap())

    data class CsvMappingState(
        val text: String,
        val headers: List<String>,
        val mapping: Map<String, String>,
        val error: AppError? = null,
    ) {
        val missing: List<String>
            get() = TransactionCsvCodec.missingRequired(headers, mapping)
    }

    data class ChangePassphraseState(
        val current: String = "",
        val new: String = "",
        val confirm: String = "",
        val error: AppError? = null,
        val busy: Boolean = false,
        /** True when converting the demo profile, where the current passphrase is unknown to the user. */
        val demo: Boolean = false,
    )

    data class UiState(
        val baseCurrency: String = "",
        val language: AppLanguage = AppLanguage.SYSTEM,
        val lockTimeout: LockTimeout = LockTimeout.DEFAULT,
        val biometricAvailable: Boolean = false,
        val biometricEnabled: Boolean = false,
        val hideAmounts: Boolean = false,
        val navigationSlots: List<String> = NavigationPreferences.DEFAULT,
        val isDemo: Boolean = false,
        val showDeleteDemoDialog: Boolean = false,
        val busy: Boolean = false,
        val message: UiMessage? = null,
        val restore: RestoreState? = null,
        val importPreview: ImportPreview? = null,
        val csvMapping: CsvMappingState? = null,
        val importReport: ImportReport? = null,
        val changePassphrase: ChangePassphraseState? = null,
    )

    private data class LocalState(
        val language: AppLanguage = AppLanguage.SYSTEM,
        val lockTimeout: LockTimeout = LockTimeout.DEFAULT,
        val biometricEnabled: Boolean = false,
        val showDeleteDemoDialog: Boolean = false,
        val busy: Boolean = false,
        val message: UiMessage? = null,
        val restore: RestoreState? = null,
        val importPreview: ImportPreview? = null,
        val csvMapping: CsvMappingState? = null,
        val importReport: ImportReport? = null,
        val changePassphrase: ChangePassphraseState? = null,
    )

    private val local = MutableStateFlow(
        LocalState(
            language = LanguagePreferences.get(context),
            lockTimeout = SecurityPreferences.lockTimeout(context),
            biometricEnabled = session.isBiometricEnabled(),
        ),
    )

    val uiState: StateFlow<UiState> = combine(
        settingsRepository.observeBaseCurrency(),
        settingsRepository.observeHideAmounts(),
        navigationPreferences.slots,
        local,
    ) { base, hideAmounts, slots, state ->
        UiState(
            baseCurrency = base?.value.orEmpty(),
            language = state.language,
            lockTimeout = state.lockTimeout,
            biometricAvailable = session.isBiometricAvailable(),
            biometricEnabled = state.biometricEnabled,
            hideAmounts = hideAmounts,
            navigationSlots = slots,
            isDemo = session.isDemoProfile(),
            showDeleteDemoDialog = state.showDeleteDemoDialog,
            busy = state.busy,
            message = state.message,
            restore = state.restore,
            importPreview = state.importPreview,
            csvMapping = state.csvMapping,
            importReport = state.importReport,
            changePassphrase = state.changePassphrase,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun setLanguage(language: AppLanguage) {
        LanguagePreferences.set(context, language)
        local.update { it.copy(language = language) }
    }

    fun setLockTimeout(timeout: LockTimeout) {
        SecurityPreferences.setLockTimeout(context, timeout)
        local.update { it.copy(lockTimeout = timeout) }
    }

    fun setBiometric(enabled: Boolean) {
        val changed = if (enabled) session.enableBiometric() else {
            session.disableBiometric()
            true
        }
        local.update {
            it.copy(
                biometricEnabled = session.isBiometricEnabled(),
                message = if (changed) null else errorMessage(AppError.UNKNOWN),
            )
        }
    }

    fun setHideAmounts(hidden: Boolean) {
        viewModelScope.launch { settingsRepository.setHideAmounts(hidden) }
    }

    fun setNavigationSlot(index: Int, route: String) {
        navigationPreferences.setSlot(index, route)
    }

    fun startChangePassphrase() {
        local.update { it.copy(changePassphrase = ChangePassphraseState()) }
    }

    fun startDemoPassphraseChange() {
        local.update { it.copy(changePassphrase = ChangePassphraseState(demo = true)) }
    }

    fun requestDeleteDemo() = local.update { it.copy(showDeleteDemoDialog = true) }

    fun cancelDeleteDemo() = local.update { it.copy(showDeleteDemoDialog = false) }

    fun confirmDeleteDemo() {
        viewModelScope.launch {
            local.update { it.copy(showDeleteDemoDialog = false, busy = true) }
            session.deleteDemo()
            local.update { it.copy(busy = false) }
        }
    }

    fun changePassphraseCurrent(value: String) = updatePassphrase { it.copy(current = value, error = null) }

    fun changePassphraseNew(value: String) = updatePassphrase { it.copy(new = value, error = null) }

    fun changePassphraseConfirm(value: String) = updatePassphrase { it.copy(confirm = value, error = null) }

    fun cancelChangePassphrase() {
        local.update { it.copy(changePassphrase = null) }
    }

    fun submitChangePassphrase() {
        val dialog = local.value.changePassphrase ?: return
        if (dialog.busy) return
        val error = when {
            !PassphrasePolicy.isLongEnough(dialog.new) -> AppError.PASSPHRASE_TOO_SHORT
            dialog.new != dialog.confirm -> AppError.PASSPHRASE_MISMATCH
            !dialog.demo && dialog.new == dialog.current -> AppError.PASSPHRASE_UNCHANGED
            else -> null
        }
        if (error != null) {
            updatePassphrase { it.copy(error = error) }
            return
        }
        viewModelScope.launch {
            updatePassphrase { it.copy(busy = true, error = null) }
            val result = if (dialog.demo) {
                session.changeDemoPassphrase(dialog.new.toCharArray())
            } else {
                session.changePassphrase(
                    dialog.current.toCharArray(),
                    dialog.new.toCharArray(),
                )
            }
            when (result) {
                is Outcome.Success -> local.update {
                    it.copy(
                        changePassphrase = null,
                        message = successMessage(
                            if (dialog.demo) R.string.demo_converted
                            else R.string.settings_passphrase_changed,
                        ),
                    )
                }

                is Outcome.Failure -> updatePassphrase {
                    it.copy(busy = false, error = result.error)
                }
            }
        }
    }

    private fun updatePassphrase(block: (ChangePassphraseState) -> ChangePassphraseState) {
        local.update { state ->
            val dialog = state.changePassphrase ?: return@update state
            state.copy(changePassphrase = block(dialog))
        }
    }

    fun exportBackup(uri: Uri) = runBusy {
        when (val result = session.exportBackup(uri)) {
            is Outcome.Success -> postSuccess(R.string.settings_backup_saved)
            is Outcome.Failure -> postError(result.error)
        }
    }

    fun restoreStart(uri: Uri) {
        local.update { it.copy(restore = RestoreState(uri)) }
    }

    fun restoreCancel() {
        local.update { it.copy(restore = null) }
    }

    fun restorePassphraseChange(value: String) {
        local.update { it.copy(restore = it.restore?.copy(passphrase = value, error = null)) }
    }

    fun restore() {
        val restore = local.value.restore ?: return
        if (restore.passphrase.isEmpty() || restore.busy) return
        viewModelScope.launch {
            local.update { it.copy(restore = it.restore?.copy(busy = true, error = null)) }
            when (val result = session.restoreBackup(restore.uri, restore.passphrase.toCharArray())) {
                is Outcome.Success -> local.update {
                    it.copy(restore = null, message = successMessage(R.string.settings_restore_done))
                }
                is Outcome.Failure -> local.update {
                    it.copy(restore = it.restore?.copy(busy = false, error = result.error))
                }
            }
        }
    }

    fun exportTransactionsCsv(uri: Uri) = runBusy {
        if (write(uri, dataTransfer.exportTransactionsCsv())) {
            postSuccess(R.string.settings_csv_exported)
        } else {
            postError(AppError.UNKNOWN)
        }
    }

    fun exportCategoriesCsv(uri: Uri) = runBusy {
        if (write(uri, dataTransfer.exportCategoriesCsv())) {
            postSuccess(R.string.settings_csv_exported)
        } else {
            postError(AppError.UNKNOWN)
        }
    }

    fun previewImport(uri: Uri) {
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
            }
            if (text == null) {
                postError(AppError.UNKNOWN)
                return@launch
            }
            val headers = try {
                TransactionCsvCodec.headers(text)
            } catch (e: CsvParseException) {
                postError(AppError.IMPORT_INVALID_ROW)
                return@launch
            }
            if (headers.isEmpty()) {
                postError(AppError.IMPORT_EMPTY)
                return@launch
            }
            val mapping = TransactionCsvCodec.autoMapping(headers)
            if (TransactionCsvCodec.missingRequired(headers, mapping).isNotEmpty()) {
                local.update { it.copy(csvMapping = CsvMappingState(text, headers, mapping)) }
                return@launch
            }
            val rows = try {
                TransactionCsvCodec.decode(text, mapping).size
            } catch (e: CsvParseException) {
                postError(AppError.IMPORT_INVALID_ROW)
                return@launch
            }
            if (rows == 0) {
                postError(AppError.IMPORT_EMPTY)
                return@launch
            }
            local.update { it.copy(importPreview = ImportPreview(rows, text, mapping)) }
        }
    }

    fun onCsvMappingChange(field: String, header: String) {
        local.update { state ->
            val mapping = state.csvMapping ?: return@update state
            state.copy(
                csvMapping = mapping.copy(
                    mapping = mapping.mapping + (field to header),
                    error = null,
                ),
            )
        }
    }

    fun confirmCsvMapping() {
        val mapping = local.value.csvMapping ?: return
        if (mapping.missing.isNotEmpty()) {
            local.update { it.copy(csvMapping = mapping.copy(error = AppError.IMPORT_INVALID_ROW)) }
            return
        }
        val rows = try {
            TransactionCsvCodec.decode(mapping.text, mapping.mapping).size
        } catch (e: CsvParseException) {
            local.update { it.copy(csvMapping = mapping.copy(error = AppError.IMPORT_INVALID_ROW)) }
            return
        }
        if (rows == 0) {
            local.update { it.copy(csvMapping = null) }
            postError(AppError.IMPORT_EMPTY)
            return
        }
        local.update {
            it.copy(
                csvMapping = null,
                importPreview = ImportPreview(rows, mapping.text, mapping.mapping),
            )
        }
    }

    fun cancelCsvMapping() {
        local.update { it.copy(csvMapping = null) }
    }

    fun cancelImport() {
        local.update { it.copy(importPreview = null) }
    }

    fun confirmImport() {
        val preview = local.value.importPreview ?: return
        viewModelScope.launch {
            local.update { it.copy(busy = true, importPreview = null) }
            when (val result = dataTransfer.importTransactionsCsv(preview.text, preview.mapping)) {
                is Outcome.Success -> local.update { it.copy(busy = false, importReport = result.value) }
                is Outcome.Failure -> local.update { it.copy(busy = false, message = errorMessage(result.error)) }
            }
        }
    }

    fun dismissImportReport() {
        local.update { it.copy(importReport = null) }
    }

    fun clearMessage() {
        local.update { it.copy(message = null) }
    }

    private fun runBusy(block: suspend () -> Unit) {
        viewModelScope.launch {
            local.update { it.copy(busy = true) }
            try {
                block()
            } finally {
                local.update { it.copy(busy = false) }
            }
        }
    }

    private suspend fun write(uri: Uri, text: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri)
                ?.use { it.write(text.toByteArray(Charsets.UTF_8)) } != null
        }.getOrDefault(false)
    }

    private fun postSuccess(@StringRes res: Int) {
        local.update { it.copy(message = successMessage(res)) }
    }

    private fun postError(error: AppError) {
        local.update { it.copy(message = errorMessage(error)) }
    }

    private fun successMessage(@StringRes res: Int) = UiMessage(successRes = res)

    private fun errorMessage(error: AppError) = UiMessage(error = error)
}
