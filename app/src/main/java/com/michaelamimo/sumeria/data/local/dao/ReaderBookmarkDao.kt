package com.michaelamimo.sumeria.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.michaelamimo.sumeria.data.local.entity.ReaderBookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReaderBookmarkDao {
    @Query("SELECT * FROM reader_bookmarks WHERE bookId = :bookId ORDER BY createdAtMillis DESC")
    fun observeForBook(bookId: String): Flow<List<ReaderBookmarkEntity>>
    @Upsert
    suspend fun upsert(bookmark: ReaderBookmarkEntity)
    @Query("DELETE FROM reader_bookmarks WHERE id = :bookmarkId")
    suspend fun delete(bookmarkId: String)
}
