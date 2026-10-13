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
import androidx.compose.ui.input.pointer.pointerInput
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
    onPositionChanged: (blockIndex: Int, sectionIndex: Int, scrollOffset: Int) -> Unit
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
                    ReaderTextBlock(block, prefs)
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
                    ReaderTextBlock(blocks[page], prefs)
                }
            }
        }
    }
}

@Composable
private fun ReaderTextBlock(block: ReaderBlock, prefs: ReaderPreferences) {
    if (block.isHeading) {
        Text(
            text = block.text,
            fontFamily = prefs.font.family,
            fontWeight = FontWeight.Bold,
            fontSize = (prefs.fontSize + 8).sp,
            color = prefs.theme.text,
            modifier = Modifier.padding(bottom = 32.dp)
        )
    } else {
        SelectionContainer {
            Text(
                text = block.text,
                fontFamily = prefs.font.family,
                fontSize = prefs.fontSize.sp,
                lineHeight = (prefs.fontSize * prefs.lineSpacing).sp,
                color = prefs.theme.text,
                textAlign = if (prefs.textAlignment == TextAlignment.Left) TextAlign.Start else TextAlign.Justify,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}
