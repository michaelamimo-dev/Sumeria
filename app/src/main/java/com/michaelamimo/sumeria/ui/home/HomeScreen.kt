package com.michaelamimo.sumeria.ui.home

import androidx.compose.foundation.layout.statusBars
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import com.michaelamimo.sumeria.R
import com.michaelamimo.sumeria.ui.theme.SumeriaTurtleLogo
import kotlinx.coroutines.launch

private val Ink = Color(0xFF123B35)
private val Paper = Color(0xFFFFFCF5)
private val Cream = Color(0xFFFFF4DE)
private val Mint = Color(0xFFEAF4EC)
private val Sage = Color(0xFF829D8D)
private val Amber = Color(0xFFE6A843)
private val Track = Color(0xFFDCE7DC)
private val GoalMetGreen = Color(0xFFDCEFE0)
private val Coral = Color(0xFFF3A58C)
private val Lavender = Color(0xFFE8E3F5)
private val Sky = Color(0xFFDCEEF1)
private val RecentSectionSurface = Color(0xFFF5F1E7)
private val GoalsSectionSurface = Color(0xFFFFF5E5)

@Composable
fun HomeScreen() {
    val todayGoal = remember { MockHomeRepository.getTodayGoal() }
    val weeklyProgress = remember { MockHomeRepository.getWeeklyProgress() }
    val recentBooks = remember { MockHomeRepository.getRecentlyReadBooks() }

    val featuredBook = recentBooks.firstOrNull()
    val otherRecentBooks = recentBooks.drop(1)

    val view = LocalView.current
    val activity = remember(view) { view.context.findActivity() }

    SideEffect {
        activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Ink)
                .windowInsetsTopHeight(
                    androidx.compose.foundation.layout.WindowInsets.statusBars
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Paper)
                .verticalScroll(rememberScrollState())
        ) {
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
            Spacer(Modifier.height(120.dp))
        }
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
    val cardColors = listOf(Coral, Lavender, Sky, GoalMetGreen)
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        shape = RoundedCornerShape(28.dp),
        color = RecentSectionSurface,
        border = BorderStroke(1.dp, Ink.copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(vertical = 17.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
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

                    Spacer(Modifier.width(10.dp))

                    Icon(
                        painter = painterResource(id = R.drawable.ic_recently_read),
                        contentDescription = "Reader enjoying a book",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(54.dp)
                    )
                }

                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            val nextIndex = (listState.firstVisibleItemIndex + 1)
                                .coerceAtMost(books.lastIndex)
                            listState.animateScrollToItem(nextIndex)
                        }
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.72f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = "Scroll to the next recently read book",
                        tint = Ink,
                        modifier = Modifier.size(25.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(books) { index, book ->
                    val progress = book.progressPercentage.coerceIn(0f, 1f)
                    val cardColor = cardColors[index % cardColors.size]

                    Surface(
                        modifier = Modifier
                            .width(252.dp)
                            .clickable { /* Connect to the book details action. */ },
                        shape = RoundedCornerShape(22.dp),
                        color = cardColor
                    ) {
                        Column(modifier = Modifier.padding(15.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                AsyncImage(
                                    model = book.coverUrl,
                                    contentDescription = "Cover of ${book.title}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .width(78.dp)
                                        .height(112.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                )

                                Spacer(Modifier.width(13.dp))

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(112.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "BACK ON YOUR SHELF",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.7.sp
                                            ),
                                            color = Ink.copy(alpha = 0.68f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(7.dp))
                                        Text(
                                            text = book.title,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = Ink,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(3.dp))
                                        Text(
                                            text = "By ${book.author}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Ink.copy(alpha = 0.7f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.62f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(
                                                horizontal = 9.dp,
                                                vertical = 5.dp
                                            ),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${(progress * 100).toInt()}% read",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                color = Ink
                                            )
                                            Spacer(Modifier.width(3.dp))
                                            Icon(
                                                imageVector = Icons.Rounded.ChevronRight,
                                                contentDescription = null,
                                                tint = Ink,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            LinearProgressIndicator(
                                progress = progress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(CircleShape),
                                color = Ink,
                                trackColor = Color.White.copy(alpha = 0.62f)
                            )

                            Spacer(Modifier.height(7.dp))

                            Text(
                                text = "Page ${book.currentPage} of ${book.totalPages}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Ink.copy(alpha = 0.72f)
                            )
                        }
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
    val target = goal.targetMinutes
    val progress = if (target > 0) {
        (goal.currentMinutes.toFloat() / target).coerceIn(0f, 1f)
    } else {
        0f
    }
    val goalMet = target > 0 && goal.currentMinutes >= target
    val daysRead = weeklyProgress.count { it.percentage > 0f }
    val remainingMinutes = (target - goal.currentMinutes).coerceAtLeast(0)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        shape = RoundedCornerShape(28.dp),
        color = GoalsSectionSurface,
        border = BorderStroke(1.dp, Amber.copy(alpha = 0.18f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
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
                }

                Icon(
                    painter = painterResource(id = R.drawable.ic_reading_goals),
                    contentDescription = "Reading goal illustration",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(width = 72.dp, height = 78.dp)
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Ink
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF174B40),
                                    Ink,
                                    Color(0xFF0D302B)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "TODAY'S READING",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                ),
                                color = Mint.copy(alpha = 0.78f)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = formatReadingTime(goal.currentMinutes),
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 40.sp
                                ),
                                color = Paper
                            )
                            Text(
                                text = if (target > 0) {
                                    "of your $target-minute goal"
                                } else {
                                    "Set a daily goal to begin"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Mint.copy(alpha = 0.82f)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                        ) {
                            Text(
                                text = if (goalMet) "Goal met ✨" else "Keep going",
                                modifier = Modifier.padding(
                                    horizontal = 11.dp,
                                    vertical = 8.dp
                                ),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Paper
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(9.dp)
                            .clip(CircleShape),
                        color = Amber,
                        trackColor = Color.White.copy(alpha = 0.18f)
                    )

                    Spacer(Modifier.height(9.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = when {
                                target <= 0 -> "Your next chapter starts here"
                                goalMet -> "Today's goal complete 🎉"
                                else -> "$remainingMinutes minutes to go"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = Mint.copy(alpha = 0.9f)
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Paper
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GoalHighlight(
                            emoji = "🔥",
                            value = "$streakDays",
                            label = "day streak",
                            modifier = Modifier.weight(1f)
                        )
                        GoalHighlight(
                            emoji = "📚",
                            value = "$daysRead",
                            label = "days read this week",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Every minute counts. Keep making room for a good story.",
                style = MaterialTheme.typography.bodySmall,
                color = Sage,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}

@Composable
private fun GoalHighlight(
    emoji: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(17.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(17.dp)
            )
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.width(9.dp))
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Paper
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Mint.copy(alpha = 0.78f)
            )
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

private fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}