package com.michaelamimo.sumeria.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.michaelamimo.sumeria.data.local.entity.ReadingProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingProgressDao {
    @Query("SELECT * FROM reading_progress WHERE ownerUserId = :ownerUserId ORDER BY updatedAtMillis DESC") fun observeAll(ownerUserId: String): Flow<List<ReadingProgressEntity>>
    @Query("SELECT * FROM reading_progress WHERE ownerUserId = :ownerUserId AND bookId = :bookId LIMIT 1") fun observe(ownerUserId: String, bookId: String): Flow<ReadingProgressEntity?>
    @Query("SELECT * FROM reading_progress WHERE ownerUserId = :ownerUserId AND bookId = :bookId LIMIT 1") suspend fun get(ownerUserId: String, bookId: String): ReadingProgressEntity?
    @Query("SELECT * FROM reading_progress WHERE ownerUserId = :ownerUserId") suspend fun getAll(ownerUserId: String): List<ReadingProgressEntity>
    @Upsert suspend fun upsert(progress: ReadingProgressEntity)
    @Query("DELETE FROM reading_progress WHERE ownerUserId = :ownerUserId AND bookId = :bookId") suspend fun delete(ownerUserId: String, bookId: String)
}
