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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlin.math.abs
import javax.inject.Inject

enum class TransactionSort {
    DATE_DESC,
    DATE_ASC,
    AMOUNT_DESC,
    AMOUNT_ASC,
    TYPE,
    ACCOUNT,
    CATEGORY,
}

@HiltViewModel
class EntriesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    entryRepository: EntryRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    private val categoryFilter = MutableStateFlow(savedStateHandle.get<String>("categoryId"))
    private val sort = MutableStateFlow(TransactionSort.DATE_DESC)

    data class UiState(
        val entries: List<Entry> = emptyList(),
        val accounts: Map<String, Account> = emptyMap(),
        val categories: Map<String, Category> = emptyMap(),
        val sort: TransactionSort = TransactionSort.DATE_DESC,
        val filterCategory: Category? = null,
    )

    val uiState: StateFlow<UiState> = combine(
        entryRepository.observeAllWithLines(),
        accountRepository.observeAccounts(),
        categoryRepository.observeCategories(),
        sort,
        categoryFilter,
    ) { entries, accounts, categories, currentSort, filterId ->
        val accountsById = accounts.associateBy { it.id }
        val categoryById = categories.associateBy { it.id }
        val filterIds = filterId?.let { id ->
            setOf(id) + categories.filter { it.parentId == id }.map { it.id }
        }
        val filtered = if (filterIds == null) {
            entries
        } else {
            entries.filter { entry ->
                (entry.categoryId != null && entry.categoryId in filterIds) ||
                    entry.lines.any { it.categoryId in filterIds }
            }
        }
        val sorted = when (currentSort) {
            TransactionSort.DATE_DESC -> filtered.sortedWith(
                compareByDescending<Entry> { it.date }.thenByDescending { it.id },
            )

            TransactionSort.DATE_ASC -> filtered.sortedWith(
                compareBy<Entry> { it.date }.thenBy { it.id },
            )

            TransactionSort.AMOUNT_DESC -> filtered.sortedByDescending { abs(it.amount.amountMinor) }
            TransactionSort.AMOUNT_ASC -> filtered.sortedBy { abs(it.amount.amountMinor) }
            TransactionSort.TYPE -> filtered.sortedBy { it.type.name }
            TransactionSort.ACCOUNT -> filtered.sortedBy { accountsById[it.accountId]?.name ?: "" }
            TransactionSort.CATEGORY -> filtered.sortedBy { categoryName(it, categoryById) }
        }
        UiState(
            entries = sorted,
            accounts = accountsById,
            categories = categoryById,
            sort = currentSort,
            filterCategory = filterId?.let { categoryById[it] },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun setSort(value: TransactionSort) {
        sort.value = value
    }

    fun clearFilter() {
        categoryFilter.value = null
    }

    private fun categoryName(entry: Entry, categoryById: Map<String, Category>): String {
        val id = entry.categoryId ?: entry.lines.firstOrNull()?.categoryId
        return id?.let { categoryById[it]?.name } ?: ""
    }
}

@Composable
fun TransactionsScreen(
    onOpenEntry: (String) -> Unit,
    onNewTransfer: () -> Unit,
    onOpenTransfer: (String) -> Unit,
    viewModel: EntriesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var sortMenuOpen by remember { mutableStateOf(false) }

    TalexScreen(
        title = stringResource(R.string.transactions_title),
        actions = {
            IconButton(onClick = { sortMenuOpen = true }) {
                Icon(
                    imageVector = Icons.Outlined.Sort,
                    contentDescription = stringResource(R.string.sort_action),
                )
            }
            DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                TransactionSort.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(sortLabel(option)) },
                        onClick = {
                            viewModel.setSort(option)
                            sortMenuOpen = false
                        },
                    )
                }
            }
            IconButton(onClick = onNewTransfer) {
                Icon(
                    imageVector = Icons.Outlined.SwapHoriz,
                    contentDescription = stringResource(R.string.add_transfer_action),
                )
            }
        },
    ) { padding ->
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
                state.filterCategory?.let { category ->
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AssistChip(
                                onClick = {},
                                label = {
                                    Text(
                                        stringResource(R.string.filtered_by) + ": " +
                                            categoryDisplayName(category),
                                    )
                                },
                            )
                            TextButton(onClick = viewModel::clearFilter) {
                                Text(stringResource(R.string.clear_filter))
                            }
                        }
                    }
                }
                items(state.entries, key = { it.id }) { entry ->
                    val onClick: (() -> Unit)? = when (entry.type) {
                        EntryType.OPENING, EntryType.ADJUSTMENT -> null
                        EntryType.TRANSFER -> entry.transferGroupId?.let { groupId ->
                            { onOpenTransfer(groupId) }
                        }

                        else -> ({ onOpenEntry(entry.id) })
                    }
                    TransactionRow(
                        entry = entry,
                        accountName = state.accounts[entry.accountId]?.name,
                        category = entry.categoryId?.let { state.categories[it] },
                        onClick = onClick,
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
    val description = when (entry.type) {
        EntryType.OPENING -> stringResource(R.string.entry_opening_description)
        EntryType.ADJUSTMENT -> stringResource(R.string.entry_adjustment_description)
        else -> entry.description.ifBlank { stringResource(R.string.no_description) }
    }
    Card(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = description, style = MaterialTheme.typography.bodyLarge)
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

@Composable
private fun sortLabel(sort: TransactionSort): String = stringResource(
    when (sort) {
        TransactionSort.DATE_DESC -> R.string.sort_date_desc
        TransactionSort.DATE_ASC -> R.string.sort_date_asc
        TransactionSort.AMOUNT_DESC -> R.string.sort_amount_desc
        TransactionSort.AMOUNT_ASC -> R.string.sort_amount_asc
        TransactionSort.TYPE -> R.string.sort_type
        TransactionSort.ACCOUNT -> R.string.sort_account
        TransactionSort.CATEGORY -> R.string.sort_category
    },
)
