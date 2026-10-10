package com.michaelamimo.sumeria.ui.library

import android.app.Activity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.statusBars
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
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

private val Ink = Color(0xFF123B35)
private val Paper = Color(0xFFFFFCF5)
private val Cream = Color(0xFFFFF4DE)
private val Mint = Color(0xFFEAF4EC)
private val Sage = Color(0xFF829D8D)
private val Amber = Color(0xFFE6A843)
private val Coral = Color(0xFFF3A58C)
private val Lavender = Color(0xFFE8E3F5)
private val Sky = Color(0xFFDCEEF1)
private val ShelfSurface = Color(0xFFF5F1E7)

@Composable
fun LibraryScreen() {
    var selectedCollection by remember { mutableStateOf<LibraryCollection?>(null) }
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
        // Keep the dark status bar background stationary while content scrolls.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Ink)
                .windowInsetsTopHeight(
                    androidx.compose.foundation.layout.WindowInsets.statusBars
                )
        )

        if (selectedCollection == null) {
            MainCollectionsView(
                collections = collections,
                onCollectionClick = { selectedCollection = it }
            )
        } else {
            CollectionDetailView(
                collection = selectedCollection!!,
                onBackClick = { selectedCollection = null }
            )
        }
    }
}

@Composable
fun MainCollectionsView(
    collections: List<LibraryCollection>,
    onCollectionClick: (LibraryCollection) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Spacer(Modifier.height(22.dp))
            LibraryHeader()
            Spacer(Modifier.height(28.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "MAKE YOURSELF AT HOME",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = Sage
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = "Your collections",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Ink
                    )
                }

                Text(
                    text = "${collections.size} shelves",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Sage,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Spacer(Modifier.height(15.dp))
        }

        items(collections) { collection ->
            CollectionCard(
                collection = collection,
                onClick = { onCollectionClick(collection) }
            )
            Spacer(Modifier.height(14.dp))
        }

        item {
            Spacer(Modifier.height(15.dp))
            LibraryFooter()
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun LibraryHeader() {
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
            Column {
                Text(
                    text = "SUMERIA LIBRARY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = Sage
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "A home for your\nnext great read.",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 32.sp
                    ),
                    color = Ink
                )
            }

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Cream
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_bookshelves),
                    contentDescription = "Books on a shelf",
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .size(width = 82.dp, height = 82.dp)
                        .padding(8.dp)
                )
            }
        }

        Spacer(Modifier.height(19.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(17.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Ink.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search books or collections",
                        tint = Sage,
                        modifier = Modifier.size(21.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Find a book or collection",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Sage
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Surface(
                modifier = Modifier.size(50.dp),
                shape = RoundedCornerShape(17.dp),
                color = Ink,
                onClick = { /* Show the add collection action. */ }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add collection",
                        tint = Paper,
                        modifier = Modifier.size(25.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CollectionCard(
    collection: LibraryCollection,
    onClick: () -> Unit
) {
    val cardColor = collectionColor(collection.title)
    val isFeatured = collection.title.equals("All", ignoreCase = true)
    val titleColor = if (isFeatured) Paper else Ink
    val supportingColor = if (isFeatured) Paper.copy(alpha = 0.75f) else Sage

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { /* Show the edit/delete collection actions. */ }
            ),
        shape = RoundedCornerShape(25.dp),
        color = cardColor,
        border = if (isFeatured) {
            null
        } else {
            BorderStroke(1.dp, Ink.copy(alpha = 0.05f))
        }
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isFeatured) "YOUR WHOLE LIBRARY" else "COLLECTION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = supportingColor
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = collection.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = titleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = if (isFeatured) Color.White.copy(alpha = 0.14f)
                    else Color.White.copy(alpha = 0.72f)
                ) {
                    Text(
                        text = "${collection.books.size} books",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = titleColor,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                if (collection.books.isEmpty()) {
                    Text(
                        text = "Your next favorite is waiting to be added.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = supportingColor
                    )
                } else {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy((-13).dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        collection.books.take(4).forEach { book ->
                            AsyncImage(
                                model = book.coverUrl,
                                contentDescription = "Cover of ${book.title}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(57.dp)
                                    .height(82.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = 2.dp,
                                        color = cardColor,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            )
                        }

                        if (collection.books.size > 4) {
                            Surface(
                                modifier = Modifier
                                    .padding(start = 1.dp, bottom = 1.dp)
                                    .size(39.dp),
                                shape = CircleShape,
                                color = if (isFeatured) Color.White.copy(alpha = 0.18f)
                                else Color.White.copy(alpha = 0.78f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "+${collection.books.size - 4}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = titleColor
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Open collection",
                    tint = titleColor,
                    modifier = Modifier
                        .size(22.dp)
                        .padding(2.dp)
                        .graphicsFlipHorizontally()
                )
            }

            Spacer(Modifier.height(13.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isFeatured) {
                        "Everything you’ve added, all in one place"
                    } else {
                        "Open this shelf"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = supportingColor
                )
                Text(
                    text = "EXPLORE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = titleColor
                )
            }
        }
    }
}

@Composable
private fun LibraryFooter() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(25.dp),
        color = Cream
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "A SHELF OF YOUR OWN",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = Sage
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "Your next story is just a page away.",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Ink
                )
            }

            Icon(
                painter = painterResource(id = R.drawable.ic_bookshelves),
                contentDescription = "Bookshelves illustration",
                tint = Color.Unspecified,
                modifier = Modifier.size(width = 82.dp, height = 78.dp)
            )
        }
    }
}

@Composable
fun CollectionDetailView(
    collection: LibraryCollection,
    onBackClick: () -> Unit
) {
    val shelfColors = listOf(
        Amber.copy(alpha = 0.6f),
        Sky.copy(alpha = 0.75f),
        Mint,
        Coral.copy(alpha = 0.55f)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 24.dp, top = 18.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back to collections",
                    tint = Ink
                )
            }

            Spacer(Modifier.width(7.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "YOUR SHELF",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = Sage
                )
                Text(
                    text = collection.title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Ink
                )
            }

            Surface(
                shape = CircleShape,
                color = Cream
            ) {
                Text(
                    text = "${collection.books.size}",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Ink,
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp)
                )
            }
        }

        if (collection.books.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "This shelf is ready for its first book.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Sage
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 12.dp,
                    bottom = 120.dp,
                    start = 24.dp,
                    end = 24.dp
                )
            ) {
                val rows = collection.books.chunked(3)

                items(rows.size) { rowIndex ->
                    val rowBooks = rows[rowIndex]
                    val shelfColor = shelfColors[rowIndex % shelfColors.size]

                    BookShelfRow(
                        books = rowBooks,
                        shelfColor = shelfColor
                    )
                    Spacer(Modifier.height(30.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookShelfRow(
    books: List<LibraryBook>,
    shelfColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            color = shelfColor
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.5f))
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.5f))
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            for (index in 0..2) {
                if (index < books.size) {
                    val book = books[index]

                    Box(
                        modifier = Modifier
                            .width(86.dp)
                            .height(130.dp)
                            .combinedClickable(
                                onClick = { /* Open the reader. */ },
                                onLongClick = { /* Show book actions. */ }
                            )
                    ) {
                        AsyncImage(
                            model = book.coverUrl,
                            contentDescription = book.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                        )

                        if (book.isCloud) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp)
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
                } else {
                    Spacer(Modifier.width(86.dp))
                }
            }
        }
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

private fun collectionColor(title: String): Color {
    return when (title) {
        "All" -> Ink
        "Read" -> Mint
        "Favorites" -> Cream
        "To Read" -> Lavender
        "Design" -> Coral.copy(alpha = 0.45f)
        "Psychology" -> Sky
        else -> Color.White
    }
}

private fun Modifier.graphicsFlipHorizontally(): Modifier {
    return this.then(
        Modifier.graphicsLayer {
            scaleX = -1f
        }
    )
}

private fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}

// Mock data

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
        "https://m.media-amazon.com/images/I/81bsw6fnUiL._AC_UF1000,1000_QL80_.jpg",
        "https://m.media-amazon.com/images/I/71QKQ9mwV7L._AC_UF1000,1000_QL80_.jpg",
        "https://m.media-amazon.com/images/I/71aG+xDKSYL._AC_UF1000,1000_QL80_.jpg",
        "https://m.media-amazon.com/images/I/81ZGKEZW0RL._AC_UF1000,1000_QL80_.jpg",
        "https://m.media-amazon.com/images/I/71yNgTMEcpL._AC_UF1000,1000_QL80_.jpg"
    )

    fun getCollections(): List<LibraryCollection> {
        val allBooks = List(15) { index ->
            LibraryBook(
                title = "Sample Book $index",
                author = "Author $index",
                coverUrl = sampleCovers[index % sampleCovers.size],
                isCloud = index % 3 == 0
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