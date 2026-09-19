package com.example.core.excel

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

data class ExcelSheetData(
    val sheetName: String,
    val headers: List<String>,
    val rows: List<List<String>>,
    val totalRows: Int,
    val totalCols: Int
)

object ExcelParser {

    /**
     * Parse spreadsheet file (.xlsx, .csv, .tsv, or .xls)
     */
    fun parse(file: File, maxRows: Int = 200): ExcelSheetData {
        val ext = file.extension.lowercase()
        return try {
            when (ext) {
                "xlsx" -> parseXlsx(file, maxRows)
                "csv" -> parseDelimited(file, ',', maxRows)
                "tsv" -> parseDelimited(file, '\t', maxRows)
                "xls" -> parseLegacyXlsOrFallback(file, maxRows)
                else -> parseDelimited(file, ',', maxRows)
            }
        } catch (e: Exception) {
            // Graceful fallback to text parsing if complex formatting fails
            parseTextFallback(file, e.localizedMessage ?: "Parse error", maxRows)
        }
    }

    private fun parseXlsx(file: File, maxRows: Int): ExcelSheetData {
        val sharedStrings = mutableListOf<String>()
        var sheetXmlBytes: ByteArray? = null

        // Step 1: Read ZIP entries
        ZipInputStream(FileInputStream(file)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                when {
                    entry.name.equals("xl/sharedStrings.xml", ignoreCase = true) -> {
                        sharedStrings.addAll(parseSharedStrings(zis.readBytes()))
                    }
                    entry.name.startsWith("xl/worksheets/sheet", ignoreCase = true) &&
                            entry.name.endsWith(".xml", ignoreCase = true) -> {
                        if (sheetXmlBytes == null) {
                            sheetXmlBytes = zis.readBytes()
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        if (sheetXmlBytes == null) {
            return ExcelSheetData("Sheet1", listOf("A"), listOf(listOf("Empty XLSX sheet")), 1, 1)
        }

        // Step 2: Parse sheet XML
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(sheetXmlBytes!!.inputStream(), "UTF-8")

        val rawRows = mutableListOf<MutableList<String>>()
        var currentRow: MutableList<String>? = null
        var currentCellRef = ""
        var cellType = ""
        var cellValue = ""
        var inV = false
        var inT = false

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT && rawRows.size < maxRows) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> {
                            currentRow = mutableListOf()
                        }
                        "c" -> {
                            currentCellRef = parser.getAttributeValue(null, "r") ?: ""
                            cellType = parser.getAttributeValue(null, "t") ?: ""
                            cellValue = ""
                        }
                        "v" -> inV = true
                        "t" -> inT = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inV || inT) {
                        cellValue += parser.text
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "v" -> inV = false
                        "t" -> inT = false
                        "c" -> {
                            val resolvedText = if (cellType == "s") {
                                val idx = cellValue.trim().toIntOrNull()
                                if (idx != null && idx in sharedStrings.indices) {
                                    sharedStrings[idx]
                                } else {
                                    cellValue
                                }
                            } else {
                                cellValue
                            }
                            currentRow?.add(resolvedText.trim())
                        }
                        "row" -> {
                            currentRow?.let {
                                if (it.isNotEmpty()) rawRows.add(it)
                            }
                            currentRow = null
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        if (rawRows.isEmpty()) {
            return ExcelSheetData("Sheet1", listOf("A"), listOf(listOf("Empty workbook")), 1, 1)
        }

        // Determine max columns
        val maxCols = rawRows.maxOfOrNull { it.size } ?: 1
        // Normalize rows
        val normalizedRows = rawRows.map { row ->
            if (row.size < maxCols) {
                row + List(maxCols - row.size) { "" }
            } else row
        }

        val headers = (0 until maxCols).map { colIndex ->
            generateColumnHeader(colIndex)
        }

        return ExcelSheetData(
            sheetName = file.nameWithoutExtension,
            headers = headers,
            rows = normalizedRows,
            totalRows = rawRows.size,
            totalCols = maxCols
        )
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val strings = mutableListOf<String>()
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(bytes.inputStream(), "UTF-8")

        var inT = false
        var currentString = StringBuilder()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "t") {
                        inT = true
                    } else if (parser.name == "si") {
                        currentString = StringBuilder()
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inT) {
                        currentString.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "t") {
                        inT = false
                    } else if (parser.name == "si") {
                        strings.add(currentString.toString())
                    }
                }
            }
            eventType = parser.next()
        }
        return strings
    }

    private fun parseDelimited(file: File, delimiter: Char, maxRows: Int): ExcelSheetData {
        val rows = mutableListOf<List<String>>()
        BufferedReader(InputStreamReader(FileInputStream(file))).useLines { lines ->
            lines.take(maxRows).forEach { line ->
                val cells = parseDelimitedLine(line, delimiter)
                if (cells.isNotEmpty()) {
                    rows.add(cells)
                }
            }
        }

        if (rows.isEmpty()) {
            return ExcelSheetData(file.nameWithoutExtension, listOf("A"), listOf(listOf("Empty file")), 1, 1)
        }

        val maxCols = rows.maxOfOrNull { it.size } ?: 1
        val headers = (0 until maxCols).map { generateColumnHeader(it) }

        val normalized = rows.map { row ->
            if (row.size < maxCols) row + List(maxCols - row.size) { "" } else row
        }

        return ExcelSheetData(
            sheetName = file.nameWithoutExtension,
            headers = headers,
            rows = normalized,
            totalRows = rows.size,
            totalCols = maxCols
        )
    }

    private fun parseDelimitedLine(line: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        for (ch in line) {
            when {
                ch == '\"' -> inQuotes = !inQuotes
                ch == delimiter && !inQuotes -> {
                    result.add(current.toString().trim())
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        result.add(current.toString().trim())
        return result
    }

    private fun parseLegacyXlsOrFallback(file: File, maxRows: Int): ExcelSheetData {
        // Binary .xls extraction: extract readable text tokens
        val bytes = file.readBytes()
        val text = String(bytes, Charsets.ISO_8859_1)
        val tokens = text.split("\r\n", "\n", "\t")
            .map { it.filter { c -> c.isLetterOrDigit() || c in " .,-()/_:;%$#" }.trim() }
            .filter { it.length > 1 }

        if (tokens.isEmpty()) {
            return ExcelSheetData(file.nameWithoutExtension, listOf("A"), listOf(listOf("Binary XLS Document - Use External App")), 1, 1)
        }

        val chunked = tokens.chunked(4).take(maxRows)
        val maxCols = chunked.maxOfOrNull { it.size } ?: 1
        val headers = (0 until maxCols).map { generateColumnHeader(it) }

        return ExcelSheetData(
            sheetName = file.nameWithoutExtension,
            headers = headers,
            rows = chunked,
            totalRows = chunked.size,
            totalCols = maxCols
        )
    }

    private fun parseTextFallback(file: File, errorMsg: String, maxRows: Int): ExcelSheetData {
        return ExcelSheetData(
            sheetName = file.nameWithoutExtension,
            headers = listOf("A", "B"),
            rows = listOf(
                listOf("STATUS", "FALLBACK_PARSER"),
                listOf("FILE", file.name),
                listOf("NOTE", errorMsg)
            ),
            totalRows = 3,
            totalCols = 2
        )
    }

    fun generateColumnHeader(index: Int): String {
        var n = index
        val sb = StringBuilder()
        while (n >= 0) {
            sb.append(('A'.code + (n % 26)).toChar())
            n = (n / 26) - 1
        }
        return sb.reverse().toString()
    }

    fun saveSheet(file: File, headers: List<String>, rows: List<List<String>>): Result<Unit> {
        return try {
            val ext = file.extension.lowercase()
            val delimiter = if (ext == "tsv") "\t" else ","
            file.bufferedWriter().use { writer ->
                writer.write(headers.joinToString(delimiter) { escapeCsv(it, delimiter) })
                writer.newLine()
                for (row in rows) {
                    writer.write(row.joinToString(delimiter) { escapeCsv(it, delimiter) })
                    writer.newLine()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun escapeCsv(value: String, delimiter: String): String {
        return if (delimiter == "," && (value.contains(",") || value.contains("\"") || value.contains("\n"))) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    fun evaluateFormula(formula: String, headers: List<String>, rows: List<List<String>>): String {
        val trimmed = formula.trim()
        if (!trimmed.startsWith("=")) return formula

        val expr = trimmed.removePrefix("=").trim()

        // Helper to get numeric value from cell coordinate like "A1", "B3"
        fun getCellNum(coord: String): Double? {
            val colStr = coord.filter { it.isLetter() }.uppercase()
            val rowStr = coord.filter { it.isDigit() }
            val colIdx = headers.indexOf(colStr).takeIf { it >= 0 }
                ?: colStr.fold(0) { acc, c -> acc * 26 + (c - 'A' + 1) } - 1
            val rowIdx = (rowStr.toIntOrNull() ?: 1) - 1
            if (rowIdx in rows.indices && colIdx in 0 until (rows[rowIdx].size)) {
                return rows[rowIdx][colIdx].toDoubleOrNull()
            }
            return null
        }

        // Helper to extract list of numbers from range like "A1:A5"
        fun getRangeNumbers(rangeStr: String): List<Double> {
            val parts = rangeStr.split(":")
            if (parts.size != 2) {
                val single = getCellNum(rangeStr)
                return if (single != null) listOf(single) else emptyList()
            }
            val startCol = parts[0].filter { it.isLetter() }.uppercase()
            val startRow = parts[0].filter { it.isDigit() }.toIntOrNull() ?: 1
            val endCol = parts[1].filter { it.isLetter() }.uppercase()
            val endRow = parts[1].filter { it.isDigit() }.toIntOrNull() ?: 1

            val startColIdx = headers.indexOf(startCol).takeIf { it >= 0 } ?: 0
            val endColIdx = headers.indexOf(endCol).takeIf { it >= 0 } ?: startColIdx

            val list = mutableListOf<Double>()
            for (r in (startRow - 1)..(endRow - 1)) {
                if (r in rows.indices) {
                    for (c in startColIdx..endColIdx) {
                        if (c in rows[r].indices) {
                            rows[r][c].toDoubleOrNull()?.let { list.add(it) }
                        }
                    }
                }
            }
            return list
        }

        return try {
            val upper = expr.uppercase()
            when {
                upper.startsWith("SUM(") && upper.endsWith(")") -> {
                    val range = upper.removePrefix("SUM(").removeSuffix(")")
                    val nums = getRangeNumbers(range)
                    String.format(java.util.Locale.US, "%.2f", nums.sum()).removeSuffix(".00")
                }
                (upper.startsWith("AVERAGE(") || upper.startsWith("AVG(")) && upper.endsWith(")") -> {
                    val range = upper.substringAfter("(").removeSuffix(")")
                    val nums = getRangeNumbers(range)
                    if (nums.isNotEmpty()) {
                        String.format(java.util.Locale.US, "%.2f", nums.average()).removeSuffix(".00")
                    } else "0"
                }
                upper.startsWith("COUNT(") && upper.endsWith(")") -> {
                    val range = upper.removePrefix("COUNT(").removeSuffix(")")
                    val nums = getRangeNumbers(range)
                    nums.size.toString()
                }
                upper.startsWith("MIN(") && upper.endsWith(")") -> {
                    val range = upper.removePrefix("MIN(").removeSuffix(")")
                    val nums = getRangeNumbers(range)
                    (nums.minOrNull() ?: 0.0).toString().removeSuffix(".0")
                }
                upper.startsWith("MAX(") && upper.endsWith(")") -> {
                    val range = upper.removePrefix("MAX(").removeSuffix(")")
                    val nums = getRangeNumbers(range)
                    (nums.maxOrNull() ?: 0.0).toString().removeSuffix(".0")
                }
                expr.contains("+") -> {
                    val parts = expr.split("+")
                    val v1 = getCellNum(parts[0].trim()) ?: parts[0].trim().toDoubleOrNull() ?: 0.0
                    val v2 = getCellNum(parts[1].trim()) ?: parts[1].trim().toDoubleOrNull() ?: 0.0
                    (v1 + v2).toString().removeSuffix(".0")
                }
                expr.contains("-") -> {
                    val parts = expr.split("-")
                    val v1 = getCellNum(parts[0].trim()) ?: parts[0].trim().toDoubleOrNull() ?: 0.0
                    val v2 = getCellNum(parts[1].trim()) ?: parts[1].trim().toDoubleOrNull() ?: 0.0
                    (v1 - v2).toString().removeSuffix(".0")
                }
                expr.contains("*") -> {
                    val parts = expr.split("*")
                    val v1 = getCellNum(parts[0].trim()) ?: parts[0].trim().toDoubleOrNull() ?: 0.0
                    val v2 = getCellNum(parts[1].trim()) ?: parts[1].trim().toDoubleOrNull() ?: 0.0
                    (v1 * v2).toString().removeSuffix(".0")
                }
                expr.contains("/") -> {
                    val parts = expr.split("/")
                    val v1 = getCellNum(parts[0].trim()) ?: parts[0].trim().toDoubleOrNull() ?: 0.0
                    val v2 = getCellNum(parts[1].trim()) ?: parts[1].trim().toDoubleOrNull() ?: 1.0
                    if (v2 != 0.0) (v1 / v2).toString().removeSuffix(".0") else "#DIV/0!"
                }
                else -> {
                    getCellNum(expr)?.toString()?.removeSuffix(".0") ?: formula
                }
            }
        } catch (_: Exception) {
            formula
        }
    }
}
