package io.github.flassuu.talex.feature.overview

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
import io.github.flassuu.talex.core.money.MoneyFormat
import io.github.flassuu.talex.core.money.convertMinor
import io.github.flassuu.talex.domain.model.Account
import io.github.flassuu.talex.domain.model.AccountType
import io.github.flassuu.talex.domain.model.Category
import io.github.flassuu.talex.domain.model.Entry
import io.github.flassuu.talex.domain.model.EntryType
import io.github.flassuu.talex.domain.model.Valuation
import io.github.flassuu.talex.domain.repository.AccountRepository
import io.github.flassuu.talex.domain.repository.CategoryRepository
import io.github.flassuu.talex.domain.repository.EntryRepository
import io.github.flassuu.talex.domain.repository.RateRepository
import io.github.flassuu.talex.domain.repository.SettingsRepository
import io.github.flassuu.talex.domain.repository.ValuationRepository
import io.github.flassuu.talex.ui.component.TalexScreen
import io.github.flassuu.talex.ui.component.categoryDisplayName
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class OverviewViewModel @Inject constructor(
    accountRepository: AccountRepository,
    entryRepository: EntryRepository,
    categoryRepository: CategoryRepository,
    settingsRepository: SettingsRepository,
    rateRepository: RateRepository,
    valuationRepository: ValuationRepository,
) : ViewModel() {

    data class CategoryTotal(val category: Category, val amount: Money)

    data class UiState(
        val baseCurrency: CurrencyCode = CurrencyCode.EUR,
        val netWorth: Money = Money.zero(CurrencyCode.EUR),
        val works: Money = Money.zero(CurrencyCode.EUR),
        val idle: Money = Money.zero(CurrencyCode.EUR),
        val lost: Money = Money.zero(CurrencyCode.EUR),
        val monthIncome: Money = Money.zero(CurrencyCode.EUR),
        val monthExpenses: Money = Money.zero(CurrencyCode.EUR),
        val savingsRate: Double? = null,
        val trend: List<Long> = emptyList(),
        val topCategories: List<CategoryTotal> = emptyList(),
    )

    private val accountData = combine(
        accountRepository.observeAccounts(),
        valuationRepository.observeAll(),
    ) { accounts, valuations -> accounts to valuations }

    val uiState: StateFlow<UiState> = combine(
        accountData,
        entryRepository.observeAllWithLines(),
        categoryRepository.observeCategories(),
        settingsRepository.observeBaseCurrency(),
        rateRepository.observeLatest(),
    ) { (accounts, valuations), entries, categories, base, rates ->
        computeCapital(
            accounts,
            entries,
            categories,
            valuations,
            base ?: CurrencyCode.EUR,
            rates,
            LocalDate.now(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    companion object {
        const val TREND_DAYS = 30
    }
}

internal fun computeCapital(
    accounts: List<Account>,
    entries: List<Entry>,
    categories: List<Category>,
    valuations: List<Valuation>,
    base: CurrencyCode,
    rates: Map<Pair<String, String>, String>,
    today: LocalDate,
): OverviewViewModel.UiState {
    val monthStart = today.withDayOfMonth(1)

    fun convert(money: Money): Long = convertMinor(money.amountMinor, money.currency, base, rates)

    // Assets are value-bearing, so valuation accounts join investment accounts in
    // "works" (ADR-0025). Liabilities stay out of the three metrics.
    val works = accounts
        .filter { it.type == AccountType.INVESTMENT || it.type.isValuationBased }
        .sumOf { convert(it.balance) }
    val idle = accounts
        .filter { it.type == AccountType.CHECKING || it.type == AccountType.SAVINGS || it.type == AccountType.CASH }
        .sumOf { convert(it.balance) }
    val netWorth = accounts.sumOf { convert(it.balance) }

    val monthEntries = entries.filter { !it.date.isBefore(monthStart) && !it.date.isAfter(today) }
    val categoryById = categories.associateBy { it.id }
    fun isUnnecessary(categoryId: String?): Boolean =
        categoryId != null && categoryById[categoryId]?.unnecessary == true

    // Lost = subscriptions (recurring) plus spending in unnecessary categories. A
    // recurring entry is counted once, not again for its category (ADR-0024).
    val lost = monthEntries
        .filter { it.type == EntryType.EXPENSE }
        .sumOf { entry ->
            if (entry.sourceRuleId != null) {
                -convert(entry.amount)
            } else {
                val shares = if (entry.lines.isNotEmpty()) {
                    entry.lines.map { it.categoryId to it.amount }
                } else {
                    listOfNotNull(entry.categoryId?.let { it to entry.amount })
                }
                shares.filter { (categoryId, _) -> isUnnecessary(categoryId) }
                    .sumOf { (_, amount) -> -convert(amount) }
            }
        }
    val income = monthEntries.filter { it.type == EntryType.INCOME }.sumOf { convert(it.amount) }
    val expenses = monthEntries.filter { it.type == EntryType.EXPENSE }.sumOf { -convert(it.amount) }
    val savingsRate = if (income > 0) (income - expenses).toDouble() / income else null

    // The trend is net worth: entry-based balances plus the latest valuation of
    // each asset on or before each day. Grouping first keeps it linear per day.
    val entriesByAccount = entries.groupBy { it.accountId }
    val valuationsByAccount = valuations.groupBy { it.accountId }
    val trend = (0 until OverviewViewModel.TREND_DAYS).map { offset ->
        val day = today.minusDays((OverviewViewModel.TREND_DAYS - 1 - offset).toLong())
        accounts.sumOf { account ->
            if (account.type.isValuationBased) {
                valuationsByAccount[account.id].orEmpty()
                    .filter { !it.date.isAfter(day) }
                    .maxByOrNull { it.date }
                    ?.let { convert(it.amount) }
                    ?: 0L
            } else {
                entriesByAccount[account.id].orEmpty()
                    .filter { !it.date.isAfter(day) }
                    .sumOf { convert(it.amount) }
            }
        }
    }

    val categoryTotals = mutableMapOf<String, Long>()
    monthEntries.filter { it.type == EntryType.EXPENSE }.forEach { entry ->
        val shares = if (entry.lines.isNotEmpty()) {
            entry.lines.map { it.categoryId to it.amount }
        } else {
            listOfNotNull(entry.categoryId?.let { it to entry.amount })
        }
        shares.forEach { (categoryId, amount) ->
            val category = categoryById[categoryId] ?: return@forEach
            val topId = category.parentId ?: category.id
            categoryTotals[topId] = (categoryTotals[topId] ?: 0L) + (-convert(amount))
        }
    }
    val topCategories = categoryTotals
        .mapNotNull { (id, total) ->
            categoryById[id]?.let { OverviewViewModel.CategoryTotal(it, Money(total, base)) }
        }
        .sortedByDescending { it.amount.amountMinor }
        .take(5)

    return OverviewViewModel.UiState(
        baseCurrency = base,
        netWorth = Money(netWorth, base),
        works = Money(works, base),
        idle = Money(idle, base),
        lost = Money(lost, base),
        monthIncome = Money(income, base),
        monthExpenses = Money(expenses, base),
        savingsRate = savingsRate,
        trend = trend,
        topCategories = topCategories,
    )
}

@Composable
fun OverviewScreen(viewModel: OverviewViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    TalexScreen(title = stringResource(R.string.overview_title)) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                MetricCard(
                    label = stringResource(R.string.net_worth),
                    money = state.netWorth,
                    hint = stringResource(R.string.net_worth_hint),
                )
            }
            item {
                MetricCard(
                    label = stringResource(R.string.capital_works),
                    money = state.works,
                    hint = stringResource(R.string.capital_works_hint),
                )
            }
            item {
                MetricCard(
                    label = stringResource(R.string.capital_idle),
                    money = state.idle,
                    hint = stringResource(R.string.capital_idle_hint),
                )
            }
            item {
                MetricCard(
                    label = stringResource(R.string.capital_lost),
                    money = state.lost,
                    hint = stringResource(R.string.capital_lost_hint),
                )
            }
            item {
                MetricCard(
                    label = stringResource(R.string.month_income),
                    money = state.monthIncome,
                    hint = stringResource(R.string.this_month),
                )
            }
            item {
                MetricCard(
                    label = stringResource(R.string.month_expenses),
                    money = state.monthExpenses,
                    hint = stringResource(R.string.this_month),
                )
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.savings_rate),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = state.savingsRate?.let { formatPercent(it) }
                                ?: stringResource(R.string.savings_rate_none),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.cash_trend),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(8.dp))
                        if (state.trend.any { it != 0L } && state.trend.size > 1) {
                            Sparkline(
                                values = state.trend,
                                modifier = Modifier.fillMaxWidth().height(80.dp),
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.cash_trend_empty),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.top_categories),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(8.dp))
                        if (state.topCategories.isEmpty()) {
                            Text(
                                text = stringResource(R.string.top_categories_empty),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            state.topCategories.forEach { total ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                ) {
                                    Text(
                                        text = categoryDisplayName(total.category),
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(text = MoneyFormat.format(total.amount))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, money: Money, hint: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = label, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(text = MoneyFormat.format(money), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Sparkline(values: List<Long>, modifier: Modifier) {
    val min = values.minOrNull() ?: 0L
    val max = values.maxOrNull() ?: 0L
    val range = (max - min).coerceAtLeast(1L)
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val stepX = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = index * stepX
            val y = size.height - ((value - min).toFloat() / range) * size.height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = color, style = Stroke(width = 4f))
    }
}

private fun formatPercent(value: Double): String =
    String.format(Locale.getDefault(), "%.0f%%", value * 100)
