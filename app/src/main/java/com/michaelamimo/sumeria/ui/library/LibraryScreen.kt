package com.michaelamimo.sumeria.ui.library

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.michaelamimo.sumeria.R

// ---------------------------------------------------------------------------
// Palette (same family as the Home screen)
// ---------------------------------------------------------------------------

private val Ink = Color(0xFF123B35)
private val Paper = Color(0xFFFFFCF5)
private val Cream = Color(0xFFFFF4DE)
private val Mint = Color(0xFFEAF4EC)
private val MintStage = Color(0xFFD6F1E3) // slightly stronger mint for the header "stage"
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
fun LibraryScreen(
    onBookClick: (String) -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showImportOptions by remember { mutableStateOf(false) }

    val openFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.importFile(it)
        }
    }
    val openFolderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.scanFolder(it)
        }
    }

    val importedBooks = remember(uiState.books) {
        uiState.books.map { book ->
            LibraryBook(
                title = book.title,
                author = book.authors.joinToString().ifBlank { "Unknown author" },
                coverUrl = book.coverUri,
                isCloud = false,
                id = book.id
            )
        }
    }
    val mockCollections = remember { MockLibraryRepository.getCollections() }
    val collections = remember(importedBooks, mockCollections) {
        mockCollections.map { collection ->
            if (collection.title.equals("All", ignoreCase = true)) collection.copy(books = importedBooks)
            else collection
        }
    }
    var selectedCollection by remember { mutableStateOf<LibraryCollection?>(null) }
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

    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Paper)) {
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
                        isImporting = uiState.isImporting,
                        onAddBooks = { showImportOptions = true },
                        onBookClick = onBookClick,
                        onCollectionClick = { selectedCollection = it }
                    )
                } else {
                    CollectionDetailView(
                        collection = selected,
                        onBookClick = onBookClick,
                        onBackClick = { selectedCollection = null }
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 16.dp)
        )

        if (showImportOptions) {
            AlertDialog(
                onDismissRequest = { showImportOptions = false },
                containerColor = Paper,
                title = { Text("Add books", color = Ink, fontWeight = FontWeight.Bold) },
                text = { Text("Choose EPUB or PDF files, or scan a folder you select.", color = Sage) },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = {
                            showImportOptions = false
                            openFilePicker.launch(arrayOf("application/pdf", "application/epub+zip", "application/epub"))
                        }) { Text("Choose files", color = Ink) }
                        TextButton(onClick = {
                            showImportOptions = false
                            openFolderPicker.launch(null)
                        }) { Text("Scan folder", color = Ink) }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportOptions = false }) { Text("Cancel", color = Sage) }
                }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Home (collections overview)
// ---------------------------------------------------------------------------

@Composable
private fun CollectionsHome(
    collections: List<LibraryCollection>,
    isImporting: Boolean,
    onAddBooks: () -> Unit,
    onBookClick: (String) -> Unit,
    onCollectionClick: (LibraryCollection) -> Unit
) {
    val allCollection = collections.firstOrNull { it.title.equals("All", ignoreCase = true) }
    val shelves = collections.filterNot { it.title.equals("All", ignoreCase = true) }
    val tiles: List<LibraryCollection?> = shelves + null // trailing null = "New shelf" tile

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Spacer(Modifier.height(20.dp))
            LibraryHeader(
                totalBooks = allCollection?.books?.size ?: 0,
                isImporting = isImporting,
                onAddBooks = onAddBooks
            )
            Spacer(Modifier.height(26.dp))
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

// ---------------------------------------------------------------------------
// Header: split card (text on cream, illustration on a mint "stage")
// ---------------------------------------------------------------------------

@Composable
fun LibraryHeader(totalBooks: Int, isImporting: Boolean, onAddBooks: () -> Unit) {
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
            // Left: mascot + label, headline, books pill
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
                        text = "YOUR LIBRARY",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp
                        ),
                        color = Ink,
                        maxLines = 1
                    )
                }

                Text(
                    text = "A world\nbetween\ncovers.",
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
                        Text("📚", fontSize = 14.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "$totalBooks books",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Ink
                        )
                        Spacer(Modifier.width(2.dp))
                        IconButton(
                            onClick = onAddBooks,
                            enabled = !isImporting,
                            modifier = Modifier.size(30.dp)
                        ) {
                            if (isImporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(17.dp),
                                    strokeWidth = 2.dp,
                                    color = Ink
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Import EPUB or PDF books",
                                    tint = Ink,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Right: mint stage with the illustration, flush to the card edge
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
                    painter = painterResource(id = R.drawable.ic_library_screen),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                        .height(112.dp)
                )
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
    onBookClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    var filter by remember(collection) { mutableStateOf(ShelfFilter.All) }
    var shelfQuery by remember(collection) { mutableStateOf("") }

    val totalCount = collection.books.size
    val cloudCount = remember(collection) { collection.books.count { it.isCloud } }
    val deviceCount = totalCount - cloudCount

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
        // Hero header + floating search/sort toolbar
        item {
            ShelfHeader(
                collection = collection,
                tint = tint,
                cloudCount = cloudCount,
                query = shelfQuery,
                onQueryChange = { shelfQuery = it },
                onBackClick = onBackClick
            )
        }

        // Filter chips (with counts)
        item {
            Spacer(Modifier.height(18.dp))
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
                        count = when (option) {
                            ShelfFilter.All -> totalCount
                            ShelfFilter.OnDevice -> deviceCount
                            ShelfFilter.Cloud -> cloudCount
                        },
                        selected = filter == option,
                        onClick = { filter = option }
                    )
                }
            }

            if (shelfQuery.isNotBlank() || filter != ShelfFilter.All) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "${visibleBooks.size} of $totalCount books",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Sage,
                    modifier = Modifier.padding(horizontal = SIDE.dp)
                )
            }
            Spacer(Modifier.height(26.dp))
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
                        shelfColor = shelfColors[rowIndex % shelfColors.size],
                        onBookClick = onBookClick
                    )
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

/**
 * Tinted hero with rounded bottom corners. The search/sort toolbar floats over its
 * bottom edge, so the toolbar and the hero read as one piece.
 */
@Composable
private fun ShelfHeader(
    collection: LibraryCollection,
    tint: Color,
    cloudCount: Int,
    query: String,
    onQueryChange: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val overlap = 28.dp

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = overlap),
            shape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp),
            color = tint
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        // Soft decorative shapes
                        drawCircle(
                            color = Color.White.copy(alpha = 0.34f),
                            radius = size.width * 0.46f,
                            center = Offset(size.width * 1.0f, size.height * 0.12f)
                        )
                        drawCircle(
                            color = Ink.copy(alpha = 0.05f),
                            radius = size.width * 0.58f,
                            center = Offset(size.width * 1.0f, size.height * 0.12f),
                            style = Stroke(width = 1.dp.toPx())
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.22f),
                            radius = size.width * 0.2f,
                            center = Offset(size.width * 0.02f, size.height * 0.9f)
                        )
                    }
                    .padding(
                        start = SIDE.dp,
                        end = SIDE.dp,
                        top = 18.dp,
                        bottom = overlap + 22.dp
                    )
            ) {
                // Back + emoji badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, Ink.copy(alpha = 0.06f)),
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

                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(collectionEmoji(collection.title), fontSize = 20.sp)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Title block + illustration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "YOUR SHELF",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.4.sp
                            ),
                            color = Ink.copy(alpha = 0.55f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = collection.title,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = collectionBlurb(collection.title),
                            style = MaterialTheme.typography.bodySmall,
                            color = Ink.copy(alpha = 0.7f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatPill("${collection.books.size} books")
                            if (cloudCount > 0) {
                                StatPill("$cloudCount in cloud")
                            }
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(116.dp)
                            .background(Color.White.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_library_screen_book),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .width(98.dp)
                                .height(84.dp)
                        )
                    }
                }
            }
        }

        ShelfToolbar(
            query = query,
            onQueryChange = onQueryChange,
            placeholder = if (collection.title.equals("All", ignoreCase = true))
                "Search all books"
            else
                "Search in ${collection.title}",
            onSortClick = { /* Sorting options will be added in a later phase. */ },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = SIDE.dp)
        )
    }
}

@Composable
private fun StatPill(text: String) {
    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.75f)) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Ink,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/** One raised card holding the search field and the sort control. */
@Composable
private fun ShelfToolbar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    onSortClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(28.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .shadow(
                elevation = 12.dp,
                shape = shape,
                ambientColor = Ink.copy(alpha = 0.18f),
                spotColor = Ink.copy(alpha = 0.28f)
            ),
        shape = shape,
        color = Color.White,
        border = BorderStroke(1.dp, Line)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 10.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Mint,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = Ink,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

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
                                color = Sage,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear search",
                        tint = Sage,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .width(1.dp)
                    .height(26.dp)
                    .background(Line)
            )

            // Sort control
            Surface(
                shape = CircleShape,
                color = Ink,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onSortClick)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(2.5.dp)
                                .clip(CircleShape)
                                .background(Paper)
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(13.dp)
                                .height(2.5.dp)
                                .clip(CircleShape)
                                .background(Paper)
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height(2.5.dp)
                                .clip(CircleShape)
                                .background(Amber)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryChip(
    label: String,
    count: Int,
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
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = if (selected) Paper else Ink
            )
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = CircleShape,
                color = if (selected) Amber else Mint
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Ink,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
fun BookShelfRow(
    books: List<LibraryBook>,
    shelfColor: Color,
    onBookClick: (String) -> Unit = {}
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
                        BookCover(book = books[index], modifier = Modifier.weight(1f), onBookClick = onBookClick)
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
    modifier: Modifier = Modifier,
    onBookClick: (String) -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(2f / 3f)
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = { book.id?.let(onBookClick) },
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
private fun LibraryEyebrow(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        ),
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
    val coverUrl: String?,
    val isCloud: Boolean = false,
    val id: String? = null
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
