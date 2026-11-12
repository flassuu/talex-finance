package io.github.flassuu.talex.core.csv

import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.domain.model.EntryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class TransactionCsvCodecTest {

    private fun row(
        type: EntryType = EntryType.EXPENSE,
        amount: String = "-12.34",
        groupId: String = "",
        category: String = "Food/Groceries",
    ) = TransactionCsvCodec.Row(
        line = 2,
        id = "e-1",
        type = type,
        date = LocalDate.of(2026, 3, 4),
        amount = BigDecimal(amount),
        currency = CurrencyCode.EUR,
        account = "Checking",
        description = "Lunch",
        payee = "Cafe",
        category = category,
        tags = listOf("work", "food"),
        groupId = groupId,
        externalId = "",
    )

    @Test
    fun roundTripsARow() {
        val encoded = TransactionCsvCodec.encode(listOf(row()))
        val decoded = TransactionCsvCodec.decode(encoded).single()
        assertEquals("e-1", decoded.id)
        assertEquals(EntryType.EXPENSE, decoded.type)
        assertEquals(LocalDate.of(2026, 3, 4), decoded.date)
        assertEquals(0, BigDecimal("-12.34").compareTo(decoded.amount))
        assertEquals("Checking", decoded.account)
        assertEquals("Food/Groceries", decoded.category)
        assertEquals(listOf("work", "food"), decoded.tags)
    }

    @Test
    fun decodesGroupsAndTransfers() {
        val text = """
            type,date,amount,currency,account,description,payee,category,tags,group_id,external_id
            expense,2026-03-04,-70.00,EUR,Checking,Receipt,,Food, ,g1,
            expense,2026-03-04,-30.00,EUR,Checking,Receipt,,Food/Sweets, ,g1,
            transfer,2026-03-05,-100.00,EUR,Checking,Move,,, ,t1,
            transfer,2026-03-05,110.00,USD,Savings,Move,,, ,t1,
        """.trimIndent()
        val rows = TransactionCsvCodec.decode(text)
        assertEquals(4, rows.size)
        assertEquals("g1", rows[0].groupId)
        assertEquals("t1", rows[2].groupId)
        assertEquals(EntryType.TRANSFER, rows[2].type)
    }

    @Test
    fun rejectsMissingRequiredColumn() {
        val text = "date,amount,currency,account\n2026-03-04,-1.00,EUR,Checking\n"
        assertThrows(CsvParseException::class.java) { TransactionCsvCodec.decode(text) }
    }

    @Test
    fun rejectsInvalidDate() {
        val text = "type,date,amount,currency,account\nexpense,not-a-date,-1.00,EUR,Checking\n"
        assertThrows(CsvParseException::class.java) { TransactionCsvCodec.decode(text) }
    }

    @Test
    fun rejectsZeroAmount() {
        val text = "type,date,amount,currency,account\nexpense,2026-03-04,0.00,EUR,Checking\n"
        assertThrows(CsvParseException::class.java) { TransactionCsvCodec.decode(text) }
    }

    @Test
    fun rejectsUnknownType() {
        val text = "type,date,amount,currency,account\nsideways,2026-03-04,-1.00,EUR,Checking\n"
        assertThrows(CsvParseException::class.java) { TransactionCsvCodec.decode(text) }
    }
}
