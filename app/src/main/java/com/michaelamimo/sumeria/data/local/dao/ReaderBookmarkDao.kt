package com.michaelamimo.sumeria.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.michaelamimo.sumeria.data.local.entity.ReaderBookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReaderBookmarkDao {
    @Query("SELECT * FROM reader_bookmarks WHERE ownerUserId = :ownerUserId AND bookId = :bookId ORDER BY createdAtMillis DESC")
    fun observeForBook(ownerUserId: String, bookId: String): Flow<List<ReaderBookmarkEntity>>
    @Query("SELECT * FROM reader_bookmarks WHERE ownerUserId = :ownerUserId")
    fun observeAll(ownerUserId: String): Flow<List<ReaderBookmarkEntity>>
    @Query("SELECT * FROM reader_bookmarks WHERE ownerUserId = :ownerUserId")
    suspend fun getAll(ownerUserId: String): List<ReaderBookmarkEntity>
    @Upsert
    suspend fun upsert(bookmark: ReaderBookmarkEntity)
    @Query("DELETE FROM reader_bookmarks WHERE ownerUserId = :ownerUserId AND id = :bookmarkId")
    suspend fun delete(ownerUserId: String, bookmarkId: String)
}
