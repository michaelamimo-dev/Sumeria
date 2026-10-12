package com.michaelamimo.sumeria.ui.reader

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReadingView(
    prefs: ReaderPreferences,
    onToggleOverlay: () -> Unit,
    onScroll: () -> Unit
) {
    val chapterTitle = "Chapter One: The Fundamentals"
    val paragraphs = listOf(
        "Design is not just what it looks like and feels like. Design is how it works. When we observe the physical world, affordances provide strong clues to the operations of things. Plates are for pushing. Knobs are for turning. Slots are for inserting things into. But in the digital realm, we must rely on signifiers.",
        "Signifiers communicate where the action should take place. We need them because, unlike the physical world, the digital screen is flat. Without a visual cue—a shadow, a border, a specific color—a user cannot know what is interactive and what is merely decorative.",
        "As we shift from physical to digital, the principles of human-centered design remain our anchor. We must build systems that adapt to the user, rather than forcing the user to adapt to the system. This means understanding psychology, cognition, and the beautiful imperfections of human memory.",
        "Consider the reading experience itself. For centuries, ink on paper was the pinnacle of typographic evolution. Today, we have the power to change the font, the contrast, and the spacing in an instant. Yet, the goal remains identical: to immerse the reader in the narrative completely, removing the medium from their conscious awareness.",
        "When typography is invisible, it is working. The moment a reader notices the font, they have stopped reading the text. Our duty as designers is to craft that invisible bridge between the author's mind and the reader's imagination."
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onTap = { onToggleOverlay() }) }
    ) {
        if (prefs.readingStyle == ReadingStyle.Scroll) {
            // SCROLL MODE
            val listState = rememberLazyListState()
            LaunchedEffect(listState.isScrollInProgress) {
                if (listState.isScrollInProgress) onScroll()
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 64.dp, // Natural top padding, no overlap issues
                    bottom = 180.dp,
                    start = prefs.horizontalMargin.dp,
                    end = prefs.horizontalMargin.dp
                )
            ) {
                item {
                    Text(
                        text = chapterTitle,
                        fontFamily = prefs.font.family,
                        fontWeight = FontWeight.Bold,
                        fontSize = (prefs.fontSize + 8).sp,
                        color = prefs.theme.text,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )
                }
                items(paragraphs.size) { index ->
                    SelectionContainer {
                        Text(
                            text = paragraphs[index],
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
        } else {
            // SWIPE MODE
            val pagerState = rememberPagerState(pageCount = { paragraphs.size + 1 })
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
                    if (page == 0) {
                        Text(
                            text = chapterTitle,
                            fontFamily = prefs.font.family,
                            fontWeight = FontWeight.Bold,
                            fontSize = (prefs.fontSize + 8).sp,
                            color = prefs.theme.text,
                            modifier = Modifier.padding(bottom = 32.dp)
                        )
                    } else {
                        SelectionContainer {
                            Text(
                                text = paragraphs[page - 1],
                                fontFamily = prefs.font.family,
                                fontSize = prefs.fontSize.sp,
                                lineHeight = (prefs.fontSize * prefs.lineSpacing).sp,
                                color = prefs.theme.text,
                                textAlign = if (prefs.textAlignment == TextAlignment.Left) TextAlign.Start else TextAlign.Justify
                            )
                        }
                    }
                }
            }
        }
    }
}
