package com.unistream.webnovel.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.unistream.webnovel.data.*
import com.unistream.webnovel.viewmodel.NovelViewModel

@Composable
fun NovelReaderScreen(
    novelId: Long,
    chapterId: Long,
    onBack: () -> Unit,
    viewModel: NovelViewModel = hiltViewModel()
) {
    val state by viewModel.readerState.collectAsState()
    var showUi by remember { mutableStateOf(true) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showChapterList by remember { mutableStateOf(false) }

    LaunchedEffect(novelId, chapterId) {
        viewModel.loadNovelForReading(novelId, chapterId)
    }

    // Resolve background / text colors based on reader theme
    val (bgColor, textColor) = when (state.readerSettings.theme) {
        ReaderTheme.LIGHT -> ReaderLightColors
        ReaderTheme.SEPIA -> ReaderSepiaColors
        ReaderTheme.DARK -> ReaderDarkColors
        ReaderTheme.NIGHT -> ReaderNightColors
    }

    val fontFamily = when (state.readerSettings.fontFamily) {
        ReaderFont.SERIF -> FontFamily.Serif
        ReaderFont.SANS -> FontFamily.SansSerif
        ReaderFont.MONO -> FontFamily.Monospace
        else -> FontFamily.Default
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { showUi = !showUi }
    ) {
        when {
            state.isLoading || state.isDownloadingChapter -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = textColor.copy(0.6f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (state.isDownloadingChapter) "Downloading chapter..." else "Loading...",
                        color = textColor.copy(0.5f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            state.currentChapter != null -> {
                val chapter = state.currentChapter!!
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Chapter title
                    Text(
                        text = chapter.title,
                        style = TextStyle(
                            fontFamily = fontFamily,
                            fontSize = (state.readerSettings.fontSize + 4).sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            lineHeight = (state.readerSettings.fontSize * state.readerSettings.lineSpacing + 4).sp
                        ),
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Chapter content – split into paragraphs
                    val paragraphs = chapter.content
                        .split("\n")
                        .filter { it.isNotBlank() }

                    paragraphs.forEach { paragraph ->
                        Text(
                            text = paragraph.trim(),
                            style = TextStyle(
                                fontFamily = fontFamily,
                                fontSize = state.readerSettings.fontSize.sp,
                                color = textColor,
                                lineHeight = (state.readerSettings.fontSize * state.readerSettings.lineSpacing).sp,
                                textIndent = TextIndent(firstLine = 24.sp)
                            ),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    // Navigation at bottom
                    Spacer(modifier = Modifier.height(32.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val currentIndex = state.chapters.indexOfFirst { it.id == chapter.id }
                        OutlinedButton(
                            onClick = viewModel::navigateToPreviousChapter,
                            enabled = currentIndex > 0
                        ) {
                            Icon(Icons.Default.ChevronLeft, null)
                            Text("Prev")
                        }
                        Text(
                            "Ch. ${chapter.chapterNumber.toInt()} / ${state.chapters.size}",
                            style = MaterialTheme.typography.labelMedium.copy(color = textColor.copy(0.5f)),
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        Button(
                            onClick = viewModel::navigateToNextChapter,
                            enabled = currentIndex < state.chapters.size - 1
                        ) {
                            Text("Next")
                            Icon(Icons.Default.ChevronRight, null)
                        }
                    }
                }
            }
            else -> {
                Text(
                    "No chapter content",
                    color = textColor,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        // Top UI overlay
        AnimatedVisibility(
            visible = showUi,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgColor.copy(alpha = 0.95f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = {
                        Text(
                            state.currentChapter?.title ?: "",
                            maxLines = 1,
                            style = MaterialTheme.typography.titleSmall.copy(color = textColor)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, null, tint = textColor)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showChapterList = true }) {
                            Icon(Icons.Default.FormatListNumbered, null, tint = textColor)
                        }
                        IconButton(onClick = { viewModel.addBookmark(novelId, "") }) {
                            Icon(Icons.Default.Bookmark, null, tint = textColor)
                        }
                        IconButton(onClick = { showSettingsSheet = true }) {
                            Icon(Icons.Default.Settings, null, tint = textColor)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        }
    }

    // Reader Settings Sheet
    if (showSettingsSheet) {
        ReaderSettingsSheet(
            settings = state.readerSettings,
            onSettingsChanged = viewModel::updateReaderSettings,
            onDismiss = { showSettingsSheet = false }
        )
    }

    // Chapter List Sheet
    if (showChapterList) {
        ChapterListSheet(
            chapters = state.chapters,
            currentChapterId = state.currentChapter?.id,
            onChapterSelected = { chapter ->
                viewModel.loadChapter(chapter.id)
                showChapterList = false
            },
            onDismiss = { showChapterList = false }
        )
    }
}

// ── Color schemes per reader theme ──────────────────────────────────────
private val ReaderLightColors = Pair(Color(0xFFFFFFFF), Color(0xFF1A1A1A))
private val ReaderSepiaColors = Pair(Color(0xFFF5DEB3), Color(0xFF5C4033))
private val ReaderDarkColors = Pair(Color(0xFF121212), Color(0xFFE0E0E0))
private val ReaderNightColors = Pair(Color(0xFF0A0A0A), Color(0xFFBBBBBB))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderSettingsSheet(
    settings: ReaderSettings,
    onSettingsChanged: (ReaderSettings) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            Text("Reader Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            // Theme selector
            Text("Theme", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReaderTheme.values().forEach { theme ->
                    FilterChip(
                        selected = settings.theme == theme,
                        onClick = { onSettingsChanged(settings.copy(theme = theme)) },
                        label = { Text(theme.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Font size
            Text("Font Size: ${settings.fontSize.toInt()}sp", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = settings.fontSize,
                onValueChange = { onSettingsChanged(settings.copy(fontSize = it)) },
                valueRange = 12f..28f,
                steps = 7
            )

            // Line spacing
            Text("Line Spacing: ${"%.1f".format(settings.lineSpacing)}x", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = settings.lineSpacing,
                onValueChange = { onSettingsChanged(settings.copy(lineSpacing = it)) },
                valueRange = 1.2f..2.5f,
                steps = 12
            )

            // Font family
            Text("Font", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReaderFont.values().forEach { font ->
                    FilterChip(
                        selected = settings.fontFamily == font,
                        onClick = { onSettingsChanged(settings.copy(fontFamily = font)) },
                        label = { Text(font.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChapterListSheet(
    chapters: List<com.unistream.webnovel.data.ChapterEntity>,
    currentChapterId: Long?,
    onChapterSelected: (com.unistream.webnovel.data.ChapterEntity) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Chapters (${chapters.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(chapters.size) { i ->
                    val chapter = chapters[i]
                    ListItem(
                        headlineContent = {
                            Text(
                                chapter.title,
                                fontWeight = if (chapter.id == currentChapterId) FontWeight.Bold else FontWeight.Normal,
                                color = if (chapter.id == currentChapterId) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        supportingContent = {
                            Text(
                                "${chapter.wordCount} words",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        trailingContent = {
                            if (!chapter.isDownloaded) {
                                Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(16.dp))
                            }
                        },
                        modifier = Modifier.clickable { onChapterSelected(chapter) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

