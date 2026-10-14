package com.michaelamimo.sumeria.data.repository

import com.michaelamimo.sumeria.data.local.dao.ReadingSessionDao
import com.michaelamimo.sumeria.data.auth.CurrentProfileProvider
import com.michaelamimo.sumeria.data.local.entity.toDomain
import com.michaelamimo.sumeria.data.local.entity.toEntity
import com.michaelamimo.sumeria.domain.model.ReadingSession
import com.michaelamimo.sumeria.domain.repository.ReadingSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomReadingSessionRepository @Inject constructor(
    private val dao: ReadingSessionDao,
    private val profileProvider: CurrentProfileProvider
) : ReadingSessionRepository {
    override fun observeForBook(bookId: String): Flow<List<ReadingSession>> = dao.observeForBook(profileProvider.currentProfileId(), bookId).map { rows -> rows.map { it.toDomain() } }
    override fun observeBetween(startInclusiveMillis: Long, endExclusiveMillis: Long): Flow<List<ReadingSession>> =
        dao.observeBetween(profileProvider.currentProfileId(), startInclusiveMillis, endExclusiveMillis).map { rows -> rows.map { it.toDomain() } }
    override suspend fun save(session: ReadingSession) = dao.upsert(session.toEntity(profileProvider.currentProfileId()))
    override suspend fun delete(sessionId: String) = dao.delete(profileProvider.currentProfileId(), sessionId)
}
