package com.michaelamimo.sumeria.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun AudioPlayerView(
    onBackToText: () -> Unit
) {
    val bgColor = Color(0xFF121212) // True dark
    val contentColor = Color(0xFFFFFFFF)
    val accentColor = Color(0xFFE6A843) // Sumeria Amber

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Standalone Back Button (returns to Text Mode)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(
                onClick = onBackToText,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Close Audio", tint = contentColor)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Premium Heavy Shadow Book Cover
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .aspectRatio(1f)
                .shadow(24.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = "https://m.media-amazon.com/images/I/71aG+xDKSYL._AC_UF1000,1000_QL80_.jpg",
                contentDescription = "Book Cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Titles
        Text(
            text = "Chapter One",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = contentColor
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "The 48 Laws of Power",
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Modern Scrubber
        var progress by remember { mutableFloatStateOf(0.3f) }
        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = progress,
                onValueChange = { progress = it },
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = contentColor.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("13:45", color = contentColor.copy(alpha = 0.5f), fontSize = 12.sp)
                Text("51:45", color = contentColor.copy(alpha = 0.5f), fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Playback Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { }) {
                Icon(Icons.Rounded.Replay10, contentDescription = "Rewind", tint = contentColor, modifier = Modifier.size(32.dp))
            }

            // Big Play/Pause Button
            Surface(
                shape = CircleShape,
                color = accentColor,
                modifier = Modifier.size(80.dp)
            ) {
                IconButton(onClick = { }) {
                    Icon(Icons.Rounded.Pause, contentDescription = "Pause", tint = bgColor, modifier = Modifier.size(40.dp))
                }
            }

            IconButton(onClick = { }) {
                Icon(Icons.Rounded.Forward10, contentDescription = "Forward", tint = contentColor, modifier = Modifier.size(32.dp))
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}