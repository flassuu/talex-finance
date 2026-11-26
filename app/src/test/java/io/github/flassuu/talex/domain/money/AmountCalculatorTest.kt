package io.github.flassuu.talex.domain.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class AmountCalculatorTest {

    @Test
    fun subtractsWithALocaleDecimalComma() {
        assertEquals(0, BigDecimal("8.10").compareTo(AmountCalculator.evaluate("20,10-12")))
    }

    @Test
    fun multiplicationHasPrecedenceOverAddition() {
        assertEquals(0, BigDecimal("30").compareTo(AmountCalculator.evaluate("20+5*2")))
    }

    @Test
    fun evaluatesParenthesesWithPrecedence() {
        assertEquals(0, BigDecimal("75").compareTo(AmountCalculator.evaluate("(120+30)*0.5")))
        assertEquals(0, BigDecimal("30").compareTo(AmountCalculator.evaluate("20+5*(1+1)")))
    }

    @Test
    fun evaluatesNestedParentheses() {
        assertEquals(0, BigDecimal("15").compareTo(AmountCalculator.evaluate("((2+3)*4)-5")))
    }

    @Test
    fun percentDividesByOneHundred() {
        assertEquals(0, BigDecimal("0.5").compareTo(AmountCalculator.evaluate("50%")))
        assertEquals(0, BigDecimal("20").compareTo(AmountCalculator.evaluate("(100+100)*10%")))
    }

    @Test
    fun unbalancedParenthesesAreInvalid() {
        assertNull(AmountCalculator.evaluate("(2+3"))
        assertNull(AmountCalculator.evaluate("2+3)"))
    }

    @Test
    fun supportsDivisionAndUnaryMinus() {
        assertEquals(0, BigDecimal("5").compareTo(AmountCalculator.evaluate("10/2")))
        assertEquals(0, BigDecimal("-5").compareTo(AmountCalculator.evaluate("-10/2")))
    }

    @Test
    fun ignoresSpacesAndNonBreakingSpaces() {
        assertEquals(0, BigDecimal("7").compareTo(AmountCalculator.evaluate("1 000-993")))
        assertEquals(0, BigDecimal("7").compareTo(AmountCalculator.evaluate("1\u00A0000-993")))
    }

    @Test
    fun aPlainNumberIsReturnedUnchanged() {
        assertEquals(0, BigDecimal("12.5").compareTo(AmountCalculator.evaluate("12,5")))
    }

    @Test
    fun anInvalidExpressionFallsBackToNull() {
        assertNull(AmountCalculator.evaluate(""))
        assertNull(AmountCalculator.evaluate("20+"))
        assertNull(AmountCalculator.evaluate("abc"))
        assertNull(AmountCalculator.evaluate("1/0"))
    }
}
