package io.github.flassuu.talex.core.csv

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvTest {

    @Test
    fun roundTripsQuotedFields() {
        val rows = listOf(
            listOf("a", "b", "c"),
            listOf("one,two", "he said \"hi\"", "line\nbreak"),
            listOf("", "plain", "end"),
        )
        val text = Csv.encode(rows)
        assertEquals(rows, Csv.decode(text).let { listOf(it.headers) + it.rows })
    }

    @Test
    fun decodesCrlfAndKeepsEmptyFields() {
        val table = Csv.decode("a,b,c\r\n1,,3\r\n")
        assertEquals(listOf("a", "b", "c"), table.headers)
        assertEquals(listOf(listOf("1", "", "3")), table.rows)
    }

    @Test
    fun headerLookupIgnoresCaseAndSpace() {
        val table = Csv.decode(" Type , Amount \nx,1\n")
        assertEquals(0, table.indexOf("type"))
        assertEquals(1, table.indexOf("amount"))
        assertEquals("1", table.value(table.rows.first(), "AMOUNT"))
    }

    @Test
    fun dropsTrailingBlankLines() {
        val table = Csv.decode("a\n1\n\n")
        assertEquals(1, table.rows.size)
    }

    @Test
    fun parsesEscapedQuotes() {
        val table = Csv.decode("a\n\"quoted \"\"word\"\"\"\n")
        assertEquals("quoted \"word\"", table.rows.first().first())
    }
}
