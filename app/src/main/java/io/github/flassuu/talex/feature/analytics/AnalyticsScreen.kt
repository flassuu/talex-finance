package io.github.flassuu.talex.feature.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.flassuu.talex.R
import io.github.flassuu.talex.core.money.Money
import io.github.flassuu.talex.core.money.MoneyFormat
import io.github.flassuu.talex.ui.component.CategoryShareBar
import io.github.flassuu.talex.ui.component.GroupedBarChart
import io.github.flassuu.talex.ui.component.LineChart
import io.github.flassuu.talex.ui.component.PeriodSelector
import io.github.flassuu.talex.ui.component.SignedBarChart
import io.github.flassuu.talex.ui.component.TalexScreen
import io.github.flassuu.talex.ui.component.categoryDisplayName

@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    TalexScreen(title = stringResource(R.string.analytics_title)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PeriodSelector(period = state.period, onPeriodChange = viewModel::setPeriod)

            val data = state.data
            if (data == null) return@Column

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryTile(
                    label = stringResource(R.string.analytics_income),
                    money = data.totalIncome,
                    modifier = Modifier.weight(1f),
                )
                SummaryTile(
                    label = stringResource(R.string.analytics_expenses),
                    money = data.totalExpenses,
                    modifier = Modifier.weight(1f),
                )
            }
            SummaryTile(
                label = stringResource(R.string.analytics_cash_flow_total),
                money = data.totalCashFlow,
                modifier = Modifier.fillMaxWidth(),
            )

            if (data.incomplete) {
                Text(
                    text = stringResource(R.string.analytics_incomplete),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            ChartCard(title = stringResource(R.string.analytics_income_expenses)) {
                Legend()
                Spacer(Modifier.height(8.dp))
                GroupedBarChart(
                    values = data.points.map { it.income.amountMinor to it.expenses.amountMinor },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                )
                Spacer(Modifier.height(4.dp))
                BucketLabels(data.points.map { it.label })
            }

            ChartCard(title = stringResource(R.string.analytics_cash_flow)) {
                SignedBarChart(
                    values = data.points.map { it.cashFlow.amountMinor },
                    modifier = Modifier.fillMaxWidth().height(110.dp),
                )
                Spacer(Modifier.height(4.dp))
                BucketLabels(data.points.map { it.label })
            }

            ChartCard(title = stringResource(R.string.analytics_categories)) {
                if (data.categories.isEmpty()) {
                    EmptyHint(stringResource(R.string.analytics_categories_empty))
                } else {
                    val max = data.categories.maxOf { it.amount.amountMinor }.coerceAtLeast(1L)
                    data.categories.take(MAX_CATEGORIES).forEach { slice ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = categoryDisplayName(slice.category),
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(text = MoneyFormat.format(slice.amount))
                        }
                        CategoryShareBar(
                            fraction = slice.amount.amountMinor.toFloat() / max,
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        )
                    }
                }
            }

            ChartCard(title = stringResource(R.string.analytics_net_worth)) {
                if (data.netWorth.all { it.isZero }) {
                    EmptyHint(stringResource(R.string.analytics_net_worth_empty))
                } else {
                    Text(
                        text = MoneyFormat.format(data.netWorth.last()),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    LineChart(
                        values = data.netWorth.map { it.amountMinor },
                        modifier = Modifier.fillMaxWidth().height(110.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryTile(label: String, money: Money, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(text = MoneyFormat.format(money), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ChartCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LegendDot(label = stringResource(R.string.analytics_income), color = MaterialTheme.colorScheme.primary)
        LegendDot(label = stringResource(R.string.analytics_expenses), color = MaterialTheme.colorScheme.tertiary)
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

/** Shows up to a handful of evenly spaced bucket labels under a chart. */
@Composable
private fun BucketLabels(labels: List<String>) {
    if (labels.isEmpty()) return
    val maxLabels = 5
    val step = ((labels.size - 1).coerceAtLeast(1)) / maxLabels + 1
    val shown = labels.filterIndexed { index, _ -> index % step == 0 || index == labels.lastIndex }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        shown.forEach {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private const val MAX_CATEGORIES = 10
