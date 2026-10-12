package com.michaelamimo.sumeria.ui.reader

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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

    // Enforce True Immersive Mode
    DisposableEffect(Unit) {
        val window = activity?.window
        var insetsController: WindowInsetsControllerCompat? = null
        if (window != null) {
            insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars()) // Hide status & nav bars completely
        }

        onDispose {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
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
                        onBookmarkToggle = { prefs = prefs.copy(isBookmarked = !prefs.isBookmarked) }
                    )
                }
                ReadingMode.Audio -> {
                    AudioPlayerView(
                        onBackToText = { currentMode = ReadingMode.Text }
                    )
                }
            }
        }

        if (showSettingsSheet) {
            ReaderSettingsSheet(
                prefs = prefs,
                onPrefsChange = { prefs = it },
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