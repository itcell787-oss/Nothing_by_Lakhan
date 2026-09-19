package com.example.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object SampleMediaGenerator {

    fun ensureSampleFiles(context: Context): File {
        val sampleDir = File(context.filesDir, "sample_media")
        if (!sampleDir.exists()) {
            sampleDir.mkdirs()
        }

        try {
            createSampleDocx(File(sampleDir, "Project_Specification.docx"))
            createSamplePdf(File(sampleDir, "Nothing_Manual.pdf"))
            createSampleXlsx(File(sampleDir, "Fiscal_Budget_2026.xlsx"))
            createSampleCsv(File(sampleDir, "System_Metrics.csv"))
            createSampleWav(File(sampleDir, "Nothing_Synth_Chime.wav"), 440.0)
            createSampleWav(File(sampleDir, "Ambient_Pulse.wav"), 330.0)
            createSampleImage(File(sampleDir, "Nothing_Wallpaper.png"), "NOTHING (R)", "EXPLORER OS // PHOTO VIEWER")
            createSampleImage(File(sampleDir, "Monochrome_Glyph.png"), "NOTHING GLYPH", "HARDWARE MATRIX // 1080P")
            createSampleImage(File(sampleDir, "DotMatrix_Dark.png"), "DOT MATRIX", "MINIMALIST SENSOR GRID")
            createSampleText(File(sampleDir, "Architecture_Notes.txt"))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return sampleDir
    }

    private fun createSamplePdf(file: File) {
        if (file.exists() && file.length() > 0) return
        val doc = PdfDocument()

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 14f
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
        }

        val redPaint = Paint().apply {
            color = Color.parseColor("#D71921")
            isAntiAlias = true
        }

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 22f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 12f
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
        }

        // Page 1
        val pageInfo1 = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page1 = doc.startPage(pageInfo1)
        val c1 = page1.canvas
        c1.drawColor(Color.WHITE)

        // Accent header
        c1.drawCircle(50f, 60f, 6f, redPaint)
        c1.drawText("NOTHING EXPLORER // v2.4", 66f, 66f, titlePaint)
        c1.drawText("SPECIFICATION & PROTOCOL DOCUMENTATION", 50f, 96f, subtitlePaint)
        c1.drawLine(50f, 110f, 545f, 110f, paint)

        var y = 140f
        val lines = listOf(
            "1.0 EXECUTIVE SUMMARY",
            "Nothing Explorer delivers advanced filesystem access with",
            "pure monochromatic aesthetics, root-level partition inspection,",
            "and real-time network and cloud storage mounting.",
            "",
            "2.0 ARCHITECTURAL SUBSYSTEMS",
            "- Local Storage: /storage/emulated/0 (Primary User Flash)",
            "- System Partition: /system, /vendor, /apex, /data (Direct ext4)",
            "- Network Protocol: SMB 2/3 (Port 445), RFC-959 FTP (Port 21)",
            "- Cloud Integration: Google Drive v3, Dropbox API v2, Microsoft Graph",
            "",
            "3.0 MULTI-FORMAT MEDIA PIPELINE",
            "The integrated media engine renders lossless previews for:",
            "[+] Photos & Vector Graphics (JPEG, PNG, WEBP, SVG)",
            "[+] Monospace Text & Configuration Files",
            "[+] Video Media & Streams (MP4, MKV, WEBM)",
            "[+] High-Fidelity Audio Tracks (WAV, MP3, FLAC)",
            "[+] Portable Document Format (PDF Multi-page Vector)",
            "[+] Office Spreadsheets (XLSX, XLS, CSV Tabular Grids)",
            "",
            "PAGE 01 / 02   ---   CONTINUED ON NEXT PAGE"
        )
        for (line in lines) {
            c1.drawText(line, 50f, y, paint)
            y += 24f
        }
        doc.finishPage(page1)

        // Page 2
        val pageInfo2 = PdfDocument.PageInfo.Builder(595, 842, 2).create()
        val page2 = doc.startPage(pageInfo2)
        val c2 = page2.canvas
        c2.drawColor(Color.WHITE)

        c2.drawCircle(50f, 60f, 6f, redPaint)
        c2.drawText("SECURITY & CLOUD VERIFICATION", 66f, 66f, titlePaint)
        c2.drawLine(50f, 85f, 545f, 85f, paint)

        y = 120f
        val lines2 = listOf(
            "4.0 CLOUD CREDENTIAL VERIFICATION",
            "When mounting external cloud storage, Nothing Explorer verifies",
            "OAuth Bearer tokens and REST endpoints in real-time over HTTPS.",
            "",
            "Verified Providers:",
            "  * Google Drive: https://www.googleapis.com/drive/v3",
            "  * Dropbox API:  https://api.dropboxapi.com/2",
            "  * MS OneDrive:  https://graph.microsoft.com/v1.0",
            "",
            "5.0 CRYPTOGRAPHIC CHECKSUM PROTOCOL",
            "Every file inspection automatically executes an MD5 / SHA-256",
            "hash verification to guarantee data integrity across boundaries.",
            "",
            "EOF - NOTHING TECHNOLOGY LTD."
        )
        for (line in lines2) {
            c2.drawText(line, 50f, y, paint)
            y += 24f
        }
        doc.finishPage(page2)

        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
    }

    private fun createSampleXlsx(file: File) {
        if (file.exists() && file.length() > 0) return

        val sharedStringsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="18" uniqueCount="18">
<si><t>RECORD ID</t></si>
<si><t>DEPARTMENT</t></si>
<si><t>PROJECT NAME</t></si>
<si><t>Q1 ALLOCATION ($)</t></si>
<si><t>STATUS</t></si>
<si><t>SYS-101</t></si>
<si><t>Hardware Engineering</t></si>
<si><t>Glyph Matrix Display</t></si>
<si><t>APPROVED</t></si>
<si><t>SYS-102</t></si>
<si><t>Firmware &amp; Kernel</t></si>
<si><t>Ext4 Root Subsystem</t></si>
<si><t>DEPLOYED</t></si>
<si><t>SYS-103</t></si>
<si><t>Cloud Services</t></si>
<si><t>Drive Synchronization</t></si>
<si><t>ACTIVE</t></si>
<si><t>SYS-104</t></si>
</sst>""".trimIndent()

        val sheet1Xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<sheetData>
<row r="1">
<c r="A1" t="s"><v>0</v></c>
<c r="B1" t="s"><v>1</v></c>
<c r="C1" t="s"><v>2</v></c>
<c r="D1" t="s"><v>3</v></c>
<c r="E1" t="s"><v>4</v></c>
</row>
<row r="2">
<c r="A2" t="s"><v>5</v></c>
<c r="B2" t="s"><v>6</v></c>
<c r="C2" t="s"><v>7</v></c>
<c r="D2"><v>145000</v></c>
<c r="E2" t="s"><v>8</v></c>
</row>
<row r="3">
<c r="A3" t="s"><v>9</v></c>
<c r="B3" t="s"><v>10</v></c>
<c r="C3" t="s"><v>11</v></c>
<c r="D3"><v>89500</v></c>
<c r="E3" t="s"><v>12</v></c>
</row>
<row r="4">
<c r="A4" t="s"><v>13</v></c>
<c r="B4" t="s"><v>14</v></c>
<c r="C4" t="s"><v>15</v></c>
<c r="D4"><v>230000</v></c>
<c r="E4" t="s"><v>16</v></c>
</row>
</sheetData>
</worksheet>""".trimIndent()

        val workbookXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets><sheet name="Budget Q1" sheetId="1" r:id="rId1"/></sheets>
</workbook>""".trimIndent()

        val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
<Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
</Types>""".trimIndent()

        val relsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""".trimIndent()

        val workbookRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
</Relationships>""".trimIndent()

        ZipOutputStream(FileOutputStream(file)).use { zos ->
            fun addEntry(name: String, content: String) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
            addEntry("[Content_Types].xml", contentTypesXml)
            addEntry("_rels/.rels", relsXml)
            addEntry("xl/workbook.xml", workbookXml)
            addEntry("xl/_rels/workbook.xml.rels", workbookRelsXml)
            addEntry("xl/sharedStrings.xml", sharedStringsXml)
            addEntry("xl/worksheets/sheet1.xml", sheet1Xml)
        }
    }

    private fun createSampleCsv(file: File) {
        if (file.exists() && file.length() > 0) return
        val csv = """
SERVER_NODE,IP_ADDRESS,PORT,LATENCY_MS,THROUGHPUT_MBPS,ENCRYPTION,HEALTH
node-alpha-1,10.0.1.15,445,1.2,950,TLS 1.3 / AES-256,OPTIMAL
node-alpha-2,10.0.1.16,445,1.8,910,TLS 1.3 / AES-256,OPTIMAL
ftp-backup-01,10.0.2.80,21,4.5,420,EXPLICIT_FTPS,SYNCING
cloud-gateway-1,192.168.100.1,443,12.4,1200,CHACHA20-POLY1305,VERIFIED
cloud-gateway-2,192.168.100.2,443,14.1,1180,CHACHA20-POLY1305,VERIFIED
""".trimIndent()
        file.writeText(csv)
    }

    private fun createSampleWav(file: File, baseFreq: Double = 440.0) {
        if (file.exists() && file.length() > 0) return
        val sampleRate = 44100
        val durationSeconds = 3
        val numSamples = sampleRate * durationSeconds
        val pcm = ShortArray(numSamples)

        val chordFreqs = doubleArrayOf(baseFreq, baseFreq * 1.2599, baseFreq * 1.4983)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val decay = Math.exp(-t * 1.5)
            var sampleVal = 0.0
            for (f in chordFreqs) {
                sampleVal += Math.sin(2.0 * Math.PI * f * t) * (1.0 / chordFreqs.size)
            }
            sampleVal *= (0.8 + 0.2 * Math.sin(2.0 * Math.PI * 6.0 * t)) * decay
            pcm[i] = (sampleVal * 32767.0 * 0.7).toInt().coerceIn(-32768, 32767).toShort()
        }

        // Write WAV RIFF header
        val byteDataSize = numSamples * 2
        val totalSize = 36 + byteDataSize

        val header = ByteBuffer.allocate(44).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            put("RIFF".toByteArray())
            putInt(totalSize)
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16) // Subchunk1Size
            putShort(1) // AudioFormat (1 = PCM)
            putShort(1) // NumChannels (1 = Mono)
            putInt(sampleRate)
            putInt(sampleRate * 2) // ByteRate
            putShort(2) // BlockAlign
            putShort(16) // BitsPerSample
            put("data".toByteArray())
            putInt(byteDataSize)
        }.array()

        val pcmBytes = ByteArray(byteDataSize)
        val pcmBuf = ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
        for (s in pcm) {
            pcmBuf.putShort(s)
        }

        FileOutputStream(file).use { fos ->
            fos.write(header)
            fos.write(pcmBytes)
        }
    }

    private fun createSampleImage(file: File, title: String = "NOTHING (R)", subtitle: String = "EXPLORER OS // PHOTO VIEWER") {
        if (file.exists() && file.length() > 0) return
        val width = 1080
        val height = 1080
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Monochromatic pitch black
        canvas.drawColor(Color.parseColor("#0A0A0A"))

        val redPaint = Paint().apply {
            color = Color.parseColor("#D71921")
            isAntiAlias = true
        }

        val whitePaint = Paint().apply {
            color = Color.WHITE
            textSize = 52f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }

        val grayPaint = Paint().apply {
            color = Color.parseColor("#7A7A7A")
            textSize = 26f
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
        }

        val borderPaint = Paint().apply {
            color = Color.parseColor("#262626")
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }

        // Draw outer frame
        canvas.drawRect(80f, 80f, width - 80f, height - 80f, borderPaint)

        // Draw dot matrix grid decorative accents
        val dotPaint = Paint().apply {
            color = Color.parseColor("#333333")
            isAntiAlias = true
        }
        for (x in 160..width - 160 step 60) {
            for (y in 160..height - 160 step 60) {
                canvas.drawCircle(x.toFloat(), y.toFloat(), 2.5f, dotPaint)
            }
        }

        // Central Monolith
        val cardPaint = Paint().apply {
            color = Color.parseColor("#121212")
            isAntiAlias = true
        }
        canvas.drawRoundRect(220f, 320f, width - 220f, height - 320f, 32f, 32f, cardPaint)
        canvas.drawRoundRect(220f, 320f, width - 220f, height - 320f, 32f, 32f, borderPaint)

        // Signature red glyph dot
        canvas.drawCircle(300f, 420f, 16f, redPaint)

        canvas.drawText(title, 340f, 432f, whitePaint)
        canvas.drawText(subtitle, 300f, 500f, grayPaint)
        canvas.drawText("1080 x 1080 PX  •  24-BIT RGB", 300f, 550f, grayPaint)
        canvas.drawText("SWIPE LEFT/RIGHT FOR NEXT/PREV", 300f, 600f, grayPaint)

        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
    }

    private fun createSampleText(file: File) {
        if (file.exists() && file.length() > 0) return
        val text = """
[NOTHING EXPLORER // SYSTEM SPECIFICATION]
=========================================
OS INTEGRATION   : Android Modern HAL
STORAGE DRIVERS  : ext4, f2fs, vfat, fuse, cifs, ftpfs
PARTITION ACCESS : System, Vendor, Data, Apex, Media
UI PHILOSOPHY    : Monochromatic, High-Density, Zero Bloat
GLYPH INDICATOR  : Active on read/write I/O operations

SUPPORTED PROTOCOLS:
1. Local Flash (Scoped & All-Files Management)
2. Windows SMB / Samba Shares (Port 445 / 139)
3. RFC-959 FTP Servers with User Auth
4. Google Drive v3 REST Engine
5. Dropbox API v2 Integration
6. Microsoft OneDrive Graph API

FILE VIEWING CAPABILITIES:
- Fullscreen Photo Canvas with High-Resolution Rendering
- Interactive Monospace Text & Code Inspector
- Hardware Accelerated Video Playback
- Monochromatic Audio Player with Real-Time Waveform
- Native Vector PDF Page Renderer (PdfRenderer HAL)
- Tabular Spreadsheet Viewer (XLSX, XLS, CSV, TSV)

(C) NOTHING TECHNOLOGY LIMITED - CONFIDENTIAL
""".trimIndent()
        file.writeText(text)
    }

    private fun createSampleDocx(file: File) {
        if (file.exists() && file.length() > 0) return
        val paragraphs = listOf(
            "# NOTHING EXPLORER // PROJECT SPECIFICATION",
            "This document outlines the architecture, capabilities, and system specifications of Nothing Explorer.",
            "## 1. Executive Summary",
            "Nothing Explorer is a high-performance, dark-mode file manager built around modern Android storage frameworks and the iconic Nothing glyph and dot-matrix visual identity.",
            "## 2. Core Capabilities",
            "- Unified Storage Management: Access device internal storage, SD cards, and root partitions seamlessly.",
            "- In-App Document Suite: High-speed previewers for PDF documents, Word documents (.docx), Excel spreadsheets (.xlsx, .csv), photos, audio waveforms, and video.",
            "- Word Document (.docx) Engine: Full support for OpenXML document viewing, live heading hierarchy navigation, real-time search, word/char metrics, and interactive text editing with persistent zip-repacking.",
            "- Universal Sharing: Seamless single and multi-file sharing to WhatsApp, Nearby Share, Telegram, and Drive via native Android Sharesheet.",
            "- External App Dispatch: One-touch 'Open with...' integration to launch external viewers such as Microsoft Office, Google Docs, or VLC.",
            "- Background Audio: System media session with lockscreen player, notification controls, and audio focus management.",
            "## 3. Security & Safe Folder",
            "The Safe Folder encrypts sensitive files using hardware-backed keystore credentials, ensuring complete privacy even on shared hardware.",
            "## 4. Conclusion",
            "Engineered for speed, clarity, and precision."
        )
        com.example.core.docx.DocxManager.saveDocx(file, paragraphs)
    }
}
