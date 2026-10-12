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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import com.michaelamimo.sumeria.R
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

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

@Composable
fun StatsScreen() {
    val view = LocalView.current
    val activity = remember(view) { view.context.findActivity() }

    SideEffect {
        activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = false
        }
    }

    var range by remember { mutableStateOf(StatsRange.Week) }
    val stats = remember(range) { StatsMockRepository.statsFor(range) }
    var selectedBar by remember(range) { mutableStateOf(stats.bars.indexOfLast { !it.isFuture }) }

    val today = remember { StatsMockRepository.today() }
    val clock = remember { StatsMockRepository.readingClock() }
    val genres = remember { StatsMockRepository.genres() }
    val heatmapWeeks = remember { StatsMockRepository.heatmapWeeks() }
    val badges = remember { StatsMockRepository.badges() }
    val recentBooks = remember { StatsMockRepository.recentBooks() }
    val yearGoal = remember { StatsMockRepository.yearGoal() }

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
                    GoalRingCard(today = today, modifier = Modifier.weight(1f))
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
                YearGoalCard(goal = yearGoal)
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
                    title = "Recently finished",
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
    modifier: Modifier = Modifier
) {
    val progress = min(today.minutes.toFloat() / today.goalMinutes, 1f)
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
            Text(
                text = "TODAY'S GOAL",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp
                ),
                color = Sage,
                modifier = Modifier.align(Alignment.Start)
            )

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
                    Text(top.emoji, fontSize = 26.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "You're $persona",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Ink
                        )
                        Text(
                            text = "${top.percent}% of your reading time falls in the ${top.label.lowercase()}.",
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
    val top = genres.maxBy { it.percent }

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
                        text = "${top.name} leads your reading, with ${top.percent}% of your time.",
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
private fun YearGoalCard(goal: YearGoal) {
    val progress = goal.finished.toFloat() / goal.target
    val remaining = max(goal.target - goal.finished, 0)
    val perMonth = if (goal.monthsLeft > 0f) remaining / goal.monthsLeft else 0f
    val perMonthText = (perMonth * 10).roundToInt() / 10f

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
                            text = "✓ On track",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Ink,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                        )
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

    val insights = listOf(
        Insight(
            emoji = top.emoji,
            title = "${top.label}s are your sweet spot",
            body = "${top.percent}% of your reading happens in the ${top.label.lowercase()}.",
            color = Mint
        ),
        Insight(
            emoji = if (delta >= 0) "📈" else "📉",
            title = if (delta >= 0) "You're reading more" else "A quieter stretch",
            body = "${abs(delta)}% ${if (delta >= 0) "more" else "less"} time with books ${stats.comparison}.",
            color = Cream
        ),
        Insight(
            emoji = "🔥",
            title = if (toBest == 0) "Personal best streak!" else "Your best streak is within reach",
            body = if (toBest == 0) "You're on your longest run ever."
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
// MODELS & MOCK DATA
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

private object StatsMockRepository {

    fun today() = TodayStats(
        minutes = 32,
        goalMinutes = 45,
        streak = 7,
        bestStreak = 14,
        weekRead = listOf(true, true, true, true, true, true, true)
    )

    fun statsFor(range: StatsRange): RangeStats = when (range) {
        StatsRange.Week -> RangeStats(
            periodLabel = "This week",
            comparison = "vs last week",
            chartTitle = "Daily minutes",
            goalLabel = "Goal · 45 min/day",
            barUnit = "days",
            goalPerBar = 45,
            bars = listOf(
                BarPoint("Mon", "Monday", 28),
                BarPoint("Tue", "Tuesday", 45),
                BarPoint("Wed", "Wednesday", 18),
                BarPoint("Thu", "Thursday", 52),
                BarPoint("Fri", "Friday", 36),
                BarPoint("Sat", "Saturday", 61),
                BarPoint("Sun", "Sunday · today", 32)
            ),
            deltaPercent = 18,
            sessions = 10,
            avgSessionMin = 27,
            pages = 172,
            pacePagesPerHour = 38,
            paceNote = "up 4 on last week",
            booksFinished = 1,
            booksNote = "of 18 this year",
            pagesNote = "≈ 25 pages a day",
            longestSessionMin = 84,
            longestNote = "Saturday evening",
            activeDays = 7,
            periodDays = 7,
            activeNote = "Every day this week"
        )

        StatsRange.Month -> RangeStats(
            periodLabel = "Last 4 weeks",
            comparison = "vs previous 4 weeks",
            chartTitle = "Weekly minutes",
            goalLabel = "Goal · 5h 15m/week",
            barUnit = "weeks",
            goalPerBar = 315,
            bars = listOf(
                BarPoint("Wk 1", "Sep 14 – Sep 20", 168),
                BarPoint("Wk 2", "Sep 21 – Sep 27", 231),
                BarPoint("Wk 3", "Sep 28 – Oct 4", 173),
                BarPoint("Wk 4", "Oct 5 – Oct 11", 272)
            ),
            deltaPercent = 12,
            sessions = 31,
            avgSessionMin = 27,
            pages = 535,
            pacePagesPerHour = 38,
            paceNote = "up 2 on last month",
            booksFinished = 2,
            booksNote = "of 18 this year",
            pagesNote = "≈ 19 pages a day",
            longestSessionMin = 96,
            longestNote = "Sat, Oct 3 · evening",
            activeDays = 23,
            periodDays = 28,
            activeNote = "82% of days"
        )

        StatsRange.Year -> RangeStats(
            periodLabel = "2026",
            comparison = "vs the same time last year",
            chartTitle = "Monthly minutes",
            goalLabel = "Goal · 20h/month",
            barUnit = "months",
            goalPerBar = 1200,
            bars = listOf(
                BarPoint("J", "January", 1020),
                BarPoint("F", "February", 910),
                BarPoint("M", "March", 1180),
                BarPoint("A", "April", 1290),
                BarPoint("M", "May", 1150),
                BarPoint("J", "June", 980),
                BarPoint("J", "July", 760),
                BarPoint("A", "August", 940),
                BarPoint("S", "September", 1240),
                BarPoint("O", "October · so far", 368),
                BarPoint("N", "November", 0, isFuture = true),
                BarPoint("D", "December", 0, isFuture = true)
            ),
            deltaPercent = 9,
            sessions = 364,
            avgSessionMin = 27,
            pages = 5903,
            pacePagesPerHour = 36,
            paceNote = "steady all year",
            booksFinished = 14,
            booksNote = "4 to reach 18",
            pagesNote = "≈ 21 pages a day",
            longestSessionMin = 142,
            longestNote = "Sun, Apr 19 · afternoon",
            activeDays = 231,
            periodDays = 284,
            activeNote = "81% of days"
        )
    }

    fun readingClock() = listOf(
        DayPart("🌅", "Morning", "5 AM – 11 AM", 42, Amber),
        DayPart("☀️", "Afternoon", "11 AM – 5 PM", 15, Teal),
        DayPart("🌆", "Evening", "5 PM – 10 PM", 34, Coral),
        DayPart("🌙", "Night", "10 PM – 5 AM", 9, LavenderDeep)
    )

    fun genres() = listOf(
        GenreShare("Self-help", 38, Teal),
        GenreShare("Finance", 22, Amber),
        GenreShare("Fiction", 18, Coral),
        GenreShare("Design", 12, LavenderDeep),
        GenreShare("Psychology", 10, SkyDeep)
    )

    fun yearGoal() = YearGoal(year = 2026, finished = 14, target = 18, monthsLeft = 2.7f)

    /**
     * 35 days of reading minutes, oldest first, grouped into Mon..Sun weeks.
     * The first cell is Mon Sep 7, 2026 and the last is today, Sun Oct 11, 2026.
     */
    fun heatmapWeeks(): List<HeatWeek> {
        val minutes = listOf(
            20, 35, 0, 15, 40, 22, 30,
            25, 0, 0, 30, 45, 50, 18,
            30, 42, 38, 0, 18, 55, 48,
            22, 40, 15, 36, 0, 60, 0,
            28, 45, 18, 52, 36, 61, 32
        )

        var year = 2026
        var month = 9
        var day = 7

        val cells = minutes.map { value ->
            val cell = HeatCell(minutes = value, day = day, month = month, year = year)
            day++
            if (day > daysInMonth(year, month)) {
                day = 1
                month++
                if (month > 12) {
                    month = 1
                    year++
                }
            }
            cell
        }
        return cells.chunked(7).map { HeatWeek(it) }
    }

    fun badges() = listOf(
        Badge("📘", "First book", "Earned Feb 12", 1f, Mint),
        Badge("🔥", "7-day streak", "Earned today", 1f, Cream),
        Badge("🏅", "10 books", "Earned Aug 30", 1f, Lavender),
        Badge("🧭", "Genre explorer", "5 genres read", 1f, Sky),
        Badge("⏳", "Marathon", "84 / 180 min", 0.47f, Coral.copy(alpha = 0.45f)),
        Badge("🦉", "Night owl", "3 / 10 nights", 0.3f, Lavender)
    )

    fun recentBooks() = listOf(
        RecentBook(
            title = "The 48 Laws of Power",
            author = "Robert Greene",
            coverUrl = "https://m.media-amazon.com/images/I/71aG+xDKSYL._AC_UF1000,1000_QL80_.jpg",
            pages = 452,
            days = 14,
            finishedOn = "Oct 8"
        ),
        RecentBook(
            title = "Sapiens",
            author = "Yuval Noah Harari",
            coverUrl = "https://m.media-amazon.com/images/I/81ZGKEZW0RL._AC_UF1000,1000_QL80_.jpg",
            pages = 443,
            days = 12,
            finishedOn = "Sep 24"
        ),
        RecentBook(
            title = "Think and Grow Rich",
            author = "Napoleon Hill",
            coverUrl = "https://m.media-amazon.com/images/I/71yNgTMEcpL._AC_UF1000,1000_QL80_.jpg",
            pages = 238,
            days = 5,
            finishedOn = "Sep 9"
        )
    )
}