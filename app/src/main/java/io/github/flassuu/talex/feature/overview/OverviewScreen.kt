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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import io.github.flassuu.talex.core.money.convertMinor
import io.github.flassuu.talex.domain.insights.Advice
import io.github.flassuu.talex.domain.insights.AdviceKind
import io.github.flassuu.talex.domain.insights.Alert
import io.github.flassuu.talex.domain.insights.AlertKind
import io.github.flassuu.talex.domain.insights.InsightSettings
import io.github.flassuu.talex.domain.insights.computeInsights
import io.github.flassuu.talex.domain.model.Account
import io.github.flassuu.talex.domain.model.Budget
import io.github.flassuu.talex.domain.model.AccountType
import io.github.flassuu.talex.domain.model.CapitalRole
import io.github.flassuu.talex.domain.model.Category
import io.github.flassuu.talex.domain.model.Entry
import io.github.flassuu.talex.domain.model.EntryType
import io.github.flassuu.talex.domain.model.RiskTier
import io.github.flassuu.talex.domain.model.Valuation
import io.github.flassuu.talex.domain.ledger.refundFactor
import io.github.flassuu.talex.domain.ledger.refundsByOriginal
import io.github.flassuu.talex.domain.repository.AccountRepository
import io.github.flassuu.talex.domain.repository.BudgetRepository
import io.github.flassuu.talex.domain.repository.CategoryRepository
import io.github.flassuu.talex.domain.repository.EntryRepository
import io.github.flassuu.talex.domain.repository.RateRepository
import io.github.flassuu.talex.domain.repository.SettingsRepository
import io.github.flassuu.talex.domain.repository.ValuationRepository
import io.github.flassuu.talex.feature.settings.PrivacyViewModel
import io.github.flassuu.talex.ui.component.TalexScreen
import io.github.flassuu.talex.ui.component.capitalRoleLabel
import io.github.flassuu.talex.ui.component.categoryDisplayName
import io.github.flassuu.talex.ui.component.formatMoney
import io.github.flassuu.talex.ui.component.riskTierLabel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToLong

@HiltViewModel
class OverviewViewModel @Inject constructor(
    accountRepository: AccountRepository,
    entryRepository: EntryRepository,
    categoryRepository: CategoryRepository,
    settingsRepository: SettingsRepository,
    rateRepository: RateRepository,
    valuationRepository: ValuationRepository,
    budgetRepository: BudgetRepository,
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
        val roleTotals: List<Pair<CapitalRole, Money>> = emptyList(),
        val riskTotals: List<Pair<RiskTier, Money>> = emptyList(),
        /** Share of medium and high risk in capital; drives the 80/20 alert (ADR-0035). */
        val riskyShare: Double? = null,
        /** Active alerts, with the risky-allocation one muted (shown in the risk card). */
        val alerts: List<Alert> = emptyList(),
        val advice: List<Advice> = emptyList(),
    )

    private data class Extras(
        val base: CurrencyCode?,
        val rates: Map<Pair<String, String>, String>,
        val insightSettings: InsightSettings,
        val budgets: List<Budget>,
    )

    private val accountData = combine(
        accountRepository.observeAccounts(),
        valuationRepository.observeAll(),
    ) { accounts, valuations -> accounts to valuations }

    private val extras = combine(
        settingsRepository.observeBaseCurrency(),
        rateRepository.observeLatest(),
        settingsRepository.observeInsights(),
        budgetRepository.observeBudgets(),
    ) { base, rates, insightSettings, budgets ->
        Extras(base, rates, insightSettings, budgets)
    }

    val uiState: StateFlow<UiState> = combine(
        accountData,
        entryRepository.observeAllWithLines(),
        categoryRepository.observeCategories(),
        extras,
    ) { (accounts, valuations), entries, categories, extra ->
        computeCapital(
            accounts,
            entries,
            categories,
            valuations,
            extra.base ?: CurrencyCode.EUR,
            extra.rates,
            LocalDate.now(),
            extra.budgets,
            extra.insightSettings,
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
    budgets: List<Budget> = emptyList(),
    insightSettings: InsightSettings = InsightSettings(),
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
    // Refunds reduce the original expense instead of counting as income (Q-042).
    val refunds = refundsByOriginal(entries)
    fun isUnnecessary(categoryId: String?): Boolean =
        categoryId != null && categoryById[categoryId]?.unnecessary == true

    // Lost = subscriptions (recurring) plus spending in unnecessary categories. A
    // recurring entry is counted once, not again for its category (ADR-0024).
    val lost = monthEntries
        .filter { it.type == EntryType.EXPENSE }
        .sumOf { entry ->
            val factor = refundFactor(entry, refunds)
            if (factor <= 0.0) {
                0L
            } else if (entry.sourceRuleId != null) {
                (-convert(entry.amount) * factor).roundToLong()
            } else {
                val shares = if (entry.lines.isNotEmpty()) {
                    entry.lines.map { it.categoryId to it.amount }
                } else {
                    listOfNotNull(entry.categoryId?.let { it to entry.amount })
                }
                (shares.filter { (categoryId, _) -> isUnnecessary(categoryId) }
                    .sumOf { (_, amount) -> -convert(amount) } * factor).roundToLong()
            }
        }
    val income = monthEntries.filter { it.type == EntryType.INCOME }.sumOf { convert(it.amount) }
    val expenses = monthEntries.filter { it.type == EntryType.EXPENSE }
        .sumOf { entry -> (-convert(entry.amount) * refundFactor(entry, refunds)).roundToLong() }
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
        val factor = refundFactor(entry, refunds)
        if (factor <= 0.0) return@forEach
        val shares = if (entry.lines.isNotEmpty()) {
            entry.lines.map { it.categoryId to it.amount }
        } else {
            listOfNotNull(entry.categoryId?.let { it to entry.amount })
        }
        shares.forEach { (categoryId, amount) ->
            val category = categoryById[categoryId] ?: return@forEach
            val topId = category.parentId ?: category.id
            val share = (-convert(amount) * factor).roundToLong()
            categoryTotals[topId] = (categoryTotals[topId] ?: 0L) + share
        }
    }
    val topCategories = categoryTotals
        .mapNotNull { (id, total) ->
            categoryById[id]?.let { OverviewViewModel.CategoryTotal(it, Money(total, base)) }
        }
        .sortedByDescending { it.amount.amountMinor }
        .take(5)

    // Roles and risk are a second axis; the three buckets are always present so
    // the blocks read as a fixed scale (ADR-0035).
    val roleTotals = CapitalRole.entries.map { role ->
        role to Money(
            accounts
                .filter { it.type.isValuationBased && (it.role ?: CapitalRole.NEUTRAL) == role }
                .sumOf { convert(it.balance) },
            base,
        )
    }
    val capitalAccounts = accounts.filter {
        it.type == AccountType.INVESTMENT ||
            it.type.isValuationBased ||
            it.type == AccountType.CHECKING ||
            it.type == AccountType.SAVINGS ||
            it.type == AccountType.CASH
    }
    val riskTotals = RiskTier.entries.map { tier ->
        tier to Money(capitalAccounts.filter { riskOf(it) == tier }.sumOf { convert(it.balance) }, base)
    }
    val riskCapital = riskTotals.sumOf { it.second.amountMinor }
    val riskyShare = if (riskCapital > 0) {
        riskTotals.filter { it.first != RiskTier.LOW }.sumOf { it.second.amountMinor }
            .toDouble() / riskCapital
    } else {
        null
    }

    val insights = computeInsights(
        accounts = accounts,
        entries = entries,
        categories = categories,
        budgets = budgets,
        valuations = valuations,
        base = base,
        rates = rates,
        today = today,
        settings = insightSettings,
    )

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
        roleTotals = roleTotals,
        riskTotals = riskTotals,
        riskyShare = riskyShare,
        alerts = insights.alerts.filter { it.kind != AlertKind.RISKY_ALLOCATION },
        advice = insights.advice,
    )
}

/** Cash is always low risk; invested ledger accounts default to medium (ADR-0035). */
private fun riskOf(account: Account): RiskTier = when {
    account.type.isValuationBased -> account.risk
    account.type == AccountType.INVESTMENT -> RiskTier.MEDIUM
    else -> RiskTier.LOW
}

@Composable
fun OverviewScreen(
    viewModel: OverviewViewModel = hiltViewModel(),
    privacy: PrivacyViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val amountsHidden by privacy.hidden.collectAsStateWithLifecycle()

    TalexScreen(
        title = stringResource(R.string.overview_title),
        actions = {
            IconButton(onClick = privacy::toggle) {
                Icon(
                    imageVector = if (amountsHidden) Icons.Outlined.VisibilityOff
                    else Icons.Outlined.Visibility,
                    contentDescription = stringResource(
                        if (amountsHidden) R.string.overview_show_amounts
                        else R.string.overview_hide_amounts,
                    ),
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.alerts.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.alerts_title),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(Modifier.height(8.dp))
                            state.alerts.forEach { alert ->
                                Text(
                                    text = alertText(alert),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(vertical = 2.dp),
                                )
                            }
                        }
                    }
                }
            }
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
                BreakdownCard(
                    title = stringResource(R.string.capital_by_role),
                    rows = state.roleTotals.map { (role, money) ->
                        capitalRoleLabel(role) to money
                    },
                    hint = stringResource(R.string.capital_profile_hint),
                )
            }
            item {
                BreakdownCard(
                    title = stringResource(R.string.capital_by_risk),
                    rows = state.riskTotals.map { (risk, money) ->
                        riskTierLabel(risk) to money
                    },
                    hint = state.riskyShare?.let {
                        stringResource(
                            R.string.risky_share,
                            String.format(Locale.getDefault(), "%.0f%%", it * 100),
                        )
                    } ?: stringResource(R.string.risky_share_none),
                )
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.advice_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(8.dp))
                        if (state.advice.isEmpty()) {
                            Text(
                                text = stringResource(R.string.advice_good),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            state.advice.forEach { advice ->
                                Text(
                                    text = adviceText(advice),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = when (advice.tone) {
                                        io.github.flassuu.talex.domain.insights.AdviceTone.POSITIVE ->
                                            MaterialTheme.colorScheme.primary
                                        io.github.flassuu.talex.domain.insights.AdviceTone.WARNING ->
                                            MaterialTheme.colorScheme.error
                                        io.github.flassuu.talex.domain.insights.AdviceTone.NEUTRAL ->
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(vertical = 2.dp),
                                )
                            }
                        }
                    }
                }
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
                                    Text(text = formatMoney(total.amount))
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
private fun alertText(alert: Alert): String = when (alert.kind) {
    AlertKind.NEGATIVE_CASH_FLOW -> stringResource(R.string.alert_negative_cash_flow, alert.months)
    AlertKind.NET_WORTH_DECLINE -> stringResource(R.string.alert_net_worth_decline, alert.months)
    AlertKind.BUDGET_EXCEEDED -> stringResource(R.string.alert_budget_exceeded, alert.budgetCount)
    AlertKind.IDLE_CASH -> stringResource(R.string.alert_idle_cash, alert.accountName.orEmpty())
    AlertKind.RISKY_ALLOCATION -> stringResource(R.string.risky_share, formatPercent(alert.share))
}

@Composable
private fun adviceText(advice: Advice): String = when (advice.kind) {
    AdviceKind.RISING_CATEGORY -> stringResource(
        R.string.advice_rising_category,
        advice.categoryName.orEmpty(),
        formatPercent(advice.percent),
    )

    AdviceKind.UNBUDGETED_CATEGORY -> stringResource(
        R.string.advice_unbudgeted_category,
        advice.categoryName.orEmpty(),
        advice.amount?.let { formatMoney(it) }.orEmpty(),
    )

    AdviceKind.SAVINGS_RATE_TREND -> stringResource(
        R.string.advice_savings_rate,
        String.format(Locale.getDefault(), "%.1f", advice.points),
    )
}

@Composable
private fun BreakdownCard(title: String, rows: List<Pair<String, Money>>, hint: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            rows.forEach { (label, money) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(text = label, modifier = Modifier.weight(1f))
                    Text(text = formatMoney(money))
                }
            }
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
private fun MetricCard(label: String, money: Money, hint: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = label, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(text = formatMoney(money), style = MaterialTheme.typography.headlineSmall)
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
