package com.michaelamimo.sumeria.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.michaelamimo.sumeria.R
import com.michaelamimo.sumeria.ui.theme.SumeriaColors
import com.michaelamimo.sumeria.ui.theme.SumeriaTurtleLogo
import androidx.compose.foundation.border

@Composable
fun HomeScreen() {
    val scrollState = rememberScrollState()

    val todayGoal = remember { MockHomeRepository.getTodayGoal() }
    val weeklyProgress = remember { MockHomeRepository.getWeeklyProgress() }
    val recentBooks = remember { MockHomeRepository.getRecentlyReadBooks() }

    val featuredBook = recentBooks.firstOrNull()
    val otherRecentBooks = if (recentBooks.size > 1) recentBooks.drop(1) else emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SumeriaColors.SurfaceWhite)
            .verticalScroll(scrollState)
    ) {
        // Uniform Dark Green Status Bar Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SumeriaColors.ActionPrimary)
                .windowInsetsTopHeight(WindowInsets.statusBars)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 1. Header with increased Mascot size
        HeaderSection(userName = "Michael")

        Spacer(modifier = Modifier.height(48.dp))

        // 2. Continue Reading (Hero Book)
        if (featuredBook != null) {
            ContinueReadingHeroSection(book = featuredBook)
        }

        Spacer(modifier = Modifier.height(48.dp))

        // 3. Recently Read with Illustration
        if (otherRecentBooks.isNotEmpty()) {
            RecentlyReadSection(books = otherRecentBooks)
        }

        Spacer(modifier = Modifier.height(48.dp))

        // 4. Reading Goals (Styled in a soft card, fire emojis above days)
        ReadingGoalCardSection(goal = todayGoal, weeklyProgress = weeklyProgress, streakDays = 7)

        // Bottom padding to clear the floating navigation bar
        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
fun HeaderSection(userName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Increased mascot size
                SumeriaTurtleLogo(modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SUMERIA",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = SumeriaColors.TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Your reading sanctuary",
                style = MaterialTheme.typography.bodyMedium,
                color = SumeriaColors.Muted
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Good morning,\n$userName.",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = SumeriaColors.TextPrimary,
                lineHeight = 36.sp
            )
        }

        Icon(
            painter = painterResource(id = R.drawable.ic_books), // Existing SVG
            contentDescription = "Books Illustration",
            tint = Color.Unspecified,
            modifier = Modifier
                .width(90.dp)
                .height(70.dp)
        )
    }
}

@Composable
fun ContinueReadingHeroSection(book: Book) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text(
            text = "Continue reading",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = SumeriaColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = SumeriaColors.BackgroundSoft.copy(alpha = 0.3f), // Soft Mint
            border = BorderStroke(1.dp, SumeriaColors.Muted.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = book.coverUrl,
                    contentDescription = "Cover of ${book.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .aspectRatio(0.65f)
                        .clip(RoundedCornerShape(12.dp))
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = SumeriaColors.TextPrimary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "By ${book.author}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SumeriaColors.Muted
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Page ${book.currentPage} of ${book.totalPages}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SumeriaColors.Muted
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${(book.progressPercentage * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = SumeriaColors.TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = book.progressPercentage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = SumeriaColors.ActionPrimary,
                    trackColor = SumeriaColors.Muted.copy(alpha = 0.2f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    color = SumeriaColors.ActionPrimary,
                    shape = RoundedCornerShape(percent = 50),
                    modifier = Modifier.fillMaxWidth(0.8f).clickable { /* TODO */ }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Continue reading",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = SumeriaColors.SurfaceWhite
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = SumeriaColors.SurfaceWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecentlyReadSection(books: List<Book>) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "Recently Read",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = SumeriaColors.TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            // New Illustration
            Icon(
                painter = painterResource(id = R.drawable.ic_recently_read),
                contentDescription = "Recently Read Illustration",
                tint = Color.Unspecified,
                modifier = Modifier
                    .width(60.dp)
                    .height(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(books) { book ->
                Surface(
                    modifier = Modifier.width(280.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = SumeriaColors.BackgroundSoft.copy(alpha = 0.3f), // Soft Mint
                    border = BorderStroke(1.dp, SumeriaColors.Muted.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = book.coverUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(56.dp)
                                .height(84.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = book.title,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = SumeriaColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "By ${book.author}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SumeriaColors.Muted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            LinearProgressIndicator(
                                progress = book.progressPercentage,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = SumeriaColors.ActionPrimary,
                                trackColor = SumeriaColors.Muted.copy(alpha = 0.2f)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = "${(book.progressPercentage * 100).toInt()}% complete",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = SumeriaColors.Muted
                                )
                                Text(
                                    text = "Page ${book.currentPage} of ${book.totalPages}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = SumeriaColors.Muted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = SumeriaColors.Muted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReadingGoalCardSection(goal: ReadingGoal, weeklyProgress: List<DailyProgress>, streakDays: Int) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    text = "Reading Goals",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SumeriaColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "See your stats soar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SumeriaColors.Muted
                )
            }
            // New Illustration
            Icon(
                painter = painterResource(id = R.drawable.ic_reading_goals),
                contentDescription = "Goals Illustration",
                tint = Color.Unspecified,
                modifier = Modifier
                    .width(50.dp)
                    .height(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Wrapped in the soft mint card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = SumeriaColors.BackgroundSoft.copy(alpha = 0.3f), // Soft Mint
            border = BorderStroke(1.dp, SumeriaColors.Muted.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // THE PERFECT SEMI-CIRCLE
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp)
                        .aspectRatio(2f),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 10.dp.toPx()
                        val inset = strokeWidth / 2f
                        val arcSize = Size(size.width - (inset * 2), size.width - (inset * 2))

                        drawArc(
                            color = SumeriaColors.Muted.copy(alpha = 0.2f),
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                            size = arcSize,
                            topLeft = Offset(inset, inset)
                        )

                        val progress = if (goal.targetMinutes > 0) (goal.currentMinutes.toFloat() / goal.targetMinutes).coerceIn(0f, 1f) else 0f
                        if (progress > 0) {
                            drawArc(
                                color = SumeriaColors.ActionPrimary,
                                startAngle = 180f,
                                sweepAngle = 180f * progress,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                                size = arcSize,
                                topLeft = Offset(inset, inset)
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = "Today's Reading",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = SumeriaColors.TextPrimary
                        )

                        val timeString = if (goal.currentMinutes >= 60) {
                            val hours = goal.currentMinutes / 60
                            val mins = goal.currentMinutes % 60
                            "$hours:${mins.toString().padStart(2, '0')}"
                        } else {
                            "${goal.currentMinutes} min"
                        }

                        Text(
                            text = timeString,
                            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Normal),
                            color = SumeriaColors.TextPrimary
                        )
                        Text(
                            text = "of your ${goal.targetMinutes} minute goal >",
                            style = MaterialTheme.typography.labelSmall,
                            color = SumeriaColors.Muted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Days Row with Fire Emojis ABOVE the days
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    weeklyProgress.forEach { day ->
                        val isCompleted = day.percentage >= 1f

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // The Fire Emoji Space (Invisible if not completed to maintain alignment)
                            Text(
                                text = if (isCompleted) "🔥" else "",
                                fontSize = 14.sp,
                                modifier = Modifier.height(20.dp) // Fixed height prevents jumping
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // The Day Circle
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (isCompleted) SumeriaColors.ActionPrimary else Color.Transparent)
                                    .then(
                                        if (!isCompleted) Modifier.border(1.dp, SumeriaColors.Muted.copy(alpha = 0.5f), CircleShape)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.dayOfWeek,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isCompleted) SumeriaColors.SurfaceWhite else SumeriaColors.Muted
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "You have a streak of $streakDays days 🔥",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = SumeriaColors.TextPrimary
                )
            }
        }
    }
}