package com.michaelamimo.sumeria.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.michaelamimo.sumeria.data.local.entity.ReadingProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingProgressDao {
    @Query("SELECT * FROM reading_progress ORDER BY updatedAtMillis DESC") fun observeAll(): Flow<List<ReadingProgressEntity>>
    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId LIMIT 1") fun observe(bookId: String): Flow<ReadingProgressEntity?>
    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId LIMIT 1") suspend fun get(bookId: String): ReadingProgressEntity?
    @Upsert suspend fun upsert(progress: ReadingProgressEntity)
    @Query("DELETE FROM reading_progress WHERE bookId = :bookId") suspend fun delete(bookId: String)
}
