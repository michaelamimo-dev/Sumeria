package com.michaelamimo.sumeria.ui.reader

import androidx.compose.animation.*
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun ReaderOverlay(
    isVisible: Boolean,
    prefs: ReaderPreferences,
    currentMode: ReadingMode,
    onBackClick: () -> Unit,
    onModeSwitch: (ReadingMode) -> Unit,
    onSettingsClick: () -> Unit,
    onBookmarkToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {

        // Unified Bottom Console
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp, start = 16.dp, end = 16.dp)
        ) {
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
            val currentTime = LocalTime.now().format(timeFormatter)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                // Subtle Time & Page indicator floating above the pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = prefs.theme.controlsBackground.copy(alpha = 0.8f)
                ) {
                    Text(
                        text = "$currentTime  •  12 / 320",
                        color = prefs.theme.controlsContent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // The Main Control Pill
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = prefs.theme.controlsBackground,
                    shadowElevation = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 1. Back Button (Moved here for ergonomics & clean top screen)
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = prefs.theme.controlsContent
                            )
                        }

                        // 2. Mode Switcher
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color.Transparent,
                        ) {
                            Row(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                                    .padding(4.dp)
                            ) {
                                ModeButton(
                                    text = "Read",
                                    isSelected = currentMode == ReadingMode.Text,
                                    activeColor = prefs.theme.controlsContent,
                                    activeTextColor = prefs.theme.controlsBackground,
                                    inactiveTextColor = prefs.theme.controlsContent,
                                    onClick = { onModeSwitch(ReadingMode.Text) }
                                )
                                ModeButton(
                                    text = "Listen",
                                    isSelected = currentMode == ReadingMode.Audio,
                                    activeColor = prefs.theme.controlsContent,
                                    activeTextColor = prefs.theme.controlsBackground,
                                    inactiveTextColor = prefs.theme.controlsContent,
                                    onClick = { onModeSwitch(ReadingMode.Audio) }
                                )
                            }
                        }

                        // 3. Bookmark Button
                        IconButton(onClick = onBookmarkToggle) {
                            Icon(
                                imageVector = if (prefs.isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (prefs.isBookmarked) Color(0xFFE6A843) else prefs.theme.controlsContent // Amber if bookmarked
                            )
                        }

                        // 4. Settings Button
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = "Settings",
                                tint = prefs.theme.controlsContent
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeButton(
    text: String,
    isSelected: Boolean,
    activeColor: Color,
    activeTextColor: Color,
    inactiveTextColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) activeColor else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) activeTextColor else inactiveTextColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp
        )
    }
}