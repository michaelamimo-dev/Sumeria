package com.michaelamimo.sumeria.ui.reader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.michaelamimo.sumeria.R
import android.graphics.Typeface

enum class ReaderTheme(
    val themeName: String,
    val background: Color,
    val text: Color,
    val controlsBackground: Color,
    val controlsContent: Color
) {
    Light(
        themeName = "Light",
        background = Color(0xFFF9F9F7),
        text = Color(0xFF1E1E1E),
        controlsBackground = Color(0xFF123B35),
        controlsContent = Color(0xFFFFFCF5)
    ),
    Sepia(
        themeName = "Sepia",
        background = Color(0xFFF4ECD8),
        text = Color(0xFF4A3C31),
        controlsBackground = Color(0xFF4A3C31),
        controlsContent = Color(0xFFF4ECD8)
    ),
    Dark(
        themeName = "Dark",
        background = Color(0xFF121212),
        text = Color(0xFFD4D4D4),
        controlsBackground = Color(0xFF2C2C2E),
        controlsContent = Color(0xFFE5E5EA)
    ),
    MidnightSumeria(
        themeName = "Midnight",
        background = Color(0xFF0F1715),
        text = Color(0xFFD6F1E3),
        controlsBackground = Color(0xFF1D4A43),
        controlsContent = Color(0xFFEAF4EC)
    )
}

enum class ReaderFont(val fontName: String, val family: FontFamily) {
    System("System", FontFamily.Default),
    Serif("Serif", FontFamily.Serif),
    Sans("Sans", FontFamily(Font(R.font.reader_inter))),
    Mono("Mono", FontFamily(Font(R.font.reader_jetbrains_mono))),
    Cursive("Cursive", FontFamily(Typeface.create("cursive", Typeface.NORMAL)))
}

enum class ReadingMode { Text, Audio }
enum class ReadingStyle { Scroll, Swipe }
enum class TextAlignment(val label: String) { Justified("Justified"), Left("Left") }

// Holds all customizable reading preferences
data class ReaderPreferences(
    val theme: ReaderTheme = ReaderTheme.Light,
    val font: ReaderFont = ReaderFont.System,
    val fontSize: Int = 18,
    val lineSpacing: Float = 1.6f,
    val horizontalMargin: Int = 24,
    val readingStyle: ReadingStyle = ReadingStyle.Scroll,
    val textAlignment: TextAlignment = TextAlignment.Justified,
    val isBookmarked: Boolean = false // Track bookmark state
)

data class ReaderBookmark(
    val bookTitle: String,
    val chapterTitle: String,
    val chapterNumber: Int
)
