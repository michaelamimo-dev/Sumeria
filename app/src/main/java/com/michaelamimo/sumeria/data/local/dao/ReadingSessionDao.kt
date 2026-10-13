package com.michaelamimo.sumeria.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.michaelamimo.sumeria.data.local.entity.ReadingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingSessionDao {
    @Query("SELECT * FROM reading_sessions WHERE bookId = :bookId ORDER BY startedAtMillis DESC")
    fun observeForBook(bookId: String): Flow<List<ReadingSessionEntity>>
    @Query("SELECT * FROM reading_sessions WHERE startedAtMillis >= :startInclusiveMillis AND startedAtMillis < :endExclusiveMillis ORDER BY startedAtMillis ASC")
    fun observeBetween(startInclusiveMillis: Long, endExclusiveMillis: Long): Flow<List<ReadingSessionEntity>>
    @Upsert
    suspend fun upsert(session: ReadingSessionEntity)
    @Query("DELETE FROM reading_sessions WHERE id = :sessionId")
    suspend fun delete(sessionId: String)
}
