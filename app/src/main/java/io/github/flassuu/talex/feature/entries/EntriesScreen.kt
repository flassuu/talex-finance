package io.github.flassuu.talex.feature.entries

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.flassuu.talex.R
import io.github.flassuu.talex.core.money.MoneyFormat
import io.github.flassuu.talex.domain.model.Account
import io.github.flassuu.talex.domain.model.Category
import io.github.flassuu.talex.domain.model.Entry
import io.github.flassuu.talex.domain.model.EntryType
import io.github.flassuu.talex.domain.repository.AccountRepository
import io.github.flassuu.talex.domain.repository.CategoryRepository
import io.github.flassuu.talex.domain.repository.EntryRepository
import io.github.flassuu.talex.ui.component.TalexScreen
import io.github.flassuu.talex.ui.component.categoryDisplayName
import io.github.flassuu.talex.ui.component.entryTypeLabel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class EntriesViewModel @Inject constructor(
    entryRepository: EntryRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    data class UiState(
        val entries: List<Entry> = emptyList(),
        val accounts: Map<String, Account> = emptyMap(),
        val categories: Map<String, Category> = emptyMap(),
    )

    val uiState: StateFlow<UiState> = combine(
        entryRepository.observeAll(),
        accountRepository.observeAccounts(),
        categoryRepository.observeCategories(),
    ) { entries, accounts, categories ->
        UiState(
            entries = entries,
            accounts = accounts.associateBy { it.id },
            categories = categories.associateBy { it.id },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())
}

@Composable
fun TransactionsScreen(
    onOpenEntry: (String) -> Unit,
    viewModel: EntriesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    TalexScreen(title = stringResource(R.string.transactions_title)) { padding ->
        if (state.entries.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.transactions_empty),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.transactions_empty_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.entries, key = { it.id }) { entry ->
                    TransactionRow(
                        entry = entry,
                        accountName = state.accounts[entry.accountId]?.name,
                        category = entry.categoryId?.let { state.categories[it] },
                        onClick = if (entry.type == EntryType.OPENING) null else ({ onOpenEntry(entry.id) }),
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(
    entry: Entry,
    accountName: String?,
    category: Category?,
    onClick: (() -> Unit)?,
) {
    val modifier = if (onClick != null) Modifier.fillMaxWidth().clickable(onClick = onClick) else Modifier.fillMaxWidth()
    val typeLabel = entryTypeLabel(entry.type)
    val categoryLabel = category?.let { categoryDisplayName(it) }
    Card(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (entry.type == EntryType.OPENING) {
                        stringResource(R.string.entry_opening_description)
                    } else {
                        entry.description
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = buildString {
                        append(entry.date.toString())
                        append(" · ")
                        append(typeLabel)
                        accountName?.let { append(" · ").append(it) }
                        categoryLabel?.let { append(" · ").append(it) }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = MoneyFormat.format(entry.amount),
                style = MaterialTheme.typography.titleMedium,
                color = when {
                    entry.type == EntryType.INCOME -> MaterialTheme.colorScheme.primary
                    entry.type == EntryType.EXPENSE -> MaterialTheme.colorScheme.error
                    entry.amount.isNegative -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}
