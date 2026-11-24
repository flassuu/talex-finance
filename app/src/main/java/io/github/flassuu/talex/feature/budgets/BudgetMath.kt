package io.github.flassuu.talex.feature.budgets

import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.money.Money
import io.github.flassuu.talex.domain.model.Budget
import io.github.flassuu.talex.domain.model.Category
import io.github.flassuu.talex.domain.model.Entry
import io.github.flassuu.talex.domain.model.EntryType
import io.github.flassuu.talex.domain.model.Rate
import io.github.flassuu.talex.domain.money.convertAtDate
import java.time.YearMonth

/** The status of one budget for one month (ADR-0010). */
data class BudgetStatus(
    val budget: Budget,
    /** The limit converted to the active base currency. */
    val limit: Money,
    val spent: Money,
    val remaining: Money,
    val percent: Double,
    val overBudget: Boolean,
    /** True when a foreign amount has no rate on or before its date. */
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

        val limitMinor = convertAtDate(limit.amount, base, rates, monthEnd)
        if (limitMinor == null) incomplete = true
        val resolvedLimit = limitMinor ?: 0L
        BudgetStatus(
            budget = limit,
            limit = Money(resolvedLimit, base),
            spent = Money(spentMinor, base),
            remaining = Money(resolvedLimit - spentMinor, base),
            percent = if (resolvedLimit > 0) spentMinor.toDouble() / resolvedLimit else 0.0,
            overBudget = spentMinor > resolvedLimit,
            incomplete = incomplete,
        )
    }.sortedBy { it.budget.categoryId }
}

