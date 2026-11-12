package io.github.flassuu.talex.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.flassuu.talex.R
import io.github.flassuu.talex.feature.analytics.AnalyticsPeriod
import java.time.LocalDate
import java.time.YearMonth

/**
 * The reporting window selector shared by analytics and budgets. It supports month
 * and year navigation and an optional custom date range.
 */
@Composable
fun PeriodSelector(
    period: AnalyticsPeriod,
    onPeriodChange: (AnalyticsPeriod) -> Unit,
    modifier: Modifier = Modifier,
    allowYear: Boolean = true,
    allowRange: Boolean = true,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var rangeOpen by remember { mutableStateOf(false) }
    val canNavigate = period !is AnalyticsPeriod.Range

    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { onPeriodChange(period.previous()) },
            enabled = canNavigate,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.period_previous),
            )
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            TextButton(onClick = { menuOpen = true }) {
                Text(text = period.label(), style = MaterialTheme.typography.titleMedium)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.period_this_month)) },
                    onClick = {
                        menuOpen = false
                        onPeriodChange(AnalyticsPeriod.Month(YearMonth.now()))
                    },
                )
                if (allowYear) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.period_this_year)) },
                        onClick = {
                            menuOpen = false
                            onPeriodChange(AnalyticsPeriod.Year(LocalDate.now().year))
                        },
                    )
                }
                if (allowRange) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.period_custom_range)) },
                        onClick = {
                            menuOpen = false
                            rangeOpen = true
                        },
                    )
                }
            }
        }
        IconButton(
            onClick = { onPeriodChange(period.next()) },
            enabled = canNavigate,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = stringResource(R.string.period_next),
            )
        }
    }

    if (rangeOpen) {
        RangeDialog(
            initial = period as? AnalyticsPeriod.Range,
            onDismiss = { rangeOpen = false },
            onApply = { range ->
                rangeOpen = false
                onPeriodChange(range)
            },
        )
    }
}

@Composable
private fun RangeDialog(
    initial: AnalyticsPeriod.Range?,
    onDismiss: () -> Unit,
    onApply: (AnalyticsPeriod.Range) -> Unit,
) {
    val today = LocalDate.now()
    var start by remember { mutableStateOf(initial?.start?.toString() ?: today.withDayOfMonth(1).toString()) }
    var end by remember { mutableStateOf(initial?.end?.toString() ?: today.toString()) }
    val startDate = runCatching { LocalDate.parse(start.trim()) }.getOrNull()
    val endDate = runCatching { LocalDate.parse(end.trim()) }.getOrNull()
    val range = AnalyticsPeriod.rangeOrNull(startDate, endDate)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.period_custom_range)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = start,
                    onValueChange = { start = it },
                    label = { Text(stringResource(R.string.period_range_start)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = end,
                    onValueChange = { end = it },
                    label = { Text(stringResource(R.string.period_range_end)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (range == null) {
                    Text(
                        text = stringResource(R.string.period_range_invalid),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { range?.let(onApply) },
                enabled = range != null,
            ) {
                Text(stringResource(R.string.period_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_action)) }
        },
    )
}
