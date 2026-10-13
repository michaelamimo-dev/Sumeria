package com.michaelamimo.sumeria.domain.model

/** Reflowable text extracted from a publication for the Compose reader. */
data class ReaderDocument(
    val sections: List<ReaderSection>,
    val totalPages: Int? = null
)

data class ReaderSection(
    val title: String,
    val paragraphs: List<String>
)
