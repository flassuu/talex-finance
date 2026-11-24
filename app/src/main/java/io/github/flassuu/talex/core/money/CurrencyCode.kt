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

    /** Localized currency name, e.g. "Euro" or "Ukrainian Hryvnia". */
    fun displayName(locale: Locale = Locale.getDefault()): String =
        runCatching { Currency.getInstance(value).getDisplayName(locale) }
            .getOrDefault(value)

    companion object {
        val EUR = CurrencyCode("EUR")
        val USD = CurrencyCode("USD")
        val UAH = CurrencyCode("UAH")
        val GBP = CurrencyCode("GBP")
        val PLN = CurrencyCode("PLN")
        val CHF = CurrencyCode("CHF")

        /** Currencies offered first in pickers. */
        val common: List<CurrencyCode> = listOf(EUR, USD, UAH, GBP, PLN, CHF)

        /**
         * Every ISO 4217 code the platform knows, ascending by code. The list is
         * built once from [Currency.getAvailableCurrencies] so the picker needs no
         * bundled catalogue.
         */
        val all: List<CurrencyCode> by lazy {
            Currency.getAvailableCurrencies()
                .mapNotNull { runCatching { of(it.currencyCode) }.getOrNull() }
                .filter { it.value != "XXX" }
                .distinct()
                .sortedBy { it.value }
        }

        fun of(raw: String): CurrencyCode {
            val normalized = raw.trim().uppercase(Locale.ROOT)
            require(normalized.length == 3 && normalized.all { it in 'A'..'Z' }) {
                "Not an ISO 4217 currency code: $raw"
            }
            return CurrencyCode(normalized)
        }

        fun ofOrNull(raw: String): CurrencyCode? = runCatching { of(raw) }.getOrNull()
    }
}
