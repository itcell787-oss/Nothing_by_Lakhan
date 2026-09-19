package com.example.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

object PdfTextExtractor {

    /**
     * Extract readable text from a PDF file.
     * Parses PDF objects, decompresses FlateDecode streams, and extracts text tokens (Tj, TJ, string literals).
     */
    suspend fun extractText(file: File): String = withContext(Dispatchers.IO) {
        try {
            if (!file.exists() || file.length() == 0L) {
                return@withContext "Empty PDF document"
            }

            val bytes = FileInputStream(file).use { it.readBytes() }
            val extracted = StringBuilder()

            // 1. Scan for stream ... endstream blocks
            var index = 0
            val streamToken = "stream".toByteArray(Charsets.ISO_8859_1)
            val endStreamToken = "endstream".toByteArray(Charsets.ISO_8859_1)

            val streamChunks = mutableListOf<ByteArray>()

            while (index < bytes.size - 10) {
                val streamStart = indexOf(bytes, streamToken, index)
                if (streamStart == -1) break

                var dataStart = streamStart + streamToken.size
                // Skip CRLF after "stream"
                if (dataStart < bytes.size && bytes[dataStart] == '\r'.code.toByte()) dataStart++
                if (dataStart < bytes.size && bytes[dataStart] == '\n'.code.toByte()) dataStart++

                val streamEnd = indexOf(bytes, endStreamToken, dataStart)
                if (streamEnd == -1) break

                val chunk = bytes.copyOfRange(dataStart, streamEnd)
                streamChunks.add(chunk)
                index = streamEnd + endStreamToken.size
            }

            // 2. Try decompressing each stream chunk with zlib Inflater
            for (chunk in streamChunks) {
                val decompressed = tryDecompressFlate(chunk) ?: chunk
                val chunkText = parsePdfTextOperators(decompressed)
                if (chunkText.isNotBlank()) {
                    extracted.append(chunkText).append("\n\n")
                }
            }

            val result = extracted.toString().trim()
            if (result.isNotBlank()) {
                return@withContext result
            }

            // Fallback: extract string literals enclosed in parentheses throughout raw bytes
            val fallbackText = extractParenthesizedStrings(bytes)
            if (fallbackText.isNotBlank()) {
                return@withContext fallbackText
            }

            "No selectable text found in PDF document."
        } catch (e: Exception) {
            "Text extraction notice: ${e.localizedMessage ?: e.message}"
        }
    }

    private fun tryDecompressFlate(data: ByteArray): ByteArray? {
        return try {
            val inflater = Inflater(false)
            val iis = InflaterInputStream(data.inputStream(), inflater)
            val bos = ByteArrayOutputStream()
            val buf = ByteArray(1024)
            var len: Int
            while (iis.read(buf).also { len = it } != -1) {
                bos.write(buf, 0, len)
            }
            bos.toByteArray()
        } catch (_: Exception) {
            try {
                // Try nowrap = true if header is raw deflate
                val inflater = Inflater(true)
                val iis = InflaterInputStream(data.inputStream(), inflater)
                val bos = ByteArrayOutputStream()
                val buf = ByteArray(1024)
                var len: Int
                while (iis.read(buf).also { len = it } != -1) {
                    bos.write(buf, 0, len)
                }
                bos.toByteArray()
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun parsePdfTextOperators(bytes: ByteArray): String {
        val text = String(bytes, Charsets.ISO_8859_1)
        val sb = StringBuilder()

        // Match (...) Tj
        val tjRegex = Regex("""\((.*?)\)\s*Tj""")
        tjRegex.findAll(text).forEach { match ->
            val content = unescapePdfString(match.groupValues[1])
            sb.append(content).append(" ")
        }

        // Match [...] TJ (arrays of strings and kerning)
        val tjArrayRegex = Regex("""\[(.*?)\]\s*TJ""")
        tjArrayRegex.findAll(text).forEach { match ->
            val arrayBody = match.groupValues[1]
            val innerStringRegex = Regex("""\((.*?)\)""")
            innerStringRegex.findAll(arrayBody).forEach { inner ->
                val content = unescapePdfString(inner.groupValues[1])
                sb.append(content)
            }
            sb.append(" ")
        }

        // Match BT ... ET blocks if no Tj matched
        if (sb.isEmpty()) {
            val btEtRegex = Regex("""BT\s*(.*?)\s*ET""", RegexOption.DOT_MATCHES_ALL)
            btEtRegex.findAll(text).forEach { match ->
                val block = match.groupValues[1]
                val strMatches = Regex("""\((.*?)\)""").findAll(block)
                for (sm in strMatches) {
                    sb.append(unescapePdfString(sm.groupValues[1])).append(" ")
                }
                sb.append("\n")
            }
        }

        return sb.toString().trim()
    }

    private fun extractParenthesizedStrings(bytes: ByteArray): String {
        val str = String(bytes, Charsets.ISO_8859_1)
        val sb = StringBuilder()
        val regex = Regex("""\(([a-zA-Z0-9 .,:;!?@#$%&*_\-+=/<>'"()]{3,})\)""")
        regex.findAll(str).forEach { match ->
            val candidate = unescapePdfString(match.groupValues[1]).trim()
            if (candidate.length > 3 && !candidate.startsWith("/") && !candidate.all { it.isDigit() }) {
                sb.append(candidate).append("\n")
            }
        }
        return sb.toString().trim()
    }

    private fun unescapePdfString(input: String): String {
        return input
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\(", "(")
            .replace("\\)", ")")
            .replace("\\\\", "\\")
    }

    private fun indexOf(source: ByteArray, target: ByteArray, start: Int): Int {
        if (target.isEmpty()) return 0
        val max = source.size - target.size
        for (i in start..max) {
            var found = true
            for (j in target.indices) {
                if (source[i + j] != target[j]) {
                    found = false
                    break
                }
            }
            if (found) return i
        }
        return -1
    }
}
