package com.michaelamimo.sumeria.ui.home

import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import com.michaelamimo.sumeria.domain.model.LibraryBook
import com.michaelamimo.sumeria.domain.model.ReadingProgress
import com.michaelamimo.sumeria.domain.model.ReadingSession

// --- DATA CLASSES ---

data class ReadingGoal(
    val targetMinutes: Int,
    val currentMinutes: Int,
    val isCompleted: Boolean = currentMinutes >= targetMinutes
)

data class DailyProgress(
    val date: LocalDate,
    val dayOfWeek: String, // e.g., "M", "T", "W"
    val minutesRead: Int,
    val targetMinutes: Int
) {
    val percentage: Float
        get() = if (targetMinutes > 0) (minutesRead.toFloat() / targetMinutes).coerceIn(0f, 1f) else 0f
}

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val coverUrl: String?,
    val currentPage: Int,
    val totalPages: Int,
    val lastReadTimestamp: Long
) {
    val progressPercentage: Float
        get() = if (totalPages > 0) (currentPage.toFloat() / totalPages).coerceIn(0f, 1f) else 0f
}

internal fun LibraryBook.toHomeBook(progress: ReadingProgress?): Book {
    // EPUBs are reflowable and have no fixed page count; show a stable reading estimate.
    val pages = progress?.totalPages ?: totalPages ?: 100
    val page = progress?.currentPage ?: (progress?.progression?.coerceIn(0f, 1f)?.times(pages)?.toInt() ?: 0)
    return Book(
        id = id,
        title = title,
        author = authors.joinToString().ifBlank { "Unknown author" },
        coverUrl = coverUri,
        currentPage = page.coerceIn(0, pages),
        totalPages = pages,
        lastReadTimestamp = lastOpenedAtMillis ?: addedAtMillis
    )
}

internal fun ReadingSession.startedAtLocalDate(): LocalDate =
    Instant.ofEpochMilli(startedAtMillis).atZone(ZoneId.systemDefault()).toLocalDate()
