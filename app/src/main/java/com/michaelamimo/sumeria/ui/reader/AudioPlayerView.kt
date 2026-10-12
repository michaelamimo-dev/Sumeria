package com.michaelamimo.sumeria.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

@Composable
fun AudioPlayerView(
    isBookmarked: Boolean,
    onBookmarkToggle: () -> Unit,
    onBackToText: () -> Unit
) {
    val bg = Color(0xFF101A17)
    val paper = Color(0xFFF7F2E7)
    val accent = Color(0xFFE6B45C)
    val menuPaper = Color(0xFFFFFCF5)
    val menuInk = Color(0xFF123B35)
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0.27f) }
    var speed by remember { mutableFloatStateOf(1f) }
    var sleepMinutes by remember { mutableIntStateOf(0) }
    var speedMenuExpanded by remember { mutableStateOf(false) }
    var sleepMenuExpanded by remember { mutableStateOf(false) }
    val durationSeconds = 51 * 60 + 45
    val currentSeconds = (durationSeconds * progress).toInt()

    LaunchedEffect(isPlaying, speed) {
        while (isPlaying && progress < 1f) {
            delay(1_000)
            progress = (progress + speed / durationSeconds).coerceAtMost(1f)
            if (progress >= 1f) isPlaying = false
        }
    }
    LaunchedEffect(isPlaying, sleepMinutes) {
        if (isPlaying && sleepMinutes > 0) {
            delay(sleepMinutes * 60_000L)
            isPlaying = false
            sleepMinutes = 0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1B332C), bg, bg)))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .padding(top = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackToText, modifier = Modifier.size(44.dp).background(Color.White.copy(alpha = 0.08f), CircleShape)) {
                Icon(Icons.Rounded.Close, contentDescription = "Return to reading", tint = paper)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("NOW PLAYING", color = paper.copy(alpha = 0.56f), fontSize = 9.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.SemiBold)
                Text("Audiobook", color = paper, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            IconButton(
                onClick = onBookmarkToggle,
                modifier = Modifier.size(44.dp).semantics { contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark this chapter" }
            ) {
                Icon(if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, contentDescription = null, tint = if (isBookmarked) accent else paper, modifier = Modifier.size(22.dp))
            }
        }

        Spacer(Modifier.height(28.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.76f)
                .aspectRatio(0.78f)
                .shadow(28.dp, RoundedCornerShape(22.dp))
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF352B20))
        ) {
            AsyncImage(
                model = "https://m.media-amazon.com/images/I/71aG+xDKSYL._AC_UF1000,1000_QL80_.jpg",
                contentDescription = "The 48 Laws of Power book cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.26f)), startY = 380f)))
        }

        Spacer(Modifier.height(25.dp))
        Text("Chapter One", color = paper, fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
        Text("The 48 Laws of Power", color = paper.copy(alpha = 0.62f), fontSize = 14.sp, modifier = Modifier.padding(top = 5.dp))
        Spacer(Modifier.height(26.dp))

        Column(Modifier.fillMaxWidth()) {
            Slider(
                value = progress,
                onValueChange = { progress = it },
                colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent, inactiveTrackColor = paper.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Audio progress" }
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatAudioTime(currentSeconds), color = paper.copy(alpha = 0.58f), fontSize = 11.sp)
                Text(formatAudioTime(durationSeconds), color = paper.copy(alpha = 0.58f), fontSize = 11.sp)
            }
        }

        Spacer(Modifier.height(17.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { progress = (progress - 10f / durationSeconds).coerceAtLeast(0f) }, modifier = Modifier.size(52.dp)) {
                Icon(Icons.Rounded.Replay10, contentDescription = "Rewind 10 seconds", tint = paper, modifier = Modifier.size(29.dp))
            }
            Surface(
                onClick = { isPlaying = !isPlaying },
                shape = CircleShape,
                color = accent,
                modifier = Modifier.size(72.dp).semantics { contentDescription = if (isPlaying) "Pause audiobook" else "Play audiobook" }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = null, tint = bg, modifier = Modifier.size(36.dp))
                }
            }
            IconButton(onClick = { progress = (progress + 10f / durationSeconds).coerceAtMost(1f) }, modifier = Modifier.size(52.dp)) {
                Icon(Icons.Rounded.Forward10, contentDescription = "Forward 10 seconds", tint = paper, modifier = Modifier.size(29.dp))
            }
        }

        Spacer(Modifier.height(19.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                OutlinedButton(
                    onClick = { speedMenuExpanded = true },
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, paper.copy(alpha = 0.22f)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                    modifier = Modifier.semantics { contentDescription = "Choose playback speed" }
                ) { Text("${speed}× speed", color = paper, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
                DropdownMenu(expanded = speedMenuExpanded, onDismissRequest = { speedMenuExpanded = false }) {
                    Surface(color = menuPaper, shape = RoundedCornerShape(14.dp)) {
                        Column {
                            listOf(0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f).forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(if (speed == option) "✓" else "", color = accent, modifier = Modifier.width(22.dp))
                                            Text("${option}×", color = menuInk)
                                        }
                                    },
                                    onClick = { speed = option; speedMenuExpanded = false }
                                )
                            }
                        }
                    }
                }
            }

            Box {
                TextButton(onClick = { sleepMenuExpanded = true }) {
                    Icon(Icons.Rounded.Timer, contentDescription = null, tint = accent, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (sleepMinutes == 0) "Sleep timer" else "${sleepMinutes} min", color = paper.copy(alpha = 0.86f), fontSize = 12.sp)
                }
                DropdownMenu(expanded = sleepMenuExpanded, onDismissRequest = { sleepMenuExpanded = false }) {
                    Surface(color = menuPaper, shape = RoundedCornerShape(14.dp)) {
                        Column {
                            listOf(0, 15, 30, 45, 60).forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(if (sleepMinutes == option) "✓" else "", color = accent, modifier = Modifier.width(22.dp))
                                            Text(if (option == 0) "Off" else "$option minutes", color = menuInk)
                                        }
                                    },
                                    onClick = { sleepMinutes = option; sleepMenuExpanded = false }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatAudioTime(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)
