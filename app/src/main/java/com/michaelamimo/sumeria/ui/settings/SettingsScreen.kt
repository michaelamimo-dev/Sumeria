package com.michaelamimo.sumeria.ui.settings

import android.app.Activity
import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.michaelamimo.sumeria.R
import com.michaelamimo.sumeria.auth.AuthState
import com.michaelamimo.sumeria.auth.AuthViewModel
import com.michaelamimo.sumeria.ui.theme.SumeriaTurtleLogo
import com.michaelamimo.sumeria.notifications.ReadingReminderScheduler
import com.michaelamimo.sumeria.ui.library.LibraryViewModel

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

private const val SIDE = 24 // Horizontal margin across all main screens

// ---------------------------------------------------------------------------
// Main Settings Screen
// ---------------------------------------------------------------------------

@Composable
fun SettingsScreen(
    onSignOut: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
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

    // --- Interactive User & Preference States ---
    val authUser by viewModel.user.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val syncViewModel: CloudSyncViewModel = hiltViewModel()
    val syncState by syncViewModel.state.collectAsStateWithLifecycle()
    val libraryViewModel: LibraryViewModel = hiltViewModel()
    val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val audioVoiceViewModel: AudioVoiceSettingsViewModel = hiltViewModel()
    val selectedAudioVoice by audioVoiceViewModel.voiceName.collectAsStateWithLifecycle()
    var availableAudioVoices by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    val audioPreview = remember(context) {
        lateinit var engine: TextToSpeech
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                availableAudioVoices = engine.voices
                    .filter { it.name.isNotBlank() }
                    .sortedBy { it.name.lowercase() }
                    .map { it.name to it.locale.displayName }
            }
        }
        engine
    }
    DisposableEffect(audioPreview) { onDispose { audioPreview.stop(); audioPreview.shutdown() } }
    val folderPicker = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { libraryViewModel.scanFolder(it) }
    }
    var userName by remember(authUser) {
        mutableStateOf(authUser?.displayName ?: authUser?.email?.substringBefore("@") ?: "Sumeria Reader")
    }
    val userEmail = authUser?.email ?: "Not signed in"

    // Reading Goals & Preferences
    var dailyGoalMinutes by remember(syncState.preferences.dailyGoalMinutes) { mutableIntStateOf(syncState.preferences.dailyGoalMinutes) }
    var yearlyBookGoal by remember(syncState.preferences.yearlyBookGoal) { mutableIntStateOf(syncState.preferences.yearlyBookGoal) }
    var challengeBooksCount by remember { mutableIntStateOf(0) }
    var defaultReaderTheme by remember(syncState.preferences.readerTheme) { mutableStateOf(syncState.preferences.readerTheme) }
    var pageTurnStyle by remember { mutableStateOf("Scroll") }
    var offlineDictionaries by remember { mutableStateOf("English (US) & Spanish (240 MB)") }

    // Reminders & App Display
    var dailyReminders by remember(syncState.preferences.dailyRemindersEnabled) { mutableStateOf(syncState.preferences.dailyRemindersEnabled) }
    var reminderTime by remember(syncState.preferences.reminderMinutesAfterMidnight) {
        mutableStateOf(syncState.preferences.reminderMinutesAfterMidnight?.toReminderLabel() ?: "8:00 PM")
    }
    var appThemeMode by remember(syncState.preferences.appThemeMode) { mutableStateOf(syncState.preferences.appThemeMode) }

    // Storage
    var cacheSize by remember { mutableStateOf("128 MB") }

    // --- Dialog & Modal Visibility States ---
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showChangeEmailDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showYearlyGoalDialog by remember { mutableStateOf(false) }
    var showDictionaryDialog by remember { mutableStateOf(false) }
    var showVoiceSelectionDialog by remember { mutableStateOf(false) }
    var showConnectedServicesDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showBadgesModal by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    fun setDailyGoal(value: Int) {
        val bounded = value.coerceIn(5, 240)
        dailyGoalMinutes = bounded
        syncViewModel.updateDailyGoal(bounded)
    }
    val requestNotificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        dailyReminders = granted
        if (granted) {
            val minutes = reminderTime.toReminderMinutes()
            syncViewModel.updateDailyReminder(true, minutes)
            ReadingReminderScheduler.schedule(context, true, minutes)
        } else {
            Toast.makeText(context, "Allow notifications to receive reading reminders.", Toast.LENGTH_SHORT).show()
        }
    }

    fun setDailyReminderEnabled(enabled: Boolean) {
        val minutes = reminderTime.toReminderMinutes()
        if (!enabled) {
            dailyReminders = false
            syncViewModel.updateDailyReminder(false, minutes)
            ReadingReminderScheduler.schedule(context, false, minutes)
            return
        }
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            dailyReminders = true
            syncViewModel.updateDailyReminder(true, minutes)
            ReadingReminderScheduler.schedule(context, true, minutes)
        }
    }

    LaunchedEffect(authState) {
        when (val current = authState) {
            is AuthState.AccountActionCompleted -> {
                Toast.makeText(context, current.message, Toast.LENGTH_SHORT).show()
                showEditNameDialog = false
                showChangeEmailDialog = false
                showChangePasswordDialog = false
                viewModel.resetState()
            }
            is AuthState.Error -> {
                if (showEditNameDialog || showChangeEmailDialog || showChangePasswordDialog) {
                    Toast.makeText(context, current.message, Toast.LENGTH_LONG).show()
                    viewModel.resetState()
                }
            }
            else -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
    ) {
        // Dark Green Status-Bar Background (Ink)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Ink)
                .windowInsetsTopHeight(WindowInsets.statusBars)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp) // Clearance for floating bottom nav bar
        ) {
            // 1. Header with ic_settings illustration
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
                    onEditNameClick = { showEditNameDialog = true },
                    onViewBadgesClick = { showBadgesModal = true }
                )
                Spacer(Modifier.height(32.dp))
            }

            // 3. Account & Security Section
            item {
                SettingsSectionHeader(
                    eyebrow = "ACCOUNT & SECURITY",
                    title = "Credentials & Security"
                )
                Spacer(Modifier.height(14.dp))

                SettingCard {
                    // Email Management Row
                    SettingRow(
                        icon = Icons.Rounded.Email,
                        iconBg = Sky,
                        title = "Email Address",
                        subtitle = userEmail,
                        actionText = "Change Email",
                        onClick = { showChangeEmailDialog = true }
                    )

                    HorizontalDivider(color = Line, modifier = Modifier.padding(vertical = 12.dp))

                    // Password Management Row
                    SettingRow(
                        icon = Icons.Rounded.Lock,
                        iconBg = Cream,
                        title = "Password & Security",
                        subtitle = "Update password and login credentials",
                        actionText = "Change",
                        onClick = { showChangePasswordDialog = true }
                    )
                }

                Spacer(Modifier.height(32.dp))
            }

            // 4. Reading Goals & Preferences
            item {
                SettingsSectionHeader(
                    eyebrow = "READING GOALS & PREFERENCES",
                    title = "Goals & Reader Defaults"
                )
                Spacer(Modifier.height(14.dp))

                // Daily Reading Goal Card
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
                                text = "Target daily time to maintain reading habit",
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

                    // Low-level scalable quick chips (10, 15, 20, 25)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(10, 15, 20, 25).forEach { mins ->
                            ChoiceChip(
                                text = "${mins}m",
                                selected = dailyGoalMinutes == mins,
                                modifier = Modifier.weight(1f),
                                onClick = { setDailyGoal(mins) }
                            )
                        }

                        // Fine-tune stepper buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { if (dailyGoalMinutes > 5) setDailyGoal(dailyGoalMinutes - 5) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Mint, CircleShape)
                            ) {
                                Icon(Icons.Rounded.Remove, contentDescription = "Decrease goal", tint = Ink, modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { if (dailyGoalMinutes < 240) setDailyGoal(dailyGoalMinutes + 5) },
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

                // Yearly Book Challenge Card
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
                                    imageVector = Icons.Rounded.EmojiEvents,
                                    contentDescription = "Yearly Challenge",
                                    tint = Ink,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "2026 Yearly Book Challenge",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Ink
                            )
                            Text(
                                text = "Target: $yearlyBookGoal books ($challengeBooksCount selected in challenge)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Sage
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Mint,
                            border = BorderStroke(1.dp, Line),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showYearlyGoalDialog = true }
                        ) {
                            Text(
                                text = "Configure",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Ink,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Default Reader Theme & Navigation Style Card
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
                                    .clickable { defaultReaderTheme = themeLabel; syncViewModel.updateReaderTheme(themeLabel) },
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

                // Offline Dictionaries & Audio Reader Voices Group Card
                SettingCard {
                    // Offline Dictionaries
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

                    HorizontalDivider(color = Line, modifier = Modifier.padding(vertical = 16.dp))

                    // Audio Reader Voices
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
                                    imageVector = Icons.Rounded.RecordVoiceOver,
                                    contentDescription = "Audio Voices",
                                    tint = Ink,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Audio Reader Voice",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Ink
                            )
                            Text(
                                text = selectedAudioVoice.ifBlank { "Device default voice" },
                                style = MaterialTheme.typography.bodySmall,
                                color = Sage,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Cream,
                            border = BorderStroke(1.dp, Amber.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showVoiceSelectionDialog = true }
                        ) {
                            Text(
                                text = "Voice",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Ink,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
            }

            // 5. Achievements & Reading Tiers
            item {
                SettingsSectionHeader(
                    eyebrow = "ACHIEVEMENTS & TIERS",
                    title = "Milestones & Badges"
                )
                Spacer(Modifier.height(14.dp))

                SettingCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Amber,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("✦", fontSize = 24.sp, color = Ink)
                            }
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Scholar Tier Member",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Ink
                            )
                            Text(
                                text = "42 Book Streak • 78% to Master Scholar",
                                style = MaterialTheme.typography.bodySmall,
                                color = Sage
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Mint,
                            border = BorderStroke(1.dp, Line),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showBadgesModal = true }
                        ) {
                            Text(
                                text = "All Badges",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Ink,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Badges Quick Preview Strip
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val badgeList = listOf(
                            "🔥" to "Streak Master",
                            "💯" to "Centurion",
                            "🦉" to "Night Owl",
                            "🧭" to "Explorer",
                            "📚" to "Collector"
                        )
                        items(badgeList) { (icon, badgeName) ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Cream,
                                border = BorderStroke(1.dp, Amber.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(icon, fontSize = 14.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = badgeName,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Ink
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
            }

            // 6. Connected Services & Cloud
            item {
                SettingsSectionHeader(
                    eyebrow = "CONNECTED SERVICES & CLOUD",
                    title = "Cloud Accounts & Backup"
                )
                Spacer(Modifier.height(14.dp))

                SettingCard {
                    // Connected Services Item
                    SettingRow(
                        icon = Icons.Rounded.CloudQueue,
                        iconBg = Sky,
                        title = "Third-Party Storage Accounts",
                        subtitle = "Export your library or save through a cloud storage provider",
                        actionText = "Manage",
                        onClick = { showConnectedServicesDialog = true }
                    )

                    HorizontalDivider(color = Line, modifier = Modifier.padding(vertical = 12.dp))

                    // Cloud Sync Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Library & Stats Sync",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Ink
                            )
                            Text(
                                text = syncState.status,
                                style = MaterialTheme.typography.bodySmall,
                                color = Sage
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Cream,
                            border = BorderStroke(1.dp, Amber.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = !syncState.isSyncing) { syncViewModel.syncNow() }
                        ) {
                            Text(
                                text = if (syncState.isSyncing) "Syncing…" else "Sync Now",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Ink,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Auto Sync Toggle
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
                            checked = syncState.autoSyncEnabled,
                            onCheckedChange = syncViewModel::setAutoSync,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Paper,
                                checkedTrackColor = Ink,
                                uncheckedThumbColor = Sage,
                                uncheckedTrackColor = Mint
                            )
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))
            }

            // 7. Reminders & App Display
            item {
                SettingsSectionHeader(
                    eyebrow = "REMINDERS & DISPLAY",
                    title = "Notifications & Theme"
                )
                Spacer(Modifier.height(14.dp))

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
                            onCheckedChange = ::setDailyReminderEnabled,
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
                                    onClick = { appThemeMode = mode; syncViewModel.updateAppTheme(mode) }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
            }

            // 8. About & Support
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
                        icon = Icons.Rounded.Help,
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

            // 9. Logout Button
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

    // 1. Edit Name Dialog
    if (showEditNameDialog) {
        var tempName by remember { mutableStateOf(userName) }

        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            containerColor = Paper,
            titleContentColor = Ink,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Edit Display Name",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
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
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateDisplayName(tempName)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = Sage)) {
                    Text("Cancel")
                }
            }
        )
    }

    // 2. Change Email Dialog
    if (showChangeEmailDialog) {
        var tempEmail by remember { mutableStateOf(userEmail) }
        var confirmEmail by remember { mutableStateOf("") }
        var currentEmailPassword by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showChangeEmailDialog = false },
            containerColor = Paper,
            titleContentColor = Ink,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Change Email Address",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text("Enter your new email address below. You may need to verify this email.", style = MaterialTheme.typography.bodySmall, color = Sage)
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = tempEmail,
                        onValueChange = { tempEmail = it },
                        label = { Text("New Email") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ink, unfocusedBorderColor = Line, focusedLabelColor = Ink),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = confirmEmail,
                        onValueChange = { confirmEmail = it },
                        label = { Text("Confirm New Email") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ink, unfocusedBorderColor = Line, focusedLabelColor = Ink),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = currentEmailPassword,
                        onValueChange = { currentEmailPassword = it },
                        label = { Text("Current Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ink, unfocusedBorderColor = Line, focusedLabelColor = Ink),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempEmail != confirmEmail) {
                            Toast.makeText(context, "Email addresses do not match.", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.updateEmail(tempEmail, currentEmailPassword)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Update Email")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangeEmailDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = Sage)) {
                    Text("Cancel")
                }
            }
        )
    }

    // 3. Change Password Dialog
    if (showChangePasswordDialog) {
        var currentPass by remember { mutableStateOf("") }
        var newPass by remember { mutableStateOf("") }
        var confirmPass by remember { mutableStateOf("") }
        var passVisible by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            containerColor = Paper,
            titleContentColor = Ink,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Change Password",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = currentPass,
                        onValueChange = { currentPass = it },
                        label = { Text("Current Password") },
                        singleLine = true,
                        visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passVisible = !passVisible }) {
                                Icon(if (passVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, contentDescription = if (passVisible) "Hide password" else "Show password", tint = Sage)
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ink, unfocusedBorderColor = Line, focusedLabelColor = Ink),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("New Password") },
                        singleLine = true,
                        visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passVisible = !passVisible }) {
                                Icon(if (passVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, contentDescription = if (passVisible) "Hide password" else "Show password", tint = Sage)
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ink, unfocusedBorderColor = Line, focusedLabelColor = Ink),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = confirmPass,
                        onValueChange = { confirmPass = it },
                        label = { Text("Confirm New Password") },
                        singleLine = true,
                        visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passVisible = !passVisible }) {
                                Icon(if (passVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, contentDescription = if (passVisible) "Hide password" else "Show password", tint = Sage)
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ink, unfocusedBorderColor = Line, focusedLabelColor = Ink),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPass != confirmPass) {
                            Toast.makeText(context, "New passwords do not match.", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.updatePassword(currentPass, newPass)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Update Password")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = Sage)) {
                    Text("Cancel")
                }
            }
        )
    }

    // 4. Yearly Reading Challenge Dialog
    if (showYearlyGoalDialog) {
        var tempGoal by remember { mutableIntStateOf(yearlyBookGoal) }
        val availableBooks = libraryState.books
        var selectedBooks by remember(availableBooks) { mutableStateOf(emptySet<String>()) }

        AlertDialog(
            onDismissRequest = { showYearlyGoalDialog = false },
            containerColor = Paper,
            titleContentColor = Ink,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "2026 Yearly Reading Challenge",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text("Set target number of books for 2026:", style = MaterialTheme.typography.bodySmall, color = Sage)
                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Target Books:", fontWeight = FontWeight.Bold, color = Ink)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { if (tempGoal > 1) tempGoal-- }, modifier = Modifier.size(32.dp).background(Mint, CircleShape)) {
                                Icon(Icons.Rounded.Remove, contentDescription = null, tint = Ink)
                            }
                            Text("$tempGoal", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
                            IconButton(onClick = { tempGoal++ }, modifier = Modifier.size(32.dp).background(Mint, CircleShape)) {
                                Icon(Icons.Rounded.Add, contentDescription = null, tint = Ink)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("Select challenge books from library:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Ink)
                    Spacer(Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (availableBooks.isEmpty()) {
                            Text("Add books to your library first to choose books for this challenge.", color = Sage, fontSize = 13.sp)
                        }
                        availableBooks.forEach { book ->
                            val isSelected = selectedBooks.contains(book.id)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Mint else Color.White,
                                border = BorderStroke(1.dp, Line),
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable {
                                    selectedBooks = if (isSelected) selectedBooks - book.id else selectedBooks + book.id
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(book.title, fontSize = 13.sp, color = Ink)
                                    if (isSelected) Text("✓ Included", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Ink)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        yearlyBookGoal = tempGoal
                        challengeBooksCount = selectedBooks.size
                        syncViewModel.updateYearlyBookGoal(tempGoal)
                        showYearlyGoalDialog = false
                        Toast.makeText(context, "2026 challenge updated ($yearlyBookGoal books)", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Challenge")
                }
            },
            dismissButton = {
                TextButton(onClick = { showYearlyGoalDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = Sage)) {
                    Text("Cancel")
                }
            }
        )
    }

    // 5. Audio Reader Voices Selection Dialog
    if (showVoiceSelectionDialog) {
        AlertDialog(
            onDismissRequest = { showVoiceSelectionDialog = false },
            containerColor = Paper,
            titleContentColor = Ink,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Audio Reader Voice",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose and preview a voice available on this device:", style = MaterialTheme.typography.bodySmall, color = Sage)
                    Spacer(Modifier.height(4.dp))

                    if (availableAudioVoices.isEmpty()) {
                        Text("No installed text-to-speech voices are available yet.", color = Sage, style = MaterialTheme.typography.bodyMedium)
                    }
                    availableAudioVoices.forEach { (voiceTitle, voiceDesc) ->
                        val isSelected = selectedAudioVoice == voiceTitle
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Mint else Color.White,
                            border = BorderStroke(1.dp, if (isSelected) Ink else Line),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    audioVoiceViewModel.selectVoice(voiceTitle)
                                    audioPreview.voices.firstOrNull { it.name == voiceTitle }?.let(audioPreview::setVoice)
                                    audioPreview.speak(
                                        "Welcome to Sumeria. This is a sample of your selected reading voice.",
                                        TextToSpeech.QUEUE_FLUSH,
                                        null,
                                        "voice-preview"
                                    )
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Rounded.VolumeUp else Icons.Rounded.RecordVoiceOver,
                                    contentDescription = null,
                                    tint = if (isSelected) Ink else Sage,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(voiceTitle.replace('_', ' '), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Ink)
                                    Text(voiceDesc, fontSize = 11.sp, color = Sage)
                                }
                                Text(if (isSelected) "Selected · tap to sample" else "Tap to select & sample", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Ink)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVoiceSelectionDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = Ink)) {
                    Text("Close")
                }
            }
        )
    }

    // 6. Connected Services Dialog
    if (showConnectedServicesDialog) {
        AlertDialog(
            onDismissRequest = { showConnectedServicesDialog = false },
            containerColor = Paper,
            titleContentColor = Ink,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Connected Storage Accounts",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Connect a cloud library to browse its EPUB and PDF files from Sumeria.", style = MaterialTheme.typography.bodySmall, color = Sage)
                    listOf("Dropbox", "Google Drive").forEach { service ->
                        OutlinedButton(
                            onClick = {
                                showConnectedServicesDialog = false
                                folderPicker.launch(null)
                                Toast.makeText(context, "Choose $service in the Android storage picker.", Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink),
                            border = BorderStroke(1.dp, Line),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Connect $service") }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showConnectedServicesDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Done")
                }
            }
        )
    }

    // 7. Flexible Custom Time Picker Dialog
    if (showTimePickerDialog) {
        var selectedPresetTime by remember { mutableStateOf(reminderTime) }
        var customHour by remember { mutableStateOf("08") }
        var customMinute by remember { mutableStateOf("00") }
        var customAmPm by remember { mutableStateOf("PM") }

        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            containerColor = Paper,
            titleContentColor = Ink,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Reading Reminder Time",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text("Select a preset slot or enter your preferred custom time:", style = MaterialTheme.typography.bodySmall, color = Sage)
                    Spacer(Modifier.height(14.dp))

                    Text("Quick Presets:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Ink)
                    Spacer(Modifier.height(6.dp))

                    // Preset chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("7:00 AM", "12:30 PM", "8:00 PM", "9:30 PM").forEach { preset ->
                            ChoiceChip(
                                text = preset,
                                selected = selectedPresetTime == preset,
                                onClick = { selectedPresetTime = preset }
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    HorizontalDivider(color = Line)
                    Spacer(Modifier.height(16.dp))

                    Text("Custom Time Input:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Ink)
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        OutlinedTextField(
                            value = customHour,
                            onValueChange = { if (it.length <= 2) { customHour = it; selectedPresetTime = "" } },
                            label = { Text("Hour") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.width(72.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ink, unfocusedBorderColor = Line)
                        )

                        Text(" : ", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Ink, modifier = Modifier.padding(horizontal = 4.dp))

                        OutlinedTextField(
                            value = customMinute,
                            onValueChange = { if (it.length <= 2) { customMinute = it; selectedPresetTime = "" } },
                            label = { Text("Min") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.width(72.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ink, unfocusedBorderColor = Line)
                        )

                        Spacer(Modifier.width(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            ChoiceChip("AM", customAmPm == "AM") { customAmPm = "AM" }
                            ChoiceChip("PM", customAmPm == "PM") { customAmPm = "PM" }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val formattedCustom = "${customHour.padStart(2, '0')}:${customMinute.padStart(2, '0')} $customAmPm"
                        reminderTime = if (selectedPresetTime.isNotEmpty()) selectedPresetTime else formattedCustom
                        val minutes = reminderTime.toReminderMinutes()
                        syncViewModel.updateDailyReminder(dailyReminders, minutes)
                        ReadingReminderScheduler.schedule(context, dailyReminders, minutes)
                        showTimePickerDialog = false
                        Toast.makeText(context, "Reminder set for $reminderTime", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Set Time")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = Sage)) {
                    Text("Cancel")
                }
            }
        )
    }

    // 8. Milestone Badges Showcase Modal
    if (showBadgesModal) {
        val badges = listOf(
            Triple("🔥", "Streak Master", "Maintained a 30+ day consecutive reading streak"),
            Triple("💯", "Centurion", "Read over 100 hours in Sumeria"),
            Triple("🦉", "Night Owl", "Completed 15 late night reading sessions"),
            Triple("🧭", "Genre Explorer", "Read books across 5 different categories"),
            Triple("📚", "Library Builder", "Gathered over 50 books in personal shelves"),
            Triple("⚡", "Speed Reader", "Finished 3 books in a single week"),
            Triple("🎧", "Audio Scholar", "Listened to 20 hours of audio narration"),
            Triple("✦", "Scholar Legend", "Unlocked Tier 4 Scholar status")
        )

        AlertDialog(
            onDismissRequest = { showBadgesModal = false },
            containerColor = Paper,
            titleContentColor = Ink,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Sumeria Milestone Badges",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Unlocked reading achievements and milestone badges:", style = MaterialTheme.typography.bodySmall, color = Sage)
                    Spacer(Modifier.height(4.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(badges) { (emoji, badgeTitle, badgeDesc) ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Cream,
                                border = BorderStroke(1.dp, Amber.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(emoji, fontSize = 24.sp)
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(badgeTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Ink)
                                        Text(badgeDesc, fontSize = 11.sp, color = Sage)
                                    }
                                    Text("✓ Unlocked", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Ink)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showBadgesModal = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close")
                }
            }
        )
    }

    // 9. Clear Cache Dialog
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
                    text = "This will remove temporary book covers and cached audio streams ($cacheSize). Your books and stats will remain safe.",
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
                TextButton(onClick = { showClearCacheDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = Sage)) {
                    Text("Cancel")
                }
            }
        )
    }

    // 10. Logout Confirmation Dialog
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
                        viewModel.signOut()
                        Toast.makeText(context, "Logged out of Sumeria", Toast.LENGTH_LONG).show()
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Paper),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Log Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = Sage)) {
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

        // Headline + R.drawable.ic_settings illustration
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
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

            Spacer(Modifier.width(12.dp))

            Image(
                painter = painterResource(id = R.drawable.ic_settings),
                contentDescription = "Settings illustration",
                modifier = Modifier
                    .width(96.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }
    }
}

@Composable
private fun ProfileHeroCard(
    name: String,
    email: String,
    onEditNameClick: () -> Unit,
    onViewBadgesClick: () -> Unit
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
                    border = BorderStroke(1.dp, Mint.copy(alpha = 0.25f)),
                    modifier = Modifier.clip(CircleShape).clickable(onClick = onViewBadgesClick)
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
                    .clickable(onClick = onEditNameClick)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit Display Name",
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
    actionText: String? = null,
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

        Spacer(Modifier.width(8.dp))

        if (actionText != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Mint,
                border = BorderStroke(1.dp, Line)
            ) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Ink,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        } else if (showChevron) {
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

private fun String.toReminderMinutes(): Int = runCatching {
    val parts = trim().split(Regex("\\s+"))
    val time = parts[0].split(":")
    val rawHour = time[0].toInt().coerceIn(1, 12)
    val minute = time[1].toInt().coerceIn(0, 59)
    val isPm = parts.getOrNull(1)?.equals("PM", ignoreCase = true) == true
    val hour = (rawHour % 12) + if (isPm) 12 else 0
    hour * 60 + minute
}.getOrDefault(20 * 60)

private fun Int.toReminderLabel(): String {
    val hour24 = (this / 60).coerceIn(0, 23)
    val minute = (this % 60).coerceIn(0, 59)
    val suffix = if (hour24 < 12) "AM" else "PM"
    val hour12 = when (val hour = hour24 % 12) { 0 -> 12; else -> hour }
    return "$hour12:${minute.toString().padStart(2, '0')} $suffix"
}
