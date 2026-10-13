package com.michaelamimo.sumeria.data.repository

import com.michaelamimo.sumeria.data.local.dao.BookDao
import com.michaelamimo.sumeria.data.local.entity.toDomain
import com.michaelamimo.sumeria.data.local.entity.toEntity
import com.michaelamimo.sumeria.domain.model.LibraryBook
import com.michaelamimo.sumeria.domain.repository.LibraryBookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomLibraryBookRepository @Inject constructor(private val dao: BookDao) : LibraryBookRepository {
    override fun observeAll(): Flow<List<LibraryBook>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }
    override fun observeById(bookId: String): Flow<LibraryBook?> = dao.observeById(bookId).map { it?.toDomain() }
    override suspend fun getById(bookId: String): LibraryBook? = dao.getById(bookId)?.toDomain()
    override suspend fun search(query: String): List<LibraryBook> = dao.search(query.trim()).map { it.toDomain() }
    override suspend fun upsert(book: LibraryBook) = dao.upsert(book.toEntity())
    override suspend fun delete(bookId: String) = dao.delete(bookId)
}
