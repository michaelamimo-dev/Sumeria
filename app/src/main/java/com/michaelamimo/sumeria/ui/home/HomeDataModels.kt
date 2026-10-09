package com.michaelamimo.sumeria.ui.home

import java.time.LocalDate

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
    val coverUrl: String, // We'll use a placeholder URL for now
    val currentPage: Int,
    val totalPages: Int,
    val lastReadTimestamp: Long
) {
    val progressPercentage: Float
        get() = if (totalPages > 0) (currentPage.toFloat() / totalPages).coerceIn(0f, 1f) else 0f
}

data class LibraryCategory(
    val name: String,
    val books: List<Book>
)

// --- MOCK DATA REPOSITORY ---

object MockHomeRepository {

    fun getTodayGoal(): ReadingGoal {
        return ReadingGoal(targetMinutes = 45, currentMinutes = 32)
    }

    fun getWeeklyProgress(): List<DailyProgress> {
        val today = LocalDate.now()
        return listOf(
            DailyProgress(today.minusDays(6), "M", 45, 45), // Completed
            DailyProgress(today.minusDays(5), "T", 20, 45), // Partial
            DailyProgress(today.minusDays(4), "W", 0, 45),  // None
            DailyProgress(today.minusDays(3), "T", 60, 45), // Over-achieved
            DailyProgress(today.minusDays(2), "F", 15, 45), // Partial
            DailyProgress(today.minusDays(1), "S", 45, 45), // Completed
            DailyProgress(today, "S", 32, 45)               // Today
        )
    }

    fun getRecentlyReadBooks(): List<Book> {
        return listOf(
            Book(
                id = "1",
                title = "Atomic Habits",
                author = "James Clear",
                coverUrl = "https://images.unsplash.com/photo-1589829085413-56de8ae18c73?auto=format&fit=crop&q=80&w=200", // Placeholder generic book cover
                currentPage = 78,
                totalPages = 320,
                lastReadTimestamp = System.currentTimeMillis()
            ),
            Book(
                id = "2",
                title = "The Total Money Makeover",
                author = "Dave Ramsey",
                coverUrl = "https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&q=80&w=200",
                currentPage = 15,
                totalPages = 250,
                lastReadTimestamp = System.currentTimeMillis() - 86400000 // 1 day ago
            ),
            Book(
                id = "3",
                title = "Refactoring UI",
                author = "Adam Wathan",
                coverUrl = "https://images.unsplash.com/photo-1618666012174-83b441c0bc76?auto=format&fit=crop&q=80&w=200",
                currentPage = 200,
                totalPages = 250,
                lastReadTimestamp = System.currentTimeMillis() - 172800000 // 2 days ago
            )
        )
    }

    fun getExploreCategories(): List<LibraryCategory> {
        val mockBooks = getRecentlyReadBooks()
        return listOf(
            LibraryCategory("Self-Improvement", mockBooks),
            LibraryCategory("Design / Usability", mockBooks.shuffled()),
            LibraryCategory("Fiction & Literature", mockBooks.shuffled())
        )
    }
}