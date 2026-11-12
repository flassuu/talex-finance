package io.github.flassuu.talex.core.money

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Converts [amount] minor units from [from] to [to] using the latest rate per
 * (base, quote) pair. A missing rate falls back to the unconverted amount, which
 * only affects display until the user records a rate.
 */
fun convertMinor(
    amount: Long,
    from: CurrencyCode,
    to: CurrencyCode,
    rates: Map<Pair<String, String>, String>,
): Long {
    if (from == to) return amount
    val direct = rates[from.value to to.value]?.toBigDecimalOrNull()
    if (direct != null) {
        return BigDecimal(amount).multiply(direct).setScale(0, RoundingMode.HALF_UP).toLong()
    }
    val inverse = rates[to.value to from.value]?.toBigDecimalOrNull()
    if (inverse != null && inverse.signum() != 0) {
        return BigDecimal(amount).divide(inverse, 0, RoundingMode.HALF_UP).toLong()
    }
    return amount
}
