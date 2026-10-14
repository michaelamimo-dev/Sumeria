package com.michaelamimo.sumeria.ui.reader

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import java.util.concurrent.atomic.AtomicReference

data class AudioPassage(
    val sectionIndex: Int,
    val paragraphIndex: Int,
    val sectionTitle: String,
    val text: String
)

@Composable
fun AudioPlayerView(
    bookTitle: String,
    author: String,
    coverUri: String?,
    passages: List<AudioPassage>,
    initialPassageIndex: Int,
    isBookmarked: Boolean,
    onBookmarkToggle: () -> Unit,
    onPassageChanged: (AudioPassage) -> Unit,
    onBackToText: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val bg = Color(0xFF101A17)
    val paper = Color(0xFFF7F2E7)
    val accent = Color(0xFFE6B45C)
    val menuPaper = Color(0xFFFFFCF5)
    val menuInk = Color(0xFF123B35)
    var isPlaying by remember { mutableStateOf(false) }
    var passageIndex by remember(passages) {
        mutableIntStateOf(initialPassageIndex.coerceIn(0, (passages.lastIndex).coerceAtLeast(0)))
    }
    var progress by remember(passages) {
        mutableFloatStateOf(if (passages.size <= 1) 0f else passageIndex.toFloat() / passages.lastIndex)
    }
    var speed by remember { mutableFloatStateOf(1f) }
    var sleepMinutes by remember { mutableIntStateOf(0) }
    var speedMenuExpanded by remember { mutableStateOf(false) }
    var sleepMenuExpanded by remember { mutableStateOf(false) }
    var ttsReady by remember { mutableStateOf(false) }
    var ttsError by remember { mutableStateOf<String?>(null) }
    val activeUtteranceId = remember { AtomicReference<String?>(null) }
    val latestIsPlaying by rememberUpdatedState(isPlaying)
    val latestPassageIndex by rememberUpdatedState(passageIndex)
    val latestPassageCount by rememberUpdatedState(passages.size)
    val latestMoveTo by rememberUpdatedState<(Int) -> Unit> { index ->
        val bounded = index.coerceIn(0, (passages.lastIndex).coerceAtLeast(0))
        passageIndex = bounded
        progress = if (passages.size <= 1) 0f else bounded.toFloat() / passages.lastIndex
        passages.getOrNull(bounded)?.let(onPassageChanged)
    }
    val latestStop by rememberUpdatedState<() -> Unit> { isPlaying = false }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) isPlaying = false
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val tts = remember(context) {
        TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (status != TextToSpeech.SUCCESS) ttsError = "Text-to-speech is unavailable on this device."
        }
    }

    DisposableEffect(tts) {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit

            override fun onDone(utteranceId: String?) {
                if (utteranceId == null || !activeUtteranceId.compareAndSet(utteranceId, null)) return
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    if (!latestIsPlaying) return@post
                    val next = latestPassageIndex + 1
                    if (next < latestPassageCount) latestMoveTo(next) else latestStop()
                }
            }

            @Suppress("OVERRIDE_DEPRECATION")
            override fun onError(utteranceId: String?) {
                onSpeechError(utteranceId)
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                onSpeechError(utteranceId)
            }

            private fun onSpeechError(utteranceId: String?) {
                if (utteranceId == null || !activeUtteranceId.compareAndSet(utteranceId, null)) return
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    ttsError = "Narration could not read this passage."
                    latestStop()
                }
            }
        })
        onDispose {
            activeUtteranceId.set(null)
            tts.stop()
            tts.shutdown()
        }
    }

    LaunchedEffect(isPlaying, passageIndex, ttsReady, speed, passages) {
        if (!isPlaying) {
            activeUtteranceId.set(null)
            tts.stop()
        } else if (ttsReady) {
            val text = passages.getOrNull(passageIndex)?.text.orEmpty()
            if (text.isBlank()) {
                if (passageIndex < passages.lastIndex) latestMoveTo(passageIndex + 1) else isPlaying = false
            } else {
                ttsError = null
                tts.setSpeechRate(speed)
                val utteranceId = "${passageIndex}-${System.nanoTime()}"
                activeUtteranceId.set(utteranceId)
                val result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                if (result == TextToSpeech.ERROR) {
                    ttsError = "Narration could not be started."
                    isPlaying = false
                }
            }
        }
    }

    LaunchedEffect(isPlaying, sleepMinutes) {
        if (isPlaying && sleepMinutes > 0) {
            delay(sleepMinutes * 60_000L)
            isPlaying = false
            sleepMinutes = 0
        }
    }

    val passageCount = passages.size
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
                Text("Text-to-speech", color = paper, fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
            if (coverUri != null) {
                AsyncImage(
                    model = coverUri,
                    contentDescription = "Cover of $bookTitle",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.26f)), startY = 380f)))
        }

        Spacer(Modifier.height(25.dp))
        Text(passages.getOrNull(passageIndex)?.sectionTitle ?: "Narration", color = paper, fontSize = 23.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
        Text(bookTitle, color = paper.copy(alpha = 0.82f), fontSize = 14.sp, modifier = Modifier.padding(top = 5.dp), maxLines = 1)
        Text(author, color = paper.copy(alpha = 0.54f), fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp), maxLines = 1)
        Spacer(Modifier.height(26.dp))

        Column(Modifier.fillMaxWidth()) {
            Slider(
                value = progress,
                onValueChange = { progress = it },
                onValueChangeFinished = {
                    val selectedPassage = if (passageCount <= 1) 0 else (progress * (passageCount - 1)).roundToInt()
                    latestMoveTo(selectedPassage)
                },
                colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent, inactiveTrackColor = paper.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Narration progress" }
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (passageCount == 0) "No readable passage" else "Passage ${passageIndex + 1}", color = paper.copy(alpha = 0.58f), fontSize = 11.sp)
                Text(if (passageCount == 0) "" else "$passageCount total", color = paper.copy(alpha = 0.58f), fontSize = 11.sp)
            }
            ttsError?.let {
                Text(it, color = Color(0xFFFFB4AB), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }

        Spacer(Modifier.height(17.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                enabled = passageIndex > 0,
                onClick = { latestMoveTo(passageIndex - 1) },
                modifier = Modifier.size(52.dp)
            ) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous passage", tint = paper, modifier = Modifier.size(29.dp))
            }
            Surface(
                onClick = {
                    if (passageCount > 0) isPlaying = !isPlaying
                },
                shape = CircleShape,
                color = accent,
                modifier = Modifier.size(72.dp).semantics { contentDescription = if (isPlaying) "Pause narration" else "Play narration" }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = null, tint = bg, modifier = Modifier.size(36.dp))
                }
            }
            IconButton(
                enabled = passageIndex < passageCount - 1,
                onClick = { latestMoveTo(passageIndex + 1) },
                modifier = Modifier.size(52.dp)
            ) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "Next passage", tint = paper, modifier = Modifier.size(29.dp))
            }
        }

        if (isPlaying && !ttsReady && ttsError == null) {
            Text("Preparing narration…", color = paper.copy(alpha = 0.72f), fontSize = 12.sp)
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
                    modifier = Modifier.semantics { contentDescription = "Choose narration speed" }
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
