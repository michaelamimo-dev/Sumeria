package com.michaelamimo.sumeria.ui.reader

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle.State as LifecycleState
import com.michaelamimo.sumeria.domain.model.ReaderBookmark as SavedReaderBookmark
import com.michaelamimo.sumeria.domain.model.SessionMode
import org.json.JSONObject

@Composable
fun ReaderScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = remember(view) { view.context.findActivity() }

    val currentBook = state.book
    val currentDoc = state.document
    val originalScreenBrightness = remember(activity) { activity?.window?.attributes?.screenBrightness ?: -1f }

    val savedBookmarks = remember(state.bookmarks, currentBook) {
        state.bookmarks.map { bookmark -> bookmark.toUiBookmark(currentBook?.title.orEmpty()) }
    }
    val isBookmarked = viewModel.isCurrentPositionBookmarked()
    val prefs = state.preferences.copy(isBookmarked = isBookmarked)
    val currentSection = currentDoc?.sections?.getOrNull(state.currentSectionIndex)
    val progressLabel = when {
        currentDoc == null -> "0%"
        currentBook?.format == com.michaelamimo.sumeria.domain.model.BookFormat.PDF ->
            "${(state.currentSectionIndex + 1).coerceAtMost(currentDoc.totalPages ?: currentDoc.sections.size)} / ${currentDoc.totalPages ?: currentDoc.sections.size}"
        else -> "${state.currentSectionIndex + 1} / ${currentDoc.sections.size}"
    }
    val wasFullscreen = remember(activity) {
        activity?.window?.let { it.attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN != 0 } ?: false
    }

    SideEffect {
        activity?.window?.let { window ->
            window.attributes = window.attributes.apply { screenBrightness = prefs.brightness }
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            WindowCompat.getInsetsController(window, view).apply {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    DisposableEffect(activity) {
        val window = activity?.window
        onDispose {
            window?.let {
                if (wasFullscreen) it.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                else it.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                it.attributes = it.attributes.apply { screenBrightness = originalScreenBrightness }
                WindowCompat.getInsetsController(it, view).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onReaderForeground()
                Lifecycle.Event.ON_PAUSE -> viewModel.onReaderBackground()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onReaderBackground()
        }
    }

    LaunchedEffect(currentBook?.id, currentDoc) {
        if (currentBook != null && currentDoc != null && lifecycleOwner.lifecycle.currentState.isAtLeast(LifecycleState.RESUMED)) {
            viewModel.onReaderForeground()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(prefs.theme.background)
    ) {
        when {
            state.isLoading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = prefs.theme.controlsBackground
            )
            state.error != null -> Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = state.error.orEmpty(),
                    color = prefs.theme.text,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onNavigateBack) {
                    Text("Back", color = prefs.theme.controlsBackground)
                }
            }
            currentDoc == null || currentBook == null -> Text(
                text = "Choose a book from your library to start reading.",
                color = prefs.theme.text,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
            else -> {
                var currentMode by remember(currentBook.id) { mutableStateOf(ReadingMode.Text) }
                var overlayVisible by remember(currentBook.id) { mutableStateOf(true) }
                var showSettingsSheet by remember(currentBook.id) { mutableStateOf(false) }

                Crossfade(targetState = currentMode, label = "mode-transition") { mode ->
                    when (mode) {
                        ReadingMode.Text -> {
                            val publication = state.publication
                            if (publication != null) {
                                ReadiumPublicationView(
                                    publication = publication,
                                    format = currentBook.format,
                                    initialLocator = state.initialLocator,
                                    preferences = prefs,
                                    onLocatorChanged = viewModel::onNavigatorLocatorChanged,
                                    onContentTap = { overlayVisible = !overlayVisible },
                                    isDictionarySelectionActive = viewModel::isDictionarySelectionActive,
                                    onSelectedWord = viewModel::onSelectedWord,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.Center),
                                    color = prefs.theme.controlsBackground
                                )
                            }
                            ReaderOverlay(
                                isVisible = overlayVisible,
                                prefs = prefs,
                                bookTitle = currentBook.title,
                                sectionTitle = state.navigatorChapterTitle ?: currentSection?.title ?: "Reading",
                                progressLabel = progressLabel,
                                progression = state.progression,
                                currentMode = currentMode,
                                onBackClick = { viewModel.saveProgressNow(onNavigateBack) },
                                onModeSwitch = {
                                    currentMode = it
                                    viewModel.onReadingModeChanged(if (it == ReadingMode.Audio) SessionMode.AUDIO else SessionMode.TEXT)
                                    overlayVisible = true
                                },
                                onDictionaryClick = viewModel::showDictionary,
                                onSettingsClick = { showSettingsSheet = true },
                                onBookmarkToggle = viewModel::toggleBookmark
                            )
                        }
                        ReadingMode.Audio -> {
                            val passages = remember(currentDoc) {
                                currentDoc.sections.flatMapIndexed { sectionIndex, section ->
                                    section.paragraphs.mapIndexed { paragraphIndex, text ->
                                        AudioPassage(sectionIndex, paragraphIndex, section.title, text)
                                    }
                                }
                            }
                            val startSection = if (state.audioPositionInitialized) state.audioSectionIndex else state.currentSectionIndex
                            val startParagraph = if (state.audioPositionInitialized) state.audioParagraphIndex else 0
                            val initialPassageIndex = passages.indexOfFirst { passage ->
                                passage.sectionIndex == startSection && passage.paragraphIndex == startParagraph
                            }.takeIf { it >= 0 } ?: passages.indexOfFirst { it.sectionIndex == startSection }.coerceAtLeast(0)
                            AudioPlayerView(
                                bookTitle = currentBook.title,
                                author = currentBook.authors.joinToString().ifBlank { "Unknown author" },
                                coverUri = currentBook.coverUri,
                                voiceName = prefs.audioVoiceName,
                                passages = passages,
                                initialPassageIndex = initialPassageIndex,
                                initialCharOffset = if (state.audioPositionInitialized) state.audioCharOffset else 0,
                                persistInitialPosition = !state.audioPositionInitialized,
                                isBookmarked = isBookmarked,
                                onBookmarkToggle = viewModel::toggleBookmark,
                                onPassageChanged = { passage ->
                                    viewModel.onAudioParagraphChanged(passage.sectionIndex, passage.paragraphIndex)
                                },
                                onCharOffsetChanged = viewModel::onAudioCharOffsetChanged,
                                onPlaybackFinished = viewModel::onAudioPlaybackFinished,
                                onBackToText = {
                                    currentMode = ReadingMode.Text
                                    viewModel.onReadingModeChanged(SessionMode.TEXT)
                                }
                            )
                        }
                    }
                }

                state.dictionary?.let { dictionaryState ->
                    DictionaryLookupDialog(
                        state = dictionaryState,
                        onLookup = viewModel::lookupWord,
                        onDismiss = viewModel::dismissDictionary,
                        background = prefs.theme.background,
                        foreground = prefs.theme.text,
                        accent = prefs.theme.controlsBackground
                    )
                }

                if (state.dictionarySelectionActive) {
                    Text(
                        text = "Select a word in the book",
                        color = prefs.theme.controlsContent,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 76.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(prefs.theme.controlsBackground.copy(alpha = 0.94f))
                            .padding(horizontal = 18.dp, vertical = 11.dp)
                    )
                }

                if (showSettingsSheet) {
                    ReaderSettingsSheet(
                        prefs = prefs,
                        onPrefsChange = viewModel::updatePreferences,
                        bookmarks = savedBookmarks,
                        onReset = { viewModel.updatePreferences(ReaderPreferences(isBookmarked = isBookmarked)) },
                        onDismiss = { showSettingsSheet = false }
                    )
                }
            }
        }
    }
}

private fun SavedReaderBookmark.toUiBookmark(bookTitle: String): ReaderBookmark = ReaderBookmark(
    bookTitle = bookTitle,
    chapterTitle = chapterTitle ?: "Saved place",
    chapterNumber = runCatching { JSONObject(locationJson).optInt("sectionIndex", 0) + 1 }
        .getOrDefault(1)
)

private fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}
