package io.github.flassuu.talex.domain.money

import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.money.Money
import io.github.flassuu.talex.domain.model.Budget
import io.github.flassuu.talex.domain.model.Rate
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/**
 * Converts [money] to [to] using the latest stored rate on or before [date].
 * The direct pair wins; otherwise the inverse pair is applied. Returns null when
 * no rate exists, so callers can mark a value incomplete instead of guessing.
 */
fun convertAtDate(money: Money, to: CurrencyCode, rates: List<Rate>, date: LocalDate): Long? {
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

/** True when a rate from [from] to [to] exists on or before [date] (direct or inverse). */
fun hasRateAtDate(from: CurrencyCode, to: CurrencyCode, rates: List<Rate>, date: LocalDate): Boolean {
    if (from == to) return true
    return rates.any { it.base == from && it.quote == to && !it.date.isAfter(date) } ||
        rates.any {
            it.base == to && it.quote == from && !it.date.isAfter(date) && it.rate.signum() != 0
        }
}

/** One open budget limit rescaled to a new base currency, or null when no rate exists. */
data class BudgetLimitConversion(
    val budget: Budget,
    val converted: Money?,
)

/** The plan for changing the base currency; [blocked] when a needed rate is missing. */
data class BaseCurrencyConversion(
    val newBase: CurrencyCode,
    val budgets: List<BudgetLimitConversion>,
) {
    val missingRate: List<Budget> get() = budgets.filter { it.converted == null }.map { it.budget }
    val blocked: Boolean get() = missingRate.isNotEmpty()
}

/**
 * Builds the conversion plan for every open budget limit (ADR-0033). Closed
 * limits keep their own currency and are ignored.
 */
fun planBaseCurrencyConversion(
    budgets: List<Budget>,
    newBase: CurrencyCode,
    rates: List<Rate>,
    date: LocalDate,
): BaseCurrencyConversion {
    val open = budgets.filter { it.closedAt == null }
    return BaseCurrencyConversion(
        newBase = newBase,
        budgets = open.map { budget ->
            val converted = convertAtDate(budget.amount, newBase, rates, date)
                ?.takeIf { it > 0 }
                ?.let { Money(it, newBase) }
            BudgetLimitConversion(budget, converted)
        },
    )
}
