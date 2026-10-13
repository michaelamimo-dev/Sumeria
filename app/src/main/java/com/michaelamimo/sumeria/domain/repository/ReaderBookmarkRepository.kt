package com.michaelamimo.sumeria.domain.repository

import com.michaelamimo.sumeria.domain.model.ReaderBookmark
import kotlinx.coroutines.flow.Flow

interface ReaderBookmarkRepository {
    fun observeForBook(bookId: String): Flow<List<ReaderBookmark>>
    suspend fun save(bookmark: ReaderBookmark)
    suspend fun delete(bookmarkId: String)
}
