package com.michaelamimo.sumeria.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.michaelamimo.sumeria.ui.theme.SumeriaColors

@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SumeriaColors.SurfaceWhite)
    ) {
        // Correct implementation: Fixed dark green box drawn strictly behind the status bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SumeriaColors.ActionPrimary)
                .windowInsetsTopHeight(WindowInsets.statusBars)
        )

        // Centered Content
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Home",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = SumeriaColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "More coming soon",
                    style = MaterialTheme.typography.bodyLarge,
                    color = SumeriaColors.Muted
                )
            }
        }
    }
}