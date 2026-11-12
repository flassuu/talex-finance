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
    settingsRepository: SettingsRepository,
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

    data class ImportPreview(val rowCount: Int, val text: String)

    data class UiState(
        val baseCurrency: String = "",
        val language: AppLanguage = AppLanguage.SYSTEM,
        val busy: Boolean = false,
        val message: UiMessage? = null,
        val restore: RestoreState? = null,
        val importPreview: ImportPreview? = null,
        val importReport: ImportReport? = null,
    )

    private data class LocalState(
        val language: AppLanguage = AppLanguage.SYSTEM,
        val busy: Boolean = false,
        val message: UiMessage? = null,
        val restore: RestoreState? = null,
        val importPreview: ImportPreview? = null,
        val importReport: ImportReport? = null,
    )

    private val local = MutableStateFlow(LocalState(language = LanguagePreferences.get(context)))

    val uiState: StateFlow<UiState> = combine(
        settingsRepository.observeBaseCurrency(),
        local,
    ) { base, state ->
        UiState(
            baseCurrency = base?.value.orEmpty(),
            language = state.language,
            busy = state.busy,
            message = state.message,
            restore = state.restore,
            importPreview = state.importPreview,
            importReport = state.importReport,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun setLanguage(language: AppLanguage) {
        LanguagePreferences.set(context, language)
        local.update { it.copy(language = language) }
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
            val rows = try {
                TransactionCsvCodec.decode(text).size
            } catch (e: CsvParseException) {
                postError(AppError.IMPORT_INVALID_ROW)
                return@launch
            }
            if (rows == 0) {
                postError(AppError.IMPORT_EMPTY)
                return@launch
            }
            local.update { it.copy(importPreview = ImportPreview(rows, text)) }
        }
    }

    fun cancelImport() {
        local.update { it.copy(importPreview = null) }
    }

    fun confirmImport() {
        val preview = local.value.importPreview ?: return
        viewModelScope.launch {
            local.update { it.copy(busy = true, importPreview = null) }
            when (val result = dataTransfer.importTransactionsCsv(preview.text)) {
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
