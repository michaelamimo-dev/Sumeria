package com.michaelamimo.sumeria.ui.library

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import com.michaelamimo.sumeria.R

// ---------------------------------------------------------------------------
// Palette (same family as the Home screen)
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
private val Line = Ink.copy(alpha = 0.08f)

private const val SIDE = 24 // horizontal page margin, matches Home

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

@Composable
fun LibraryScreen() {
    var selectedCollection by remember { mutableStateOf<LibraryCollection?>(null) }
    var query by remember { mutableStateOf("") }
    val collections = remember { MockLibraryRepository.getCollections() }

    val view = LocalView.current
    val activity = remember(view) { view.context.findActivity() }

    SideEffect {
        activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = false
        }
    }

    BackHandler(enabled = selectedCollection != null) {
        selectedCollection = null
    }

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

        Crossfade(targetState = selectedCollection, label = "library-content") { selected ->
            if (selected == null) {
                CollectionsHome(
                    collections = collections,
                    query = query,
                    onQueryChange = { query = it },
                    onCollectionClick = { selectedCollection = it }
                )
            } else {
                CollectionDetailView(
                    collection = selected,
                    onBackClick = { selectedCollection = null }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Home (collections overview)
// ---------------------------------------------------------------------------

@Composable
private fun CollectionsHome(
    collections: List<LibraryCollection>,
    query: String,
    onQueryChange: (String) -> Unit,
    onCollectionClick: (LibraryCollection) -> Unit
) {
    val allCollection = collections.firstOrNull { it.title.equals("All", ignoreCase = true) }
    val shelves = collections.filterNot { it.title.equals("All", ignoreCase = true) }
    val tiles: List<LibraryCollection?> = shelves + null // trailing null = "New shelf" tile

    val searching = query.isNotBlank()
    val results = remember(query, allCollection) {
        if (!searching) emptyList()
        else allCollection?.books.orEmpty().filter {
            it.title.contains(query.trim(), ignoreCase = true) ||
                    it.author.contains(query.trim(), ignoreCase = true)
        }
    }
    val shelfColors = remember { shelfPalette() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Spacer(Modifier.height(24.dp))
            LibraryHeader(
                query = query,
                onQueryChange = onQueryChange,
                totalBooks = allCollection?.books?.size ?: 0
            )
            Spacer(Modifier.height(32.dp))
        }

        if (searching) {
            item {
                SectionHeader(
                    eyebrow = "SEARCH",
                    title = "Matching books",
                    trailing = "${results.size} found"
                )
                Spacer(Modifier.height(20.dp))
            }

            if (results.isEmpty()) {
                item {
                    EmptyState(message = "Nothing matches “${query.trim()}” yet.")
                }
            } else {
                itemsIndexed(results.chunked(3)) { index, row ->
                    Box(Modifier.padding(horizontal = SIDE.dp)) {
                        BookShelfRow(
                            books = row,
                            shelfColor = shelfColors[index % shelfColors.size]
                        )
                    }
                    Spacer(Modifier.height(28.dp))
                }
            }
        } else {
            item {
                SectionHeader(
                    eyebrow = "YOUR BOOK WORLD",
                    title = "Browse by mood",
                    trailing = "${collections.size} shelves"
                )
                Spacer(Modifier.height(18.dp))
            }

            if (allCollection != null) {
                item {
                    CollectionFeatureCard(
                        collection = allCollection,
                        onClick = { onCollectionClick(allCollection) }
                    )
                    Spacer(Modifier.height(14.dp))
                }
            }

            items(tiles.chunked(2)) { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SIDE.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    pair.forEach { collection ->
                        if (collection != null) {
                            CollectionTile(
                                collection = collection,
                                modifier = Modifier.weight(1f),
                                onClick = { onCollectionClick(collection) }
                            )
                        } else {
                            NewShelfTile(
                                modifier = Modifier.weight(1f),
                                onClick = { /* Show the create shelf action. */ }
                            )
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            item {
                Spacer(Modifier.height(24.dp))
                LibraryIllustrationFooter()
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Header
// ---------------------------------------------------------------------------

@Composable
fun LibraryHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    totalBooks: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SIDE.dp)
    ) {
        // Eyebrow + book count pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LibraryEyebrow("YOUR LIBRARY", larger = true)

            Surface(
                shape = CircleShape,
                color = Mint,
                border = BorderStroke(1.dp, Line)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📚", fontSize = 15.sp)
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = "$totalBooks books",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Ink
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Headline + illustration (same rhythm as the Home greeting)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "A world\nbetween\ncovers.",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 40.sp
                ),
                color = Ink
            )
            Image(
                painter = painterResource(id = R.drawable.ic_library_screen),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(150.dp)
                    .height(124.dp)
            )
        }

        Spacer(Modifier.height(10.dp))
        Text(
            text = "Find a familiar favorite or make room for a new one.",
            style = MaterialTheme.typography.bodyMedium,
            color = Sage
        )

        Spacer(Modifier.height(22.dp))

        LibrarySearchField(
            query = query,
            onQueryChange = onQueryChange,
            placeholder = "Search your library"
        )
    }
}

@Composable
private fun LibrarySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Line)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Search",
                tint = Sage,
                modifier = Modifier.size(21.dp)
            )
            Spacer(Modifier.width(10.dp))

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Ink),
                cursorBrush = SolidColor(Ink),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Sage
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear search",
                        tint = Sage,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
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
            LibraryEyebrow(eyebrow)
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

// ---------------------------------------------------------------------------
// Collection cards
// ---------------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollectionFeatureCard(
    collection: LibraryCollection,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SIDE.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { /* Show collection actions. */ }
            ),
        shape = RoundedCornerShape(30.dp),
        color = Ink
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // Soft decorative circles for depth
                    drawCircle(
                        color = Color.White.copy(alpha = 0.05f),
                        radius = size.width * 0.38f,
                        center = Offset(size.width * 0.95f, size.height * 0.1f)
                    )
                    drawCircle(
                        color = Amber.copy(alpha = 0.10f),
                        radius = size.width * 0.16f,
                        center = Offset(size.width * 0.05f, size.height * 1.0f)
                    )
                }
                .padding(start = 22.dp, top = 22.dp, bottom = 22.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "EVERYTHING YOU'VE ADDED",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp
                    ),
                    color = Mint.copy(alpha = 0.76f)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "All books",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Paper
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${collection.books.size} books, gathered in one place.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Mint.copy(alpha = 0.82f)
                )
                Spacer(Modifier.height(18.dp))
                Surface(shape = CircleShape, color = Amber) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Open shelf",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Ink
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Rounded.ArrowForward,
                            contentDescription = "Open all books",
                            tint = Ink,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Fanned covers
            val rotations = listOf(-9f, -3f, 3f, 9f)
            Box(
                modifier = Modifier
                    .width(136.dp)
                    .height(116.dp)
            ) {
                collection.books.take(4).forEachIndexed { index, book ->
                    AsyncImage(
                        model = book.coverUrl,
                        contentDescription = "Cover of ${book.title}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = (index * 24).dp)
                            .width(62.dp)
                            .height(94.dp)
                            .graphicsLayer { rotationZ = rotations[index] }
                            .clip(RoundedCornerShape(8.dp))
                            .border(2.dp, Ink, RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollectionTile(
    collection: LibraryCollection,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tileColor = collectionColor(collection.title)
    val coverTilt = listOf(-5f, 0f, 5f)

    Surface(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = { /* Show collection actions. */ }
            ),
        shape = RoundedCornerShape(26.dp),
        color = tileColor,
        border = BorderStroke(1.dp, Ink.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(236.dp)
                .drawBehind {
                    // Soft decorative shapes that fill the middle of the card
                    drawCircle(
                        color = Color.White.copy(alpha = 0.38f),
                        radius = size.width * 0.44f,
                        center = Offset(size.width * 0.96f, size.height * 0.04f)
                    )
                    drawCircle(
                        color = Ink.copy(alpha = 0.06f),
                        radius = size.width * 0.54f,
                        center = Offset(size.width * 0.96f, size.height * 0.04f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.24f),
                        radius = size.width * 0.24f,
                        center = Offset(size.width * 0.02f, size.height * 0.64f)
                    )
                }
                .padding(16.dp)
        ) {
            // Emoji badge + count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(collectionEmoji(collection.title), fontSize = 19.sp)
                    }
                }

                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.6f)) {
                    Text(
                        text = collection.books.size.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Ink,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = collectionSubtitle(collection.title).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.9.sp
                ),
                color = Ink.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = collection.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = collectionBlurb(collection.title),
                style = MaterialTheme.typography.bodySmall,
                color = Ink.copy(alpha = 0.68f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.weight(1f))

            // Covers resting on a little plank
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy((-12).dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        collection.books.take(3).forEachIndexed { index, book ->
                            AsyncImage(
                                model = book.coverUrl,
                                contentDescription = "Cover of ${book.title}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(44.dp)
                                    .height(64.dp)
                                    .graphicsLayer { rotationZ = coverTilt[index] }
                                    .clip(RoundedCornerShape(7.dp))
                                    .border(2.dp, tileColor, RoundedCornerShape(7.dp))
                            )
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .width(112.dp)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.7f))
                    )
                }

                Surface(shape = CircleShape, color = Ink) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowForward,
                        contentDescription = "Open ${collection.title}",
                        tint = Paper,
                        modifier = Modifier
                            .padding(7.dp)
                            .size(17.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NewShelfTile(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(26.dp),
        color = Color.White.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(236.dp)
                .drawBehind {
                    // Dashed outline: an open invitation to add a shelf
                    val inset = 1.dp.toPx()
                    drawRoundRect(
                        color = Ink.copy(alpha = 0.22f),
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - inset * 2, size.height - inset * 2),
                        cornerRadius = CornerRadius(26.dp.toPx()),
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f))
                        )
                    )
                }
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(shape = CircleShape, color = Mint) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Create a new shelf",
                    tint = Ink,
                    modifier = Modifier
                        .padding(14.dp)
                        .size(26.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "New shelf",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Ink
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "Make room for\na new mood",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = Sage
            )
        }
    }
}

/** Large closing illustration, same spirit as the Home screen's closing section. */
@Composable
private fun LibraryIllustrationFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SIDE.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_bookshelves),
            contentDescription = "Illustration of bookshelves",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "A LITTLE SPACE FOR BIG IDEAS",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            ),
            color = Sage
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Your next favorite is waiting on the shelf.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Ink
        )
    }
}

// ---------------------------------------------------------------------------
// Shelf detail
// ---------------------------------------------------------------------------

private enum class ShelfFilter(val label: String) {
    All("All"),
    OnDevice("On device"),
    Cloud("In cloud")
}

@Composable
private fun CollectionDetailView(
    collection: LibraryCollection,
    onBackClick: () -> Unit
) {
    var filter by remember(collection) { mutableStateOf(ShelfFilter.All) }
    var shelfQuery by remember(collection) { mutableStateOf("") }

    val visibleBooks = remember(collection, filter, shelfQuery) {
        val byFilter = when (filter) {
            ShelfFilter.All -> collection.books
            ShelfFilter.OnDevice -> collection.books.filterNot { it.isCloud }
            ShelfFilter.Cloud -> collection.books.filter { it.isCloud }
        }
        val q = shelfQuery.trim()
        if (q.isEmpty()) byFilter
        else byFilter.filter {
            it.title.contains(q, ignoreCase = true) ||
                    it.author.contains(q, ignoreCase = true)
        }
    }
    val tint = collectionColor(collection.title)
    val shelfColors = remember { shelfPalette() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // Top bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = SIDE.dp, end = SIDE.dp, top = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Mint,
                    border = BorderStroke(1.dp, Line),
                    modifier = Modifier.size(44.dp)
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back to library",
                            tint = Ink
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Library",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Sage
                )
            }
            Spacer(Modifier.height(20.dp))
        }

        // Banner
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SIDE.dp),
                shape = RoundedCornerShape(30.dp),
                color = tint,
                border = BorderStroke(1.dp, Ink.copy(alpha = 0.05f))
            ) {
                Row(
                    modifier = Modifier.padding(start = 22.dp, top = 22.dp, bottom = 22.dp, end = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "YOUR SHELF",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            ),
                            color = Ink.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = collection.title,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = collectionBlurb(collection.title),
                            style = MaterialTheme.typography.bodySmall,
                            color = Ink.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.height(14.dp))
                        Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.7f)) {
                            Text(
                                text = "${collection.books.size} books",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Ink,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                    Image(
                        painter = painterResource(id = R.drawable.ic_library_screen_book),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .width(112.dp)
                            .height(96.dp)
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // Per-shelf search
        item {
            Box(Modifier.padding(horizontal = SIDE.dp)) {
                LibrarySearchField(
                    query = shelfQuery,
                    onQueryChange = { shelfQuery = it },
                    placeholder = if (collection.title.equals("All", ignoreCase = true))
                        "Search all books"
                    else
                        "Search in ${collection.title}"
                )
            }
            Spacer(Modifier.height(14.dp))
        }

        // Filter chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = SIDE.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShelfFilter.entries.forEach { option ->
                    LibraryChip(
                        label = option.label,
                        selected = filter == option,
                        onClick = { filter = option }
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
        }

        if (visibleBooks.isEmpty()) {
            item {
                EmptyState(
                    message = when {
                        collection.books.isEmpty() -> "This shelf is ready for its first book."
                        shelfQuery.isNotBlank() -> "Nothing matches “${shelfQuery.trim()}” on this shelf."
                        else -> "Nothing here with this filter yet."
                    }
                )
            }
        } else {
            itemsIndexed(visibleBooks.chunked(3)) { rowIndex, row ->
                Box(Modifier.padding(horizontal = SIDE.dp)) {
                    BookShelfRow(
                        books = row,
                        shelfColor = shelfColors[rowIndex % shelfColors.size]
                    )
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun LibraryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = if (selected) Ink else Color.White,
        border = if (selected) null else BorderStroke(1.dp, Line)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = if (selected) Paper else Ink,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
        )
    }
}

@Composable
fun BookShelfRow(
    books: List<LibraryBook>,
    shelfColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Covers resting on the plank
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(shelfColor)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .padding(bottom = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                for (index in 0..2) {
                    if (index < books.size) {
                        BookCover(book = books[index], modifier = Modifier.weight(1f))
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Captions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            for (index in 0..2) {
                if (index < books.size) {
                    BookCaption(book = books[index], modifier = Modifier.weight(1f))
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookCover(
    book: LibraryBook,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(2f / 3f)
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = { /* Open the reader. */ },
                onLongClick = { /* Show book actions. */ }
            )
    ) {
        AsyncImage(
            model = book.coverUrl,
            contentDescription = book.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (book.isCloud) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp)
                    .size(24.dp),
                shape = CircleShape,
                color = Ink.copy(alpha = 0.84f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Cloud,
                    contentDescription = "Cloud stored",
                    tint = Paper,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}

@Composable
private fun BookCaption(
    book: LibraryBook,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = book.title,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = book.author,
            style = MaterialTheme.typography.labelSmall,
            color = Sage,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ---------------------------------------------------------------------------
// Empty state
// ---------------------------------------------------------------------------

@Composable
private fun EmptyState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SIDE.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SproutIllustration(
            modifier = Modifier
                .width(72.dp)
                .height(72.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = Sage
        )
    }
}

/** A tiny sprout for empty states. */
@Composable
private fun SproutIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawLine(
            color = Sage,
            start = Offset(w * 0.5f, h * 0.95f),
            end = Offset(w * 0.5f, h * 0.45f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
        rotate(degrees = -35f, pivot = Offset(w * 0.5f, h * 0.5f)) {
            drawOval(
                color = Teal.copy(alpha = 0.85f),
                topLeft = Offset(w * 0.10f, h * 0.28f),
                size = Size(w * 0.40f, h * 0.22f)
            )
        }
        rotate(degrees = 35f, pivot = Offset(w * 0.5f, h * 0.6f)) {
            drawOval(
                color = Teal.copy(alpha = 0.6f),
                topLeft = Offset(w * 0.5f, h * 0.42f),
                size = Size(w * 0.40f, h * 0.22f)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

@Composable
private fun LibraryEyebrow(text: String, larger: Boolean = false) {
    Text(
        text = text,
        style = if (larger) {
            MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                fontSize = 13.sp
            )
        } else {
            MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
        },
        color = Sage
    )
}

private fun shelfPalette(): List<Color> = listOf(
    Amber.copy(alpha = 0.65f),
    Sky.copy(alpha = 0.9f),
    Mint,
    Coral.copy(alpha = 0.6f),
    Lavender
)

private fun collectionColor(title: String): Color {
    return when (title) {
        "All" -> Mint
        "Read" -> Mint
        "Favorites" -> Cream
        "To Read" -> Lavender
        "Design" -> Coral.copy(alpha = 0.45f)
        "Psychology" -> Sky
        else -> Color.White
    }
}

private fun collectionEmoji(title: String): String {
    return when (title) {
        "Read" -> "📖"
        "Favorites" -> "💛"
        "To Read" -> "🔖"
        "Design" -> "🎨"
        "Psychology" -> "🧠"
        else -> "📚"
    }
}

private fun collectionSubtitle(title: String): String {
    return when (title) {
        "Read" -> "Finished"
        "Favorites" -> "Loved"
        "To Read" -> "Up next"
        "Design" -> "Inspiration"
        "Psychology" -> "Curious minds"
        else -> "Collected"
    }
}

private fun collectionBlurb(title: String): String {
    return when (title) {
        "All" -> "Everything you've added, in one place."
        "Read" -> "Stories you've seen all the way through."
        "Favorites" -> "The ones you keep coming back to."
        "To Read" -> "Waiting patiently for their turn."
        "Design" -> "Ideas worth looking at twice."
        "Psychology" -> "Books about how we think and feel."
        else -> "A little corner of your library."
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

// ==========================================
// MOCK DATA & MODELS
// ==========================================

data class LibraryCollection(
    val title: String,
    val books: List<LibraryBook>
)

data class LibraryBook(
    val title: String,
    val author: String,
    val coverUrl: String,
    val isCloud: Boolean = false
)

object MockLibraryRepository {
    private val sampleCovers = listOf(
        "https://m.media-amazon.com/images/I/81bsw6fnUiL._AC_UF1000,1000_QL80_.jpg", // Atomic Habits
        "https://m.media-amazon.com/images/I/71QKQ9mwV7L._AC_UF1000,1000_QL80_.jpg", // Subtle Art
        "https://m.media-amazon.com/images/I/71aG+xDKSYL._AC_UF1000,1000_QL80_.jpg", // 48 Laws
        "https://m.media-amazon.com/images/I/81ZGKEZW0RL._AC_UF1000,1000_QL80_.jpg", // Sapiens
        "https://m.media-amazon.com/images/I/71yNgTMEcpL._AC_UF1000,1000_QL80_.jpg"  // Think and Grow Rich
    )

    fun getCollections(): List<LibraryCollection> {
        val allBooks = List(15) {
            LibraryBook(
                title = "Sample Book $it",
                author = "Author $it",
                coverUrl = sampleCovers[it % sampleCovers.size],
                isCloud = it % 3 == 0 // Every 3rd book is in the cloud
            )
        }

        return listOf(
            LibraryCollection("All", allBooks),
            LibraryCollection("Read", allBooks.take(4)),
            LibraryCollection("Favorites", allBooks.shuffled().take(6)),
            LibraryCollection("To Read", allBooks.takeLast(5)),
            LibraryCollection("Design", allBooks.shuffled().take(3)),
            LibraryCollection("Psychology", allBooks.shuffled().take(7))
        )
    }
}
