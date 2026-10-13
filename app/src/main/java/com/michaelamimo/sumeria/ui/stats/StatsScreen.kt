package com.michaelamimo.sumeria.ui.stats

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import com.michaelamimo.sumeria.R
import com.michaelamimo.sumeria.domain.model.ReadingSession
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// Palette (same family as Home and Library)
// ---------------------------------------------------------------------------

private val Ink = Color(0xFF123B35)
private val Paper = Color(0xFFFFFCF5)
private val Cream = Color(0xFFFFF4DE)
private val Mint = Color(0xFFEAF4EC)
private val MintStage = Color(0xFFD6F1E3)
private val Sage = Color(0xFF829D8D)
private val Amber = Color(0xFFE6A843)
private val Coral = Color(0xFFF3A58C)
private val Lavender = Color(0xFFE8E3F5)
private val LavenderDeep = Color(0xFFB9ACE6)
private val Sky = Color(0xFFDCEEF1)
private val SkyDeep = Color(0xFF8CCAD6)
private val Teal = Color(0xFF6FD3B5)
private val Green = Color(0xFF2E9B7C)
private val Line = Ink.copy(alpha = 0.08f)

private const val SIDE = 24 // horizontal page margin, matches Home and Library

private val MonthNames = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)
private val MonthFullNames = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)

private sealed class EditableGoal(val value: Int) {
    class Daily(minutes: Int) : EditableGoal(minutes)
    class Yearly(books: Int) : EditableGoal(books)
}

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val data by viewModel.uiState.collectAsStateWithLifecycle()
    val view = LocalView.current
    val activity = remember(view) { view.context.findActivity() }

    SideEffect {
        activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = false
        }
    }

    var range by remember { mutableStateOf(StatsRange.Week) }
    val stats = remember(range, data) { StatsData.statsFor(range, data) }
    var selectedBar by remember(range) { mutableStateOf(stats.bars.indexOfLast { !it.isFuture }) }

    val today = remember(data) { StatsData.today(data.sessions, data.dailyGoalMinutes) }
    val clock = remember(data) { StatsData.readingClock(data.sessions) }
    val genres = remember(data) { StatsData.genres(data) }
    val heatmapWeeks = remember(data) { StatsData.heatmapWeeks(data.sessions) }
    val badges = remember(data) { StatsData.badges(data, today.streak) }
    val recentBooks = remember(data) { StatsData.recentBooks(data) }
    val yearGoal = remember(data) { StatsData.yearGoal(data) }
    var editingGoal by remember { mutableStateOf<EditableGoal?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
    ) {
        // Fixed status-bar background, outside the scrolling content.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Ink)
                .windowInsetsTopHeight(WindowInsets.statusBars)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Header
            item {
                Spacer(Modifier.height(20.dp))
                StatsHeader(streak = today.streak)
                Spacer(Modifier.height(24.dp))
            }

            // Range selector + summary
            item {
                RangeSelector(selected = range, onSelect = { range = it })
                Spacer(Modifier.height(16.dp))
                TimeReadCard(stats = stats)
                Spacer(Modifier.height(14.dp))
            }

            // Today's goal + streak
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SIDE.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GoalRingCard(
                        today = today,
                        modifier = Modifier.weight(1f),
                        onEditGoal = { editingGoal = EditableGoal.Daily(data.dailyGoalMinutes) }
                    )
                    StreakCard(today = today, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(32.dp))
            }

            // Activity chart
            item {
                StatsSectionHeader(
                    eyebrow = "YOUR RHYTHM",
                    title = stats.chartTitle,
                    trailing = stats.goalLabel
                )
                Spacer(Modifier.height(16.dp))
                ActivityCard(
                    stats = stats,
                    selected = selectedBar,
                    onSelect = { selectedBar = it }
                )
                Spacer(Modifier.height(32.dp))
            }

            // Highlights
            item {
                StatsSectionHeader(
                    eyebrow = "AT A GLANCE",
                    title = "Highlights",
                    trailing = stats.periodLabel
                )
                Spacer(Modifier.height(16.dp))

                val tiles = listOf(
                    StatTileData("📖", "Books finished", "${stats.booksFinished}", stats.booksNote, Mint),
                    StatTileData("📄", "Pages read", formatNumber(stats.pages), stats.pagesNote, Cream),
                    StatTileData("⚡", "Reading pace", "${stats.pacePagesPerHour} pg/hr", stats.paceNote, Lavender),
                    StatTileData("⏱️", "Avg session", "${stats.avgSessionMin} min", "${stats.sessions} sessions", Sky),
                    StatTileData("🏆", "Longest session", formatDuration(stats.longestSessionMin), stats.longestNote, Coral.copy(alpha = 0.45f)),
                    StatTileData("📅", "Active days", "${stats.activeDays}/${stats.periodDays}", stats.activeNote, Mint)
                )
                tiles.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = SIDE.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEach { StatTile(data = it, modifier = Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Spacer(Modifier.height(20.dp))
            }

            // Reading clock
            item {
                StatsSectionHeader(
                    eyebrow = "WHEN YOU READ",
                    title = "Your reading clock",
                    trailing = null
                )
                Spacer(Modifier.height(16.dp))
                ReadingClockCard(parts = clock)
                Spacer(Modifier.height(32.dp))
            }

            // Genres
            item {
                StatsSectionHeader(
                    eyebrow = "WHAT YOU READ",
                    title = "Genre mix",
                    trailing = "by reading time"
                )
                Spacer(Modifier.height(16.dp))
                GenreCard(genres = genres)
                Spacer(Modifier.height(32.dp))
            }

            // Yearly goal
            item {
                StatsSectionHeader(
                    eyebrow = "THE BIG PICTURE",
                    title = "Yearly goal",
                    trailing = null
                )
                Spacer(Modifier.height(16.dp))
                YearGoalCard(goal = yearGoal, onEditGoal = { editingGoal = EditableGoal.Yearly(data.yearlyBookGoal) })
                Spacer(Modifier.height(32.dp))
            }

            // Consistency heatmap
            item {
                val allCells = heatmapWeeks.flatMap { it.cells }
                val activeDays = allCells.count { it.minutes > 0 }
                StatsSectionHeader(
                    eyebrow = "CONSISTENCY",
                    title = "Last 5 weeks",
                    trailing = "$activeDays of ${allCells.size} days"
                )
                Spacer(Modifier.height(16.dp))
                HeatmapCard(weeks = heatmapWeeks, goal = today.goalMinutes)
                Spacer(Modifier.height(32.dp))
            }

            // Insights
            item {
                StatsSectionHeader(
                    eyebrow = "WORTH NOTICING",
                    title = "Insights",
                    trailing = null
                )
                Spacer(Modifier.height(16.dp))
                InsightsList(stats = stats, clock = clock, today = today)
                Spacer(Modifier.height(32.dp))
            }

            // Milestones
            item {
                StatsSectionHeader(
                    eyebrow = "MILESTONES",
                    title = "Badges",
                    trailing = "${badges.count { it.progress >= 1f }} of ${badges.size} earned"
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = SIDE.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    badges.forEach { BadgeItem(badge = it) }
                }
                Spacer(Modifier.height(32.dp))
            }

            // Recently finished
            item {
                StatsSectionHeader(
                    eyebrow = "ON THE SHELF",
                    title = "Recently read",
                    trailing = null
                )
                Spacer(Modifier.height(16.dp))
                recentBooks.forEach { book ->
                    RecentBookRow(book = book)
                    Spacer(Modifier.height(10.dp))
                }
                Spacer(Modifier.height(24.dp))
            }

            // Footer
            item {
                StatsFooter()
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    editingGoal?.let { goal ->
        GoalEditDialog(
            goal = goal,
            onDismiss = { editingGoal = null },
            onSave = { newValue ->
                when (goal) {
                    is EditableGoal.Daily -> viewModel.updateGoals(newValue, data.yearlyBookGoal)
                    is EditableGoal.Yearly -> viewModel.updateGoals(data.dailyGoalMinutes, newValue)
                }
                editingGoal = null
            }
        )
    }
}

@Composable
private fun GoalEditDialog(
    goal: EditableGoal,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var value by remember(goal) { mutableStateOf(goal.value.toString()) }
    val daily = goal is EditableGoal.Daily
    val parsedValue = value.toIntOrNull()?.takeIf { it > 0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (daily) "Daily reading goal" else "Yearly book goal") },
        text = {
            Column {
                Text(if (daily) "Choose how many minutes you want to read each day." else "Choose how many books you want to finish this year.")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.filter { character -> character.isDigit() } },
                    label = { Text(if (daily) "Minutes per day" else "Books per year") },
                    suffix = { Text(if (daily) "min/day" else "books/year") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = parsedValue != null,
                onClick = { parsedValue?.let(onSave) }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ---------------------------------------------------------------------------
// Header: split card (text on cream, illustration on a mint stage)
// ---------------------------------------------------------------------------

@Composable
private fun StatsHeader(streak: Int) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SIDE.dp),
        shape = RoundedCornerShape(32.dp),
        color = Cream,
        border = BorderStroke(1.dp, Amber.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1.25f)
                    .fillMaxHeight()
                    .padding(start = 20.dp, top = 18.dp, bottom = 18.dp, end = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_sumeria_turtle),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "YOUR STATS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp
                        ),
                        color = Ink,
                        maxLines = 1
                    )
                }

                Text(
                    text = "Your\nreading\nrhythm.",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 33.sp
                    ),
                    color = Ink
                )

                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(1.dp, Line)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔥", fontSize = 14.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "$streak day streak",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Ink
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp))
                    .background(MintStage)
                    .drawBehind {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.45f),
                            radius = size.width * 0.55f,
                            center = Offset(size.width * 0.95f, size.height * 0.08f)
                        )
                        drawCircle(
                            color = Ink.copy(alpha = 0.05f),
                            radius = size.width * 0.7f,
                            center = Offset(size.width * 0.95f, size.height * 0.08f),
                            style = Stroke(width = 1.dp.toPx())
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.3f),
                            radius = size.width * 0.26f,
                            center = Offset(size.width * 0.1f, size.height * 0.96f)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_stats_screen_top),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .height(112.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Range selector + summary
// ---------------------------------------------------------------------------

@Composable
private fun RangeSelector(
    selected: StatsRange,
    onSelect: (StatsRange) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SIDE.dp)
            .height(52.dp),
        shape = CircleShape,
        color = Color.White,
        border = BorderStroke(1.dp, Line)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            StatsRange.entries.forEach { option ->
                val isSelected = option == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(if (isSelected) Ink else Color.Transparent)
                        .clickable { onSelect(option) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = if (isSelected) Paper else Sage
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeReadCard(stats: RangeStats) {
    val up = stats.deltaPercent >= 0

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SIDE.dp),
        shape = RoundedCornerShape(30.dp),
        color = Ink
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.05f),
                        radius = size.width * 0.38f,
                        center = Offset(size.width * 0.95f, size.height * 0.05f)
                    )
                    drawCircle(
                        color = Amber.copy(alpha = 0.10f),
                        radius = size.width * 0.16f,
                        center = Offset(size.width * 0.05f, size.height * 1.0f)
                    )
                }
                .padding(22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "TIME READ · ${stats.periodLabel.uppercase()}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp
                        ),
                        color = Mint.copy(alpha = 0.76f)
                    )
                    Spacer(Modifier.height(8.dp))
                    DurationText(minutes = stats.totalMinutes, color = Paper)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.12f)) {
                        Text(
                            text = "${if (up) "▲" else "▼"} ${abs(stats.deltaPercent)}%",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (up) Teal else Coral,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stats.comparison,
                        style = MaterialTheme.typography.labelSmall,
                        color = Mint.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InkMiniStat("${stats.sessions}", "sessions", Modifier.weight(1f))
                InkMiniStat("${stats.avgSessionMin}m", "avg session", Modifier.weight(1f))
                InkMiniStat(formatNumber(stats.pages), "pages", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun InkMiniStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Paper,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Mint.copy(alpha = 0.72f),
                maxLines = 1
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Today: goal ring + streak
// ---------------------------------------------------------------------------

@Composable
private fun GoalRingCard(
    today: TodayStats,
    modifier: Modifier = Modifier,
    onEditGoal: () -> Unit
) {
    val progress = min(today.minutes.toFloat() / today.goalMinutes.coerceAtLeast(1), 1f)
    val percent = (progress * 100).roundToInt()
    val remaining = max(today.goalMinutes - today.minutes, 0)

    Surface(
        modifier = modifier.height(214.dp),
        shape = RoundedCornerShape(26.dp),
        color = Cream,
        border = BorderStroke(1.dp, Amber.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "TODAY'S GOAL",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp
                    ),
                    color = Sage
                )
                TextButton(onClick = onEditGoal, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                    Text("Edit", style = MaterialTheme.typography.labelSmall, color = Ink)
                }
            }

            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 11.dp.toPx()
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    drawArc(
                        color = Ink.copy(alpha = 0.08f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = Amber,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Ink
                    )
                    Text(
                        text = "done",
                        style = MaterialTheme.typography.labelSmall,
                        color = Sage
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = "${today.minutes} of ${today.goalMinutes} min",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Ink
            )
            Text(
                text = if (remaining > 0) "$remaining min to go" else "Goal reached 🎉",
                style = MaterialTheme.typography.labelSmall,
                color = Sage
            )
        }
    }
}

@Composable
private fun StreakCard(
    today: TodayStats,
    modifier: Modifier = Modifier
) {
    val letters = listOf("M", "T", "W", "T", "F", "S", "S")

    Surface(
        modifier = modifier.height(214.dp),
        shape = RoundedCornerShape(26.dp),
        color = Mint,
        border = BorderStroke(1.dp, Ink.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.4f),
                        radius = size.width * 0.5f,
                        center = Offset(size.width * 1.0f, size.height * 0.0f)
                    )
                }
                .padding(16.dp)
        ) {
            Text(
                text = "STREAK",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp
                ),
                color = Sage
            )

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔥", fontSize = 30.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${today.streak}",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Ink
                )
            }
            Text(
                text = "days in a row",
                style = MaterialTheme.typography.labelMedium,
                color = Ink.copy(alpha = 0.65f)
            )

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                today.weekRead.forEachIndexed { index, read ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = letters[index],
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Sage
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        index == today.weekRead.lastIndex && read -> Ink
                                        read -> Teal
                                        else -> Ink.copy(alpha = 0.08f)
                                    }
                                )
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.75f)) {
                Text(
                    text = "Best · ${today.bestStreak} days",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Ink,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Activity chart
// ---------------------------------------------------------------------------

@Composable
private fun ActivityCard(
    stats: RangeStats,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    val bars = stats.bars
    val selectedBar = bars.getOrNull(selected)
    val maxValue = max(bars.maxOf { it.minutes }, stats.goalPerBar) * 1.12f
    val pastCount = bars.count { !it.isFuture }
    val metCount = bars.count { !it.isFuture && it.minutes >= stats.goalPerBar }
    val spacing = if (bars.size > 7) 5.dp else 10.dp
    val barShape = RoundedCornerShape(
        topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SIDE.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Line)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Readout for the selected bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = (selectedBar?.fullLabel ?: "").uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp
                        ),
                        color = Sage,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    DurationText(
                        minutes = selectedBar?.minutes ?: 0,
                        color = Ink,
                        bigSp = 28,
                        smallSp = 14
                    )
                }
                Surface(shape = CircleShape, color = Mint) {
                    Text(
                        text = "≈ ${formatNumber(((selectedBar?.minutes ?: 0) * stats.pacePagesPerHour) / 60)} pages",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Ink,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Bars + goal line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val y = size.height * (1f - stats.goalPerBar / maxValue)
                    drawLine(
                        color = Amber,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
                    )
                }

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    bars.forEachIndexed { index, bar ->
                        val fraction = if (bar.isFuture) 0.03f
                        else (bar.minutes / maxValue).coerceIn(0.03f, 1f)
                        val color = when {
                            index == selected && !bar.isFuture -> Ink
                            bar.isFuture -> Line
                            bar.minutes >= stats.goalPerBar -> Teal
                            else -> Teal.copy(alpha = 0.35f)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(10.dp))
                                .then(
                                    if (bar.isFuture) Modifier
                                    else Modifier.clickable { onSelect(index) }
                                ),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(fraction)
                                    .clip(barShape)
                                    .background(color)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                bars.forEachIndexed { index, bar ->
                    Text(
                        text = bar.label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (index == selected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (index == selected) Ink else Sage,
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Goal summary
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Cream
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎯", fontSize = 16.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Goal reached in $metCount of $pastCount ${stats.barUnit}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Ink
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Highlight tiles
// ---------------------------------------------------------------------------

private data class StatTileData(
    val emoji: String,
    val label: String,
    val value: String,
    val note: String,
    val color: Color
)

@Composable
private fun StatTile(
    data: StatTileData,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        color = data.color,
        border = BorderStroke(1.dp, Ink.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(152.dp)
                .drawBehind {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.38f),
                        radius = size.width * 0.42f,
                        center = Offset(size.width * 0.98f, size.height * 0.02f)
                    )
                    drawCircle(
                        color = Ink.copy(alpha = 0.06f),
                        radius = size.width * 0.52f,
                        center = Offset(size.width * 0.98f, size.height * 0.02f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
                .padding(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(data.emoji, fontSize = 19.sp)
                }
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = data.value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = data.label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = Ink.copy(alpha = 0.72f),
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = data.note,
                style = MaterialTheme.typography.labelSmall,
                color = Ink.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Reading clock
// ---------------------------------------------------------------------------

private data class DayPart(
    val emoji: String,
    val label: String,
    val hours: String,
    val percent: Int,
    val color: Color
)

@Composable
private fun ReadingClockCard(parts: List<DayPart>) {
    val top = parts.maxBy { it.percent }
    val hasReading = top.percent > 0
    val persona = when (top.label) {
        "Morning" -> "a morning reader"
        "Afternoon" -> "an afternoon reader"
        "Evening" -> "an evening reader"
        else -> "a night owl"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SIDE.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Line)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Mint
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (hasReading) top.emoji else "📚", fontSize = 26.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (hasReading) "You're $persona" else "Your reading clock starts here",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Ink
                        )
                        Text(
                            text = if (hasReading) "${top.percent}% of your reading time falls in the ${top.label.lowercase()}." else "Start a session to see when you read most.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Ink.copy(alpha = 0.68f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            parts.forEach { part ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(part.emoji, fontSize = 18.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = part.label,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Ink
                            )
                            Text(
                                text = "${part.percent}%",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Ink
                            )
                        }
                        Spacer(Modifier.height(5.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(CircleShape)
                                .background(Line)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(part.percent / 100f)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(part.color)
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = part.hours,
                            style = MaterialTheme.typography.labelSmall,
                            color = Sage
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniFact(
                    label = "MOST ACTIVE DAY",
                    value = "Saturday",
                    note = "avg 52 min",
                    color = Cream,
                    modifier = Modifier.weight(1f)
                )
                MiniFact(
                    label = "PEAK HOUR",
                    value = "7–8 AM",
                    note = "your focus window",
                    color = Sky,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MiniFact(
    label: String,
    value: String,
    note: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = color
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                ),
                color = Ink.copy(alpha = 0.55f),
                maxLines = 1
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Ink
            )
            Text(
                text = note,
                style = MaterialTheme.typography.labelSmall,
                color = Ink.copy(alpha = 0.6f)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Genres (donut)
// ---------------------------------------------------------------------------

private data class GenreShare(val name: String, val percent: Int, val color: Color)

@Composable
private fun GenreCard(genres: List<GenreShare>) {
    val top = genres.maxByOrNull { it.percent }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SIDE.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Line)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(132.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 22.dp.toPx()
                        val gap = 3f
                        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                        var start = -90f
                        genres.forEach { genre ->
                            val sweep = 360f * genre.percent / 100f
                            drawArc(
                                color = genre.color,
                                startAngle = start + gap / 2,
                                sweepAngle = sweep - gap,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                            )
                            start += sweep
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${genres.size}",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Ink
                        )
                        Text(
                            text = "genres",
                            style = MaterialTheme.typography.labelSmall,
                            color = Sage
                        )
                    }
                }

                Spacer(Modifier.width(20.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    genres.forEach { genre ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(genre.color)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = genre.name,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${genre.percent}%",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Ink
                            )
                        }
                    }
                    if (genres.isEmpty()) {
                        Text(
                            text = "Genre insights appear after you read books with genre details.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Sage
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Lavender
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💡", fontSize = 16.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = top?.let { "${it.name} leads your reading, with ${it.percent}% of your time." }
                            ?: "Genre insights will appear here once you start reading.",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Ink
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Yearly goal
// ---------------------------------------------------------------------------

private data class YearGoal(
    val year: Int,
    val finished: Int,
    val target: Int,
    val monthsLeft: Float
)

@Composable
private fun YearGoalCard(goal: YearGoal, onEditGoal: () -> Unit) {
    val progress = goal.finished.toFloat() / goal.target
    val remaining = max(goal.target - goal.finished, 0)
    val perMonth = if (goal.monthsLeft > 0f) remaining / goal.monthsLeft else 0f
    val perMonthText = (perMonth * 10).roundToInt() / 10f
    val currentDate = LocalDate.now()
    val expectedProgress = currentDate.dayOfYear.toFloat() / currentDate.lengthOfYear()
    val goalStatus = when {
        goal.finished >= goal.target -> "✓ Goal reached 🎉"
        progress >= expectedProgress -> "✓ On track"
        else -> "↗ Keep going"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SIDE.dp),
        shape = RoundedCornerShape(30.dp),
        color = Cream,
        border = BorderStroke(1.dp, Amber.copy(alpha = 0.18f))
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${goal.year} READING GOAL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp
                        ),
                        color = Sage
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold)) {
                                append("${goal.finished}")
                            }
                            withStyle(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)) {
                                append(" / ${goal.target} books")
                            }
                        },
                        style = MaterialTheme.typography.headlineLarge,
                        color = Ink
                    )
                    Spacer(Modifier.height(8.dp))
                    Surface(shape = CircleShape, color = Teal.copy(alpha = 0.28f)) {
                        Text(
                            text = goalStatus,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Ink,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                        )
                    }
                    TextButton(onClick = onEditGoal, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                        Text("Change goal", style = MaterialTheme.typography.labelSmall, color = Ink)
                    }
                }

                Image(
                    painter = painterResource(id = R.drawable.ic_stats_screen_goals),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .width(96.dp)
                        .height(92.dp)
                )
            }

            Spacer(Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape)
                    .background(Ink.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(Amber)
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GoalFact("${(progress * 100).roundToInt()}%", "complete", Modifier.weight(1f))
                GoalFact("$remaining", "books to go", Modifier.weight(1f))
                GoalFact("$perMonthText", "books / month", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GoalFact(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.7f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Ink
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Sage,
                maxLines = 1
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Heatmap (with week date ranges and month dividers)
// ---------------------------------------------------------------------------

private data class HeatCell(
    val minutes: Int,
    val day: Int,
    val month: Int, // 1..12
    val year: Int
)

private data class HeatWeek(val cells: List<HeatCell>)

/** "Sep 7 – 13", or "Sep 28 –\nOct 4" when the week crosses a month. */
private fun HeatWeek.rangeLabel(): String {
    val first = cells.first()
    val last = cells.last()
    return if (first.month == last.month) {
        "${MonthNames[first.month - 1]} ${first.day} – ${last.day}"
    } else {
        "${MonthNames[first.month - 1]} ${first.day} –\n${MonthNames[last.month - 1]} ${last.day}"
    }
}

@Composable
private fun HeatmapCard(weeks: List<HeatWeek>, goal: Int) {
    val letters = listOf("M", "T", "W", "T", "F", "S", "S")
    val allCells = weeks.flatMap { it.cells }
    val firstCell = allCells.first()
    val lastCell = allCells.last()
    val labelWidth = 64.dp
    val cellGap = 6.dp

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SIDE.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Line)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Overall date span
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${MonthNames[firstCell.month - 1]} ${firstCell.day} – " +
                            "${MonthNames[lastCell.month - 1]} ${lastCell.day}, ${lastCell.year}".uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp
                    ),
                    color = Sage
                )
                Surface(shape = CircleShape, color = Mint) {
                    Text(
                        text = "${weeks.size} weeks",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Ink,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Weekday letters, aligned with the grid
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.width(labelWidth))
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(cellGap)
                ) {
                    letters.forEach { letter ->
                        Text(
                            text = letter,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Sage
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            weeks.forEachIndexed { weekIndex, week ->
                val weekStartMonth = week.cells.first().month
                val previousStartMonth = weeks.getOrNull(weekIndex - 1)?.cells?.first()?.month

                // Month divider where a new month begins
                if (weekStartMonth != previousStartMonth) {
                    val monthYear = week.cells.first().year
                    val monthCells = allCells.filter {
                        it.month == weekStartMonth && it.year == monthYear
                    }
                    Spacer(Modifier.height(if (weekIndex == 0) 4.dp else 12.dp))
                    MonthDivider(
                        name = MonthFullNames[weekStartMonth - 1],
                        readDays = monthCells.count { it.minutes > 0 },
                        totalDays = monthCells.size
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = week.rangeLabel(),
                        modifier = Modifier.width(labelWidth),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Ink.copy(alpha = 0.7f)
                    )

                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(cellGap)
                    ) {
                        week.cells.forEachIndexed { dayIndex, cell ->
                            val isToday = weekIndex == weeks.lastIndex &&
                                    dayIndex == week.cells.lastIndex
                            val isMonthStart = cell.day == 1

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(heatColor(cell.minutes, goal))
                                    .then(
                                        if (isToday) Modifier.border(2.dp, Ink, RoundedCornerShape(9.dp))
                                        else if (cell.minutes == 0) Modifier.border(1.dp, Line, RoundedCornerShape(9.dp))
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    if (isMonthStart) {
                                        Text(
                                            text = MonthNames[cell.month - 1],
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 7.sp,
                                                lineHeight = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = heatTextColor(cell.minutes, goal)
                                        )
                                    }
                                    Text(
                                        text = "${cell.day}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            lineHeight = 11.sp,
                                            fontWeight = if (isToday || isMonthStart) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = heatTextColor(cell.minutes, goal)
                                    )
                                }
                            }
                        }
                    }
                }

                if (weekIndex != weeks.lastIndex) Spacer(Modifier.height(6.dp))
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ringed square is today",
                    style = MaterialTheme.typography.labelSmall,
                    color = Sage
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Less",
                        style = MaterialTheme.typography.labelSmall,
                        color = Sage
                    )
                    Spacer(Modifier.width(6.dp))
                    listOf(0, 10, 20, 35, goal).forEach { sample ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .size(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(heatColor(sample, goal))
                                .then(
                                    if (sample == 0) Modifier.border(1.dp, Line, RoundedCornerShape(4.dp))
                                    else Modifier
                                )
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "More",
                        style = MaterialTheme.typography.labelSmall,
                        color = Sage
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthDivider(
    name: String,
    readDays: Int,
    totalDays: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            ),
            color = Ink.copy(alpha = 0.6f)
        )
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Line)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "$readDays of $totalDays days",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = Sage
        )
    }
}

private fun heatColor(minutes: Int, goal: Int): Color {
    return when {
        minutes <= 0 -> Mint
        minutes < 15 -> Teal.copy(alpha = 0.30f)
        minutes < 30 -> Teal.copy(alpha = 0.55f)
        minutes < goal -> Teal.copy(alpha = 0.85f)
        else -> Green
    }
}

private fun heatTextColor(minutes: Int, goal: Int): Color {
    return when {
        minutes >= goal -> Paper
        minutes <= 0 -> Sage
        else -> Ink.copy(alpha = 0.7f)
    }
}

// ---------------------------------------------------------------------------
// Insights
// ---------------------------------------------------------------------------

private data class Insight(val emoji: String, val title: String, val body: String, val color: Color)

@Composable
private fun InsightsList(
    stats: RangeStats,
    clock: List<DayPart>,
    today: TodayStats
) {
    val top = clock.maxBy { it.percent }
    val delta = stats.deltaPercent
    val toBest = max(today.bestStreak - today.streak, 0)
    val hasReading = stats.sessions > 0

    val insights = listOf(
        Insight(
            emoji = if (hasReading) top.emoji else "📚",
            title = if (hasReading) "${top.label}s are your sweet spot" else "Your rhythm starts here",
            body = if (hasReading) "${top.percent}% of your reading happens in the ${top.label.lowercase()}." else "Start a reading session to discover your habits.",
            color = Mint
        ),
        Insight(
            emoji = if (delta >= 0) "📈" else "📉",
            title = if (!hasReading) "A fresh page" else if (delta > 0) "You're reading more" else if (delta < 0) "A quieter stretch" else "A steady stretch",
            body = if (!hasReading) "Your reading time will appear here as sessions are recorded." else "${abs(delta)}% ${if (delta >= 0) "more" else "less"} time with books ${stats.comparison}.",
            color = Cream
        ),
        Insight(
            emoji = "🔥",
            title = if (today.bestStreak == 0) "Build your first streak" else if (toBest == 0) "Personal best streak!" else "Your best streak is within reach",
            body = if (today.bestStreak == 0) "A short daily reading session is a good start."
            else if (toBest == 0) "You're on your longest run ever."
            else "$toBest more ${if (toBest == 1) "day" else "days"} ties your record of ${today.bestStreak}.",
            color = Lavender
        ),
        Insight(
            emoji = "⚡",
            title = "Steady pace",
            body = "You average ${stats.pacePagesPerHour} pages an hour, ${stats.paceNote.lowercase()}.",
            color = Sky
        )
    )

    insights.forEach { insight ->
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SIDE.dp),
            shape = RoundedCornerShape(22.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Line)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = insight.color,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(insight.emoji, fontSize = 21.sp)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = insight.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Ink
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = insight.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = Sage
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

// ---------------------------------------------------------------------------
// Badges
// ---------------------------------------------------------------------------

private data class Badge(
    val emoji: String,
    val name: String,
    val caption: String,
    val progress: Float,
    val tint: Color
)

@Composable
private fun BadgeItem(badge: Badge) {
    val earned = badge.progress >= 1f

    Column(
        modifier = Modifier.width(92.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(78.dp)) {
            Surface(
                modifier = Modifier
                    .size(72.dp)
                    .align(Alignment.Center),
                shape = CircleShape,
                color = if (earned) badge.tint else Color.White,
                border = BorderStroke(
                    width = if (earned) 2.dp else 1.dp,
                    color = if (earned) Ink.copy(alpha = 0.12f) else Line
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = badge.emoji,
                        fontSize = 30.sp,
                        modifier = Modifier.graphicsLayer { alpha = if (earned) 1f else 0.35f }
                    )
                }
            }

            if (earned) {
                Surface(
                    modifier = Modifier
                        .size(22.dp)
                        .align(Alignment.TopEnd),
                    shape = CircleShape,
                    color = Ink
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "✓",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Paper
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = badge.name,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Ink,
            maxLines = 2
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = badge.caption,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            color = Sage,
            maxLines = 1
        )

        if (!earned) {
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Line)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(badge.progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(Amber)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Recently finished
// ---------------------------------------------------------------------------

private data class RecentBook(
    val title: String,
    val author: String,
    val coverUrl: String,
    val pages: Int,
    val days: Int,
    val finishedOn: String
)

@Composable
private fun RecentBookRow(book: RecentBook) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SIDE.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Line)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = book.coverUrl,
                contentDescription = "Cover of ${book.title}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(56.dp)
                    .height(82.dp)
                    .clip(RoundedCornerShape(10.dp))
            )

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.labelMedium,
                    color = Sage,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallPill("${book.pages} pages", Mint)
                    SmallPill("${book.days} days", Cream)
                }
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = book.finishedOn,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = Sage
            )
        }
    }
}

@Composable
private fun SmallPill(text: String, color: Color) {
    Surface(shape = CircleShape, color = color) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Ink,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            maxLines = 1
        )
    }
}

// ---------------------------------------------------------------------------
// Footer
// ---------------------------------------------------------------------------

@Composable
private fun StatsFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SIDE.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_stats_screen_bottom),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = "EVERY MINUTE COUNTS",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            ),
            color = Sage
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Small pages, steady habits.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Ink
        )
    }
}

// ---------------------------------------------------------------------------
// Section header + formatting helpers
// ---------------------------------------------------------------------------

@Composable
private fun StatsSectionHeader(
    eyebrow: String,
    title: String,
    trailing: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SIDE.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Text(
                text = eyebrow,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                ),
                color = Sage
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Ink
            )
        }
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = Sage,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }
    }
}

/** "4h 32m" with a big number and smaller units. */
@Composable
private fun DurationText(
    minutes: Int,
    color: Color,
    bigSp: Int = 40,
    smallSp: Int = 18
) {
    val h = minutes / 60
    val m = minutes % 60
    val big = SpanStyle(fontSize = bigSp.sp, fontWeight = FontWeight.Bold)
    val small = SpanStyle(fontSize = smallSp.sp, fontWeight = FontWeight.SemiBold)

    Text(
        text = buildAnnotatedString {
            if (h > 0) {
                withStyle(big) { append("$h") }
                withStyle(small) { append("h ") }
            }
            withStyle(big) { append("$m") }
            withStyle(small) { append("m") }
        },
        style = MaterialTheme.typography.headlineLarge,
        color = color
    )
}

private fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "${m}m"
        m == 0 -> "${h}h"
        else -> "${h}h ${m}m"
    }
}

private fun formatNumber(value: Int): String = "%,d".format(value)

private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    4, 6, 9, 11 -> 30
    2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
    else -> 31
}

private fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}

// ==========================================
// MODELS & LIVE AGGREGATION
// ==========================================

private enum class StatsRange(val label: String) {
    Week("Week"),
    Month("Month"),
    Year("Year")
}

private data class BarPoint(
    val label: String,
    val fullLabel: String,
    val minutes: Int,
    val isFuture: Boolean = false
)

private data class TodayStats(
    val minutes: Int,
    val goalMinutes: Int,
    val streak: Int,
    val bestStreak: Int,
    val weekRead: List<Boolean> // Mon..Sun
)

private data class RangeStats(
    val periodLabel: String,
    val comparison: String,
    val chartTitle: String,
    val goalLabel: String,
    val barUnit: String,
    val goalPerBar: Int,
    val bars: List<BarPoint>,
    val deltaPercent: Int,
    val sessions: Int,
    val avgSessionMin: Int,
    val pages: Int,
    val pacePagesPerHour: Int,
    val paceNote: String,
    val booksFinished: Int,
    val booksNote: String,
    val pagesNote: String,
    val longestSessionMin: Int,
    val longestNote: String,
    val activeDays: Int,
    val periodDays: Int,
    val activeNote: String
) {
    val totalMinutes: Int get() = bars.sumOf { it.minutes }
}

private object StatsData {
    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val todayDate: LocalDate get() = LocalDate.now(zone)

    fun today(sessions: List<ReadingSession>, dailyGoalMinutes: Int): TodayStats {
        val today = todayDate
        val minutesByDay = minutesByDay(sessions)
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val week = (0..6).map { weekStart.plusDays(it.toLong()) }.map { (minutesByDay[it] ?: 0) >= 1 }
        val readingDays = minutesByDay.filterValues { it > 0 }.keys
        var streak = 0
        var cursor = if (today in readingDays) today else today.minusDays(1)
        while (cursor in readingDays) {
            streak++
            cursor = cursor.minusDays(1)
        }
        var best = 0
        var run = 0
        var previous: LocalDate? = null
        readingDays.sorted().forEach { date ->
            run = if (previous?.plusDays(1) == date) run + 1 else 1
            best = max(best, run)
            previous = date
        }
        return TodayStats(
            minutes = minutesByDay[today] ?: 0,
            goalMinutes = dailyGoalMinutes.coerceAtLeast(1),
            streak = streak,
            bestStreak = best,
            weekRead = week
        )
    }

    fun statsFor(range: StatsRange, data: StatsDataState): RangeStats {
        val today = todayDate
        val currentYear = today.year
        val weeklyGoalMinutes = (data.dailyGoalMinutes.coerceAtLeast(1).toLong() * 7L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val monthlyGoalMinutes = (data.dailyGoalMinutes.coerceAtLeast(1).toLong() * 30L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val starts: List<LocalDate>
        val bars: List<BarPoint>
        val start: LocalDate
        val endExclusive: LocalDate
        val periodLabel: String
        val chartTitle: String
        val goalLabel: String
        val barUnit: String
        val goalPerBar: Int
        when (range) {
            StatsRange.Week -> {
                start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                starts = (0..6).map { start.plusDays(it.toLong()) }
                endExclusive = start.plusDays(7)
                periodLabel = "This week"
                chartTitle = "Daily minutes"
                goalLabel = "Goal · ${data.dailyGoalMinutes} min/day"
                barUnit = "days"
                goalPerBar = data.dailyGoalMinutes
                bars = starts.map { day ->
                    BarPoint(day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()), day.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())) + if (day == today) " · today" else "", minutesOn(data.sessions, day), day.isAfter(today))
                }
            }
            StatsRange.Month -> {
                start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(3)
                starts = (0..3).map { start.plusWeeks(it.toLong()) }
                endExclusive = starts.last().plusWeeks(1)
                periodLabel = "Last 4 weeks"
                chartTitle = "Weekly minutes"
                goalLabel = "Goal · ${formatDuration(weeklyGoalMinutes)}/week"
                barUnit = "weeks"
                goalPerBar = weeklyGoalMinutes
                bars = starts.mapIndexed { index, weekStart ->
                    val weekEnd = weekStart.plusDays(6)
                    BarPoint("Wk ${index + 1}", "${weekStart.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))} – ${weekEnd.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))}", minutesBetween(data.sessions, weekStart, weekStart.plusWeeks(1)))
                }
            }
            StatsRange.Year -> {
                start = LocalDate.of(currentYear, 1, 1)
                starts = (1..12).map { LocalDate.of(currentYear, it, 1) }
                endExclusive = today.plusDays(1)
                periodLabel = currentYear.toString()
                chartTitle = "Monthly minutes"
                goalLabel = "Goal · ${formatDuration(monthlyGoalMinutes)}/month"
                barUnit = "months"
                goalPerBar = monthlyGoalMinutes
                bars = starts.map { month ->
                    val full = month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                    BarPoint(month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(1), if (month.monthValue == today.monthValue) "$full · so far" else full, minutesBetween(data.sessions, month, month.plusMonths(1)), month.isAfter(today.withDayOfMonth(1)))
                }
            }
        }
        val selected = data.sessions.filter { session -> session.date() >= start && session.date() < endExclusive }
        val previousDuration = when (range) {
            StatsRange.Week -> minutesBetween(data.sessions, start.minusWeeks(1), start)
            StatsRange.Month -> minutesBetween(data.sessions, start.minusWeeks(4), start)
            StatsRange.Year -> minutesBetween(data.sessions, start.minusYears(1), start)
        }
        val currentDuration = selected.sumOf { (it.durationMillis / MILLIS_PER_MINUTE).toInt() }
        val pages = selected.sumOf { it.pagesRead }
        val hours = selected.sumOf { it.durationMillis } / 3_600_000.0
        val activeDays = selected.map { it.date() }.distinct().size
        val periodDays = min(today.toEpochDay() - start.toEpochDay() + 1, endExclusive.toEpochDay() - start.toEpochDay()).toInt().coerceAtLeast(1)
        val finished = data.progress.count { it.progression >= 0.999f && it.updatedAtMillis.toDate() >= LocalDate.of(currentYear, 1, 1) }
        val delta = if (previousDuration == 0) 0 else (((currentDuration - previousDuration) * 100.0) / previousDuration).roundToInt()
        val comparison = when (range) {
            StatsRange.Week -> "vs last week"
            StatsRange.Month -> "vs previous 4 weeks"
            StatsRange.Year -> "vs the same time last year"
        }
        return RangeStats(
            periodLabel = periodLabel,
            comparison = comparison,
            chartTitle = chartTitle,
            goalLabel = goalLabel,
            barUnit = barUnit,
            goalPerBar = goalPerBar,
            bars = bars,
            deltaPercent = delta,
            sessions = selected.size,
            avgSessionMin = if (selected.isEmpty()) 0 else (selected.sumOf { it.durationMillis } / selected.size / MILLIS_PER_MINUTE).toInt(),
            pages = pages,
            pacePagesPerHour = if (hours <= 0.0) 0 else (pages / hours).roundToInt(),
            paceNote = "based on recorded sessions",
            booksFinished = finished,
            booksNote = "completed this year",
            pagesNote = if (periodDays == 0) "" else "≈ ${pages / periodDays} pages a day",
            longestSessionMin = (selected.maxOfOrNull { it.durationMillis } ?: 0L).div(MILLIS_PER_MINUTE).toInt(),
            longestNote = "longest recorded session",
            activeDays = activeDays,
            periodDays = periodDays,
            activeNote = if (periodDays == 0) "" else "${(activeDays * 100 / periodDays)}% of days"
        )
    }

    fun readingClock(sessions: List<ReadingSession>): List<DayPart> {
        val minutes = IntArray(4)
        sessions.forEach { session ->
            val hour = Instant.ofEpochMilli(session.startedAtMillis).atZone(zone).hour
            val bucket = when (hour) { in 5..10 -> 0; in 11..16 -> 1; in 17..21 -> 2; else -> 3 }
            minutes[bucket] += (session.durationMillis / MILLIS_PER_MINUTE).toInt()
        }
        val total = minutes.sum()
        val details = listOf("🌅" to ("Morning" to "5 AM – 11 AM"), "☀️" to ("Afternoon" to "11 AM – 5 PM"), "🌆" to ("Evening" to "5 PM – 10 PM"), "🌙" to ("Night" to "10 PM – 5 AM"))
        val colors = listOf(Amber, Teal, Coral, LavenderDeep)
        return details.mapIndexed { index, (emoji, detail) -> DayPart(emoji, detail.first, detail.second, if (total == 0) 0 else minutes[index] * 100 / total, colors[index]) }
    }

    fun genres(data: StatsDataState): List<GenreShare> {
        val durations = data.sessions.groupBy { session -> data.books.firstOrNull { it.id == session.bookId }?.genre?.takeIf(String::isNotBlank) ?: "Other" }
            .mapValues { (_, sessions) -> sessions.sumOf { it.durationMillis } }
            .filterValues { it > 0 }
        if (durations.isEmpty()) return emptyList()
        val colors = listOf(Teal, Amber, Coral, LavenderDeep, SkyDeep, Green)
        val entries = durations.entries.sortedByDescending { it.value }.take(5)
        val total = entries.sumOf { it.value }.coerceAtLeast(1L)
        return entries.mapIndexed { index, entry -> GenreShare(entry.key, (entry.value * 100 / total).toInt(), colors[index % colors.size]) }
            .let { shares -> if (shares.size > 1 && shares.sumOf { it.percent } < 100) shares.dropLast(1) + shares.last().copy(percent = shares.last().percent + 100 - shares.sumOf { it.percent }) else shares }
    }

    fun yearGoal(data: StatsDataState): YearGoal {
        val year = todayDate.year
        val finished = data.progress.count { it.progression >= 0.999f && it.updatedAtMillis.toDate() >= LocalDate.of(year, 1, 1) }
        val monthsLeft = (12 - todayDate.monthValue + 1).toFloat().coerceAtLeast(1f)
        return YearGoal(year, finished, data.yearlyBookGoal.coerceAtLeast(1), monthsLeft)
    }

    fun heatmapWeeks(sessions: List<ReadingSession>): List<HeatWeek> {
        val today = todayDate
        val first = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(4)
        val byDay = minutesByDay(sessions)
        return (0 until 35).map { offset ->
            val date = first.plusDays(offset.toLong())
            HeatCell(if (date.isAfter(today)) 0 else byDay[date] ?: 0, date.dayOfMonth, date.monthValue, date.year)
        }.chunked(7).map(::HeatWeek)
    }

    fun badges(data: StatsDataState, streak: Int): List<Badge> {
        val readBooks = data.sessions.map { it.bookId }.distinct().size
        val genres = data.books.mapNotNull { it.genre?.takeIf(String::isNotBlank) }.distinct().size
        val longest = data.sessions.maxOfOrNull { (it.durationMillis / MILLIS_PER_MINUTE).toInt() } ?: 0
        val nightSessions = data.sessions.count { Instant.ofEpochMilli(it.startedAtMillis).atZone(zone).hour >= 22 || Instant.ofEpochMilli(it.startedAtMillis).atZone(zone).hour < 5 }
        return listOf(
            Badge("📘", "First book", if (readBooks > 0) "${readBooks.coerceAtMost(1)} / 1 books" else "0 / 1 books", (readBooks / 1f).coerceAtMost(1f), Mint),
            Badge("🔥", "7-day streak", "$streak / 7 days", streak / 7f, Cream),
            Badge("🏅", "10 books", "$readBooks / 10 books", readBooks / 10f, Lavender),
            Badge("🧭", "Genre explorer", "$genres / 5 genres", genres / 5f, Sky),
            Badge("⏳", "Marathon", "$longest / 180 min", longest / 180f, Coral.copy(alpha = 0.45f)),
            Badge("🦉", "Night owl", "$nightSessions / 10 nights", nightSessions / 10f, Lavender)
        )
    }

    fun recentBooks(data: StatsDataState): List<RecentBook> = data.books
        .filter { it.lastOpenedAtMillis != null }
        .sortedByDescending { it.lastOpenedAtMillis }
        .take(3)
        .map { book ->
            val bookSessions = data.sessions.filter { it.bookId == book.id }
            val lastRead = book.lastOpenedAtMillis!!.toDate()
            RecentBook(
                title = book.title,
                author = book.authors.joinToString().ifBlank { "Unknown author" },
                coverUrl = book.coverUri.orEmpty(),
                pages = book.totalPages ?: bookSessions.sumOf { it.pagesRead },
                days = bookSessions.map { it.date() }.distinct().size,
                finishedOn = lastRead.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
            )
        }

    private fun minutesByDay(sessions: List<ReadingSession>): Map<LocalDate, Int> = sessions.groupBy { it.date() }
        .mapValues { (_, rows) -> (rows.sumOf { it.durationMillis } / MILLIS_PER_MINUTE).toInt() }

    private fun minutesOn(sessions: List<ReadingSession>, day: LocalDate) = minutesByDay(sessions)[day] ?: 0
    private fun minutesBetween(sessions: List<ReadingSession>, from: LocalDate, until: LocalDate): Int =
        sessions.filter { it.date() >= from && it.date() < until }.sumOf { (it.durationMillis / MILLIS_PER_MINUTE).toInt() }

    private fun ReadingSession.date(): LocalDate = startedAtMillis.toDate()
    private fun Long.toDate(): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

    private const val MILLIS_PER_MINUTE = 60_000L
}
