package com.example.core.docx

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.StringReader
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class DocxParagraph(
    val text: String,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val headingLevel: Int = 0
)

data class DocxDocumentData(
    val title: String,
    val paragraphs: List<DocxParagraph>,
    val wordCount: Int,
    val charCount: Int,
    val rawText: String
)

object DocxManager {

    /**
     * Reads and parses a .docx file into structured paragraphs and metadata.
     */
    fun readDocx(file: File): DocxDocumentData {
        val paragraphs = mutableListOf<DocxParagraph>()
        val fullTextBuilder = StringBuilder()

        try {
            var documentXmlContent: String? = null

            FileInputStream(file).use { fis ->
                ZipInputStream(fis).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        if (entry.name == "word/document.xml") {
                            val baos = ByteArrayOutputStream()
                            val buffer = ByteArray(4096)
                            var len: Int
                            while (zis.read(buffer).also { len = it } > 0) {
                                baos.write(buffer, 0, len)
                            }
                            documentXmlContent = baos.toString(StandardCharsets.UTF_8.name())
                            break
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }

            if (!documentXmlContent.isNullOrBlank()) {
                parseDocumentXml(documentXmlContent!!, paragraphs, fullTextBuilder)
            } else {
                paragraphs.add(DocxParagraph(text = "[ Empty or unreadable Word Document ]"))
            }
        } catch (e: Exception) {
            paragraphs.add(DocxParagraph(text = "Error reading document: ${e.message}"))
            fullTextBuilder.append("Error: ${e.message}")
        }

        val raw = fullTextBuilder.toString()
        val words = if (raw.isBlank()) 0 else raw.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size
        val chars = raw.length

        return DocxDocumentData(
            title = file.nameWithoutExtension,
            paragraphs = if (paragraphs.isEmpty()) listOf(DocxParagraph(text = "")) else paragraphs,
            wordCount = words,
            charCount = chars,
            rawText = raw
        )
    }

    private fun parseDocumentXml(
        xml: String,
        outParagraphs: MutableList<DocxParagraph>,
        fullTextBuilder: StringBuilder
    ) {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml))

        var eventType = parser.eventType
        var currentParagraph = StringBuilder()
        var currentRunBold = false
        var currentRunItalic = false
        var headingLevel = 0
        var insideParagraph = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name?.lowercase() ?: ""

            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (tagName) {
                        "w:p", "p" -> {
                            insideParagraph = true
                            currentParagraph.clear()
                            headingLevel = 0
                        }
                        "w:pstyle", "pstyle" -> {
                            val styleVal = parser.getAttributeValue(null, "w:val")
                                ?: parser.getAttributeValue(null, "val") ?: ""
                            if (styleVal.contains("Heading1", ignoreCase = true) || styleVal == "1") {
                                headingLevel = 1
                            } else if (styleVal.contains("Heading2", ignoreCase = true) || styleVal == "2") {
                                headingLevel = 2
                            } else if (styleVal.contains("Heading3", ignoreCase = true) || styleVal == "3") {
                                headingLevel = 3
                            }
                        }
                        "w:b", "b" -> {
                            val bVal = parser.getAttributeValue(null, "w:val")
                            currentRunBold = (bVal == null || bVal == "1" || bVal == "true")
                        }
                        "w:i", "i" -> {
                            val iVal = parser.getAttributeValue(null, "w:val")
                            currentRunItalic = (iVal == null || iVal == "1" || iVal == "true")
                        }
                        "w:tab", "tab" -> {
                            currentParagraph.append("    ")
                        }
                        "w:br", "br", "w:cr", "cr" -> {
                            currentParagraph.append("\n")
                        }
                        "w:t", "t" -> {
                            try {
                                val text = parser.nextText()
                                currentParagraph.append(text)
                            } catch (_: Exception) {}
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (tagName) {
                        "w:r", "r" -> {
                            currentRunBold = false
                            currentRunItalic = false
                        }
                        "w:p", "p" -> {
                            val paragraphText = currentParagraph.toString()
                            outParagraphs.add(
                                DocxParagraph(
                                    text = paragraphText,
                                    isBold = currentRunBold || headingLevel > 0,
                                    isItalic = currentRunItalic,
                                    headingLevel = headingLevel
                                )
                            )
                            if (fullTextBuilder.isNotEmpty()) {
                                fullTextBuilder.append("\n")
                            }
                            fullTextBuilder.append(paragraphText)
                            insideParagraph = false
                        }
                    }
                }
            }
            eventType = parser.next()
        }
    }

    /**
     * Saves updated content back to a .docx file.
     * Can accept a list of paragraph strings or raw newline-separated text.
     */
    fun saveDocx(file: File, paragraphs: List<String>): Boolean {
        return try {
            val existingEntries = mutableMapOf<String, ByteArray>()

            if (file.exists()) {
                try {
                    FileInputStream(file).use { fis ->
                        ZipInputStream(fis).use { zis ->
                            var entry = zis.nextEntry
                            while (entry != null) {
                                if (entry.name != "word/document.xml") {
                                    val baos = ByteArrayOutputStream()
                                    val buffer = ByteArray(4096)
                                    var len: Int
                                    while (zis.read(buffer).also { len = it } > 0) {
                                        baos.write(buffer, 0, len)
                                    }
                                    existingEntries[entry.name] = baos.toByteArray()
                                }
                                zis.closeEntry()
                                entry = zis.nextEntry
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // Generate new word/document.xml
            val docXml = buildDocumentXml(paragraphs)
            existingEntries["word/document.xml"] = docXml.toByteArray(StandardCharsets.UTF_8)

            // Ensure minimal required OpenXML package structure if created anew
            if (!existingEntries.containsKey("[Content_Types].xml")) {
                existingEntries["[Content_Types].xml"] = buildContentTypesXml().toByteArray(StandardCharsets.UTF_8)
            }
            if (!existingEntries.containsKey("_rels/.rels")) {
                existingEntries["_rels/.rels"] = buildRootRelsXml().toByteArray(StandardCharsets.UTF_8)
            }
            if (!existingEntries.containsKey("word/_rels/document.xml.rels")) {
                existingEntries["word/_rels/document.xml.rels"] = buildDocRelsXml().toByteArray(StandardCharsets.UTF_8)
            }

            // Write out to file
            val tempFile = File(file.parentFile, "${file.name}.tmp")
            FileOutputStream(tempFile).use { fos ->
                ZipOutputStream(fos).use { zos ->
                    for ((entryName, bytes) in existingEntries) {
                        val entry = ZipEntry(entryName)
                        zos.putNextEntry(entry)
                        zos.write(bytes)
                        zos.closeEntry()
                    }
                }
            }

            if (tempFile.exists()) {
                if (file.exists()) file.delete()
                tempFile.renameTo(file)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun buildDocumentXml(paragraphs: List<String>): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
        sb.append("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" ")
        sb.append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">\n")
        sb.append("  <w:body>\n")

        for (p in paragraphs) {
            sb.append("    <w:p>\n")
            if (p.isNotEmpty()) {
                sb.append("      <w:r>\n")
                sb.append("        <w:t xml:space=\"preserve\">").append(escapeXml(p)).append("</w:t>\n")
                sb.append("      </w:r>\n")
            }
            sb.append("    </w:p>\n")
        }

        sb.append("    <w:sectPr>\n")
        sb.append("      <w:pgSz w:w=\"11906\" w:h=\"16838\"/>\n")
        sb.append("      <w:pgMar w:top=\"1440\" w:right=\"1440\" w:bottom=\"1440\" w:left=\"1440\" w:header=\"708\" w:footer=\"708\" w:gutter=\"0\"/>\n")
        sb.append("    </w:sectPr>\n")
        sb.append("  </w:body>\n")
        sb.append("</w:document>\n")
        return sb.toString()
    }

    private fun buildContentTypesXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>"""
    }

    private fun buildRootRelsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""
    }

    private fun buildDocRelsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"/>"""
    }
}
