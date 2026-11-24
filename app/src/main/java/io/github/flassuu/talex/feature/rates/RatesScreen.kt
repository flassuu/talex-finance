package io.github.flassuu.talex.feature.rates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.flassuu.talex.R
import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.ui.component.CurrencySelector
import io.github.flassuu.talex.ui.component.TalexScreen
import io.github.flassuu.talex.ui.component.errorMessage
import java.math.BigDecimal

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
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
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
            if (state.pairs.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
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
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.pairs, key = { it.quote.value }) { row ->
                        RateRow(
                            base = state.base,
                            row = row,
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
            onConfirm = viewModel::save,
            onDismiss = viewModel::closeForm,
        )
    }

    state.selected?.let { quote ->
        HistoryDialog(
            quote = quote,
            history = state.history,
            onDelete = viewModel::delete,
            onDismiss = viewModel::closeHistory,
        )
    }
}

@Composable
private fun RateRow(
    base: CurrencyCode,
    row: RatesViewModel.PairRow,
    onEdit: () -> Unit,
    onHistory: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                        text = "${rateText(best.value)} · ${best.date} · ${sourceLabel(best.source)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TextButton(onClick = onHistory) {
                Text(stringResource(R.string.rates_history_action))
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
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rates_add_action)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.rates_form_hint, base.value))
                form.quote?.let { quote ->
                    CurrencySelector(
                        selected = quote,
                        onSelect = onQuoteChange,
                        label = stringResource(R.string.rates_quote_label),
                        modifier = Modifier.fillMaxWidth(),
                    )
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
                                    text = "${row.rate.date} · ${rateText(row.value)}",
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
private fun sourceLabel(source: String): String = when (source) {
    "manual" -> stringResource(R.string.rates_source_manual)
    "fetched" -> stringResource(R.string.rates_source_fetched)
    else -> source
}

private fun rateText(value: BigDecimal): String = value.stripTrailingZeros().toPlainString()
