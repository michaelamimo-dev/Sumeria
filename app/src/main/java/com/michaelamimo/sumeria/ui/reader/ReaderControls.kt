package com.michaelamimo.sumeria.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun ReaderOverlay(
    isVisible: Boolean,
    prefs: ReaderPreferences,
    bookTitle: String,
    sectionTitle: String,
    progressLabel: String,
    progression: Float,
    currentMode: ReadingMode,
    onBackClick: () -> Unit,
    onModeSwitch: (ReadingMode) -> Unit,
    onSettingsClick: () -> Unit,
    onBookmarkToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var clockTime by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            clockTime = LocalTime.now()
        }
    }
    val clockLabel = remember(clockTime) { clockTime.format(DateTimeFormatter.ofPattern("h:mm a")) }
    val ink = prefs.theme.controlsContent
    val accent = Color(0xFFE6B45C)

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 3 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 3 }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                color = prefs.theme.controlsBackground.copy(alpha = 0.94f),
                shape = RoundedCornerShape(22.dp),
                shadowElevation = 10.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back to library", tint = ink)
                    }
                    Column(modifier = Modifier.weight(1f).padding(start = 5.dp)) {
                        Text(bookTitle.uppercase(), color = ink.copy(alpha = 0.68f), fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp, maxLines = 1)
                        Text(sectionTitle, color = ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                    IconButton(
                        onClick = onBookmarkToggle,
                        modifier = Modifier
                            .size(44.dp)
                            .semantics { contentDescription = if (prefs.isBookmarked) "Remove bookmark" else "Add bookmark" }
                    ) {
                        Icon(
                            imageVector = if (prefs.isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                            contentDescription = null,
                            tint = if (prefs.isBookmarked) accent else ink,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 22.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    color = prefs.theme.controlsBackground.copy(alpha = 0.84f),
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(clockLabel, color = ink.copy(alpha = 0.78f), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(9.dp))
                        Box(Modifier.size(5.dp).clip(CircleShape).background(accent))
                        Spacer(Modifier.width(8.dp))
                        Text(progressLabel, color = ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(9.dp))
                        Box(Modifier.width(56.dp).height(2.dp).clip(CircleShape).background(ink.copy(alpha = 0.22f))) {
                            Box(Modifier.fillMaxWidth(progression.coerceIn(0f, 1f)).fillMaxHeight().clip(CircleShape).background(accent))
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = prefs.theme.controlsBackground.copy(alpha = 0.97f),
                    shadowElevation = 16.dp,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        ModeButton("Read", currentMode == ReadingMode.Text, ink, prefs.theme.controlsBackground) {
                            onModeSwitch(ReadingMode.Text)
                        }
                        ModeButton("Listen", currentMode == ReadingMode.Audio, ink, prefs.theme.controlsBackground) {
                            onModeSwitch(ReadingMode.Audio)
                        }
                        Box(Modifier.width(1.dp).height(24.dp).background(ink.copy(alpha = 0.2f)).padding(horizontal = 4.dp))
                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier.size(44.dp).semantics { contentDescription = "Reading settings" }
                        ) {
                            Icon(Icons.Rounded.Tune, contentDescription = null, tint = ink, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeButton(text: String, selected: Boolean, ink: Color, controlColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) ink else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 19.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (selected) controlColor else ink.copy(alpha = 0.78f), fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, fontSize = 13.sp)
    }
}
