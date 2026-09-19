package com.example.ui.viewer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.core.FileItem
import com.example.core.excel.ExcelParser
import com.example.core.excel.ExcelSheetData
import com.example.ui.components.nothingTextFieldColors
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingDark
import com.example.ui.theme.NothingGray
import com.example.ui.theme.NothingGreen
import com.example.ui.theme.NothingLightGray
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import java.io.File

@Composable
fun ExcelViewer(
    file: FileItem,
    data: ExcelSheetData?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var isSearchOpen by remember { mutableStateOf(false) }

    // Editable Grid State
    var headers by remember(data) {
        mutableStateOf(data?.headers ?: listOf("A", "B", "C"))
    }
    var rows by remember(data) {
        mutableStateOf(data?.rows?.map { it.toMutableList() }?.toMutableList() ?: mutableListOf(
            mutableListOf("", "", "")
        ))
    }

    var selectedRowIndex by remember { mutableStateOf<Int?>(null) }
    var selectedColIndex by remember { mutableStateOf<Int?>(null) }
    var selectedCellCoord by remember { mutableStateOf<String?>(null) }
    var cellEditValue by remember { mutableStateOf("") }
    var isEditingInBar by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showFunctionMenu by remember { mutableStateOf(false) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }

    // Gesture Zoom State (Pinch to zoom in / out)
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    val transformState = rememberTransformableState { zoomChange, _, _ ->
        zoomScale = (zoomScale * zoomChange).coerceIn(0.5f, 2.5f)
    }

    val horizontalScroll = rememberScrollState()

    // Sync selected cell value when selection changes
    fun selectCell(rowIndex: Int, colIndex: Int) {
        selectedRowIndex = rowIndex
        selectedColIndex = colIndex
        val colHeader = headers.getOrNull(colIndex) ?: ExcelParser.generateColumnHeader(colIndex)
        selectedCellCoord = "$colHeader${rowIndex + 1}"
        val curVal = rows.getOrNull(rowIndex)?.getOrNull(colIndex) ?: ""
        cellEditValue = curVal
        isEditingInBar = false
    }

    fun commitCellEdit(newVal: String) {
        val r = selectedRowIndex ?: return
        val c = selectedColIndex ?: return
        val evaluated = if (newVal.startsWith("=")) {
            ExcelParser.evaluateFormula(newVal, headers, rows)
        } else {
            newVal
        }
        val newRows = rows.mapIndexed { ri, rowList ->
            if (ri == r) {
                val updated = rowList.toMutableList()
                while (updated.size <= c) updated.add("")
                updated[c] = evaluated
                updated
            } else {
                rowList.toMutableList()
            }
        }.toMutableList()
        rows = newRows
        cellEditValue = evaluated
        isEditingInBar = false
        hasUnsavedChanges = true
    }

    fun addRow() {
        val newRow = MutableList(headers.size) { "" }
        val newRows = rows.toMutableList()
        newRows.add(newRow)
        rows = newRows
        hasUnsavedChanges = true
        selectCell(rows.size - 1, 0)
        Toast.makeText(context, "Row ${rows.size} added", Toast.LENGTH_SHORT).show()
    }

    fun addColumn() {
        val newColHeader = ExcelParser.generateColumnHeader(headers.size)
        val newHeaders = headers.toMutableList()
        newHeaders.add(newColHeader)
        headers = newHeaders

        val newRows = rows.map { row ->
            val updated = row.toMutableList()
            updated.add("")
            updated
        }.toMutableList()
        rows = newRows
        hasUnsavedChanges = true
        Toast.makeText(context, "Column $newColHeader added", Toast.LENGTH_SHORT).show()
    }

    fun deleteCurrentRow() {
        val r = selectedRowIndex ?: return
        if (rows.size <= 1) {
            Toast.makeText(context, "Cannot delete last remaining row", Toast.LENGTH_SHORT).show()
            return
        }
        val newRows = rows.toMutableList()
        newRows.removeAt(r)
        rows = newRows
        selectedRowIndex = (r - 1).coerceAtLeast(0)
        selectCell(selectedRowIndex ?: 0, selectedColIndex ?: 0)
        hasUnsavedChanges = true
        Toast.makeText(context, "Row deleted", Toast.LENGTH_SHORT).show()
    }

    fun saveSpreadsheet() {
        val targetFile = File(file.path)
        val result = ExcelParser.saveSheet(targetFile, headers, rows)
        if (result.isSuccess) {
            hasUnsavedChanges = false
            Toast.makeText(context, "Spreadsheet saved successfully!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Failed to save: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Apply quick math functions
    fun applyFunction(func: String) {
        val r = selectedRowIndex ?: 0
        val c = selectedColIndex ?: 0
        val colHeader = headers.getOrNull(c) ?: "A"
        val formula = when (func) {
            "SUM" -> "=SUM(${colHeader}1:${colHeader}${rows.size.coerceAtLeast(1)})"
            "AVG" -> "=AVERAGE(${colHeader}1:${colHeader}${rows.size.coerceAtLeast(1)})"
            "COUNT" -> "=COUNT(${colHeader}1:${colHeader}${rows.size.coerceAtLeast(1)})"
            "MIN" -> "=MIN(${colHeader}1:${colHeader}${rows.size.coerceAtLeast(1)})"
            "MAX" -> "=MAX(${colHeader}1:${colHeader}${rows.size.coerceAtLeast(1)})"
            "CLEAR" -> ""
            else -> ""
        }
        commitCellEdit(formula)
        showFunctionMenu = false
    }

    // Filter rows based on search query
    val filteredRowIndices = remember(rows, searchQuery) {
        if (searchQuery.isBlank()) {
            rows.indices.toList()
        } else {
            rows.indices.filter { idx ->
                rows[idx].any { it.contains(searchQuery, ignoreCase = true) }
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${rows.size} ROWS • ${headers.size} COLS",
                    color = NothingGray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
                if (hasUnsavedChanges) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NothingRed)
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text("EDITED", color = NothingWhite, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Save Button
                IconButton(
                    onClick = { saveSpreadsheet() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save Sheet",
                        tint = if (hasUnsavedChanges) NothingGreen else NothingWhite,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Add Row
                IconButton(
                    onClick = { addRow() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Row",
                        tint = NothingWhite,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Functions Menu Toggle
                IconButton(
                    onClick = { showFunctionMenu = !showFunctionMenu },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Functions,
                        contentDescription = "Functions",
                        tint = if (showFunctionMenu) NothingRed else NothingWhite,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Zoom Out
                IconButton(
                    onClick = { zoomScale = (zoomScale - 0.15f).coerceAtLeast(0.5f) },
                    modifier = Modifier.size(28.dp).testTag("excel_zoom_out")
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOut,
                        contentDescription = "Zoom Out",
                        tint = NothingLightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Zoom percentage badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NothingDark)
                        .border(1.dp, NothingBorder, RoundedCornerShape(6.dp))
                        .clickable { zoomScale = 1.0f }
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${(zoomScale * 100).toInt()}%",
                        color = if (zoomScale != 1.0f) NothingRed else NothingWhite,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }

                // Zoom In
                IconButton(
                    onClick = { zoomScale = (zoomScale + 0.15f).coerceAtMost(2.5f) },
                    modifier = Modifier.size(28.dp).testTag("excel_zoom_in")
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Zoom In",
                        tint = NothingLightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Direct Paste to Active Cell or New Row
                IconButton(
                    onClick = {
                        val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val pClip = clip?.primaryClip
                        if (pClip != null && pClip.itemCount > 0) {
                            val text = pClip.getItemAt(0).coerceToText(context).toString()
                            if (selectedRowIndex != null && selectedColIndex != null) {
                                commitCellEdit(text)
                                Toast.makeText(context, "Pasted '$text' into $selectedCellCoord", Toast.LENGTH_SHORT).show()
                            } else {
                                val newRow = MutableList(headers.size) { "" }
                                newRow[0] = text
                                val newRows = rows.toMutableList()
                                newRows.add(newRow)
                                rows = newRows
                                hasUnsavedChanges = true
                                selectCell(rows.size - 1, 0)
                                Toast.makeText(context, "Pasted into new row $selectedCellCoord", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(30.dp).testTag("excel_toolbar_paste")
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste Clipboard", tint = NothingLightGray, modifier = Modifier.size(16.dp))
                }

                // Search Toggle
                IconButton(
                    onClick = { isSearchOpen = !isSearchOpen },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (isSearchOpen) NothingRed else NothingWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // External App
                IconButton(
                    onClick = {
                        try {
                            val f = File(file.path)
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, if (file.extension.lowercase() == "csv") "text/comma-separated-values" else "application/vnd.ms-excel")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Open with Office App"))
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = "Open App", tint = NothingWhite, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Functions Quick Bar
        if (showFunctionMenu) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("SUM", "AVG", "COUNT", "MIN", "MAX", "CLEAR").forEach { fn ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NothingDark)
                            .border(1.dp, NothingRed, RoundedCornerShape(6.dp))
                            .clickable { applyFunction(fn) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "fx $fn",
                            color = NothingWhite,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NothingDark)
                        .border(1.dp, NothingBorder, RoundedCornerShape(6.dp))
                        .clickable { addColumn() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("+ COL", color = NothingLightGray, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
                if (selectedRowIndex != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NothingDark)
                            .border(1.dp, NothingRed, RoundedCornerShape(6.dp))
                            .clickable { deleteCurrentRow() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("- DEL ROW", color = NothingRed, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    }
                }
            }
        }

        if (isSearchOpen) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search spreadsheet values...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) },
                singleLine = true,
                colors = nothingTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )
        }

        // Active Cell Formula / Value Editor Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(NothingDark)
                .border(1.dp, if (selectedCellCoord != null) NothingRed else NothingBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "[${selectedCellCoord ?: "fx"}]",
                color = NothingRed,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier.padding(end = 6.dp)
            )

            OutlinedTextField(
                value = cellEditValue,
                onValueChange = {
                    cellEditValue = it
                    isEditingInBar = true
                },
                placeholder = { Text(if (selectedCellCoord == null) "Select cell to edit" else "Enter value or =SUM(...)", color = NothingLightGray, fontSize = 11.sp) },
                singleLine = true,
                enabled = selectedCellCoord != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = NothingWhite,
                    unfocusedTextColor = NothingWhite,
                    disabledTextColor = NothingGray,
                    focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                    disabledBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                    cursorColor = NothingRed
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            )

            if (selectedCellCoord != null) {
                IconButton(
                    onClick = { commitCellEdit(cellEditValue) },
                    modifier = Modifier.size(28.dp).testTag("excel_commit_cell")
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Commit", tint = NothingGreen, modifier = Modifier.size(16.dp))
                }
                IconButton(
                    onClick = {
                        val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clip?.setPrimaryClip(ClipData.newPlainText("Cell $selectedCellCoord", cellEditValue))
                        Toast.makeText(context, "Copied cell to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(28.dp).testTag("excel_copy_cell")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Cell", tint = NothingLightGray, modifier = Modifier.size(14.dp))
                }
                IconButton(
                    onClick = {
                        val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val pClip = clip?.primaryClip
                        if (pClip != null && pClip.itemCount > 0) {
                            val text = pClip.getItemAt(0).coerceToText(context).toString()
                            cellEditValue = text
                            commitCellEdit(text)
                            Toast.makeText(context, "Pasted into cell", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(28.dp).testTag("excel_paste_cell")
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste into Cell", tint = NothingLightGray, modifier = Modifier.size(14.dp))
                }
                IconButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.size(28.dp).testTag("excel_full_edit_cell")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Full Edit", tint = NothingLightGray, modifier = Modifier.size(15.dp))
                }
            }
        }

        // Scaled dimensions based on gesture zoomScale
        val scaledColWidth = (130.dp * zoomScale).coerceAtLeast(60.dp)
        val scaledIndexWidth = (44.dp * zoomScale).coerceAtLeast(28.dp)
        val scaledRowHeight = (34.dp * zoomScale).coerceAtLeast(24.dp)
        val scaledHeaderHeight = (36.dp * zoomScale).coerceAtLeast(26.dp)
        val scaledTextFontSize = (11f * zoomScale).coerceIn(8f, 22f).sp
        val scaledHeaderFontSize = (11f * zoomScale).coerceIn(8f, 22f).sp
        val scaledIndexFontSize = (10f * zoomScale).coerceIn(7f, 20f).sp

        // Table Grid with pinch-to-zoom gesture & text selection
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(androidx.compose.ui.graphics.Color.White)
                .border(1.dp, androidx.compose.ui.graphics.Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                .transformable(state = transformState)
                .horizontalScroll(horizontalScroll)
        ) {
            SelectionContainer {
                Column {
                    // Table Header Row
                    Row(
                        modifier = Modifier
                            .background(androidx.compose.ui.graphics.Color(0xFFF1F5F9))
                            .border(BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFFCBD5E1)))
                    ) {
                        Box(
                            modifier = Modifier
                                .width(scaledIndexWidth)
                                .height(scaledHeaderHeight)
                                .border(BorderStroke(0.5.dp, androidx.compose.ui.graphics.Color(0xFFCBD5E1))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("#", color = androidx.compose.ui.graphics.Color(0xFF64748B), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = scaledIndexFontSize)
                        }

                        headers.forEach { header ->
                            Box(
                                modifier = Modifier
                                    .width(scaledColWidth)
                                    .height(scaledHeaderHeight)
                                    .border(BorderStroke(0.5.dp, androidx.compose.ui.graphics.Color(0xFFCBD5E1)))
                                    .padding(horizontal = (8.dp * zoomScale).coerceAtLeast(4.dp)),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = header,
                                    color = androidx.compose.ui.graphics.Color(0xFF0F172A),
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = scaledHeaderFontSize,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Table Rows
                    LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                        itemsIndexed(filteredRowIndices) { _, originalRowIndex ->
                            val row = rows.getOrNull(originalRowIndex) ?: emptyList()
                            Row(
                                modifier = Modifier
                                    .border(BorderStroke(0.5.dp, androidx.compose.ui.graphics.Color(0xFFE2E8F0)))
                                    .background(if (originalRowIndex % 2 == 0) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color(0xFFF8FAFC))
                            ) {
                                // Row Number index
                                Box(
                                    modifier = Modifier
                                        .width(scaledIndexWidth)
                                        .height(scaledRowHeight)
                                        .background(androidx.compose.ui.graphics.Color(0xFFF1F5F9))
                                        .border(BorderStroke(0.5.dp, androidx.compose.ui.graphics.Color(0xFFCBD5E1))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${originalRowIndex + 1}",
                                        color = androidx.compose.ui.graphics.Color(0xFF64748B),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = scaledIndexFontSize
                                    )
                                }

                                // Cell Values with direct copy & paste
                                headers.indices.forEach { colIndex ->
                                    val cellText = row.getOrNull(colIndex) ?: ""
                                    val isSelected = selectedRowIndex == originalRowIndex && selectedColIndex == colIndex

                                    @OptIn(ExperimentalFoundationApi::class)
                                    Box(
                                        modifier = Modifier
                                            .width(scaledColWidth)
                                            .height(scaledRowHeight)
                                            .background(if (isSelected) androidx.compose.ui.graphics.Color(0xFFE0E7FF) else (if (originalRowIndex % 2 == 0) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color(0xFFF8FAFC)))
                                            .border(BorderStroke(0.5.dp, if (isSelected) NothingRed else androidx.compose.ui.graphics.Color(0xFFCBD5E1)))
                                            .combinedClickable(
                                                onClick = {
                                                    if (isSelected) {
                                                        showEditDialog = true
                                                    } else {
                                                        selectCell(originalRowIndex, colIndex)
                                                    }
                                                },
                                                onLongClick = {
                                                    selectCell(originalRowIndex, colIndex)
                                                    if (cellText.isNotBlank()) {
                                                        val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                        clip?.setPrimaryClip(ClipData.newPlainText("Cell ${selectedCellCoord ?: ""}", cellText))
                                                        Toast.makeText(context, "Copied '$cellText' to clipboard", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        showEditDialog = true
                                                    }
                                                }
                                            )
                                            .padding(horizontal = (8.dp * zoomScale).coerceAtLeast(4.dp)),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = cellText,
                                            color = if (isSelected) NothingRed else androidx.compose.ui.graphics.Color(0xFF0F172A),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = scaledTextFontSize,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Edit Cell Dialog
    if (showEditDialog && selectedCellCoord != null) {
        var tempValue by remember { mutableStateOf(cellEditValue) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = "Edit Cell [${selectedCellCoord}]",
                    color = NothingWhite,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = tempValue,
                        onValueChange = { tempValue = it },
                        label = { Text("Cell Value or Formula", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        placeholder = { Text("e.g. 250, Text, or =SUM(A1:A5)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        colors = nothingTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Supported formulas: =SUM(A1:A5), =AVG(A1:A5), =A1+B1, =A1*10",
                        color = NothingGray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        commitCellEdit(tempValue)
                        showEditDialog = false
                    }
                ) {
                    Text("DONE", color = NothingRed, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("CANCEL", color = NothingGray, fontFamily = FontFamily.Monospace)
                }
            },
            containerColor = NothingBlack
        )
    }
}
