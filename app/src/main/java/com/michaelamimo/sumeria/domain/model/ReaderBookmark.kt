package com.michaelamimo.sumeria.domain.model

data class ReaderBookmark(
    val id: String,
    val bookId: String,
    val locationJson: String,
    val chapterTitle: String? = null,
    val excerpt: String? = null,
    val createdAtMillis: Long
)
