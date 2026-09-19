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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.FileItem
import com.example.core.FileShareHelper
import com.example.core.docx.DocxDocumentData
import com.example.core.docx.DocxManager
import com.example.ui.components.NothingBadge
import com.example.ui.components.NothingButton
import com.example.ui.components.nothingTextFieldColors
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingDark
import com.example.ui.theme.NothingGray
import com.example.ui.theme.NothingGreen
import com.example.ui.theme.NothingLightGray
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingSurface
import com.example.ui.theme.NothingWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun DocxViewer(
    file: FileItem,
    onContentSaved: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var docData by remember { mutableStateOf<DocxDocumentData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isEditMode by remember { mutableStateOf(false) }
    var editedText by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager }

    fun loadDocument() {
        isLoading = true
        coroutineScope.launch {
            val data = withContext(Dispatchers.IO) {
                DocxManager.readDocx(File(file.path))
            }
            docData = data
            editedText = data.paragraphs.joinToString("\n\n") { it.text }
            isLoading = false
        }
    }

    LaunchedEffect(file.path) {
        loadDocument()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NothingBlack)
            .padding(8.dp)
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(NothingDark)
                .border(1.dp, NothingBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = "Word Docx",
                    tint = Color(0xFF2B579A), // Word Blue Accent
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = file.name.uppercase(),
                        color = NothingWhite,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    docData?.let { data ->
                        Text(
                            text = "${data.paragraphs.size} PARAGRAPHS  //  ${data.wordCount} WORDS",
                            color = NothingGray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Share File Option (WhatsApp, Nearby Share, etc.)
                IconButton(
                    onClick = {
                        FileShareHelper.shareFile(context, file)
                    },
                    modifier = Modifier.size(32.dp).testTag("docx_share_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Document",
                        tint = NothingWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Open with External App (Microsoft Word, Google Docs, etc.)
                IconButton(
                    onClick = {
                        FileShareHelper.openFileWithExternalApp(context, file)
                    },
                    modifier = Modifier.size(32.dp).testTag("docx_open_with_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open With App",
                        tint = NothingWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Search in Docx
                if (!isEditMode) {
                    IconButton(
                        onClick = { isSearchVisible = !isSearchVisible },
                        modifier = Modifier.size(32.dp).testTag("docx_search_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (isSearchVisible) NothingRed else NothingLightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Copy Document Content Button
                IconButton(
                    onClick = {
                        val textToCopy = if (isEditMode) editedText else docData?.paragraphs?.joinToString("\n\n") { it.text } ?: ""
                        if (textToCopy.isNotBlank()) {
                            clipboardManager?.setPrimaryClip(ClipData.newPlainText(file.name, textToCopy))
                            Toast.makeText(context, "Copied document text to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(32.dp).testTag("docx_copy_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Text",
                        tint = NothingLightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Direct Paste from clipboard in all modes
                IconButton(
                    onClick = {
                        val clip = clipboardManager?.primaryClip
                        if (clip != null && clip.itemCount > 0) {
                            val pasteText = clip.getItemAt(0).coerceToText(context).toString()
                            if (!isEditMode) {
                                isEditMode = true
                            }
                            editedText = if (editedText.isBlank()) pasteText else "$editedText\n\n$pasteText"
                            Toast.makeText(context, "Pasted text directly from clipboard", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(32.dp).testTag("docx_paste_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Paste Text",
                        tint = NothingLightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Mode Switch: View vs Edit
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isEditMode) NothingRed else NothingSurface)
                        .border(1.dp, if (isEditMode) NothingRed else NothingBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            if (isEditMode) {
                                isEditMode = false
                            } else {
                                isEditMode = true
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("docx_toggle_edit_btn")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isEditMode) Icons.Default.Visibility else Icons.Default.Edit,
                            contentDescription = if (isEditMode) "View Mode" else "Edit Mode",
                            tint = NothingWhite,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isEditMode) "VIEW" else "EDIT",
                            color = NothingWhite,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }

                // Save button if in edit mode
                if (isEditMode) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NothingGreen)
                            .clickable(enabled = !isSaving) {
                                isSaving = true
                                coroutineScope.launch {
                                    val paragraphsList = editedText.split("\n\n", "\n")
                                    val success = withContext(Dispatchers.IO) {
                                        DocxManager.saveDocx(File(file.path), paragraphsList)
                                    }
                                    isSaving = false
                                    if (success) {
                                        Toast.makeText(context, "Document saved successfully!", Toast.LENGTH_SHORT).show()
                                        isEditMode = false
                                        loadDocument()
                                        onContentSaved?.invoke()
                                    } else {
                                        Toast.makeText(context, "Error saving document", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("docx_save_btn")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSaving) {
                                CircularProgressIndicator(color = NothingBlack, modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = "Save",
                                    tint = NothingBlack,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SAVE",
                                color = NothingBlack,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Search Bar if active
        if (isSearchVisible && !isEditMode) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text("Search in document...", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                singleLine = true,
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                colors = nothingTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("docx_search_input")
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Main Document Content Box (White background by default for authentic document reading)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = NothingRed, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("PARSING WORD DOCUMENT (.DOCX)...", fontFamily = FontFamily.Monospace, color = Color(0xFF64748B), fontSize = 11.sp)
                }
            } else if (isEditMode) {
                // Multi-line editor
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "// WORD DOCUMENT EDITOR (LIVE OOXML)",
                            color = NothingRed,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "SEPARATE PARAGRAPHS WITH BLANK LINES",
                            color = Color(0xFF64748B),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    TextField(
                        value = editedText,
                        onValueChange = { editedText = it },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("docx_edit_textarea"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF1E293B),
                            cursorColor = NothingRed
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    )
                }
            } else {
                // Formatted Reading View
                val paragraphs = docData?.paragraphs ?: emptyList()
                val filteredParagraphs = if (searchQuery.isBlank()) {
                    paragraphs
                } else {
                    paragraphs.filter { it.text.contains(searchQuery, ignoreCase = true) }
                }

                if (filteredParagraphs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "NO MATCHES FOUND FOR \"$searchQuery\"" else "[ Empty Document ]",
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                    }
                } else {
                    SelectionContainer {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("docx_paragraph_list"),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(filteredParagraphs) { index, para ->
                                val isHeading = para.headingLevel > 0
                                val fontSize = when (para.headingLevel) {
                                    1 -> 16.sp
                                    2 -> 14.sp
                                    3 -> 13.sp
                                    else -> 12.sp
                                }

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    if (isHeading) {
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(20.dp)
                                                .background(NothingRed)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }

                                    Text(
                                        text = para.text.ifEmpty { " " },
                                        color = if (isHeading) Color(0xFF0F172A) else Color(0xFF1E293B),
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = if (para.isBold || isHeading) FontWeight.Bold else FontWeight.Normal,
                                        fontStyle = if (para.isItalic) FontStyle.Italic else FontStyle.Normal,
                                        fontSize = fontSize,
                                        lineHeight = (fontSize.value * 1.5).sp
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
