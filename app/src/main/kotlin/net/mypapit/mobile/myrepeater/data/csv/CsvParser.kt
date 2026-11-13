package net.mypapit.mobile.myrepeater.data.csv

import java.io.Reader

/** Thrown when a downloaded file is not the CSV we expect. */
class CsvFormatException(message: String) : Exception(message)

/**
 * Minimal RFC 4180 reader: quoted fields, doubled quotes, embedded newlines,
 * CRLF or LF line endings and a leading byte-order mark. Blank lines are skipped.
 */
object CsvParser {

    fun parse(reader: Reader): Sequence<List<String>> = sequence {
        val field = StringBuilder()
        var row = ArrayList<String>()
        var inQuotes = false

        var c = reader.read()
        if (c == 0xFEFF) c = reader.read()

        while (c != -1) {
            val ch = c.toChar()
            if (inQuotes) {
                if (ch == '"') {
                    val next = reader.read()
                    if (next == '"'.code) {
                        field.append('"')
                        c = reader.read()
                    } else {
                        inQuotes = false
                        c = next
                    }
                    continue
                }
                field.append(ch)
            } else {
                when (ch) {
                    '"' -> if (field.isEmpty()) inQuotes = true else field.append(ch)
                    ',' -> {
                        row.add(field.toString())
                        field.setLength(0)
                    }
                    '\r' -> Unit
                    '\n' -> {
                        row.add(field.toString())
                        field.setLength(0)
                        if (!row.isBlankLine()) yield(row)
                        row = ArrayList()
                    }
                    else -> field.append(ch)
                }
            }
            c = reader.read()
        }

        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            if (!row.isBlankLine()) yield(row)
        }
    }

    private fun List<String>.isBlankLine() = all { it.isBlank() }
}

/** Looks up cells by header name so column order and extra columns do not matter. */
internal class CsvHeader(header: List<String>) {
    private val names = header.map { it.trim().lowercase() }

    /** Index of the first column whose name starts with [prefix], or -1. */
    fun find(prefix: String): Int = names.indexOfFirst { it.startsWith(prefix) }

    fun require(prefix: String): Int =
        find(prefix).takeIf { it >= 0 } ?: throw CsvFormatException("Missing column: $prefix")
}

internal fun List<String>.cell(index: Int): String =
    if (index in indices) this[index].trim() else ""
