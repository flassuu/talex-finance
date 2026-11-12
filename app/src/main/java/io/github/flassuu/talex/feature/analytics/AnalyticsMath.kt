package io.github.flassuu.talex.feature.analytics

import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.money.Money
import io.github.flassuu.talex.core.money.convertMinor
import io.github.flassuu.talex.domain.model.Account
import io.github.flassuu.talex.domain.model.Category
import io.github.flassuu.talex.domain.model.Entry
import io.github.flassuu.talex.domain.model.EntryType
import io.github.flassuu.talex.domain.model.Rate
import io.github.flassuu.talex.domain.model.Valuation
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.Locale

/** One time bucket: income, expenses and the resulting cash flow. */
data class AnalyticsPoint(
    val label: String,
    val income: Money,
    val expenses: Money,
) {
    val cashFlow: Money get() = income - expenses
}

/** One top-level category share of the period's spending. */
data class CategorySlice(val category: Category, val amount: Money)

data class AnalyticsData(
    val base: CurrencyCode,
    val points: List<AnalyticsPoint>,
    val categories: List<CategorySlice>,
    val netWorth: List<Money>,
    val totalIncome: Money,
    val totalExpenses: Money,
    /** True when a foreign-currency amount had no rate on or before its date. */
    val incomplete: Boolean,
) {
    val totalCashFlow: Money get() = totalIncome - totalExpenses
}

/**
 * Computes the analytics series for [period]. Flows convert at the transaction-date
 * rate (matching budgets, ADR-0010); the net-worth series converts balances with the
 * latest rate (matching the capital overview). Transfers, opening and adjustment
 * entries never count as income or expenses.
 */
fun computeAnalytics(
    accounts: List<Account>,
    entries: List<Entry>,
    categories: List<Category>,
    valuations: List<Valuation>,
    rates: List<Rate>,
    base: CurrencyCode,
    period: AnalyticsPeriod,
    locale: Locale = Locale.getDefault(),
): AnalyticsData {
    val buckets = periodBuckets(period, locale)
    val flows = entries.filter { it.type == EntryType.INCOME || it.type == EntryType.EXPENSE }
    var incomplete = false

    fun convert(money: Money, date: LocalDate): Long? =
        convertAtDate(money.amountMinor, money.currency, base, rates, date)

    val points = buckets.map { bucket ->
        var income = 0L
        var expenses = 0L
        flows.forEach { entry ->
            if (entry.date.isBefore(bucket.start) || entry.date.isAfter(bucket.end)) return@forEach
            val converted = convert(entry.amount, entry.date)
            if (converted == null) {
                incomplete = true
                return@forEach
            }
            if (entry.type == EntryType.INCOME) {
                income += converted
            } else {
                expenses += -converted
            }
        }
        AnalyticsPoint(
            label = bucket.label,
            income = Money(income, base),
            expenses = Money(expenses, base),
        )
    }

    val categoryById = categories.associateBy { it.id }
    val totals = mutableMapOf<String, Long>()
    flows.filter { it.type == EntryType.EXPENSE }.forEach { entry ->
        if (entry.date.isBefore(period.startDate) || entry.date.isAfter(period.endDate)) return@forEach
        val shares = if (entry.lines.isNotEmpty()) {
            entry.lines.map { it.categoryId to it.amount }
        } else {
            listOfNotNull(entry.categoryId?.let { it to entry.amount })
        }
        shares.forEach { (categoryId, amount) ->
            val category = categoryById[categoryId] ?: return@forEach
            val topId = category.parentId ?: category.id
            val converted = convert(amount, entry.date)
            if (converted == null) {
                incomplete = true
            } else {
                totals[topId] = (totals[topId] ?: 0L) + (-converted)
            }
        }
    }
    val sliceList = totals
        .mapNotNull { (id, total) ->
            categoryById[id]?.let { CategorySlice(it, Money(total, base)) }
        }
        .sortedByDescending { it.amount.amountMinor }

    val latestRates = rates.associate { (it.base.value to it.quote.value) to it.rate.toPlainString() }
    val entriesByAccount = entries.groupBy { it.accountId }
    val valuationsByAccount = valuations.groupBy { it.accountId }
    val netWorth = buckets.map { bucket ->
        val date = bucket.end
        var total = 0L
        accounts.forEach { account ->
            if (account.type.isValuationBased) {
                valuationsByAccount[account.id].orEmpty()
                    .filter { !it.date.isAfter(date) }
                    .maxByOrNull { it.date }
                    ?.let { total += convertMinor(it.amount.amountMinor, it.amount.currency, base, latestRates) }
            } else {
                entriesByAccount[account.id].orEmpty()
                    .filter { !it.date.isAfter(date) }
                    .forEach { total += convertMinor(it.amount.amountMinor, it.amount.currency, base, latestRates) }
            }
        }
        Money(total, base)
    }

    return AnalyticsData(
        base = base,
        points = points,
        categories = sliceList,
        netWorth = netWorth,
        totalIncome = Money(points.sumOf { it.income.amountMinor }, base),
        totalExpenses = Money(points.sumOf { it.expenses.amountMinor }, base),
        incomplete = incomplete,
    )
}

/** Converts using the latest rate on or before [date]; null when no rate exists. */
internal fun convertAtDate(
    amountMinor: Long,
    from: CurrencyCode,
    to: CurrencyCode,
    rates: List<Rate>,
    date: LocalDate,
): Long? {
    if (from == to) return amountMinor
    val direct = rates
        .filter { it.base == from && it.quote == to && !it.date.isAfter(date) }
        .maxByOrNull { it.date }
    if (direct != null) {
        return BigDecimal(amountMinor).multiply(direct.rate)
            .setScale(0, RoundingMode.HALF_UP).toLong()
    }
    val inverse = rates
        .filter { it.base == to && it.quote == from && !it.date.isAfter(date) }
        .maxByOrNull { it.date }
    if (inverse != null && inverse.rate.signum() != 0) {
        return BigDecimal(amountMinor).divide(inverse.rate, 0, RoundingMode.HALF_UP).toLong()
    }
    return null
}
