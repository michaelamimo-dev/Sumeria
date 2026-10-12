package com.michaelamimo.sumeria.ui.reader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    prefs: ReaderPreferences,
    onPrefsChange: (ReaderPreferences) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFFFFCF5), // Sumeria Paper
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
        ) {
            Text(
                text = "Reading Settings",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF123B35) // Ink
            )
            Spacer(modifier = Modifier.height(24.dp))

            // 1. Theme
            SettingsLabel("Appearance")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ReaderTheme.entries.forEach { theme ->
                    ThemeCircle(
                        theme = theme,
                        isSelected = prefs.theme == theme,
                        onClick = { onPrefsChange(prefs.copy(theme = theme)) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // 2. Typography
            SettingsLabel("Typography")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReaderFont.entries.forEach { font ->
                    val isSelected = prefs.font == font
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFF123B35) else Color.White,
                        border = BorderStroke(1.dp, if (isSelected) Color.Transparent else Color.LightGray),
                        onClick = { onPrefsChange(prefs.copy(font = font)) }
                    ) {
                        Text(
                            text = font.fontName,
                            fontFamily = font.family,
                            color = if (isSelected) Color.White else Color.Black,
                            modifier = Modifier.padding(vertical = 12.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // 3. Layout (Size & Margin)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    SettingsLabel("Font Size")
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            IconButton(onClick = { if (prefs.fontSize > 12) onPrefsChange(prefs.copy(fontSize = prefs.fontSize - 2)) }) {
                                Icon(Icons.Rounded.Remove, contentDescription = "Smaller", tint = Color.Black)
                            }
                            Text("${prefs.fontSize}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { if (prefs.fontSize < 32) onPrefsChange(prefs.copy(fontSize = prefs.fontSize + 2)) }) {
                                Icon(Icons.Rounded.Add, contentDescription = "Larger", tint = Color.Black)
                            }
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    SettingsLabel("Margins")
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            MarginButton("Aa", isSelected = prefs.horizontalMargin == 16, modifier = Modifier.weight(1f)) {
                                onPrefsChange(prefs.copy(horizontalMargin = 16))
                            }
                            MarginButton(" Aa ", isSelected = prefs.horizontalMargin == 24, modifier = Modifier.weight(1f)) {
                                onPrefsChange(prefs.copy(horizontalMargin = 24))
                            }
                            MarginButton("  Aa  ", isSelected = prefs.horizontalMargin == 36, modifier = Modifier.weight(1f)) {
                                onPrefsChange(prefs.copy(horizontalMargin = 36))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // 4. Style (Scroll vs Swipe)
            SettingsLabel("Reading Style")
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color.LightGray)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    StyleButton(
                        text = "Scroll",
                        isSelected = prefs.readingStyle == ReadingStyle.Scroll,
                        modifier = Modifier.weight(1f),
                        onClick = { onPrefsChange(prefs.copy(readingStyle = ReadingStyle.Scroll)) }
                    )
                    StyleButton(
                        text = "Swipe",
                        isSelected = prefs.readingStyle == ReadingStyle.Swipe,
                        modifier = Modifier.weight(1f),
                        onClick = { onPrefsChange(prefs.copy(readingStyle = ReadingStyle.Swipe)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsLabel(text: String) {
    Text(text, fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun MarginButton(text: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .background(if (isSelected) Color(0xFFEAF4EC) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if(isSelected) Color(0xFF123B35) else Color.Gray)
    }
}

@Composable
private fun StyleButton(text: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .background(if (isSelected) Color(0xFF123B35) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if(isSelected) Color.White else Color.Black)
    }
}

@Composable
private fun ThemeCircle(theme: ReaderTheme, isSelected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(theme.background)
                .border(width = if (isSelected) 3.dp else 1.dp, color = if (isSelected) Color(0xFFE6A843) else Color.LightGray, shape = CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text("Aa", color = theme.text, fontSize = 20.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(theme.themeName, fontSize = 12.sp, color = Color.Gray, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}