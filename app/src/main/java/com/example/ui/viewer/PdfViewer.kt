package com.example.ui.viewer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.core.FileItem
import com.example.core.PdfTextExtractor
import com.example.ui.components.NothingBadge
import com.example.ui.components.NothingButton
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingDark
import com.example.ui.theme.NothingGray
import com.example.ui.theme.NothingGreen
import com.example.ui.theme.NothingLightGray
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

enum class PdfDisplayMode {
    PAGE_RENDER,
    TEXT_SELECTION
}

@Composable
fun PdfViewer(
    file: FileItem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var totalPages by remember { mutableIntStateOf(1) }
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var displayMode by remember { mutableStateOf(PdfDisplayMode.PAGE_RENDER) }
    var extractedText by remember { mutableStateOf("") }
    var isExtractingText by remember { mutableStateOf(false) }

    // Adobe Acrobat Reader Features:
    // 1. Night Mode (Color Inversion)
    var isNightMode by remember { mutableStateOf(false) }

    // 2. In-PDF Text Search
    var isSearchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchMatches by remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentMatchIndex by remember { mutableIntStateOf(0) }

    // 3. Bookmarks
    var bookmarkedPages by remember { mutableStateOf<Set<Int>>(emptySet()) }

    // 4. Zoom & Pan
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.8f, 4f)
        offset += offsetChange
    }

    // Invert Color Matrix for Night Mode reading (White backgrounds -> Black, Black text -> White)
    val invertColorMatrix = remember {
        ColorMatrix(
            floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    // Extract PDF text
    LaunchedEffect(file.path) {
        isExtractingText = true
        val text = PdfTextExtractor.extractText(File(file.path))
        extractedText = text
        isExtractingText = false
    }

    // Search Query Matching inside PDF
    LaunchedEffect(searchQuery, extractedText) {
        if (searchQuery.length >= 2 && extractedText.isNotBlank()) {
            val matches = mutableListOf<Int>()
            var idx = extractedText.indexOf(searchQuery, 0, ignoreCase = true)
            while (idx >= 0) {
                matches.add(idx)
                idx = extractedText.indexOf(searchQuery, idx + searchQuery.length, ignoreCase = true)
            }
            searchMatches = matches
            currentMatchIndex = 0
        } else {
            searchMatches = emptyList()
            currentMatchIndex = 0
        }
    }

    // Render PDF page to bitmap
    LaunchedEffect(file.path, currentPageIndex) {
        isLoading = true
        errorMessage = null
        withContext(Dispatchers.IO) {
            var pfd: ParcelFileDescriptor? = null
            var renderer: PdfRenderer? = null
            try {
                val f = File(file.path)
                pfd = ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY)
                renderer = PdfRenderer(pfd)
                totalPages = renderer.pageCount

                val validIndex = currentPageIndex.coerceIn(0, totalPages - 1)
                val page = renderer.openPage(validIndex)

                val renderWidth = page.width * 2
                val renderHeight = page.height * 2
                val bmp = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                pageBitmap = bmp
                isLoading = false
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Failed to render PDF page"
                isLoading = false
            } finally {
                try { renderer?.close() } catch (_: Exception) {}
                try { pfd?.close() } catch (_: Exception) {}
            }
        }
    }

    fun copyTextToClipboard(text: String, label: String = "PDF Text") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "PDF Text Copied to Clipboard", Toast.LENGTH_SHORT).show()
    }

    fun sharePdf() {
        try {
            val pdfFile = File(file.path)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share PDF Document"))
        } catch (e: Exception) {
            Toast.makeText(context, "Share error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Top Adobe Toolbar: Mode Toggle, Search, Night Mode, Bookmarks, Share
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Page view mode
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (displayMode == PdfDisplayMode.PAGE_RENDER) NothingWhite else NothingDark)
                        .border(1.dp, if (displayMode == PdfDisplayMode.PAGE_RENDER) NothingWhite else NothingBorder, RoundedCornerShape(8.dp))
                        .clickable { displayMode = PdfDisplayMode.PAGE_RENDER }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("pdf_mode_page")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Page View",
                            tint = if (displayMode == PdfDisplayMode.PAGE_RENDER) NothingBlack else NothingLightGray,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PAGE",
                            color = if (displayMode == PdfDisplayMode.PAGE_RENDER) NothingBlack else NothingLightGray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Text view mode
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (displayMode == PdfDisplayMode.TEXT_SELECTION) NothingWhite else NothingDark)
                        .border(1.dp, if (displayMode == PdfDisplayMode.TEXT_SELECTION) NothingWhite else NothingBorder, RoundedCornerShape(8.dp))
                        .clickable { displayMode = PdfDisplayMode.TEXT_SELECTION }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("pdf_mode_text")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Text Mode",
                            tint = if (displayMode == PdfDisplayMode.TEXT_SELECTION) NothingBlack else NothingLightGray,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "TEXT",
                            color = if (displayMode == PdfDisplayMode.TEXT_SELECTION) NothingBlack else NothingLightGray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Action Icons (Search, Night Mode, Bookmark, Share)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Search in PDF
                IconButton(
                    onClick = { isSearchOpen = !isSearchOpen },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search PDF",
                        tint = if (isSearchOpen) NothingRed else NothingWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Text Mode (Selection & Direct Copy of Any Text)
                IconButton(
                    onClick = {
                        displayMode = if (displayMode == PdfDisplayMode.PAGE_RENDER) PdfDisplayMode.TEXT_SELECTION else PdfDisplayMode.PAGE_RENDER
                    },
                    modifier = Modifier.size(28.dp).testTag("pdf_toggle_text_mode")
                ) {
                    Icon(
                        imageVector = Icons.Default.TextFields,
                        contentDescription = "Select Any Text",
                        tint = if (displayMode == PdfDisplayMode.TEXT_SELECTION) NothingRed else NothingWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Direct Copy Document Text to Clipboard
                IconButton(
                    onClick = {
                        if (extractedText.isNotBlank()) {
                            copyTextToClipboard(extractedText, "PDF Document Text")
                        } else {
                            Toast.makeText(context, "No text detected in this PDF", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(28.dp).testTag("pdf_copy_document_text")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy PDF Text",
                        tint = NothingLightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Night Mode / Invert toggle
                IconButton(
                    onClick = { isNightMode = !isNightMode },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isNightMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Night Mode",
                        tint = if (isNightMode) NothingGreen else NothingWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Bookmark Current Page
                IconButton(
                    onClick = {
                        bookmarkedPages = if (bookmarkedPages.contains(currentPageIndex)) {
                            bookmarkedPages - currentPageIndex
                        } else {
                            bookmarkedPages + currentPageIndex
                        }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (bookmarkedPages.contains(currentPageIndex)) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (bookmarkedPages.contains(currentPageIndex)) NothingRed else NothingWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Share PDF
                IconButton(
                    onClick = { sharePdf() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = NothingWhite, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Search Bar Overlay (Adobe Acrobat Find feature)
        if (isSearchOpen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, NothingRed, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search text in PDF...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                // Direct Paste to Search
                IconButton(
                    onClick = {
                        val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val pClip = clip?.primaryClip
                        if (pClip != null && pClip.itemCount > 0) {
                            val text = pClip.getItemAt(0).coerceToText(context).toString()
                            searchQuery = text
                            Toast.makeText(context, "Pasted into search", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(24.dp).testTag("pdf_search_paste")
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste to Search", tint = NothingLightGray, modifier = Modifier.size(14.dp))
                }

                if (searchMatches.isNotEmpty()) {
                    Text(
                        text = "${currentMatchIndex + 1}/${searchMatches.size}",
                        color = NothingRed,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    IconButton(
                        onClick = {
                            if (currentMatchIndex > 0) currentMatchIndex--
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous Match", tint = NothingWhite, modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = {
                            if (currentMatchIndex < searchMatches.size - 1) currentMatchIndex++
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next Match", tint = NothingWhite, modifier = Modifier.size(16.dp))
                    }
                }

                IconButton(
                    onClick = {
                        isSearchOpen = false
                        searchQuery = ""
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close Search", tint = NothingGray, modifier = Modifier.size(14.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }

        // Bookmarks Quick-Jump Bar
        if (bookmarkedPages.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "BOOKMARKS:", color = NothingGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                bookmarkedPages.sorted().forEach { pageIdx ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (currentPageIndex == pageIdx) NothingRed else NothingDark)
                            .border(1.dp, NothingBorder, RoundedCornerShape(6.dp))
                            .clickable { currentPageIndex = pageIdx }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "P.${pageIdx + 1}",
                            color = NothingWhite,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }

        // Secondary controls row (Pagination & Zoom)
        if (displayMode == PdfDisplayMode.PAGE_RENDER) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pagination controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (currentPageIndex > 0) {
                                currentPageIndex--
                                scale = 1f
                                offset = Offset.Zero
                            }
                        },
                        enabled = currentPageIndex > 0,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous Page",
                            tint = if (currentPageIndex > 0) NothingWhite else NothingGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = String.format("PAGE %02d / %02d", currentPageIndex + 1, totalPages),
                        color = NothingWhite,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    IconButton(
                        onClick = {
                            if (currentPageIndex < totalPages - 1) {
                                currentPageIndex++
                                scale = 1f
                                offset = Offset.Zero
                            }
                        },
                        enabled = currentPageIndex < totalPages - 1,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next Page",
                            tint = if (currentPageIndex < totalPages - 1) NothingWhite else NothingGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Zoom controls
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { scale = (scale + 0.3f).coerceAtMost(4f) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = NothingWhite, modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = { scale = (scale - 0.3f).coerceAtLeast(0.8f) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = NothingWhite, modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = {
                            scale = 1f
                            offset = Offset.Zero
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Reset Zoom", tint = NothingGray, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Page scrubber slider (Adobe Acrobat style smooth glide)
            if (totalPages > 1) {
                Slider(
                    value = currentPageIndex.toFloat(),
                    onValueChange = { frac ->
                        currentPageIndex = frac.toInt().coerceIn(0, totalPages - 1)
                    },
                    valueRange = 0f..(totalPages - 1).toFloat(),
                    steps = (totalPages - 2).coerceAtLeast(0),
                    colors = SliderDefaults.colors(
                        thumbColor = NothingRed,
                        activeTrackColor = NothingWhite,
                        inactiveTrackColor = NothingBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        // Main Document Surface
        when (displayMode) {
            PdfDisplayMode.PAGE_RENDER -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isNightMode) NothingBlack else Color.White)
                        .border(1.dp, if (isNightMode) NothingBorder else Color(0xFFD1D5DB), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = NothingRed, strokeWidth = 2.dp)
                    } else if (errorMessage != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "RENDER ERROR", color = NothingRed, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = errorMessage ?: "", color = NothingGray, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        }
                    } else {
                        pageBitmap?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Page ${currentPageIndex + 1}",
                                contentScale = ContentScale.Fit,
                                colorFilter = if (isNightMode) ColorFilter.colorMatrix(invertColorMatrix) else null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        translationX = offset.x
                                        translationY = offset.y
                                    }
                                    .transformable(state = transformState)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onDoubleTap = {
                                                if (scale > 1f) {
                                                    scale = 1f
                                                    offset = Offset.Zero
                                                } else {
                                                    scale = 2.2f
                                                }
                                            },
                                            onLongPress = {
                                                displayMode = PdfDisplayMode.TEXT_SELECTION
                                                Toast.makeText(context, "Gesture Selection: Select or copy text from document", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                            )
                        }
                    }
                }
            }

            PdfDisplayMode.TEXT_SELECTION -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isNightMode) NothingDark else Color.White)
                        .border(1.dp, if (isNightMode) NothingBorder else Color(0xFFD1D5DB), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    if (isExtractingText) {
                        CircularProgressIndicator(color = NothingRed, strokeWidth = 2.dp, modifier = Modifier.align(Alignment.Center))
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SELECTABLE DOCUMENT TEXT",
                                    color = if (isNightMode) NothingLightGray else Color(0xFF334155),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                NothingButton(
                                    text = "COPY ALL",
                                    onClick = { copyTextToClipboard(extractedText) },
                                    isPrimary = false
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                SelectionContainer {
                                    Text(
                                        text = extractedText.ifBlank { "No extractable text found in this PDF document." },
                                        color = if (isNightMode) NothingWhite else Color(0xFF0F172A),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
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
