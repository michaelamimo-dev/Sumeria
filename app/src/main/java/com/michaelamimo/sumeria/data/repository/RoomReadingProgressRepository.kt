package com.michaelamimo.sumeria.data.repository

import com.michaelamimo.sumeria.data.local.dao.ReadingProgressDao
import com.michaelamimo.sumeria.data.auth.CurrentProfileProvider
import com.michaelamimo.sumeria.data.local.entity.toDomain
import com.michaelamimo.sumeria.data.local.entity.toEntity
import com.michaelamimo.sumeria.domain.model.ReadingProgress
import com.michaelamimo.sumeria.domain.repository.ReadingProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomReadingProgressRepository @Inject constructor(
    private val dao: ReadingProgressDao,
    private val profileProvider: CurrentProfileProvider
) : ReadingProgressRepository {
    override fun observeAll(): Flow<List<ReadingProgress>> = dao.observeAll(profileProvider.currentProfileId()).map { rows -> rows.map { it.toDomain() } }
    override fun observe(bookId: String): Flow<ReadingProgress?> = dao.observe(profileProvider.currentProfileId(), bookId).map { it?.toDomain() }
    override suspend fun get(bookId: String): ReadingProgress? = dao.get(profileProvider.currentProfileId(), bookId)?.toDomain()
    override suspend fun save(progress: ReadingProgress) = dao.upsert(progress.toEntity(profileProvider.currentProfileId()))
    override suspend fun delete(bookId: String) = dao.delete(profileProvider.currentProfileId(), bookId)
}
