package io.github.flassuu.talex.domain.money

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class RateFormatTest {

    @Test
    fun roundsHalfUpToTheChosenPrecision() {
        assertEquals("1.2346", formatRate(BigDecimal("1.23456"), 4))
        assertEquals("1.23", formatRate(BigDecimal("1.23456"), 2))
    }

    @Test
    fun dropsTrailingZeros() {
        assertEquals("1.1", formatRate(BigDecimal("1.100000"), 6))
    }

    @Test
    fun clampsThePrecisionToTheSupportedRange() {
        assertEquals("1.23", formatRate(BigDecimal("1.23456"), 0))
        assertEquals("1.23456789", formatRate(BigDecimal("1.234567891"), 20))
    }
}
