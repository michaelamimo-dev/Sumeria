package com.michaelamimo.sumeria.ui.reader

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import android.view.WindowManager
import android.view.ViewTreeObserver
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

private val Ink = Color(0xFF123B35)
private val Paper = Color(0xFFFFFCF5)
private val MutedInk = Color(0xFF727873)
private val Gold = Color(0xFFC7953D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    prefs: ReaderPreferences,
    onPrefsChange: (ReaderPreferences) -> Unit,
    bookmarks: List<ReaderBookmark>,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Paper,
        contentColor = Ink,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(Modifier.padding(top = 12.dp, bottom = 4.dp).size(width = 36.dp, height = 4.dp).clip(CircleShape).background(Ink.copy(alpha = 0.2f)))
        },
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        KeepSheetWindowImmersive()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 760.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Reading settings", fontSize = 23.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                    Text("Make this page yours", fontSize = 13.sp, color = MutedInk, modifier = Modifier.padding(top = 3.dp))
                }
                Text("Aa", fontSize = 22.sp, fontWeight = FontWeight.Medium, color = Ink.copy(alpha = 0.7f))
            }
            Spacer(Modifier.height(25.dp))

            SectionLabel("PAPER")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ReaderTheme.entries.forEach { theme ->
                    ThemeSwatch(theme, prefs.theme == theme, Modifier.weight(1f)) {
                        onPrefsChange(prefs.copy(theme = theme))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))

            SectionLabel("TYPEFACE")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ReaderFont.entries.forEach { font ->
                    val selected = prefs.font == font
                    Surface(
                        onClick = { onPrefsChange(prefs.copy(font = font)) },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) Ink else Color.White.copy(alpha = 0.72f),
                        border = BorderStroke(1.dp, if (selected) Ink else Ink.copy(alpha = 0.1f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(font.fontName, fontFamily = font.family, color = if (selected) Paper else Ink, fontSize = 10.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("TEXT SIZE", Modifier.weight(1f))
                Text("${prefs.fontSize} sp", color = MutedInk, fontSize = 12.sp, modifier = Modifier.padding(bottom = 10.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { onPrefsChange(prefs.copy(fontSize = (prefs.fontSize - 1).coerceAtLeast(14))) },
                    modifier = Modifier.size(42.dp).background(Color.White, CircleShape).semantics { contentDescription = "Decrease text size" }
                ) { Icon(Icons.Rounded.Remove, contentDescription = null, tint = Ink) }
                Slider(
                    value = prefs.fontSize.toFloat(),
                    onValueChange = { onPrefsChange(prefs.copy(fontSize = it.toInt().coerceIn(14, 32))) },
                    valueRange = 14f..32f,
                    steps = 17,
                    modifier = Modifier.weight(1f).semantics { contentDescription = "Text size" },
                    colors = SliderDefaults.colors(thumbColor = Ink, activeTrackColor = Ink, inactiveTrackColor = Ink.copy(alpha = 0.16f))
                )
                IconButton(
                    onClick = { onPrefsChange(prefs.copy(fontSize = (prefs.fontSize + 1).coerceAtMost(32))) },
                    modifier = Modifier.size(42.dp).background(Color.White, CircleShape).semantics { contentDescription = "Increase text size" }
                ) { Icon(Icons.Rounded.Add, contentDescription = null, tint = Ink) }
            }
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("LINE SPACING", Modifier.weight(1f))
                Text("${String.format("%.1f", prefs.lineSpacing)}×", color = MutedInk, fontSize = 12.sp, modifier = Modifier.padding(bottom = 10.dp))
            }
            Slider(
                value = prefs.lineSpacing,
                onValueChange = { onPrefsChange(prefs.copy(lineSpacing = it)) },
                valueRange = 1.2f..2.2f,
                steps = 9,
                colors = SliderDefaults.colors(thumbColor = Ink, activeTrackColor = Ink, inactiveTrackColor = Ink.copy(alpha = 0.16f))
            )
            Spacer(Modifier.height(14.dp))

            SectionLabel("LIVE PREVIEW")
            Surface(
                color = prefs.theme.background,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Ink.copy(alpha = 0.08f))
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = prefs.horizontalMargin.dp, vertical = 16.dp)) {
                    Text(
                        "A quieter way to read",
                        fontFamily = prefs.font.family,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = (prefs.fontSize + 2).sp,
                        color = prefs.theme.text,
                        modifier = Modifier.padding(bottom = 7.dp)
                    )
                    Text(
                        "The quick brown fox jumps over the lazy dog. A good page lets every word find its own comfortable pace.",
                        fontFamily = prefs.font.family,
                        fontSize = prefs.fontSize.sp,
                        lineHeight = (prefs.fontSize * prefs.lineSpacing).sp,
                        color = prefs.theme.text,
                        textAlign = if (prefs.textAlignment == TextAlignment.Left) TextAlign.Start else TextAlign.Justify
                    )
                }
            }
            Spacer(Modifier.height(22.dp))

            SectionLabel("PAGE")
            SettingCard {
                Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    ChoiceChip("Scroll", prefs.readingStyle == ReadingStyle.Scroll, Modifier.weight(1f)) {
                        onPrefsChange(prefs.copy(readingStyle = ReadingStyle.Scroll))
                    }
                    ChoiceChip("Page turn", prefs.readingStyle == ReadingStyle.Swipe, Modifier.weight(1f)) {
                        onPrefsChange(prefs.copy(readingStyle = ReadingStyle.Swipe))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            SettingCard {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Margins", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 4.dp, end = 4.dp))
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        listOf(16 to "Narrow", 24 to "Comfort", 36 to "Wide").forEach { (margin, label) ->
                            ChoiceChip(label, prefs.horizontalMargin == margin, Modifier.weight(1f)) {
                                onPrefsChange(prefs.copy(horizontalMargin = margin))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            SettingCard {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Text alignment", color = Ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    ChoiceChip("Left", prefs.textAlignment == TextAlignment.Left) {
                        onPrefsChange(prefs.copy(textAlignment = TextAlignment.Left))
                    }
                    Spacer(Modifier.width(6.dp))
                    ChoiceChip("Justified", prefs.textAlignment == TextAlignment.Justified) {
                        onPrefsChange(prefs.copy(textAlignment = TextAlignment.Justified))
                    }
                }
            }
            Spacer(Modifier.height(22.dp))

            SectionLabel("READING COMFORT")
            SettingCard {
                Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Brightness6, contentDescription = null, tint = Ink, modifier = Modifier.size(18.dp))
                    Text("  Screen brightness", color = Ink, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text("Coming soon", color = MutedInk, fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(22.dp))
            SectionLabel("BOOKMARKED CHAPTERS")
            if (bookmarks.isEmpty()) {
                SettingCard {
                    Text(
                        "Save a chapter with the bookmark icon and it will appear here, grouped by book.",
                        color = MutedInk,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.fillMaxWidth().padding(15.dp)
                    )
                }
            } else {
                bookmarks.groupBy { it.bookTitle }.toSortedMap().forEach { (book, chapters) ->
                    SettingCard {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 12.dp)) {
                            Text(book, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            chapters.sortedBy { it.chapterNumber }.forEach { chapter ->
                                Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Bookmark, contentDescription = null, tint = Gold, modifier = Modifier.size(17.dp))
                                    Spacer(Modifier.width(9.dp))
                                    Text("Chapter ${chapter.chapterNumber}  ·  ${chapter.chapterTitle}", color = Ink.copy(alpha = 0.86f), fontSize = 12.sp, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(20.dp))
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Ink.copy(alpha = 0.15f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink)
            ) {
                Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Reset reading settings")
            }
            Spacer(Modifier.height(12.dp))
            Text("Changes apply as you read", color = MutedInk, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun KeepSheetWindowImmersive() {
    val view = LocalView.current
    val window = (view.parent as? DialogWindowProvider)?.window
        ?: (view as? DialogWindowProvider)?.window
        ?: view.context.findHostActivity()?.window

    SideEffect { window?.enforceReaderImmersiveMode() }

    DisposableEffect(window) {
        if (window == null) return@DisposableEffect onDispose { }
        val decor = window.decorView
        val observer = decor.viewTreeObserver
        val listener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (hasFocus) window.enforceReaderImmersiveMode()
        }
        observer.addOnWindowFocusChangeListener(listener)
        decor.post { if (decor.isAttachedToWindow) window.enforceReaderImmersiveMode() }
        onDispose {
            if (observer.isAlive) observer.removeOnWindowFocusChangeListener(listener)
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier.padding(bottom = 10.dp), fontSize = 10.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.Bold, color = MutedInk)
}

@Composable
private fun SettingCard(content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(17.dp), color = Color.White.copy(alpha = 0.76f), border = BorderStroke(1.dp, Ink.copy(alpha = 0.07f))) { content() }
}

@Composable
private fun ChoiceChip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Ink else Color.Transparent)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 11.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (selected) Paper else Ink, fontSize = 11.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun ThemeSwatch(theme: ReaderTheme, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier = modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(theme.background)
                .border(if (selected) 2.dp else 1.dp, if (selected) Gold else Ink.copy(alpha = 0.1f), RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Aa", color = theme.text, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        }
        Text(theme.themeName, color = if (selected) Ink else MutedInk, fontSize = 10.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.padding(top = 7.dp), maxLines = 1)
    }
}

private fun Context.findHostActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private fun Window.enforceReaderImmersiveMode() {
    WindowCompat.setDecorFitsSystemWindows(this, false)
    addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    WindowCompat.getInsetsController(this, decorView).apply {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        hide(WindowInsetsCompat.Type.systemBars())
    }
}
