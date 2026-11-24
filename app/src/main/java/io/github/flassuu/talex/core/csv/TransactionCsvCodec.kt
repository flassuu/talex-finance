package io.github.flassuu.talex.core.csv

import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.domain.model.EntryType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.Locale

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
    fun decode(text: String): List<Row> = decode(text, emptyMap())

    /**
     * Parses using [mapping] from a canonical field to a header in the file. Missing
     * entries in [mapping] fall back to the canonical header name, so a standard file
     * needs no mapping.
     */
    fun decode(text: String, mapping: Map<String, String>): List<Row> {
        val table = Csv.decode(text)
        if (table.headers.isEmpty()) throw CsvParseException("The file is empty")
        val columns = HEADERS.associateWith { field ->
            table.indexOf(sourceHeader(field, mapping))
        }
        REQUIRED.forEach { field ->
            if (columns[field]!! < 0) {
                throw CsvParseException("Missing column: ${sourceHeader(field, mapping)}")
            }
        }
        return table.rows.mapIndexed { lineIndex, row ->
            parseRow(row, lineIndex + 2, columns)
        }
    }

    /** The header row of a file, for the column-mapping UI. */
    fun headers(text: String): List<String> = Csv.decode(text).headers

    /** Maps each canonical field to a matching header in the file, or to empty. */
    fun autoMapping(headers: List<String>): Map<String, String> =
        HEADERS.associateWith { field ->
            headers.firstOrNull { it.trim().equals(field, ignoreCase = true) } ?: ""
        }

    /** Required fields that [mapping] does not resolve to a header present in the file. */
    fun missingRequired(headers: List<String>, mapping: Map<String, String>): List<String> {
        val normalized = headers.map { it.trim().lowercase(Locale.ROOT) }.toSet()
        return REQUIRED.filter { field ->
            sourceHeader(field, mapping).trim().lowercase(Locale.ROOT) !in normalized
        }
    }

    private fun sourceHeader(field: String, mapping: Map<String, String>): String =
        mapping[field]?.takeIf { it.isNotBlank() } ?: field

    private fun parseRow(row: List<String>, line: Int, columns: Map<String, Int>): Row {
        fun fail(reason: String): Nothing = throw CsvParseException("Line $line: $reason")

        fun value(field: String): String {
            val index = columns[field] ?: -1
            return if (index in row.indices) row[index].trim() else ""
        }

        val type = when (value("type").lowercase()) {
            "income" -> EntryType.INCOME
            "expense" -> EntryType.EXPENSE
            "transfer" -> EntryType.TRANSFER
            else -> fail("unknown type")
        }
        val date = try {
            LocalDate.parse(value("date"))
        } catch (e: DateTimeParseException) {
            fail("invalid date")
        }
        val amount = value("amount").replace(" ", "").replace(',', '.').toBigDecimalOrNull()
            ?: fail("invalid amount")
        if (amount.signum() == 0) fail("zero amount")
        val currency = runCatching { CurrencyCode.of(value("currency")) }
            .getOrElse { fail("invalid currency") }
        val account = value("account").ifEmpty { fail("missing account") }
        val tags = value("tags")
            .split(';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        return Row(
            line = line,
            id = value("id"),
            type = type,
            date = date,
            amount = amount,
            currency = currency,
            account = account,
            description = value("description"),
            payee = value("payee"),
            category = value("category"),
            tags = tags,
            groupId = value("group_id"),
            externalId = value("external_id"),
        )
    }

    /** Fields a file must provide before it can be imported. */
    val REQUIRED = listOf("type", "date", "amount", "currency", "account")
}

class CsvParseException(message: String) : Exception(message)
