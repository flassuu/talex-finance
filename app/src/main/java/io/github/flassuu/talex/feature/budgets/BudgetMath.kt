package io.github.flassuu.talex.feature.budgets

import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.money.Money
import io.github.flassuu.talex.domain.model.Budget
import io.github.flassuu.talex.domain.model.Category
import io.github.flassuu.talex.domain.model.Entry
import io.github.flassuu.talex.domain.model.EntryType
import io.github.flassuu.talex.domain.model.Rate
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

/** The status of one budget for one month (ADR-0010). */
data class BudgetStatus(
    val budget: Budget,
    val spent: Money,
    val remaining: Money,
    val percent: Double,
    val overBudget: Boolean,
    /** True when a foreign-currency expense has no rate on or before its date. */
    val incomplete: Boolean,
)

/**
 * The limit in force for [month]: the latest row whose effective date is on or
 * before the last day of the month and that was not closed before it. A removed
 * budget closes the current period, so past months keep their old limit.
 */
internal fun budgetInForce(budgets: List<Budget>, categoryId: String, month: YearMonth): Budget? {
    val end = month.atEndOfMonth()
    return budgets
        .filter { it.categoryId == categoryId }
        .filter { !it.effectiveFrom.isAfter(end) }
        .filter { it.closedAt == null || it.closedAt.isAfter(end) }
        .maxByOrNull { it.effectiveFrom }
}

/** A budget category's own id plus its direct children (one level, ADR-0011). */
private fun treeIds(categoryId: String, categories: List<Category>): Set<String> =
    (categories.filter { it.parentId == categoryId }.map { it.id } + categoryId).toSet()

/**
 * Computes budget statuses for [month]. Spending includes the category, its direct
 * children and split lines, converted to [base] at the transaction-date rate. A
 * missing rate marks the status incomplete instead of guessing.
 */
fun computeBudgetStatuses(
    budgets: List<Budget>,
    entries: List<Entry>,
    categories: List<Category>,
    base: CurrencyCode,
    rates: List<Rate>,
    month: YearMonth,
): List<BudgetStatus> {
    val monthStart = month.atDay(1)
    val monthEnd = month.atEndOfMonth()
    val monthExpenses = entries.filter {
        it.type == EntryType.EXPENSE && !it.date.isBefore(monthStart) && !it.date.isAfter(monthEnd)
    }

    return budgets.map { it.categoryId }.distinct().mapNotNull { categoryId ->
        val limit = budgetInForce(budgets, categoryId, month) ?: return@mapNotNull null
        val ids = treeIds(categoryId, categories)
        var spentMinor = 0L
        var incomplete = false

        monthExpenses.forEach { entry ->
            val shares = if (entry.lines.isNotEmpty()) {
                entry.lines.map { it.categoryId to it.amount }
            } else {
                listOfNotNull(entry.categoryId?.let { it to entry.amount })
            }
            shares.forEach { (shareCategoryId, amount) ->
                if (shareCategoryId in ids) {
                    val converted = convertAtDate(amount, base, rates, entry.date)
                    if (converted == null) incomplete = true else spentMinor += -converted
                }
            }
        }

        val limitMinor = limit.amount.amountMinor
        BudgetStatus(
            budget = limit,
            spent = Money(spentMinor, base),
            remaining = Money(limitMinor - spentMinor, base),
            percent = if (limitMinor > 0) spentMinor.toDouble() / limitMinor else 0.0,
            overBudget = spentMinor > limitMinor,
            incomplete = incomplete,
        )
    }.sortedBy { it.budget.categoryId }
}

private fun convertAtDate(
    money: Money,
    to: CurrencyCode,
    rates: List<Rate>,
    date: LocalDate,
): Long? {
    if (money.currency == to) return money.amountMinor
    val direct = rates
        .filter { it.base == money.currency && it.quote == to && !it.date.isAfter(date) }
        .maxByOrNull { it.date }
    if (direct != null) {
        return BigDecimal(money.amountMinor).multiply(direct.rate)
            .setScale(0, RoundingMode.HALF_UP).toLong()
    }
    val inverse = rates
        .filter { it.base == to && it.quote == money.currency && !it.date.isAfter(date) }
        .maxByOrNull { it.date }
    if (inverse != null && inverse.rate.signum() != 0) {
        return BigDecimal(money.amountMinor).divide(inverse.rate, 0, RoundingMode.HALF_UP).toLong()
    }
    return null
}
