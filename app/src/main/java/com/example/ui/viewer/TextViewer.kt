package com.example.ui.viewer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.FileItem
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
fun TextViewer(
    file: FileItem,
    content: String,
    modifier: Modifier = Modifier,
    onContentSaved: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    var isEditing by remember { mutableStateOf(false) }
    var currentContent by remember(content) { mutableStateOf(content) }
    var editorValue by remember(currentContent) {
        mutableStateOf(TextFieldValue(currentContent, TextRange(currentContent.length)))
    }
    var hasUnsavedChanges by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchOpen by remember { mutableStateOf(false) }

    val lines = remember(if (isEditing) editorValue.text else currentContent) {
        (if (isEditing) editorValue.text else currentContent).lines()
    }
    val totalChars = (if (isEditing) editorValue.text else currentContent).length
    val totalWords = remember(if (isEditing) editorValue.text else currentContent) {
        (if (isEditing) editorValue.text else currentContent).split("\\s+".toRegex()).count { it.isNotBlank() }
    }

    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()

    Column(modifier = modifier.fillMaxSize()) {
        // Toolbar with Stats and Actions (Scrollable to prevent button overflow on compact screens)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${lines.size} LINES • $totalWords WORDS • $totalChars CHARS" + if (hasUnsavedChanges) " • MODIFIED" else "",
                color = if (hasUnsavedChanges) NothingRed else NothingGray,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.weight(1f, fill = false)
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEditing) {
                    // Paste from clipboard button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NothingDark)
                            .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                val clip = clipboardManager.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    val pasteText = clip.getItemAt(0).text?.toString() ?: ""
                                    val currentText = editorValue.text
                                    val start = editorValue.selection.min.coerceIn(0, currentText.length)
                                    val end = editorValue.selection.max.coerceIn(0, currentText.length)
                                    val newText = currentText.substring(0, start) + pasteText + currentText.substring(end)
                                    val newCursor = start + pasteText.length
                                    editorValue = TextFieldValue(newText, TextRange(newCursor))
                                    hasUnsavedChanges = true
                                    Toast.makeText(context, "Pasted text", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("text_editor_paste_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = NothingWhite, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PASTE", color = NothingWhite, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    // Select All button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NothingDark)
                            .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                editorValue = editorValue.copy(selection = TextRange(0, editorValue.text.length))
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("text_editor_select_all_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.SelectAll, contentDescription = "Select All", tint = NothingWhite, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ALL", color = NothingWhite, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    // Save Button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (hasUnsavedChanges) NothingRed else NothingGreen)
                            .clickable {
                                try {
                                    val fileOnDisk = File(file.path)
                                    fileOnDisk.writeText(editorValue.text)
                                    currentContent = editorValue.text
                                    hasUnsavedChanges = false
                                    isEditing = false
                                    onContentSaved?.invoke(editorValue.text)
                                    Toast.makeText(context, "Saved changes to ${file.name}", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                            .testTag("text_editor_save_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save", tint = NothingWhite, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SAVE", color = NothingWhite, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    // Cancel editing
                    IconButton(
                        onClick = {
                            editorValue = TextFieldValue(currentContent, TextRange(currentContent.length))
                            hasUnsavedChanges = false
                            isEditing = false
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel Edit", tint = NothingGray, modifier = Modifier.size(16.dp))
                    }
                } else {
                    // View Mode Actions: Edit, Search, Copy All
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NothingDark)
                            .border(1.dp, NothingRed, RoundedCornerShape(8.dp))
                            .clickable {
                                editorValue = TextFieldValue(currentContent, TextRange(currentContent.length))
                                hasUnsavedChanges = false
                                isEditing = true
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("text_viewer_edit_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = NothingRed, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("EDIT", color = NothingWhite, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = { isSearchOpen = !isSearchOpen },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search text",
                            tint = if (isSearchOpen) NothingRed else NothingWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            clipboardManager.setPrimaryClip(ClipData.newPlainText(file.name, currentContent))
                            Toast.makeText(context, "Copied all text to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy all",
                            tint = NothingWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        if (isSearchOpen && !isEditing) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter lines...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) },
                singleLine = true,
                colors = nothingTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }

        // Code/Text Inspector & Editor Box
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(NothingDark)
                .border(1.dp, if (isEditing) NothingRed else NothingBorder, RoundedCornerShape(12.dp))
                .padding(10.dp)
        ) {
            if (isEditing) {
                // Interactive Editable Text Area with selection & copy/paste gestures
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(verticalScroll)
                ) {
                    // Line numbers gutter
                    Column(
                        modifier = Modifier.padding(end = 12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        lines.forEachIndexed { index, _ ->
                            Text(
                                text = String.format("%02d", index + 1),
                                color = NothingGray,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    // Vertical Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height((lines.size.coerceAtLeast(1) * 20).dp)
                            .background(NothingBorder)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // BasicTextField with selection, cursor, cut, copy, paste
                    BasicTextField(
                        value = editorValue,
                        onValueChange = { newVal ->
                            if (newVal.text != editorValue.text) {
                                hasUnsavedChanges = true
                            }
                            editorValue = newVal
                        },
                        textStyle = TextStyle(
                            color = NothingWhite,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 20.sp
                        ),
                        cursorBrush = SolidColor(NothingRed),
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(horizontalScroll)
                            .testTag("text_editor_input_field")
                    )
                }
            } else {
                // View Mode with Native Gesture Text Selection & Copy
                SelectionContainer {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(verticalScroll)
                    ) {
                        // Line Numbers Gutter
                        Column(
                            modifier = Modifier.padding(end = 12.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            lines.forEachIndexed { index, line ->
                                if (searchQuery.isBlank() || line.contains(searchQuery, ignoreCase = true)) {
                                    Text(
                                        text = String.format("%02d", index + 1),
                                        color = NothingGray,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }

                        // Vertical Divider Line
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height((lines.size.coerceAtLeast(1) * 18).dp)
                                .background(NothingBorder)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // Text Lines Viewport
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(horizontalScroll)
                        ) {
                            lines.forEach { line ->
                                if (searchQuery.isBlank() || line.contains(searchQuery, ignoreCase = true)) {
                                    val isMatch = searchQuery.isNotBlank() && line.contains(searchQuery, ignoreCase = true)
                                    Text(
                                        text = if (line.isEmpty()) " " else line,
                                        color = if (isMatch) NothingWhite else NothingLightGray,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 18.sp
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
