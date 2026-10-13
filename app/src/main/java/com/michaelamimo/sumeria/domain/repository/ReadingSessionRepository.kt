package com.michaelamimo.sumeria.domain.repository

import com.michaelamimo.sumeria.domain.model.ReadingSession
import kotlinx.coroutines.flow.Flow

interface ReadingSessionRepository {
    fun observeForBook(bookId: String): Flow<List<ReadingSession>>
    fun observeBetween(startInclusiveMillis: Long, endExclusiveMillis: Long): Flow<List<ReadingSession>>
    suspend fun save(session: ReadingSession)
    suspend fun delete(sessionId: String)
}
