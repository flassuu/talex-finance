package io.github.flassuu.talex.feature.settings

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.flassuu.talex.R
import io.github.flassuu.talex.core.locale.AppLanguage
import io.github.flassuu.talex.domain.repository.ImportReport
import io.github.flassuu.talex.ui.component.TalexScreen
import io.github.flassuu.talex.ui.component.errorMessage

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val hostState = remember { SnackbarHostState() }
    val activity = LocalActivity.current
    val messageText = state.message?.let { message ->
        message.successRes?.let { stringResource(it) } ?: message.error?.let { errorMessage(it) }
    }
    LaunchedEffect(state.message?.id) {
        messageText?.let {
            hostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val exportBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri -> uri?.let(viewModel::exportBackup) }
    val restoreBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::restoreStart) }
    val exportCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri -> uri?.let(viewModel::exportTransactionsCsv) }
    val exportCategories = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri -> uri?.let(viewModel::exportCategoriesCsv) }
    val importCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::previewImport) }

    TalexScreen(
        title = stringResource(R.string.settings_title),
        snackbarHost = { SnackbarHost(hostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.busy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_base_currency),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(text = state.baseCurrency, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_language_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_language_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppLanguage.entries.forEach { language ->
                            FilterChip(
                                selected = state.language == language,
                                onClick = {
                                    if (state.language != language) {
                                        viewModel.setLanguage(language)
                                        activity?.recreate()
                                    }
                                },
                                label = { Text(stringResource(language.labelRes())) },
                            )
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_backup_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_backup_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { exportBackup.launch(BACKUP_FILE_NAME) },
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.settings_export_backup))
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { restoreBackup.launch(ANY_FILES) },
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.settings_restore_backup))
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_csv_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_csv_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { exportCsv.launch(TRANSACTIONS_FILE_NAME) },
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.settings_export_csv))
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { exportCategories.launch(CATEGORIES_FILE_NAME) },
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.settings_export_categories_csv))
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { importCsv.launch(CSV_FILES) },
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.settings_import_csv))
                    }
                }
            }
        }
    }

    state.restore?.let { restore ->
        RestoreDialog(
            state = restore,
            onPassphraseChange = viewModel::restorePassphraseChange,
            onConfirm = viewModel::restore,
            onDismiss = viewModel::restoreCancel,
        )
    }
    state.importPreview?.let { preview ->
        ImportPreviewDialog(
            rowCount = preview.rowCount,
            onConfirm = viewModel::confirmImport,
            onDismiss = viewModel::cancelImport,
        )
    }
    state.importReport?.let { report ->
        ImportReportDialog(report = report, onDismiss = viewModel::dismissImportReport)
    }
}

@Composable
private fun RestoreDialog(
    state: SettingsViewModel.RestoreState,
    onPassphraseChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_restore_passphrase_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.settings_restore_passphrase_hint))
                OutlinedTextField(
                    value = state.passphrase,
                    onValueChange = onPassphraseChange,
                    label = { Text(stringResource(R.string.passphrase_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = state.error != null,
                    modifier = Modifier.fillMaxWidth(),
                )
                state.error?.let { error ->
                    Text(
                        text = errorMessage(error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = state.passphrase.isNotEmpty() && !state.busy,
            ) {
                Text(stringResource(R.string.settings_restore_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_action)) }
        },
    )
}

@Composable
private fun ImportPreviewDialog(
    rowCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_import_preview_title)) },
        text = { Text(stringResource(R.string.settings_import_preview_message, rowCount)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.settings_import_action)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_action)) }
        },
    )
}

@Composable
private fun ImportReportDialog(report: ImportReport, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_import_report_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    stringResource(
                        R.string.settings_import_report_summary,
                        report.imported,
                        report.skipped.size,
                    ),
                )
                if (report.skipped.isEmpty()) {
                    Text(stringResource(R.string.settings_import_report_none))
                } else {
                    Text(
                        text = stringResource(R.string.settings_import_report_skipped),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    report.skipped.forEach { skip ->
                        Text(
                            text = "Line ${skip.line}: ${errorMessage(skip.reason)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok_action)) }
        },
    )
}

@StringRes
private fun AppLanguage.labelRes(): Int = when (this) {
    AppLanguage.SYSTEM -> R.string.language_system
    AppLanguage.ENGLISH -> R.string.language_english
    AppLanguage.UKRAINIAN -> R.string.language_ukrainian
}

private const val BACKUP_FILE_NAME = "talex-backup.talex"
private const val TRANSACTIONS_FILE_NAME = "talex-transactions.csv"
private const val CATEGORIES_FILE_NAME = "talex-categories.csv"
private val ANY_FILES = arrayOf("application/zip", "application/octet-stream", "*/*")
private val CSV_FILES = arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*")
