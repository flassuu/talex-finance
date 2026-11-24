package io.github.flassuu.talex.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.flassuu.talex.R
import io.github.flassuu.talex.core.money.CurrencyCode
import java.util.Locale

/**
 * A currency field that opens a searchable ISO 4217 picker. The six common
 * currencies come first; every other code the platform knows follows.
 */
@Composable
fun CurrencySelector(
    selected: CurrencyCode,
    onSelect: (CurrencyCode) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.account_currency_label),
) {
    var showDialog by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { showDialog = true }, modifier = modifier) {
        Text("$label: ${selected.value} – ${selected.displayName()}")
    }
    if (showDialog) {
        CurrencyPickerDialog(
            selected = selected,
            onSelect = {
                onSelect(it)
                showDialog = false
            },
            onDismiss = { showDialog = false },
        )
    }
}

@Composable
fun CurrencyPickerDialog(
    selected: CurrencyCode,
    onSelect: (CurrencyCode) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        val all = CurrencyCode.all
        if (query.isBlank()) {
            CurrencyCode.common + all.filter { it !in CurrencyCode.common }
        } else {
            all.filter { code ->
                code.value.contains(query, ignoreCase = true) ||
                    code.displayName().contains(query, ignoreCase = true)
            }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.currency_picker_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.currency_search_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(filtered, key = { it.value }) { code ->
                        ListItem(
                            headlineContent = { Text(code.value) },
                            supportingContent = {
                                Text(
                                    code.displayName(Locale.getDefault()) +
                                        " · " + code.symbol,
                                )
                            },
                            trailingContent = {
                                if (code == selected) {
                                    Icon(imageVector = Icons.Outlined.Check, contentDescription = null)
                                }
                            },
                            modifier = Modifier.clickable { onSelect(code) },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_action)) }
        },
    )
}
