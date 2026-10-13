package com.michaelamimo.sumeria.domain.model

/** Location is stored as a reader-engine-neutral serialized locator for EPUB/PDF support. */
data class ReadingProgress(
    val bookId: String,
    val locationJson: String? = null,
    val currentPage: Int? = null,
    val totalPages: Int? = null,
    val progression: Float = 0f,
    val updatedAtMillis: Long
)
