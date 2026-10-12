package com.michaelamimo.sumeria.ui.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.michaelamimo.sumeria.ui.theme.SumeriaTurtleLogo

// ---------------------------------------------------------------------------
// Palette (Adheres strictly to the Sumeria Design System)
// ---------------------------------------------------------------------------

private val Ink = Color(0xFF123B35)
private val Paper = Color(0xFFFFFCF5)
private val Cream = Color(0xFFFFF4DE)
private val Mint = Color(0xFFEAF4EC)
private val Sage = Color(0xFF829D8D)
private val Amber = Color(0xFFE6A843)
private val Coral = Color(0xFFF3A58C)
private val Lavender = Color(0xFFE8E3F5)
private val Sky = Color(0xFFDCEEF1)
private val Teal = Color(0xFF6FD3B5)
private val ErrorRed = Color(0xFFBA1A1A)
private val Line = Ink.copy(alpha = 0.08f)

private const val SIDE = 24 // Horizontal margin across all screens

// ---------------------------------------------------------------------------
// Main Settings Screen
// ---------------------------------------------------------------------------

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = remember(view) { view.context.findActivity() }

    // Status bar setup
    SideEffect {
        activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = false
        }
    }

    // Interactive Settings State
    var userName by remember { mutableStateOf("Amimo Michael") }
    var userEmail by remember { mutableStateOf("michael@sumeria.app") }
    var dailyGoalMinutes by remember { mutableIntStateOf(45) }
    var defaultReaderTheme by remember { mutableStateOf("Light") }
    var pageTurnStyle by remember { mutableStateOf("Scroll") }
    var offlineDictionaries by remember { mutableStateOf("English (US) & Spanish (240 MB)") }

    var autoCloudSync by remember { mutableStateOf(true) }
    var lastSyncTime by remember { mutableStateOf("Just now") }
    var isSyncing by remember { mutableStateOf(false) }

    var dailyReminders by remember { mutableStateOf(true) }
    var reminderTime by remember { mutableStateOf("8:00 PM") }
    var appThemeMode by remember { mutableStateOf("System") }

    var cacheSize by remember { mutableStateOf("128 MB") }

    // Dialog & Modal States
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDictionaryDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
    ) {
        // Status-bar background (Ink green)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Ink)
                .windowInsetsTopHeight(WindowInsets.statusBars)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp) // Scroll clearance for floating nav bar
        ) {
            // 1. Header
            item {
                Spacer(Modifier.height(20.dp))
                SettingsHeader()
                Spacer(Modifier.height(24.dp))
            }

            // 2. Profile Hero Card
            item {
                ProfileHeroCard(
                    name = userName,
                    email = userEmail,
                    onEditProfileClick = { showEditProfileDialog = true }
                )
                Spacer(Modifier.height(32.dp))
            }

            // 3. Reading Goals & Preferences
            item {
                SettingsSectionHeader(
                    eyebrow = "READING GOALS & PREFERENCES",
                    title = "Reading Defaults"
                )
                Spacer(Modifier.height(14.dp))

                // Daily Goal Card
                SettingCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Daily Reading Goal",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Ink
                            )
                            Text(
                                text = "Target duration to build your streak",
                                style = MaterialTheme.typography.bodySmall,
                                color = Sage
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = Cream,
                            border = BorderStroke(1.dp, Amber.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "$dailyGoalMinutes min/day",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Ink,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(30, 45, 60, 90).forEach { mins ->
                            ChoiceChip(
                                text = "${mins}m",
                                selected = dailyGoalMinutes == mins,
                                modifier = Modifier.weight(1f),
                                onClick = { dailyGoalMinutes = mins }
                            )
                        }

                        // Fine-tune stepper buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { if (dailyGoalMinutes > 10) dailyGoalMinutes -= 5 },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Mint, CircleShape)
                            ) {
                                Icon(Icons.Rounded.Remove, contentDescription = "Decrease goal", tint = Ink, modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { if (dailyGoalMinutes < 240) dailyGoalMinutes += 5 },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Mint, CircleShape)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Increase goal", tint = Ink, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Default Reader Theme & Style Card
                SettingCard {
                    Text(
                        text = "Default Reader Theme",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Ink
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Preferred paper backdrop when opening books",
                        style = MaterialTheme.typography.bodySmall,
                        color = Sage
                    )
                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Light" to Color(0xFFF9F9F7),
                            "Sepia" to Color(0xFFF4ECD8),
                            "Dark" to Color(0xFF1A1A1C),
                            "Midnight" to Color(0xFF121B19)
                        ).forEach { (themeLabel, themeBg) ->
                            val isSelected = defaultReaderTheme == themeLabel
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { defaultReaderTheme = themeLabel },
                                shape = RoundedCornerShape(14.dp),
                                color = themeBg,
                                border = BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) Amber else Ink.copy(alpha = 0.1f)
                                )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = themeLabel,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (themeLabel == "Dark" || themeLabel == "Midnight") Paper else Ink
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    HorizontalDivider(color = Line)
                    Spacer(Modifier.height(16.dp))

                    // Reading Navigation Style
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Navigation Style",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Ink
                            )
                            Text(
                                text = "Continuous vertical scroll or horizontal page swipe",
                                style = MaterialTheme.typography.bodySmall,
                                color = Sage
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ChoiceChip(
                                text = "Scroll",
                                selected = pageTurnStyle == "Scroll",
                                onClick = { pageTurnStyle = "Scroll" }
                            )
                            ChoiceChip(
                                text = "Swipe",
                                selected = pageTurnStyle == "Swipe",
                                onClick = { pageTurnStyle = "Swipe" }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Offline Dictionaries Card
                SettingCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Lavender,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Translate,
                                    contentDescription = "Dictionaries",
                                    tint = Ink,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Offline Dictionaries",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Ink
                            )
                            Text(
                                text = offlineDictionaries,
                                style = MaterialTheme.typography.bodySmall,
                                color = Sage,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Mint,
                            border = BorderStroke(1.dp, Line),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showDictionaryDialog = true }
                        ) {
                            Text(
                                text = "Manage",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Ink,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
            }

            // 4. Sync & Notifications
            item {
                SettingsSectionHeader(
                    eyebrow = "SYNC & NOTIFICATIONS",
                    title = "Cloud & Reminders"
                )
                Spacer(Modifier.height(14.dp))

                // Cloud Sync Card
                SettingCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Sky,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.CloudSync,
                                    contentDescription = "Cloud Sync",
                                    tint = Ink,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Library & Stats Sync",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Ink
                            )
                            Text(
                                text = if (isSyncing) "Syncing with cloud..." else "Last synced $lastSyncTime",
                                style = MaterialTheme.typography.bodySmall,
                                color = Sage
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Cream,
                            border = BorderStroke(1.dp, Amber.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    isSyncing = true
                                    Toast
                                        .makeText(context, "Syncing Sumeria library...", Toast.LENGTH_SHORT)
                                        .show()
                                    lastSyncTime = "Just now"
                                    isSyncing = false
                                }
                        ) {
                            Text(
                                text = "Sync Now",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Ink,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = Line)
                    Spacer(Modifier.height(14.dp))

                    // Auto-sync Toggle Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Automatic Background Sync",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = Ink
                        )

                        Switch(
                            checked = autoCloudSync,
                            onCheckedChange = { autoCloudSync = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Paper,
                                checkedTrackColor = Ink,
                                uncheckedThumbColor = Sage,
                                uncheckedTrackColor = Mint
                            )
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Notifications Card
                SettingCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Mint,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsActive,
                                    contentDescription = "Reminders",
                                    tint = Ink,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Daily Reading Reminders",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Ink
                            )
                            Text(
                                text = "Gentle nudge to maintain your streak",
                                style = MaterialTheme.typography.bodySmall,
                                color = Sage
                            )
                        }

                        Switch(
                            checked = dailyReminders,
                            onCheckedChange = { dailyReminders = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Paper,
                                checkedTrackColor = Ink,
                                uncheckedThumbColor = Sage,
                                uncheckedTrackColor = Mint
                            )
                        )
                    }

                    AnimatedVisibility(visible = dailyReminders) {
                        Column {
                            Spacer(Modifier.height(14.dp))
                            HorizontalDivider(color = Line)
                            Spacer(Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Reminder Schedule",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = Ink
                                )

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Lavender,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { showTimePickerDialog = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Schedule,
                                            contentDescription = null,
                                            tint = Ink,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = reminderTime,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Ink
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // App Theme Mode Card
                SettingCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "App Display Theme",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Ink
                            )
                            Text(
                                text = "Overall application UI appearance",
                                style = MaterialTheme.typography.bodySmall,
                                color = Sage
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("System", "Light", "Dark").forEach { mode ->
                                ChoiceChip(
                                    text = mode,
                                    selected = appThemeMode == mode,
                                    onClick = { appThemeMode = mode }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
            }

            // 5. About & Support
            item {
                SettingsSectionHeader(
                    eyebrow = "ABOUT & SUPPORT",
                    title = "App Info & Storage"
                )
                Spacer(Modifier.height(14.dp))

                // Storage & Cache Card
                SettingCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Coral.copy(alpha = 0.3f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Storage,
                                    contentDescription = "Storage",
                                    tint = Ink,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Cache & Local Storage",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Ink
                            )
                            Text(
                                text = "Cover images & temp files ($cacheSize)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Sage
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Line),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showClearCacheDialog = true }
                        ) {
                            Text(
                                text = "Clear",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Ink,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Help & Legal Card Group
                SettingCard {
                    // Help Center
                    SettingRow(
                        icon = Icons.Rounded.HelpOutline,
                        iconBg = Mint,
                        title = "Help Center & Reader Guide",
                        subtitle = "FAQs, shortcuts, and reader tips",
                        onClick = {
                            Toast.makeText(context, "Opening Sumeria Help Center...", Toast.LENGTH_SHORT).show()
                        }
                    )

                    HorizontalDivider(color = Line, modifier = Modifier.padding(vertical = 12.dp))

                    // Privacy Policy
                    SettingRow(
                        icon = Icons.Rounded.Security,
                        iconBg = Lavender,
                        title = "Privacy Policy & Terms",
                        subtitle = "How Sumeria protects your reading data",
                        onClick = {
                            Toast.makeText(context, "Opening Privacy Policy...", Toast.LENGTH_SHORT).show()
                        }
                    )

                    HorizontalDivider(color = Line, modifier = Modifier.padding(vertical = 12.dp))

                    // App Version & Status
                    SettingRow(
                        icon = Icons.Rounded.Info,
                        iconBg = Cream,
                        title = "Sumeria Version",
                        subtitle = "v1.0.0 (Build 2026.02) • Up to date ✓",
                        showChevron = false,
                        onClick = {
                            Toast.makeText(context, "Sumeria v1.0.0 is up to date!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                Spacer(Modifier.height(36.dp))
            }

            // 6. Logout Button
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SIDE.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { showLogoutDialog = true },
                    shape = RoundedCornerShape(22.dp),
                    color = ErrorRed.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 20.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = "Log Out",
                            tint = ErrorRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Log out of Sumeria",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = ErrorRed
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "Sumeria • Boundless reading, redefined",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = Sage,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Dialogs & Modals
    // ---------------------------------------------------------------------------

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        var tempName by remember { mutableStateOf(userName) }
        var tempEmail by remember { mutableStateOf(userEmail) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            containerColor = Paper,
            titleContentColor = Ink,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Edit Profile",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Ink,
                            unfocusedBorderColor = Line,
                            focusedLabelColor = Ink
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = tempEmail,
                        onValueChange = { tempEmail = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Ink,
                            unfocusedBorderColor = Line,
                            focusedLabelColor = Ink
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        userName = tempName
                        userEmail = tempEmail
                        showEditProfileDialog = false
                        Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditProfileDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Sage)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear Cache Dialog
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            containerColor = Paper,
            titleContentColor = Ink,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Clear Local Cache?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "This will remove temporary book covers and cached audio streams ($cacheSize). Your books and reading stats will remain safe.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        cacheSize = "0 MB"
                        showClearCacheDialog = false
                        Toast.makeText(context, "Cache cleared successfully", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Paper),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Clear Cache")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearCacheDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Sage)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Manage Dictionaries Dialog
    if (showDictionaryDialog) {
        AlertDialog(
            onDismissRequest = { showDictionaryDialog = false },
            containerColor = Paper,
            titleContentColor = Ink,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Offline Dictionaries",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Downloaded language packs for instant lookup during reading:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Sage
                    )
                    Spacer(Modifier.height(16.dp))

                    listOf(
                        "English (US) Dict" to "142 MB (Active)",
                        "Spanish (ES) Dict" to "98 MB (Active)",
                        "French (FR) Dict" to "110 MB (Download)"
                    ).forEach { (dictName, dictStatus) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(dictName, fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 14.sp)
                                Text(dictStatus, color = Sage, fontSize = 12.sp)
                            }
                            Surface(
                                shape = CircleShape,
                                color = Mint,
                                modifier = Modifier.clip(CircleShape).clickable {
                                    Toast.makeText(context, "Updated dictionary preferences", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("Select", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDictionaryDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Done")
                }
            }
        )
    }

    // Schedule Time Picker Dialog
    if (showTimePickerDialog) {
        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            containerColor = Paper,
            titleContentColor = Ink,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Set Reminder Time",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text("Choose when you would like to receive your daily reading reminder:", color = Sage, fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("7:00 AM", "12:30 PM", "8:00 PM", "9:30 PM").forEach { timeOption ->
                            ChoiceChip(
                                text = timeOption,
                                selected = reminderTime == timeOption,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    reminderTime = timeOption
                                    showTimePickerDialog = false
                                    Toast.makeText(context, "Reminder set for $timeOption", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimePickerDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = Ink)) {
                    Text("Close")
                }
            }
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = Paper,
            titleContentColor = Ink,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Log out of Sumeria?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Your reading progress and stats will be safely stored in the cloud until you log back in.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        Toast.makeText(context, "Logged out of Sumeria", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Paper),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Log Out")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Sage)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ---------------------------------------------------------------------------
// Header & Component Helpers
// ---------------------------------------------------------------------------

@Composable
private fun SettingsHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SIDE.dp)
    ) {
        // Brand Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SumeriaTurtleLogo(modifier = Modifier.size(35.dp))
                Spacer(Modifier.width(9.dp))
                Text(
                    text = "SUMERIA",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.7.sp
                    ),
                    color = Ink
                )
            }

            Surface(
                shape = CircleShape,
                color = Mint,
                border = BorderStroke(1.dp, Line)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚙️", fontSize = 14.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Preferences",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Ink
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = "YOUR PREFERENCES",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
            ),
            color = Sage
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Ink
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Customize reading habits, sync, and display defaults.",
            style = MaterialTheme.typography.bodyMedium,
            color = Sage
        )
    }
}

@Composable
private fun ProfileHeroCard(
    name: String,
    email: String,
    onEditProfileClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SIDE.dp),
        shape = RoundedCornerShape(30.dp),
        color = Ink
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.05f),
                        radius = size.width * 0.38f,
                        center = Offset(size.width * 0.95f, size.height * 0.1f)
                    )
                    drawCircle(
                        color = Amber.copy(alpha = 0.12f),
                        radius = size.width * 0.18f,
                        center = Offset(size.width * 0.05f, size.height * 0.95f)
                    )
                }
                .padding(22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Amber),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Ink
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Paper,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Rounded.Verified,
                        contentDescription = "Verified Member",
                        tint = Teal,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Mint.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(8.dp))

                Surface(
                    shape = CircleShape,
                    color = Mint.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, Mint.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = "✦ Scholar Tier • Member since 2026",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Mint,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Surface(
                shape = CircleShape,
                color = Paper,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onEditProfileClick)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit Profile",
                    tint = Ink,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(eyebrow: String, title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SIDE.dp)
    ) {
        Text(
            text = eyebrow,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            ),
            color = Sage
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Ink
        )
    }
}

@Composable
private fun SettingCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SIDE.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Ink.copy(alpha = 0.06f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            content = content
        )
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    showChevron: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = iconBg,
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Ink,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Ink
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Sage,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (showChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = Sage,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun ChoiceChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Ink else Mint)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Paper else Ink,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
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
