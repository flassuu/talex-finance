package io.github.flassuu.talex.feature.settings

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import io.github.flassuu.talex.ui.component.CurrencySelector
import io.github.flassuu.talex.ui.component.TalexScreen
import io.github.flassuu.talex.ui.component.categoryDisplayName
import io.github.flassuu.talex.ui.component.errorMessage
import io.github.flassuu.talex.ui.component.formatMoney

@Composable
fun ChangeBaseCurrencyScreen(
    onBack: () -> Unit,
    viewModel: ChangeBaseCurrencyViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val hostState = remember { SnackbarHostState() }
    val messageText = state.message?.let { message ->
        message.res?.let { stringResource(it) } ?: message.error?.let { errorMessage(it) }
    }
    LaunchedEffect(state.message?.id) {
        messageText?.let {
            hostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    TalexScreen(
        title = stringResource(R.string.base_change_title),
        snackbarHost = { SnackbarHost(hostState) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.back_action),
                )
            }
        },
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
            Text(
                text = stringResource(R.string.base_change_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CurrencySelector(
                selected = state.newBase,
                onSelect = viewModel::onNewBase,
                label = stringResource(R.string.base_change_new_label),
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.missingBudgetCurrencies.isNotEmpty()) {
                Text(
                    text = stringResource(
                        R.string.base_change_missing_budgets,
                        state.missingBudgetCurrencies.joinToString(", ") { it.value },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (state.usedWithoutRate.isNotEmpty()) {
                Text(
                    text = stringResource(
                        R.string.base_change_missing_used,
                        state.usedWithoutRate.joinToString(", ") { it.value },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (state.budgets.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.base_change_budgets_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(8.dp))
                        val categoryById = state.categories.associateBy { it.id }
                        state.budgets.forEach { preview ->
                            val category = categoryById[preview.categoryId]
                            val name = category?.let { categoryDisplayName(it) }
                                ?: preview.categoryId
                            val to = preview.to
                            val value = if (to == null) {
                                stringResource(R.string.base_change_budget_missing)
                            } else {
                                "${formatMoney(preview.from)}  →  ${formatMoney(to)}"
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(text = name, modifier = Modifier.weight(1f))
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (to == null) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = viewModel::apply,
                enabled = state.canApply,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.base_change_apply))
            }
        }
    }
}
