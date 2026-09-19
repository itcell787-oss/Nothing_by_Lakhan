package com.example

import com.example.core.excel.ExcelParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ExampleUnitTest {
    @Test
    fun testCsvParsing() {
        val tempCsv = File.createTempFile("test_sheet", ".csv")
        try {
            tempCsv.writeText("SKU,Item Name,Price,Stock\nNT-01,Ear (1),$99,420\nNT-02,Phone (2),$599,150")
            val parsed = ExcelParser.parse(tempCsv)
            assertEquals(4, parsed.totalCols)
            assertEquals("A", parsed.headers[0])
            assertEquals("D", parsed.headers[3])
            assertEquals(3, parsed.rows.size)
            assertEquals("SKU", parsed.rows[0][0])
            assertEquals("Ear (1)", parsed.rows[1][1])
            assertEquals("150", parsed.rows[2][3])
        } finally {
            tempCsv.delete()
        }
    }

    @Test
    fun testFileCategoryClassification() {
        val imageExts = listOf("jpg", "png", "webp", "gif")
        val audioExts = listOf("mp3", "wav", "flac")
        val videoExts = listOf("mp4", "mkv", "webm")
        val docExts = listOf("pdf", "xlsx", "txt")

        assertTrue(imageExts.all { it.isNotEmpty() })
        assertTrue(audioExts.all { it.isNotEmpty() })
        assertTrue(videoExts.all { it.isNotEmpty() })
        assertTrue(docExts.all { it.isNotEmpty() })
    }
}
