package com.michaelamimo.sumeria.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.michaelamimo.sumeria.ui.theme.SumeriaTurtleLogo
import androidx.compose.foundation.layout.statusBars

private val Ink = Color(0xFF123B35)
private val Paper = Color(0xFFFFFCF5)
private val Cream = Color(0xFFFFF4DE)
private val Mint = Color(0xFFEAF4EC)
private val Sage = Color(0xFF829D8D)
private val Amber = Color(0xFFE6A843)
private val Track = Color(0xFFDCE7DC)
private val GoalMetGreen = Color(0xFFDCEFE0)

@Composable
fun HomeScreen() {
    val todayGoal = remember { MockHomeRepository.getTodayGoal() }
    val weeklyProgress = remember { MockHomeRepository.getWeeklyProgress() }
    val recentBooks = remember { MockHomeRepository.getRecentlyReadBooks() }

    val featuredBook = recentBooks.firstOrNull()
    val otherRecentBooks = recentBooks.drop(1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Ink)
                .windowInsetsTopHeight(
                    androidx.compose.foundation.layout.WindowInsets.statusBars
                )
        )

        Spacer(Modifier.height(20.dp))
        HeaderSection(userName = "Michael")
        Spacer(Modifier.height(28.dp))

        if (featuredBook != null) {
            ContinueReadingHeroSection(book = featuredBook)
            Spacer(Modifier.height(30.dp))
        }

        if (otherRecentBooks.isNotEmpty()) {
            RecentlyReadSection(books = otherRecentBooks)
            Spacer(Modifier.height(30.dp))
        }

        ReadingGoalCardSection(
            goal = todayGoal,
            weeklyProgress = weeklyProgress,
            streakDays = 7
        )

        Spacer(Modifier.height(34.dp))
        HomeScreenBottomMessage()

        // Space for the app's bottom navigation.
        Spacer(Modifier.height(120.dp))
    }
}

@Composable
fun HeaderSection(userName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SumeriaTurtleLogo(modifier = Modifier.size(40.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "SUMERIA",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.8.sp
                    ),
                    color = Ink
                )
            }

            Surface(
                shape = CircleShape,
                color = Mint,
                border = BorderStroke(1.dp, Sage.copy(alpha = 0.18f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔥", fontSize = 14.sp)
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "7 day streak",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Ink
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "YOUR READING SANCTUARY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.3.sp
                    ),
                    color = Sage
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Good morning,\n$userName.",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 37.sp
                    ),
                    color = Ink
                )
            }

            Icon(
                painter = painterResource(id = R.drawable.ic_books),
                contentDescription = "A stack of books",
                tint = Color.Unspecified,
                modifier = Modifier
                    .width(92.dp)
                    .height(76.dp)
            )
        }

        Spacer(Modifier.height(13.dp))
        Text(
            text = "A little time with a good book goes a long way.",
            style = MaterialTheme.typography.bodyMedium,
            color = Sage
        )
    }
}

@Composable
fun ContinueReadingHeroSection(book: Book) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        SectionEyebrow(text = "YOUR CURRENT CHAPTER")
        Spacer(Modifier.height(12.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = Cream,
            border = BorderStroke(1.dp, Amber.copy(alpha = 0.22f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = book.coverUrl,
                        contentDescription = "Cover of ${book.title}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(96.dp)
                            .height(138.dp)
                            .clip(RoundedCornerShape(13.dp))
                    )

                    Spacer(Modifier.width(17.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "BACK TO THE PAGE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = Sage
                        )
                        Spacer(Modifier.height(9.dp))
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Ink,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = "By ${book.author}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Sage,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "${(book.progressPercentage * 100).toInt()}% through",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Ink
                        )
                    }
                }

                Spacer(Modifier.height(17.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Page ${book.currentPage} of ${book.totalPages}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Sage
                    )
                    Text(
                        text = "A few more pages?",
                        style = MaterialTheme.typography.labelSmall,
                        color = Sage
                    )
                }

                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = book.progressPercentage.coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = Amber,
                    trackColor = Track
                )

                Spacer(Modifier.height(17.dp))

                Surface(
                    color = Ink,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* Connect to the reader navigation action. */ }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Continue reading",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Paper
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = Paper,
                            modifier = Modifier.size(19.dp)
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
                .padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                SectionEyebrow(text = "A FEW PAGES BACK")
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "Recently read",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Ink
                )
            }

            Icon(
                painter = painterResource(id = R.drawable.ic_recently_read),
                contentDescription = "Reader enjoying a book",
                tint = Color.Unspecified,
                modifier = Modifier.size(58.dp)
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(books) { book ->
                Surface(
                    modifier = Modifier.width(278.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Mint,
                    border = BorderStroke(1.dp, Sage.copy(alpha = 0.16f))
                ) {
                    Row(
                        modifier = Modifier.padding(13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = book.coverUrl,
                            contentDescription = "Cover of ${book.title}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(62.dp)
                                .height(88.dp)
                                .clip(RoundedCornerShape(9.dp))
                        )

                        Spacer(Modifier.width(13.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = book.title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Ink,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "By ${book.author}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Sage,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = book.progressPercentage.coerceIn(0f, 1f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape),
                                color = Ink,
                                trackColor = Track
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = "${(book.progressPercentage * 100).toInt()}% complete",
                                style = MaterialTheme.typography.labelSmall,
                                color = Sage
                            )
                        }

                        Spacer(Modifier.width(5.dp))
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = "Book details",
                            tint = Sage,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReadingGoalCardSection(
    goal: ReadingGoal,
    weeklyProgress: List<DailyProgress>,
    streakDays: Int
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                SectionEyebrow(text = "YOUR DAILY RITUAL")
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "Reading goals",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Ink
                )
                Text(
                    text = "Small steps, lovely stories.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Sage
                )
            }

            Icon(
                painter = painterResource(id = R.drawable.ic_reading_goals),
                contentDescription = "Reading goal illustration",
                tint = Color.Unspecified,
                modifier = Modifier
                    .width(76.dp)
                    .height(82.dp)
            )
        }

        Spacer(Modifier.height(14.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Mint,
            border = BorderStroke(1.dp, Sage.copy(alpha = 0.16f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.65f),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 9.dp.toPx()
                            val inset = strokeWidth / 2f
                            val arcSize = Size(
                                width = size.width - strokeWidth,
                                height = (size.width - strokeWidth).coerceAtMost(size.height * 2f)
                            )
                            val topLeft = Offset(inset, size.height - arcSize.height / 2f)

                            drawArc(
                                color = Track,
                                startAngle = 180f,
                                sweepAngle = 180f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )

                            val progress = if (goal.targetMinutes > 0) {
                                (goal.currentMinutes.toFloat() / goal.targetMinutes)
                                    .coerceIn(0f, 1f)
                            } else {
                                0f
                            }

                            if (progress > 0f) {
                                drawArc(
                                    color = Ink,
                                    startAngle = 180f,
                                    sweepAngle = 180f * progress,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(bottom = 5.dp)
                        ) {
                            Text(
                                text = "TODAY'S READING",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = Sage
                            )
                            Text(
                                text = formatReadingTime(goal.currentMinutes),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Ink
                            )
                            Text(
                                text = "of ${goal.targetMinutes} minutes",
                                style = MaterialTheme.typography.labelSmall,
                                color = Sage
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Cream
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🔥", fontSize = 18.sp)
                            Text(
                                text = "$streakDays",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Ink
                            )
                            Text(
                                text = "day streak",
                                style = MaterialTheme.typography.labelSmall,
                                color = Sage
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    weeklyProgress.forEach { day ->
                        val goalMet = day.percentage >= 1f

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Top
                        ) {
                            Text(
                                text = if (goalMet) "🔥" else "",
                                fontSize = 15.sp,
                                modifier = Modifier.height(21.dp)
                            )

                            Surface(
                                shape = CircleShape,
                                color = if (goalMet) GoalMetGreen else Paper,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (goalMet) {
                                        Sage.copy(alpha = 0.18f)
                                    } else {
                                        Sage.copy(alpha = 0.28f)
                                    }
                                )
                            ) {
                                Box(
                                    modifier = Modifier.size(36.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = day.dayOfWeek,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (goalMet) Ink else Sage
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = GoalMetGreen.copy(alpha = 0.72f)
                ) {
                    Text(
                        text = "You’ve kept your reading rhythm for $streakDays days. Keep turning pages!",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = Ink
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeScreenBottomMessage() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_home_screen_bottom),
            contentDescription = "A cheerful reader",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "Every page takes you somewhere.",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Ink
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = "Settle in, find your next favorite, and enjoy the journey.",
            style = MaterialTheme.typography.bodyMedium,
            color = Sage
        )
    }
}

@Composable
private fun SectionEyebrow(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        ),
        color = Sage
    )
}

private fun formatReadingTime(minutes: Int): String {
    return if (minutes >= 60) {
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        "${hours}h ${remainingMinutes}m"
    } else {
        "$minutes min"
    }
}