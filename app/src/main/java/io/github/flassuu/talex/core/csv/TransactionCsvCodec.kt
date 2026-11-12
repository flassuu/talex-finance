package io.github.flassuu.talex.core.csv

import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.domain.model.EntryType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * The transaction CSV contract (ADR-0027). Categories are referenced by their path
 * (`Parent/Child`), tags are separated by semicolons, and rows that belong to one
 * split or one transfer pair share a `group_id`.
 */
object TransactionCsvCodec {

    val HEADERS = listOf(
        "id",
        "type",
        "date",
        "amount",
        "currency",
        "account",
        "description",
        "payee",
        "category",
        "tags",
        "group_id",
        "external_id",
    )

    /** A structurally valid CSV transaction row, before it is mapped to the database. */
    data class Row(
        val line: Int,
        val id: String,
        val type: EntryType,
        val date: LocalDate,
        val amount: BigDecimal,
        val currency: CurrencyCode,
        val account: String,
        val description: String,
        val payee: String,
        val category: String,
        val tags: List<String>,
        val groupId: String,
        val externalId: String,
    )

    fun encode(rows: List<Row>): String = Csv.encode(
        buildList {
            add(HEADERS)
            rows.forEach { add(toFields(it)) }
        },
    )

    private fun toFields(row: Row): List<String> = listOf(
        row.id,
        row.type.name.lowercase(),
        row.date.toString(),
        row.amount.toPlainString(),
        row.currency.value,
        row.account,
        row.description,
        row.payee,
        row.category,
        row.tags.joinToString(";"),
        row.groupId,
        row.externalId,
    )

    /**
     * Parses and structurally validates the whole file. Any malformed row throws, so
     * the caller can reject the batch before writing anything.
     */
    fun decode(text: String): List<Row> {
        val table = Csv.decode(text)
        if (table.headers.isEmpty()) throw CsvParseException("The file is empty")
        val required = listOf("type", "date", "amount", "currency", "account")
        required.forEach {
            if (table.indexOf(it) < 0) throw CsvParseException("Missing column: $it")
        }
        return table.rows.mapIndexed { lineIndex, row ->
            parseRow(table, row, lineIndex + 2)
        }
    }

    private fun parseRow(table: CsvTable, row: List<String>, line: Int): Row {
        fun fail(reason: String): Nothing = throw CsvParseException("Line $line: $reason")

        val type = when (table.value(row, "type").lowercase()) {
            "income" -> EntryType.INCOME
            "expense" -> EntryType.EXPENSE
            "transfer" -> EntryType.TRANSFER
            else -> fail("unknown type")
        }
        val date = try {
            LocalDate.parse(table.value(row, "date"))
        } catch (e: DateTimeParseException) {
            fail("invalid date")
        }
        val amount = table.value(row, "amount").replace(" ", "").replace(',', '.').toBigDecimalOrNull()
            ?: fail("invalid amount")
        if (amount.signum() == 0) fail("zero amount")
        val currency = runCatching { CurrencyCode.of(table.value(row, "currency")) }
            .getOrElse { fail("invalid currency") }
        val account = table.value(row, "account").ifEmpty { fail("missing account") }
        val tags = table.value(row, "tags")
            .split(';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        return Row(
            line = line,
            id = table.value(row, "id"),
            type = type,
            date = date,
            amount = amount,
            currency = currency,
            account = account,
            description = table.value(row, "description"),
            payee = table.value(row, "payee"),
            category = table.value(row, "category"),
            tags = tags,
            groupId = table.value(row, "group_id"),
            externalId = table.value(row, "external_id"),
        )
    }
}

class CsvParseException(message: String) : Exception(message)
