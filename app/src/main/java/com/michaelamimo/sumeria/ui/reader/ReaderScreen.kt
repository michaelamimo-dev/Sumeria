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
import com.michaelamimo.sumeria.domain.model.ReaderBookmark as SavedReaderBookmark
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
                WindowCompat.getInsetsController(it, view).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.saveProgressNow()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
                            ReadingView(
                                sections = currentDoc.sections,
                                initialBlockIndex = state.currentBlockIndex,
                                initialScrollOffset = state.currentScrollOffset,
                                prefs = prefs,
                                onToggleOverlay = { overlayVisible = !overlayVisible },
                                onScroll = { overlayVisible = false },
                                onPositionChanged = viewModel::onPositionChanged
                            )
                            ReaderOverlay(
                                isVisible = overlayVisible,
                                prefs = prefs,
                                bookTitle = currentBook.title,
                                sectionTitle = currentSection?.title ?: "Reading",
                                progressLabel = progressLabel,
                                progression = state.progression,
                                currentMode = currentMode,
                                onBackClick = { viewModel.saveProgressNow(onNavigateBack) },
                                onModeSwitch = { currentMode = it; overlayVisible = true },
                                onSettingsClick = { showSettingsSheet = true },
                                onBookmarkToggle = viewModel::toggleBookmark
                            )
                        }
                        ReadingMode.Audio -> {
                            AudioPlayerView(
                                isBookmarked = isBookmarked,
                                onBookmarkToggle = viewModel::toggleBookmark,
                                onBackToText = { currentMode = ReadingMode.Text }
                            )
                        }
                    }
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
