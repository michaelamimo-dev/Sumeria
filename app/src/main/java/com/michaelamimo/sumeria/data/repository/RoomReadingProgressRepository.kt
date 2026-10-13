package com.michaelamimo.sumeria.data.repository

import com.michaelamimo.sumeria.data.local.dao.ReadingProgressDao
import com.michaelamimo.sumeria.data.local.entity.toDomain
import com.michaelamimo.sumeria.data.local.entity.toEntity
import com.michaelamimo.sumeria.domain.model.ReadingProgress
import com.michaelamimo.sumeria.domain.repository.ReadingProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomReadingProgressRepository @Inject constructor(private val dao: ReadingProgressDao) : ReadingProgressRepository {
    override fun observe(bookId: String): Flow<ReadingProgress?> = dao.observe(bookId).map { it?.toDomain() }
    override suspend fun get(bookId: String): ReadingProgress? = dao.get(bookId)?.toDomain()
    override suspend fun save(progress: ReadingProgress) = dao.upsert(progress.toEntity())
    override suspend fun delete(bookId: String) = dao.delete(bookId)
}
