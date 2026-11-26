package io.github.flassuu.talex.domain.insights

import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.money.Money
import io.github.flassuu.talex.core.money.convertMinor
import io.github.flassuu.talex.domain.ledger.refundFactor
import io.github.flassuu.talex.domain.ledger.refundsByOriginal
import io.github.flassuu.talex.domain.model.Account
import io.github.flassuu.talex.domain.model.AccountType
import io.github.flassuu.talex.domain.model.Budget
import io.github.flassuu.talex.domain.model.Category
import io.github.flassuu.talex.domain.model.Entry
import io.github.flassuu.talex.domain.model.EntryType
import io.github.flassuu.talex.domain.model.Valuation
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToLong

/** Configurable thresholds and per-signal switches for local insights (ADR-0036). */
data class InsightSettings(
    val alertsEnabled: Boolean = true,
    val adviceEnabled: Boolean = true,
    val negativeCashFlowMonths: Int = 2,
    val netWorthDeclineMonths: Int = 2,
    val riskyShareLimitPercent: Int = 20,
    val idleCashThresholdMajor: Long = 1000,
    val idleDays: Int = 30,
    val risingCategoryPercent: Int = 25,
    val savingsRatePoints: Int = 5,
    val disabledAlerts: Set<String> = emptySet(),
    val disabledAdvice: Set<String> = emptySet(),
) {
    companion object {
        fun normalizeMonths(value: Int) = value.coerceIn(1, 12)
        fun normalizePercent(value: Int) = value.coerceIn(1, 100)
        fun normalizeIdleDays(value: Int) = value.coerceIn(1, 365)
        fun normalizeAmount(value: Long) = value.coerceAtLeast(0L)
    }
}

enum class AlertKind {
    NEGATIVE_CASH_FLOW,
    NET_WORTH_DECLINE,
    BUDGET_EXCEEDED,
    RISKY_ALLOCATION,
    IDLE_CASH,
}

enum class AdviceKind {
    RISING_CATEGORY,
    UNBUDGETED_CATEGORY,
    SAVINGS_RATE_TREND,
}

enum class AdviceTone {
    POSITIVE,
    NEUTRAL,
    WARNING,
}

data class Alert(
    val kind: AlertKind,
    val months: Int = 0,
    val budgetCount: Int = 0,
    val share: Double = 0.0,
    val amount: Money? = null,
    val accountName: String? = null,
)

data class Advice(
    val kind: AdviceKind,
    val tone: AdviceTone,
    val categoryName: String? = null,
    val percent: Double = 0.0,
    val amount: Money? = null,
    val points: Double = 0.0,
)

data class Insights(
    val alerts: List<Alert> = emptyList(),
    val advice: List<Advice> = emptyList(),
)

/**
 * Computes the local alerts and advice from the ledger alone (ADR-0036). No
 * background task, no network, no dependency; every signal is a pure function of
 * the data the overview already holds.
 */
fun computeInsights(
    accounts: List<Account>,
    entries: List<Entry>,
    categories: List<Category>,
    budgets: List<Budget>,
    valuations: List<Valuation>,
    base: CurrencyCode,
    rates: Map<Pair<String, String>, String>,
    today: LocalDate,
    settings: InsightSettings,
): Insights {
    fun convert(money: Money): Long =
        convertMinor(money.amountMinor, money.currency, base, rates)

    val refunds = refundsByOriginal(entries)
    val categoryById = categories.associateBy { it.id }
    fun topIdOf(categoryId: String): String =
        categoryById[categoryId]?.parentId ?: categoryId

    fun monthFlows(month: YearMonth): Pair<Long, Long> {
        var income = 0L
        var expenses = 0L
        val start = month.atDay(1)
        val end = month.atEndOfMonth()
        entries.forEach { entry ->
            if (entry.deletedAt != null) return@forEach
            if (entry.type != EntryType.INCOME && entry.type != EntryType.EXPENSE) return@forEach
            if (entry.date.isBefore(start) || entry.date.isAfter(end)) return@forEach
            val converted = convert(entry.amount)
            if (entry.type == EntryType.INCOME) {
                income += converted
            } else {
                expenses += (converted * -refundFactor(entry, refunds)).roundToLong()
            }
        }
        return income to expenses
    }

    val entriesByAccount = entries.groupBy { it.accountId }
    val valuationsByAccount = valuations.groupBy { it.accountId }

    fun netWorthAt(date: LocalDate): Long = accounts.sumOf { account ->
        if (account.type.isValuationBased) {
            valuationsByAccount[account.id].orEmpty()
                .filter { !it.date.isAfter(date) }
                .maxByOrNull { it.date }
                ?.let { convert(it.amount) }
                ?: 0L
        } else {
            entriesByAccount[account.id].orEmpty()
                .filter { it.deletedAt == null && !it.date.isAfter(date) }
                .sumOf { convert(it.amount) }
        }
    }

    val alerts = mutableListOf<Alert>()
    if (settings.alertsEnabled) {
        val completed = (1..6).map { YearMonth.from(today).minusMonths(it.toLong()) }

        // Negative cash-flow streak over completed months, newest first.
        var cashStreak = 0
        for (month in completed) {
            val (income, expenses) = monthFlows(month)
            if (income - expenses < 0) cashStreak++ else break
        }
        if (cashStreak >= settings.negativeCashFlowMonths) {
            alerts += Alert(AlertKind.NEGATIVE_CASH_FLOW, months = cashStreak)
        }

        // Net-worth decline streak: month-end capital below the previous month.
        var declineStreak = 0
        for (index in 0 until completed.size - 1) {
            val current = netWorthAt(completed[index].atEndOfMonth())
            val previous = netWorthAt(completed[index + 1].atEndOfMonth())
            if (current < previous) declineStreak++ else break
        }
        if (declineStreak >= settings.netWorthDeclineMonths) {
            alerts += Alert(AlertKind.NET_WORTH_DECLINE, months = declineStreak)
        }

        // Budget exceeded uses the current month so the user can react (ADR-0036).
        val monthStart = today.withDayOfMonth(1)
        val monthEnd = YearMonth.from(today).atEndOfMonth()
        val activeBudgets = budgets.filter {
            it.closedAt == null && !it.effectiveFrom.isAfter(monthEnd)
        }
        val overBudget = activeBudgets.count { budget ->
            val ids = (categories.filter { it.parentId == budget.categoryId }.map { it.id } +
                budget.categoryId).toSet()
            val spent = entries.filter {
                it.deletedAt == null && it.type == EntryType.EXPENSE &&
                    !it.date.isBefore(monthStart) && !it.date.isAfter(today)
            }.sumOf { entry ->
                val factor = refundFactor(entry, refunds)
                if (factor <= 0.0) return@sumOf 0L
                val shares = if (entry.lines.isNotEmpty()) {
                    entry.lines.map { it.categoryId to it.amount }
                } else {
                    listOfNotNull(entry.categoryId?.let { it to entry.amount })
                }
                (shares.filter { (id, _) -> id in ids }
                    .sumOf { (_, amount) -> -convert(amount) } * factor).roundToLong()
            }
            spent > convert(budget.amount)
        }
        if (overBudget > 0) {
            alerts += Alert(AlertKind.BUDGET_EXCEEDED, budgetCount = overBudget)
        }

        // Risky allocation: medium and high share of capital (ADR-0035).
        val capitalAccounts = accounts.filter {
            it.type == AccountType.INVESTMENT || it.type.isValuationBased ||
                it.type == AccountType.CHECKING || it.type == AccountType.SAVINGS ||
                it.type == AccountType.CASH
        }
        fun riskOf(account: Account) = when {
            account.type.isValuationBased -> account.risk
            account.type == AccountType.INVESTMENT -> io.github.flassuu.talex.domain.model.RiskTier.MEDIUM
            else -> io.github.flassuu.talex.domain.model.RiskTier.LOW
        }
        val riskCapital = capitalAccounts.sumOf { convert(it.balance) }
        val risky = capitalAccounts
            .filter { riskOf(it) != io.github.flassuu.talex.domain.model.RiskTier.LOW }
            .sumOf { convert(it.balance) }
        val riskyShare = if (riskCapital > 0) risky.toDouble() / riskCapital else 0.0
        if (riskCapital > 0 && riskyShare > settings.riskyShareLimitPercent / 100.0) {
            alerts += Alert(AlertKind.RISKY_ALLOCATION, share = riskyShare)
        }

        // Idle cash: a cash balance with no activity for N days.
        val thresholdMinor = settings.idleCashThresholdMajor * pow10(base.fractionDigits)
        val cutoff = today.minusDays(settings.idleDays.toLong())
        val idleAccount = accounts.firstOrNull { account ->
            (account.type == AccountType.CHECKING ||
                account.type == AccountType.SAVINGS ||
                account.type == AccountType.CASH) &&
                !account.archived &&
                account.balance.amountMinor > 0 &&
                account.balance.amountMinor >= thresholdMinor &&
                entries.none {
                    it.accountId == account.id && it.deletedAt == null && !it.date.isBefore(cutoff)
                }
        }
        if (idleAccount != null) {
            alerts += Alert(
                AlertKind.IDLE_CASH,
                amount = Money(idleAccount.balance.amountMinor, idleAccount.currency),
                accountName = idleAccount.name,
            )
        }
    }

    val advice = mutableListOf<Advice>()
    if (settings.adviceEnabled) {
        val currentMonth = YearMonth.from(today)
        val previousMonths = (1..3).map { currentMonth.minusMonths(it.toLong()) }

        fun categoryTotals(month: YearMonth): Map<String, Long> {
            val start = month.atDay(1)
            val end = month.atEndOfMonth()
            val totals = mutableMapOf<String, Long>()
            entries.forEach { entry ->
                if (entry.deletedAt != null || entry.type != EntryType.EXPENSE) return@forEach
                if (entry.date.isBefore(start) || entry.date.isAfter(end)) return@forEach
                val factor = refundFactor(entry, refunds)
                if (factor <= 0.0) return@forEach
                val shares = if (entry.lines.isNotEmpty()) {
                    entry.lines.map { it.categoryId to it.amount }
                } else {
                    listOfNotNull(entry.categoryId?.let { it to entry.amount })
                }
                shares.forEach { (categoryId, amount) ->
                    val id = topIdOf(categoryId)
                    totals[id] = (totals[id] ?: 0L) + (-convert(amount) * factor).roundToLong()
                }
            }
            return totals
        }

        val current = categoryTotals(currentMonth)
        val history = previousMonths.map { categoryTotals(it) }

        // Rising category: at least 25% above its three-month average.
        val rising = current.mapNotNull { (id, amount) ->
            val average = history.mapNotNull { it[id] }.takeIf { it.isNotEmpty() }
                ?.average() ?: return@mapNotNull null
            if (average <= 0.0) return@mapNotNull null
            val change = (amount - average) / average
            if (change >= settings.risingCategoryPercent / 100.0) {
                Triple(categoryById[id]?.name, amount - average, change)
            } else {
                null
            }
        }.maxByOrNull { it.third }
        if (rising != null) {
            advice += Advice(
                kind = AdviceKind.RISING_CATEGORY,
                tone = AdviceTone.WARNING,
                categoryName = rising.first,
                percent = rising.third,
                amount = Money(rising.second.roundToLong(), base),
            )
        }

        // Top unbudgeted category in the current month.
        val budgetedIds = budgets.filter { it.closedAt == null }.map { it.categoryId }.toSet()
        val topUnbudgeted = current
            .filterKeys { it !in budgetedIds }
            .maxByOrNull { it.value }
        if (topUnbudgeted != null) {
            advice += Advice(
                kind = AdviceKind.UNBUDGETED_CATEGORY,
                tone = AdviceTone.NEUTRAL,
                categoryName = categoryById[topUnbudgeted.key]?.name,
                amount = Money(topUnbudgeted.value, base),
            )
        }

        // Savings-rate trend versus the previous completed month.
        val (income, expenses) = monthFlows(currentMonth)
        val currentRate = if (income > 0) (income - expenses).toDouble() / income else null
        val (previousIncome, previousExpenses) = monthFlows(previousMonths.first())
        val previousRate = if (previousIncome > 0) {
            (previousIncome - previousExpenses).toDouble() / previousIncome
        } else {
            null
        }
        if (currentRate != null && previousRate != null) {
            val points = (currentRate - previousRate) * 100
            if (kotlin.math.abs(points) >= settings.savingsRatePoints) {
                advice += Advice(
                    kind = AdviceKind.SAVINGS_RATE_TREND,
                    tone = if (points >= 0) AdviceTone.POSITIVE else AdviceTone.WARNING,
                    points = points,
                )
            }
        }
    }

    return Insights(
        alerts = alerts.filter { it.kind.name !in settings.disabledAlerts },
        advice = advice.filter { it.kind.name !in settings.disabledAdvice },
    )
}

private fun pow10(exponent: Int): Long {
    var result = 1L
    repeat(exponent.coerceIn(0, 4)) { result *= 10 }
    return result
}
