package io.github.flassuu.talex.core.money

import java.util.Currency
import java.util.Locale

/**
 * An ISO 4217 currency code. Stored as text so it survives backup and export unchanged.
 */
@JvmInline
value class CurrencyCode private constructor(val value: String) {

    val fractionDigits: Int
        get() = runCatching { Currency.getInstance(value).defaultFractionDigits }
            .getOrDefault(2)
            .coerceIn(0, 4)

    val symbol: String
        get() = runCatching { Currency.getInstance(value).getSymbol(Locale.getDefault()) }
            .getOrDefault(value)

    companion object {
        val EUR = CurrencyCode("EUR")
        val USD = CurrencyCode("USD")
        val UAH = CurrencyCode("UAH")
        val GBP = CurrencyCode("GBP")
        val PLN = CurrencyCode("PLN")
        val CHF = CurrencyCode("CHF")

        /** Currencies offered in the first-run wizard and account form. */
        val common: List<CurrencyCode> = listOf(EUR, USD, UAH, GBP, PLN, CHF)

        fun of(raw: String): CurrencyCode {
            val normalized = raw.trim().uppercase(Locale.ROOT)
            require(normalized.length == 3 && normalized.all { it in 'A'..'Z' }) {
                "Not an ISO 4217 currency code: $raw"
            }
            return CurrencyCode(normalized)
        }
    }
}
