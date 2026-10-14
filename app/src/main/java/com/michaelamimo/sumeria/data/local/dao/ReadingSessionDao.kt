package com.michaelamimo.sumeria.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.michaelamimo.sumeria.data.local.entity.ReadingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingSessionDao {
    @Query("SELECT * FROM reading_sessions WHERE ownerUserId = :ownerUserId AND bookId = :bookId ORDER BY startedAtMillis DESC")
    fun observeForBook(ownerUserId: String, bookId: String): Flow<List<ReadingSessionEntity>>
    @Query("SELECT * FROM reading_sessions WHERE ownerUserId = :ownerUserId AND startedAtMillis >= :startInclusiveMillis AND startedAtMillis < :endExclusiveMillis ORDER BY startedAtMillis ASC")
    fun observeBetween(ownerUserId: String, startInclusiveMillis: Long, endExclusiveMillis: Long): Flow<List<ReadingSessionEntity>>
    @Query("SELECT * FROM reading_sessions WHERE ownerUserId = :ownerUserId")
    fun observeAll(ownerUserId: String): Flow<List<ReadingSessionEntity>>
    @Query("SELECT * FROM reading_sessions WHERE ownerUserId = :ownerUserId")
    suspend fun getAll(ownerUserId: String): List<ReadingSessionEntity>
    @Upsert
    suspend fun upsert(session: ReadingSessionEntity)
    @Query("DELETE FROM reading_sessions WHERE ownerUserId = :ownerUserId AND id = :sessionId")
    suspend fun delete(ownerUserId: String, sessionId: String)
}
