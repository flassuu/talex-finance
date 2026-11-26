package io.github.flassuu.talex.data.remote

import io.github.flassuu.talex.core.money.CurrencyCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class FallbackRateProviderTest {

    private val usd = CurrencyCode.USD
    private val uah = CurrencyCode.UAH

    @Test
    fun mergesPrimaryAndFallbackResults() {
        val result = FallbackRateProvider.mergeRates(
            primary = mapOf(usd to BigDecimal("1.10")),
            secondary = mapOf(uah to BigDecimal("40.5")),
            targets = setOf(usd, uah),
        )
        assertEquals(setOf(usd, uah), result.getOrThrow().keys)
    }

    @Test
    fun dropsCurrenciesOutsideTheRequestedTargets() {
        val result = FallbackRateProvider.mergeRates(
            primary = mapOf(usd to BigDecimal("1.10")),
            secondary = emptyMap(),
            targets = setOf(usd),
        )
        assertEquals(setOf(usd), result.getOrThrow().keys)
    }

    @Test
    fun anEmptyMergeIsAFailure() {
        assertTrue(
            FallbackRateProvider.mergeRates(emptyMap(), emptyMap(), setOf(usd)).isFailure,
        )
    }
}
