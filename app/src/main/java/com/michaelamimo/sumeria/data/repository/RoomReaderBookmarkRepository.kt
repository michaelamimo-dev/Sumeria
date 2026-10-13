package com.michaelamimo.sumeria.data.repository

import com.michaelamimo.sumeria.data.local.dao.ReaderBookmarkDao
import com.michaelamimo.sumeria.data.local.entity.toDomain
import com.michaelamimo.sumeria.data.local.entity.toEntity
import com.michaelamimo.sumeria.domain.model.ReaderBookmark
import com.michaelamimo.sumeria.domain.repository.ReaderBookmarkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomReaderBookmarkRepository @Inject constructor(private val dao: ReaderBookmarkDao) : ReaderBookmarkRepository {
    override fun observeForBook(bookId: String): Flow<List<ReaderBookmark>> = dao.observeForBook(bookId).map { rows -> rows.map { it.toDomain() } }
    override suspend fun save(bookmark: ReaderBookmark) = dao.upsert(bookmark.toEntity())
    override suspend fun delete(bookmarkId: String) = dao.delete(bookmarkId)
}
