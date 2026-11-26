package io.github.flassuu.talex.domain.money

import java.math.BigDecimal
import java.math.RoundingMode

/** The three states a calculation can be in, matching the system calculator. */
sealed interface Calculation {
    data class Value(val value: BigDecimal) : Calculation

    /** Not ready yet: empty, trailing operator, or an unclosed parenthesis. */
    data object Incomplete : Calculation

    /** Genuine error, for example division by zero. */
    data object Error : Calculation
}

/**
 * Evaluates a small arithmetic expression such as `20,10-12`, `20+5*2` or
 * `(120+30)*0.5`. It is a hand-written recursive descent parser for `+ - * /`,
 * parentheses and a postfix `%` (divide by 100). No code evaluation, no scripting
 * engine, no dependency; division is rounded to six decimals.
 */
object AmountCalculator {

    fun evaluate(input: String): BigDecimal? = (analyze(input) as? Calculation.Value)?.value

    /** Distinguishes a value from an incomplete expression and a real error. */
    fun analyze(input: String): Calculation {
        val text = input
            .replace("\u00A0", "")
            .replace(" ", "")
            .replace(',', '.')
        if (text.isEmpty()) return Calculation.Incomplete
        return try {
            val parser = Parser(text)
            val value = parser.parseExpression()
            if (parser.atEnd) Calculation.Value(value) else Calculation.Incomplete
        } catch (e: ArithmeticException) {
            Calculation.Error
        } catch (e: IllegalArgumentException) {
            Calculation.Incomplete
        } catch (e: NumberFormatException) {
            Calculation.Incomplete
        }
    }

    private class Parser(private val text: String) {
        private var position = 0

        val atEnd: Boolean get() = position >= text.length

        fun parseExpression(): BigDecimal {
            var value = parseTerm()
            while (!atEnd && (text[position] == '+' || text[position] == '-')) {
                val operator = text[position++]
                val right = parseTerm()
                value = if (operator == '+') value + right else value - right
            }
            return value
        }

        private fun parseTerm(): BigDecimal {
            var value = parseFactor()
            while (!atEnd && (text[position] == '*' || text[position] == '/')) {
                val operator = text[position++]
                val right = parseFactor()
                value = if (operator == '*') {
                    value * right
                } else {
                    value.divide(right, 6, RoundingMode.HALF_UP)
                }
            }
            return value
        }

        private fun parseFactor(): BigDecimal {
            var negative = false
            if (!atEnd && (text[position] == '+' || text[position] == '-')) {
                negative = text[position] == '-'
                position++
            }
            var value = if (!atEnd && text[position] == '(') {
                position++
                val inner = parseExpression()
                if (atEnd || text[position] != ')') {
                    throw IllegalArgumentException("Missing closing parenthesis")
                }
                position++
                inner
            } else {
                val start = position
                while (!atEnd && (text[position].isDigit() || text[position] == '.')) {
                    position++
                }
                if (start == position) throw IllegalArgumentException("Number expected")
                BigDecimal(text.substring(start, position).trimEnd('.'))
            }
            if (negative) value = value.negate()
            while (!atEnd && text[position] == '%') {
                position++
                value = value.divide(BigDecimal(100), 6, RoundingMode.HALF_UP)
            }
            return value
        }
    }
}
