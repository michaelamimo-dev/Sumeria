package com.michaelamimo.sumeria.ui.reader

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.michaelamimo.sumeria.domain.model.ReaderSection
import kotlinx.coroutines.flow.distinctUntilChanged

private data class ReaderBlock(
    val sectionIndex: Int,
    val text: String,
    val isHeading: Boolean
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReadingView(
    sections: List<ReaderSection>,
    initialBlockIndex: Int,
    initialScrollOffset: Int,
    prefs: ReaderPreferences,
    onToggleOverlay: () -> Unit,
    onScroll: () -> Unit,
    onPositionChanged: (blockIndex: Int, sectionIndex: Int, scrollOffset: Int) -> Unit,
    onWordSelected: (String) -> Unit
) {
    val blocks = remember(sections) {
        sections.flatMapIndexed { sectionIndex, section ->
            buildList {
                add(ReaderBlock(sectionIndex, section.title, isHeading = true))
                section.paragraphs.forEach { paragraph ->
                    add(ReaderBlock(sectionIndex, paragraph, isHeading = false))
                }
            }
        }
    }
    val startIndex = initialBlockIndex.coerceIn(0, (blocks.size - 1).coerceAtLeast(0))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onTap = { onToggleOverlay() }) }
    ) {
        if (blocks.isEmpty()) return@Box

        if (prefs.readingStyle == ReadingStyle.Scroll) {
            val listState = rememberLazyListState(
                initialFirstVisibleItemIndex = startIndex,
                initialFirstVisibleItemScrollOffset = initialScrollOffset.coerceAtLeast(0)
            )
            LaunchedEffect(listState, blocks) {
                snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
                    .distinctUntilChanged()
                    .collect { (index, offset) ->
                        blocks.getOrNull(index)?.let { block -> onPositionChanged(index, block.sectionIndex, offset) }
                    }
            }
            LaunchedEffect(listState.isScrollInProgress) {
                if (listState.isScrollInProgress) onScroll()
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 64.dp,
                    bottom = 180.dp,
                    start = prefs.horizontalMargin.dp,
                    end = prefs.horizontalMargin.dp
                )
            ) {
                itemsIndexed(blocks) { _, block ->
                    ReaderTextBlock(block, prefs, onWordSelected)
                }
            }
        } else {
            val pagerState = rememberPagerState(
                initialPage = startIndex,
                pageCount = { blocks.size }
            )
            LaunchedEffect(pagerState, blocks) {
                snapshotFlow { pagerState.currentPage }
                    .distinctUntilChanged()
                    .collect { index ->
                        blocks.getOrNull(index)?.let { block -> onPositionChanged(index, block.sectionIndex, 0) }
                    }
            }
            LaunchedEffect(pagerState.isScrollInProgress) {
                if (pagerState.isScrollInProgress) onScroll()
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSize = PageSize.Fill,
                pageSpacing = 0.dp,
                contentPadding = PaddingValues(0.dp)
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = 64.dp,
                            start = prefs.horizontalMargin.dp,
                            end = prefs.horizontalMargin.dp,
                            bottom = 32.dp
                        )
                ) {
                    ReaderTextBlock(blocks[page], prefs, onWordSelected)
                }
            }
        }
    }
}

@Composable
private fun ReaderTextBlock(block: ReaderBlock, prefs: ReaderPreferences, onWordSelected: (String) -> Unit) {
    if (block.isHeading && block.text.isNotBlank()) {
        Text(
            text = block.text,
            fontFamily = prefs.font.family,
            fontWeight = FontWeight.Bold,
            fontSize = (prefs.fontSize + 8).sp,
            color = prefs.theme.text,
            modifier = Modifier.padding(bottom = 32.dp)
        )
    } else {
        var textLayout by remember(block.text) { mutableStateOf<TextLayoutResult?>(null) }
        SelectionContainer {
            Text(
                text = block.text,
                fontFamily = prefs.font.family,
                fontSize = prefs.fontSize.sp,
                lineHeight = (prefs.fontSize * prefs.lineSpacing).sp,
                color = prefs.theme.text,
                textAlign = if (prefs.textAlignment == TextAlignment.Left) TextAlign.Start else TextAlign.Justify,
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .pointerInput(block.text) {
                        detectTapGestures(onLongPress = { position: Offset ->
                            val offset = textLayout?.getOffsetForPosition(position) ?: return@detectTapGestures
                            block.text.wordAt(offset)?.let(onWordSelected)
                        })
                    },
                onTextLayout = { textLayout = it }
            )
        }
    }
}

private fun String.wordAt(offset: Int): String? {
    if (isEmpty()) return null
    var cursor = offset.coerceIn(0, lastIndex)
    if (!this[cursor].isLetterOrDigit() && cursor > 0 && this[cursor - 1].isLetterOrDigit()) cursor--
    if (!this[cursor].isLetterOrDigit()) return null
    var start = cursor
    var end = cursor + 1
    while (start > 0 && isWordCharacter(this[start - 1])) start--
    while (end < length && isWordCharacter(this[end])) end++
    return substring(start, end).trim('-', '\'', '’').takeIf { it.isNotBlank() }
}

private fun isWordCharacter(character: Char): Boolean =
    character.isLetterOrDigit() || character == '-' || character == '\'' || character == '’'
