package io.github.flassuu.talex.core.csv

/**
 * A minimal RFC 4180 CSV reader and writer. It keeps the project free of an extra
 * dependency and handles quoted fields, escaped quotes, embedded newlines and both
 * LF and CRLF line endings.
 */
object Csv {

    /** The first row is treated as the header. Empty trailing lines are ignored. */
    fun decode(text: String): CsvTable {
        val input = text.removePrefix("\uFEFF")
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var index = 0

        fun endField() {
            row.add(field.toString())
            field.clear()
        }

        fun endRow() {
            endField()
            if (row.any { it.isNotEmpty() }) rows.add(row)
            row = mutableListOf()
        }

        while (index < input.length) {
            val c = input[index]
            if (inQuotes) {
                if (c == '"') {
                    if (index + 1 < input.length && input[index + 1] == '"') {
                        field.append('"')
                        index++
                    } else {
                        inQuotes = false
                    }
                } else {
                    field.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    ',' -> endField()
                    '\n' -> endRow()
                    '\r' -> {
                        endRow()
                        if (index + 1 < input.length && input[index + 1] == '\n') index++
                    }
                    else -> field.append(c)
                }
            }
            index++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) endRow()

        if (rows.isEmpty()) return CsvTable(emptyList(), emptyList())
        return CsvTable(headers = rows.first(), rows = rows.drop(1))
    }

    fun encode(rows: List<List<String>>): String = buildString {
        rows.forEach { row ->
            row.forEachIndexed { index, value ->
                if (index > 0) append(',')
                append(escape(value))
            }
            append('\n')
        }
    }

    private fun escape(value: String): String {
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuotes) return value
        return '"' + value.replace("\"", "\"\"") + '"'
    }
}

/** A decoded CSV table: the header row plus the data rows. */
data class CsvTable(
    val headers: List<String>,
    val rows: List<List<String>>,
) {
    /** Index of [name] ignoring case and surrounding space, or -1. */
    fun indexOf(name: String): Int =
        headers.indexOfFirst { it.trim().equals(name, ignoreCase = true) }

    fun value(row: List<String>, name: String): String {
        val index = indexOf(name)
        return if (index >= 0 && index < row.size) row[index].trim() else ""
    }
}
