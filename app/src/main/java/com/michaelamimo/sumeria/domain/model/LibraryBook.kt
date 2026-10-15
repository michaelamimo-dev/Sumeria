package com.michaelamimo.sumeria.domain.model

enum class BookFormat { EPUB, PDF }

/** App-facing book data. Storage-specific Room types stay in the data layer. */
data class LibraryBook(
    val id: String,
    val title: String,
    val authors: List<String> = emptyList(),
    val description: String? = null,
    val format: BookFormat,
    val sourceUri: String? = null,
    val coverUri: String? = null,
    val genre: String? = null,
    val totalPages: Int? = null,
    val addedAtMillis: Long,
    val lastOpenedAtMillis: Long? = null,
    val fileSizeBytes: Long? = null
)
