package com.michaelamimo.sumeria.data.integration

import com.michaelamimo.sumeria.domain.repository.GoodreadsExportRepository
import com.michaelamimo.sumeria.domain.repository.LibraryBookRepository
import com.michaelamimo.sumeria.domain.repository.ReadingProgressRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomGoodreadsExportRepository @Inject constructor(
    private val books: LibraryBookRepository,
    private val progress: ReadingProgressRepository
) : GoodreadsExportRepository {
    override suspend fun createLibraryCsv(): String {
        val readingProgress = progress.observeAll().first().associateBy { it.bookId }
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.US)
        return buildString {
            appendLine(GOODREADS_HEADERS.joinToString(",") { it.csvCell() })
            books.observeAll().first().forEach { book ->
                val shelf = when {
                    (readingProgress[book.id]?.progression ?: 0f) >= 1f -> "read"
                    (readingProgress[book.id]?.progression ?: 0f) > 0f -> "currently-reading"
                    else -> "to-read"
                }
                val row = List(GOODREADS_HEADERS.size) { "" }.toMutableList()
                row[1] = book.title
                row[2] = book.authors.firstOrNull().orEmpty()
                row[4] = book.authors.drop(1).joinToString(", ")
                row[11] = book.totalPages?.toString().orEmpty()
                row[15] = dateFormat.format(Date(book.addedAtMillis))
                row[16] = shelf
                row[18] = shelf
                appendLine(row.joinToString(",") { it.csvCell() })
            }
        }
    }

    private fun String.csvCell(): String = "\"${replace("\"", "\"\"")}\""

    private companion object {
        val GOODREADS_HEADERS = listOf(
            "Book Id", "Title", "Author", "Author l-f", "Additional Authors", "ISBN", "ISBN13",
            "My Rating", "Average Rating", "Publisher", "Binding", "Number of Pages", "Year Published",
            "Original Publication Year", "Date Read", "Date Added", "Bookshelves",
            "Bookshelves with positions", "Exclusive Shelf", "My Review", "Spoiler", "Private Notes",
            "Read Count", "Owned Copies"
        )
    }
}
