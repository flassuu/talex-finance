package io.github.flassuu.talex.core.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class MoneyTest {

    private val eur = CurrencyCode.EUR
    private val usd = CurrencyCode.USD

    @Test
    fun addsAndSubtractsOnlySameCurrency() {
        assertEquals(300L, (Money(100, eur) + Money(200, eur)).amountMinor)
        assertEquals(-100L, (Money(100, eur) - Money(200, eur)).amountMinor)
        assertThrows(IllegalArgumentException::class.java) { Money(100, eur) + Money(100, usd) }
    }

    @Test
    fun detectsZeroNegativeAndAbsoluteValue() {
        assertTrue(Money.zero(eur).isZero)
        assertTrue(Money(-1, eur).isNegative)
        assertEquals(1L, Money(-1, eur).abs().amountMinor)
    }

    @Test
    fun currencyCodeNormalizesAndValidates() {
        assertEquals("EUR", CurrencyCode.of("eur").value)
        assertThrows(IllegalArgumentException::class.java) { CurrencyCode.of("eu") }
        assertThrows(IllegalArgumentException::class.java) { CurrencyCode.of("12A") }
    }

    @Test
    fun parsesCommaDotGroupingAndInvalidInput() {
        assertEquals(123456L, MoneyFormat.parse("1 234,56", eur)?.amountMinor)
        assertEquals(123456L, MoneyFormat.parse("1234.56", eur)?.amountMinor)
        assertEquals(0L, MoneyFormat.parse("", eur)?.amountMinor)
        assertEquals(-1000L, MoneyFormat.parse("-10", eur)?.amountMinor)
        assertNull(MoneyFormat.parse("abc", eur))
    }

    @Test
    fun formatsWithLocaleAndCurrencySymbol() {
        val text = MoneyFormat.format(Money(123456, eur), Locale.US)
        assertTrue(text.contains("1,234.56"))
        assertTrue(text.contains("€"))
        assertTrue(MoneyFormat.format(Money(-123456, eur), Locale.US).startsWith("-"))
    }
}
