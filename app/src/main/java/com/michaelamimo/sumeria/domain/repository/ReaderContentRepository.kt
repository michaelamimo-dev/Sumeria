package com.michaelamimo.sumeria.domain.repository

import com.michaelamimo.sumeria.domain.model.LibraryBook
import com.michaelamimo.sumeria.domain.model.ReaderDocument

interface ReaderContentRepository {
    suspend fun load(book: LibraryBook): ReaderDocument
}
