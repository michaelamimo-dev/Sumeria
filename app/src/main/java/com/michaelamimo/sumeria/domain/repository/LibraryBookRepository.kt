package com.michaelamimo.sumeria.domain.repository

import com.michaelamimo.sumeria.domain.model.LibraryBook
import kotlinx.coroutines.flow.Flow

interface LibraryBookRepository {
    fun observeAll(): Flow<List<LibraryBook>>
    fun observeById(bookId: String): Flow<LibraryBook?>
    suspend fun getById(bookId: String): LibraryBook?
    suspend fun search(query: String): List<LibraryBook>
    suspend fun upsert(book: LibraryBook)
    suspend fun delete(bookId: String)
}
