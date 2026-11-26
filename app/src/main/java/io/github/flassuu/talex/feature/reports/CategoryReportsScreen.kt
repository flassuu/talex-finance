package io.github.flassuu.talex.feature.reports

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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.flassuu.talex.R
import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.money.Money
import io.github.flassuu.talex.domain.ledger.refundFactor
import io.github.flassuu.talex.domain.ledger.refundsByOriginal
import io.github.flassuu.talex.domain.model.Category
import io.github.flassuu.talex.domain.model.CategoryKind
import io.github.flassuu.talex.domain.model.Entry
import io.github.flassuu.talex.domain.model.EntryType
import io.github.flassuu.talex.domain.model.Rate
import io.github.flassuu.talex.domain.repository.CategoryRepository
import io.github.flassuu.talex.domain.repository.EntryRepository
import io.github.flassuu.talex.domain.repository.RateRepository
import io.github.flassuu.talex.domain.repository.SettingsRepository
import io.github.flassuu.talex.feature.analytics.AnalyticsPeriod
import io.github.flassuu.talex.feature.analytics.convertAtDate
import io.github.flassuu.talex.ui.component.PeriodSelector
import io.github.flassuu.talex.ui.component.TalexScreen
import io.github.flassuu.talex.ui.component.categoryDisplayName
import io.github.flassuu.talex.ui.component.formatMoney
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.YearMonth
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToLong

data class CategoryRankItem(val category: Category, val amount: Money, val count: Int)

data class CategoryMonthPoint(val month: YearMonth, val amount: Money)

data class CategoryDetail(
    val category: Category,
    val total: Money,
    val count: Int,
    val series: List<CategoryMonthPoint>,
)

@HiltViewModel
class CategoryReportsViewModel @Inject constructor(
    entryRepository: EntryRepository,
    categoryRepository: CategoryRepository,
    settingsRepository: SettingsRepository,
    rateRepository: RateRepository,
) : ViewModel() {

    data class UiState(
        val period: AnalyticsPeriod = AnalyticsPeriod.current(),
        val kind: CategoryKind = CategoryKind.EXPENSE,
        val ranking: List<CategoryRankItem> = emptyList(),
        val detail: CategoryDetail? = null,
        val base: CurrencyCode = CurrencyCode.EUR,
    )

    private data class Local(
        val period: AnalyticsPeriod = AnalyticsPeriod.current(),
        val kind: CategoryKind = CategoryKind.EXPENSE,
        val selectedId: String? = null,
    )

    private val local = MutableStateFlow(Local())

    private val ledgerData = combine(
        entryRepository.observeAllWithLines(),
        categoryRepository.observeCategories(),
    ) { entries, categories -> entries to categories }

    private val baseData = combine(
        settingsRepository.observeBaseCurrency(),
        rateRepository.observeAll(),
    ) { base, rates -> (base ?: CurrencyCode.EUR) to rates }

    val uiState: StateFlow<UiState> = combine(
        local,
        ledgerData,
        baseData,
    ) { state, (entries, categories), (base, rates) ->
        val refunds = refundsByOriginal(entries)
        val ranking = computeRanking(entries, categories, rates, base, state.period, state.kind, refunds)
        val selected = state.selectedId?.let { id -> categories.firstOrNull { it.id == id } }
        UiState(
            period = state.period,
            kind = state.kind,
            ranking = ranking,
            detail = selected?.let {
                computeDetail(it, entries, categories, rates, base, state.period, state.kind, refunds)
            },
            base = base,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun setPeriod(period: AnalyticsPeriod) = local.update { it.copy(period = period) }

    fun setKind(kind: CategoryKind) = local.update {
        it.copy(kind = kind, selectedId = null)
    }

    fun select(categoryId: String) = local.update { it.copy(selectedId = categoryId) }

    fun clearSelection() = local.update { it.copy(selectedId = null) }
}

private fun entryTypeOf(kind: CategoryKind): EntryType =
    if (kind == CategoryKind.INCOME) EntryType.INCOME else EntryType.EXPENSE

private fun sharesOf(entry: Entry): List<Pair<String, Money>> =
    if (entry.lines.isNotEmpty()) {
        entry.lines.map { it.categoryId to it.amount }
    } else {
        listOfNotNull(entry.categoryId?.let { it to entry.amount })
    }

private fun computeRanking(
    entries: List<Entry>,
    categories: List<Category>,
    rates: List<Rate>,
    base: CurrencyCode,
    period: AnalyticsPeriod,
    kind: CategoryKind,
    refunds: Map<String, Long>,
): List<CategoryRankItem> {
    val categoryById = categories.associateBy { it.id }
    val type = entryTypeOf(kind)
    val totals = mutableMapOf<String, Long>()
    val counts = mutableMapOf<String, MutableSet<String>>()
    entries.forEach { entry ->
        if (entry.deletedAt != null || entry.type != type) return@forEach
        if (entry.date.isBefore(period.startDate) || entry.date.isAfter(period.endDate)) return@forEach
        val factor = if (kind == CategoryKind.EXPENSE) refundFactor(entry, refunds) else 1.0
        if (factor <= 0.0) return@forEach
        sharesOf(entry).forEach { (categoryId, amount) ->
            val category = categoryById[categoryId] ?: return@forEach
            val topId = category.parentId ?: category.id
            val converted = convertAtDate(
                amount.amountMinor,
                amount.currency,
                base,
                rates,
                entry.date,
            ) ?: return@forEach
            val signed = if (kind == CategoryKind.EXPENSE) -converted else converted
            totals[topId] = (totals[topId] ?: 0L) + (signed * factor).roundToLong()
            counts.getOrPut(topId) { mutableSetOf() }.add(entry.id)
        }
    }
    return totals.mapNotNull { (id, amount) ->
        categoryById[id]?.let { CategoryRankItem(it, Money(amount, base), counts[id]?.size ?: 0) }
    }.sortedByDescending { abs(it.amount.amountMinor) }
}

private fun computeDetail(
    category: Category,
    entries: List<Entry>,
    categories: List<Category>,
    rates: List<Rate>,
    base: CurrencyCode,
    period: AnalyticsPeriod,
    kind: CategoryKind,
    refunds: Map<String, Long>,
): CategoryDetail {
    val ids = (categories.filter { it.parentId == category.id }.map { it.id } + category.id).toSet()
    val type = entryTypeOf(kind)
    val byMonth = mutableMapOf<YearMonth, Long>()
    val entryCount = mutableSetOf<String>()
    entries.forEach { entry ->
        if (entry.deletedAt != null || entry.type != type) return@forEach
        if (entry.date.isBefore(period.startDate) || entry.date.isAfter(period.endDate)) return@forEach
        val factor = if (kind == CategoryKind.EXPENSE) refundFactor(entry, refunds) else 1.0
        if (factor <= 0.0) return@forEach
        sharesOf(entry).forEach { (categoryId, amount) ->
            if (categoryId !in ids) return@forEach
            val converted = convertAtDate(
                amount.amountMinor,
                amount.currency,
                base,
                rates,
                entry.date,
            ) ?: return@forEach
            val signed = if (kind == CategoryKind.EXPENSE) -converted else converted
            val month = YearMonth.from(entry.date)
            byMonth[month] = (byMonth[month] ?: 0L) + (signed * factor).roundToLong()
            entryCount.add(entry.id)
        }
    }
    val months = runCatching {
        generateSequence(YearMonth.from(period.startDate)) { it.plusMonths(1) }
            .takeWhile { !it.isAfter(YearMonth.from(period.endDate)) }
            .toList()
    }.getOrDefault(emptyList())
    val series = months.map { month ->
        CategoryMonthPoint(month, Money(byMonth[month] ?: 0L, base))
    }
    val total = series.fold(0L) { acc, point -> acc + point.amount.amountMinor }
    return CategoryDetail(
        category = category,
        total = Money(total, base),
        count = entryCount.size,
        series = series,
    )
}

@Composable
fun CategoryReportsScreen(viewModel: CategoryReportsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val detail = state.detail

    TalexScreen(
        title = stringResource(R.string.reports_title),
        navigationIcon = if (detail != null) {
            {
                IconButton(onClick = viewModel::clearSelection) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.reports_back),
                    )
                }
            }
        } else {
            null
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            PeriodSelector(
                period = state.period,
                onPeriodChange = viewModel::setPeriod,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.kind == CategoryKind.EXPENSE,
                    onClick = { viewModel.setKind(CategoryKind.EXPENSE) },
                    label = { Text(stringResource(R.string.reports_expenses)) },
                )
                FilterChip(
                    selected = state.kind == CategoryKind.INCOME,
                    onClick = { viewModel.setKind(CategoryKind.INCOME) },
                    label = { Text(stringResource(R.string.reports_income)) },
                )
            }

            if (detail != null) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = categoryDisplayName(detail.category),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = formatMoney(detail.total),
                                    style = MaterialTheme.typography.headlineSmall,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.reports_transactions_count, detail.count),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    item {
                        Text(
                            text = stringResource(R.string.reports_month_series),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    items(detail.series) { point ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            Text(
                                text = point.month.toString(),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = formatMoney(point.amount),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (state.ranking.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.reports_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 24.dp),
                            )
                        }
                    } else {
                        items(state.ranking, key = { it.category.id }) { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.select(item.category.id) },
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = categoryDisplayName(item.category),
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                        Text(
                                            text = stringResource(
                                                R.string.reports_transactions_count,
                                                item.count,
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Text(
                                        text = formatMoney(item.amount),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
