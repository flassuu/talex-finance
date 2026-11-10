package io.github.flassuu.talex.core.money

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * A signed amount in integer minor units. Never use floating point for money (D-010).
 */
data class Money(val amountMinor: Long, val currency: CurrencyCode) {

    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "Currency mismatch: $currency vs ${other.currency}" }
        return Money(amountMinor + other.amountMinor, currency)
    }

    operator fun minus(other: Money): Money = plus(-other)

    operator fun unaryMinus(): Money = Money(-amountMinor, currency)

    val isZero: Boolean get() = amountMinor == 0L

    val isNegative: Boolean get() = amountMinor < 0L

    fun abs(): Money = if (amountMinor < 0L) -this else this

    companion object {
        fun zero(currency: CurrencyCode): Money = Money(0L, currency)
    }
}

object MoneyFormat {

    /** Parses user text such as "1 234,50" into minor units, or null when invalid. */
    fun parse(input: String, currency: CurrencyCode): Money? {
        val cleaned = input
            .trim()
            .replace("\u00A0", "")
            .replace(" ", "")
            .replace(',', '.')
        if (cleaned.isEmpty()) return Money.zero(currency)
        val decimal = cleaned.toBigDecimalOrNull() ?: return null
        val minor = decimal.movePointRight(currency.fractionDigits).setScale(0, RoundingMode.HALF_UP)
        return runCatching { Money(minor.longValueExact(), currency) }.getOrNull()
    }

    fun format(money: Money, locale: Locale = Locale.getDefault()): String {
        val digits = money.currency.fractionDigits
        val major = BigDecimal(money.amountMinor).movePointLeft(digits)
        val numberFormat = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = digits
            maximumFractionDigits = digits
        }
        val sign = if (money.isNegative) "-" else ""
        return sign + numberFormat.format(major.abs()) + "\u00A0" + money.currency.symbol
    }
}
