package io.github.flassuu.talex.domain.money

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Evaluates a small arithmetic expression typed into an amount field, such as
 * `20,10-12` or `20+5*2` (roadmap 6.8). It is a hand-written recursive descent
 * parser for `+ - * /` with the usual precedence; there is no code evaluation,
 * no scripting engine and no dependency. Division is rounded to six decimals.
 */
object AmountCalculator {

    fun evaluate(input: String): BigDecimal? {
        val text = input
            .replace("\u00A0", "")
            .replace(" ", "")
            .replace(',', '.')
        if (text.isEmpty()) return null
        return try {
            val parser = Parser(text)
            val value = parser.parseExpression()
            if (parser.atEnd) value else null
        } catch (e: ArithmeticException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        } catch (e: NumberFormatException) {
            null
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
                value = if (operator == '*') value * right else value.divide(right, 6, RoundingMode.HALF_UP)
            }
            return value
        }

        private fun parseFactor(): BigDecimal {
            var negative = false
            if (!atEnd && (text[position] == '+' || text[position] == '-')) {
                negative = text[position] == '-'
                position++
            }
            val start = position
            while (!atEnd && (text[position].isDigit() || text[position] == '.')) {
                position++
            }
            if (start == position) throw IllegalArgumentException("Number expected")
            val literal = text.substring(start, position).trimEnd('.')
            val number = BigDecimal(literal)
            return if (negative) number.negate() else number
        }
    }
}
