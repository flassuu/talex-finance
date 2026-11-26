package io.github.flassuu.talex.feature.rates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.flassuu.talex.R
import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.domain.model.RateRefreshSchedule
import io.github.flassuu.talex.domain.money.formatRate
import io.github.flassuu.talex.ui.component.CurrencySelector
import io.github.flassuu.talex.ui.component.TalexScreen
import io.github.flassuu.talex.ui.component.errorMessage

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RatesScreen(viewModel: RatesViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val hostState = remember { SnackbarHostState() }
    val messageText = state.message?.let { message ->
        message.res?.let { stringResource(it, *message.args.toTypedArray()) }
            ?: message.error?.let { errorMessage(it) }
    }
    LaunchedEffect(state.message?.id) {
        messageText?.let {
            hostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    TalexScreen(
        title = stringResource(R.string.rates_title),
        snackbarHost = { SnackbarHost(hostState) },
        actions = {
            IconButton(onClick = viewModel::openAdd, enabled = !state.busy) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.rates_add_action),
                )
            }
            IconButton(onClick = viewModel::refresh, enabled = !state.busy) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = stringResource(R.string.rates_update_action),
                )
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.busy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    SyncCard(
                        state = state,
                        onSchedule = viewModel::requestSchedule,
                        onCustomDays = viewModel::onCustomDaysChange,
                        onPrecision = viewModel::onPrecisionChange,
                    )
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.rates_base_label, state.base.value),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.rates_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (state.unavailable.isNotEmpty()) {
                            Text(
                                text = stringResource(
                                    R.string.rates_unavailable,
                                    state.unavailable.joinToString(", ") { it.value },
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
                if (state.pairs.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = stringResource(R.string.rates_empty),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.rates_empty_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    items(state.pairs, key = { it.quote.value }) { row ->
                        RateRow(
                            base = state.base,
                            precision = state.sync.precision,
                            row = row,
                            onSyncChange = { enabled ->
                                viewModel.requestCurrencySync(row.quote, enabled)
                            },
                            onEdit = { viewModel.openEdit(row) },
                            onHistory = { viewModel.selectPair(row.quote) },
                        )
                    }
                }
            }
        }
    }

    state.form?.let { form ->
        RateFormDialog(
            base = state.base,
            form = form,
            onQuoteChange = viewModel::onQuoteChange,
            onRateChange = viewModel::onRateChange,
            onDateChange = viewModel::onDateChange,
            onFetch = viewModel::fetchForForm,
            onConfirm = viewModel::save,
            onDismiss = viewModel::closeForm,
        )
    }

    state.selected?.let { quote ->
        HistoryDialog(
            quote = quote,
            precision = state.sync.precision,
            history = state.history,
            onDelete = viewModel::delete,
            onDismiss = viewModel::closeHistory,
        )
    }

    state.pendingIntro?.let {
        AlertDialog(
            onDismissRequest = viewModel::cancelIntro,
            title = { Text(stringResource(R.string.rates_sync_intro_title)) },
            text = { Text(stringResource(R.string.rates_sync_intro_text)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmIntro) {
                    Text(stringResource(R.string.rates_sync_intro_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelIntro) {
                    Text(stringResource(R.string.cancel_action))
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SyncCard(
    state: RatesViewModel.UiState,
    onSchedule: (RateRefreshSchedule) -> Unit,
    onCustomDays: (String) -> Unit,
    onPrecision: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.rates_sync_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.rates_sync_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.rates_schedule_label),
                style = MaterialTheme.typography.titleSmall,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RateRefreshSchedule.entries.forEach { schedule ->
                    FilterChip(
                        selected = state.sync.schedule == schedule,
                        onClick = { onSchedule(schedule) },
                        label = { Text(scheduleLabel(schedule)) },
                    )
                }
            }
            if (state.sync.schedule == RateRefreshSchedule.CUSTOM) {
                OutlinedTextField(
                    value = state.customDaysInput,
                    onValueChange = onCustomDays,
                    label = { Text(stringResource(R.string.rates_custom_days_label)) },
                    singleLine = true,
                    isError = state.customDaysInvalid,
                    supportingText = {
                        if (state.customDaysInvalid) {
                            Text(stringResource(R.string.rates_custom_days_invalid))
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                text = stringResource(R.string.rates_precision_label),
                style = MaterialTheme.typography.titleSmall,
            )
            OutlinedTextField(
                value = state.precisionInput,
                onValueChange = onPrecision,
                label = { Text(stringResource(R.string.rates_precision_label)) },
                singleLine = true,
                isError = state.precisionInvalid,
                supportingText = {
                    if (state.precisionInvalid) {
                        Text(stringResource(R.string.rates_precision_invalid))
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun RateRow(
    base: CurrencyCode,
    precision: Int,
    row: RatesViewModel.PairRow,
    onSyncChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onHistory: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${base.value} → ${row.quote.value}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    val best = row.best
                    if (best == null) {
                        Text(
                            text = stringResource(R.string.rates_pair_unset),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        Text(
                            text = "${formatRate(best.value, precision)} · ${best.date} · " +
                                sourceLabel(best.source),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Switch(checked = row.syncEnabled, onCheckedChange = onSyncChange)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onEdit) {
                    Text(stringResource(R.string.edit_action))
                }
                TextButton(onClick = onHistory) {
                    Text(stringResource(R.string.rates_history_action))
                }
            }
        }
    }
}

@Composable
private fun RateFormDialog(
    base: CurrencyCode,
    form: RatesViewModel.FormState,
    onQuoteChange: (CurrencyCode) -> Unit,
    onRateChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onFetch: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rates_add_action)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.rates_form_hint, base.value))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    form.quote?.let { quote ->
                        CurrencySelector(
                            selected = quote,
                            onSelect = onQuoteChange,
                            label = stringResource(R.string.rates_quote_label),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    IconButton(
                        onClick = onFetch,
                        enabled = !form.fetching && form.quote != null,
                    ) {
                        if (form.fetching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = stringResource(R.string.rates_fetch_for_form),
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = form.rate,
                    onValueChange = onRateChange,
                    label = { Text(stringResource(R.string.rates_rate_label, base.value)) },
                    singleLine = true,
                    isError = form.error != null,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = form.date,
                    onValueChange = onDateChange,
                    label = { Text(stringResource(R.string.rates_date_label)) },
                    singleLine = true,
                    isError = form.error != null,
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
            TextButton(onClick = onConfirm, enabled = form.quote != null) {
                Text(stringResource(R.string.save_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_action)) }
        },
    )
}

@Composable
private fun HistoryDialog(
    quote: CurrencyCode,
    precision: Int,
    history: List<RatesViewModel.HistoryRow>,
    onDelete: (io.github.flassuu.talex.domain.model.Rate) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rates_history_title, quote.value)) },
        text = {
            if (history.isEmpty()) {
                Text(stringResource(R.string.rates_history_empty))
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                    items(history, key = { it.rate.date.toString() + it.rate.source }) { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${row.rate.date} · ${formatRate(row.value, precision)}",
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    text = sourceLabel(row.rate.source),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(onClick = { onDelete(row.rate) }) {
                                Text(stringResource(R.string.delete_action))
                            }
                        }
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
private fun scheduleLabel(schedule: RateRefreshSchedule): String = stringResource(
    when (schedule) {
        RateRefreshSchedule.MANUAL -> R.string.rates_schedule_manual
        RateRefreshSchedule.ON_LAUNCH -> R.string.rates_schedule_launch
        RateRefreshSchedule.WEEKLY -> R.string.rates_schedule_weekly
        RateRefreshSchedule.MONTHLY -> R.string.rates_schedule_monthly
        RateRefreshSchedule.CUSTOM -> R.string.rates_schedule_custom
    },
)

@Composable
private fun sourceLabel(source: String): String = when (source) {
    "manual" -> stringResource(R.string.rates_source_manual)
    "fetched" -> stringResource(R.string.rates_source_fetched)
    else -> source
}
