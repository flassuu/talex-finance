package io.github.flassuu.talex.feature.settings

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.flassuu.talex.R
import io.github.flassuu.talex.core.csv.TransactionCsvCodec
import io.github.flassuu.talex.core.locale.AppLanguage
import io.github.flassuu.talex.core.result.AppError
import io.github.flassuu.talex.core.security.LockTimeout
import io.github.flassuu.talex.domain.repository.ImportReport
import io.github.flassuu.talex.ui.component.NavDestination
import io.github.flassuu.talex.ui.component.TalexScreen
import io.github.flassuu.talex.ui.component.errorMessage

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onOpenRates: () -> Unit = {},
    onOpenChangeBase: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
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

            if (state.isDemo) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.settings_demo_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.settings_demo_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = viewModel::startDemoPassphraseChange,
                            enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.settings_demo_set_passphrase))
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = viewModel::requestDeleteDemo,
                            enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.settings_demo_delete))
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_base_currency),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = state.baseCurrency,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = onOpenChangeBase,
                            enabled = !state.busy,
                        ) {
                            Text(stringResource(R.string.settings_change_base_currency))
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_exchange_rates),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_exchange_rates_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onOpenRates,
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.settings_open_rates))
                    }
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
                        text = stringResource(R.string.settings_bottom_bar_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_bottom_bar_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    state.navigationSlots.forEachIndexed { index, route ->
                        SlotSelector(
                            index = index,
                            route = route,
                            onSelect = { viewModel.setNavigationSlot(index, it) },
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_security_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_change_passphrase_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    if (!state.isDemo) {
                        OutlinedButton(
                            onClick = viewModel::startChangePassphrase,
                            enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.settings_change_passphrase))
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.settings_lock_timeout),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_lock_timeout_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LockTimeout.entries.forEach { timeout ->
                            FilterChip(
                                selected = state.lockTimeout == timeout,
                                onClick = { viewModel.setLockTimeout(timeout) },
                                label = { Text(stringResource(timeout.labelRes())) },
                            )
                        }
                    }

                    if (state.biometricAvailable) {
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_biometric_title),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text = stringResource(R.string.settings_biometric_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = state.biometricEnabled,
                                onCheckedChange = viewModel::setBiometric,
                            )
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_privacy_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_hide_amounts),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = stringResource(R.string.settings_hide_amounts_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = state.hideAmounts,
                            onCheckedChange = viewModel::setHideAmounts,
                        )
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_insights_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_insights_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.settings_alerts_enabled),
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = state.insights.alertsEnabled,
                            onCheckedChange = viewModel::setAlertsEnabled,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.settings_advice_enabled),
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = state.insights.adviceEnabled,
                            onCheckedChange = viewModel::setAdviceEnabled,
                        )
                    }
                    InsightNumberRow(
                        label = stringResource(R.string.settings_cash_flow_months),
                        value = state.insights.negativeCashFlowMonths.toString(),
                        onValueChange = { it.toIntOrNull()?.let(viewModel::setNegativeCashFlowMonths) },
                    )
                    InsightNumberRow(
                        label = stringResource(R.string.settings_net_worth_months),
                        value = state.insights.netWorthDeclineMonths.toString(),
                        onValueChange = { it.toIntOrNull()?.let(viewModel::setNetWorthDeclineMonths) },
                    )
                    InsightNumberRow(
                        label = stringResource(R.string.settings_risky_percent),
                        value = state.insights.riskyShareLimitPercent.toString(),
                        onValueChange = { it.toIntOrNull()?.let(viewModel::setRiskyShareLimitPercent) },
                    )
                    InsightNumberRow(
                        label = stringResource(R.string.settings_idle_amount),
                        value = state.insights.idleCashThresholdMajor.toString(),
                        onValueChange = { it.toLongOrNull()?.let(viewModel::setIdleCashThreshold) },
                    )
                    InsightNumberRow(
                        label = stringResource(R.string.settings_idle_days),
                        value = state.insights.idleDays.toString(),
                        onValueChange = { it.toIntOrNull()?.let(viewModel::setIdleDays) },
                    )
                    InsightNumberRow(
                        label = stringResource(R.string.settings_rising_percent),
                        value = state.insights.risingCategoryPercent.toString(),
                        onValueChange = { it.toIntOrNull()?.let(viewModel::setRisingCategoryPercent) },
                    )
                    InsightNumberRow(
                        label = stringResource(R.string.settings_savings_points),
                        value = state.insights.savingsRatePoints.toString(),
                        onValueChange = { it.toIntOrNull()?.let(viewModel::setSavingsRatePoints) },
                    )
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
    state.changePassphrase?.let { dialog ->
        ChangePassphraseDialog(
            state = dialog,
            onCurrentChange = viewModel::changePassphraseCurrent,
            onNewChange = viewModel::changePassphraseNew,
            onConfirmChange = viewModel::changePassphraseConfirm,
            onConfirm = viewModel::submitChangePassphrase,
            onDismiss = viewModel::cancelChangePassphrase,
        )
    }
    if (state.showDeleteDemoDialog) {
        AlertDialog(
            onDismissRequest = viewModel::cancelDeleteDemo,
            title = { Text(stringResource(R.string.settings_demo_delete)) },
            text = { Text(stringResource(R.string.settings_demo_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDeleteDemo) {
                    Text(stringResource(R.string.delete_action))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDeleteDemo) {
                    Text(stringResource(R.string.cancel_action))
                }
            },
        )
    }
    state.importPreview?.let { preview ->
        ImportPreviewDialog(
            rowCount = preview.rowCount,
            onConfirm = viewModel::confirmImport,
            onDismiss = viewModel::cancelImport,
        )
    }
    state.csvMapping?.let { mapping ->
        CsvMappingDialog(
            state = mapping,
            onChange = viewModel::onCsvMappingChange,
            onConfirm = viewModel::confirmCsvMapping,
            onDismiss = viewModel::cancelCsvMapping,
        )
    }
    state.importReport?.let { report ->
        ImportReportDialog(report = report, onDismiss = viewModel::dismissImportReport)
    }
}

@Composable
private fun ChangePassphraseDialog(
    state: SettingsViewModel.ChangePassphraseState,
    onCurrentChange: (String) -> Unit,
    onNewChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (state.demo) R.string.settings_demo_set_passphrase
                    else R.string.settings_change_passphrase_title,
                ),
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(
                        if (state.demo) R.string.settings_demo_set_passphrase_hint
                        else R.string.settings_change_passphrase_hint,
                    ),
                )
                if (!state.demo) {
                    OutlinedTextField(
                        value = state.current,
                        onValueChange = onCurrentChange,
                        label = { Text(stringResource(R.string.passphrase_current_label)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        isError = state.error == AppError.WRONG_PASSPHRASE,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = state.new,
                    onValueChange = onNewChange,
                    label = { Text(stringResource(R.string.passphrase_new_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = state.error == AppError.PASSPHRASE_TOO_SHORT ||
                        state.error == AppError.PASSPHRASE_UNCHANGED,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.confirm,
                    onValueChange = onConfirmChange,
                    label = { Text(stringResource(R.string.passphrase_confirm_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = state.error == AppError.PASSPHRASE_MISMATCH,
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
                enabled = !state.busy &&
                    (state.demo || state.current.isNotEmpty()) &&
                    state.new.isNotEmpty() &&
                    state.confirm.isNotEmpty(),
            ) {
                Text(stringResource(R.string.settings_change_passphrase_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_action)) }
        },
    )
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
private fun CsvMappingDialog(
    state: SettingsViewModel.CsvMappingState,
    onChange: (String, String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_csv_mapping_title)) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_csv_mapping_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TransactionCsvCodec.HEADERS.forEach { field ->
                    CsvColumnSelector(
                        field = field,
                        required = field in TransactionCsvCodec.REQUIRED,
                        headers = state.headers,
                        selected = state.mapping[field].orEmpty(),
                        onSelect = { onChange(field, it) },
                    )
                }
                state.error?.let {
                    Text(
                        text = if (state.missing.isNotEmpty()) {
                            stringResource(R.string.settings_csv_mapping_missing)
                        } else {
                            stringResource(R.string.settings_csv_mapping_invalid)
                        },
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = state.missing.isEmpty()) {
                Text(stringResource(R.string.settings_csv_mapping_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_action)) }
        },
    )
}

@Composable
private fun CsvColumnSelector(
    field: String,
    required: Boolean,
    headers: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val label = if (required) "$field *" else field
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "$label: " + selected.ifEmpty {
                    stringResource(R.string.settings_csv_not_mapped)
                },
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.settings_csv_not_mapped)) },
                onClick = {
                    onSelect("")
                    expanded = false
                },
            )
            headers.forEach { header ->
                DropdownMenuItem(
                    text = { Text(header) },
                    onClick = {
                        onSelect(header)
                        expanded = false
                    },
                )
            }
        }
    }
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

@Composable
private fun InsightNumberRow(label: String, value: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(104.dp),
        )
    }
}

@Composable
private fun SlotSelector(index: Int, route: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val current = NavDestination.fromRoute(route) ?: NavDestination.defaults[index]
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.settings_slot_label, index + 1) +
                    ": " + stringResource(current.labelRes),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            NavDestination.allowed.forEach { destination ->
                DropdownMenuItem(
                    text = { Text(stringResource(destination.labelRes)) },
                    onClick = {
                        onSelect(destination.route)
                        expanded = false
                    },
                )
            }
        }
    }
}

@StringRes
private fun LockTimeout.labelRes(): Int = when (this) {
    LockTimeout.IMMEDIATELY -> R.string.lock_timeout_immediately
    LockTimeout.THIRTY_SECONDS -> R.string.lock_timeout_30s
    LockTimeout.ONE_MINUTE -> R.string.lock_timeout_1m
    LockTimeout.FIVE_MINUTES -> R.string.lock_timeout_5m
    LockTimeout.FIFTEEN_MINUTES -> R.string.lock_timeout_15m
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
