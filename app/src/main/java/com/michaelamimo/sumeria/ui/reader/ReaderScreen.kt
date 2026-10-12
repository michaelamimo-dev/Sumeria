package com.michaelamimo.sumeria.ui.reader

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
fun ReaderScreen(onNavigateBack: () -> Unit) {
    val view = LocalView.current
    val activity = remember(view) { view.context.findActivity() }

    var prefs by remember { mutableStateOf(ReaderPreferences()) }
    var currentMode by remember { mutableStateOf(ReadingMode.Text) }
    var overlayVisible by remember { mutableStateOf(true) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var bookmarks by remember { mutableStateOf(emptyList<ReaderBookmark>()) }
    val currentBookmark = ReaderBookmark("The 48 Laws of Power", "Chapter One", 1)
    val isCurrentChapterBookmarked = bookmarks.any { it == currentBookmark }
    val toggleCurrentBookmark: () -> Unit = {
        bookmarks = if (isCurrentChapterBookmarked) {
            bookmarks.filterNot { it == currentBookmark }
        } else {
            (bookmarks + currentBookmark).distinct().sortedWith(compareBy<ReaderBookmark> { it.bookTitle }.thenBy { it.chapterNumber })
        }
        prefs = prefs.copy(isBookmarked = !isCurrentChapterBookmarked)
    }
    val wasFullscreen = remember(activity) {
        activity?.window?.let { it.attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN != 0 } ?: false
    }

    // Keep the reader immersive while its settings sheet is open as well.
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(prefs.theme.background)
    ) {
        Crossfade(targetState = currentMode, label = "mode-transition") { mode ->
            when (mode) {
                ReadingMode.Text -> {
                    ReadingView(
                        prefs = prefs,
                        onToggleOverlay = { overlayVisible = !overlayVisible },
                        onScroll = { overlayVisible = false }
                    )

                    ReaderOverlay(
                        isVisible = overlayVisible,
                        prefs = prefs,
                        currentMode = currentMode,
                        onBackClick = onNavigateBack,
                        onModeSwitch = { currentMode = it; overlayVisible = true },
                        onSettingsClick = { showSettingsSheet = true },
                        onBookmarkToggle = toggleCurrentBookmark
                    )
                }
                ReadingMode.Audio -> {
                    AudioPlayerView(
                        isBookmarked = isCurrentChapterBookmarked,
                        onBookmarkToggle = toggleCurrentBookmark,
                        onBackToText = { currentMode = ReadingMode.Text }
                    )
                }
            }
        }

        if (showSettingsSheet) {
            ReaderSettingsSheet(
                prefs = prefs,
                onPrefsChange = { prefs = it },
                bookmarks = bookmarks,
                onReset = { prefs = ReaderPreferences(isBookmarked = isCurrentChapterBookmarked) },
                onDismiss = { showSettingsSheet = false }
            )
        }
    }
}

private fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}
