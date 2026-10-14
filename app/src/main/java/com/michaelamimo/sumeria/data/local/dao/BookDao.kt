package com.michaelamimo.sumeria.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.michaelamimo.sumeria.data.local.entity.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books WHERE ownerUserId = :ownerUserId ORDER BY COALESCE(lastOpenedAtMillis, addedAtMillis) DESC, title COLLATE NOCASE ASC")
    fun observeAll(ownerUserId: String): Flow<List<BookEntity>>
    @Query("SELECT * FROM books WHERE ownerUserId = :ownerUserId AND id = :bookId LIMIT 1")
    fun observeById(ownerUserId: String, bookId: String): Flow<BookEntity?>
    @Query("SELECT * FROM books WHERE ownerUserId = :ownerUserId AND id = :bookId LIMIT 1")
    suspend fun getById(ownerUserId: String, bookId: String): BookEntity?
    @Query("SELECT * FROM books WHERE ownerUserId = :ownerUserId AND title = :title COLLATE NOCASE AND authorsJson = :authorsJson AND format = :format LIMIT 1")
    suspend fun findByMetadata(ownerUserId: String, title: String, authorsJson: String, format: String): BookEntity?
    @Query("SELECT * FROM books WHERE ownerUserId = :ownerUserId AND (title LIKE '%' || :query || '%' OR authorsJson LIKE '%' || :query || '%') ORDER BY title COLLATE NOCASE ASC")
    suspend fun search(ownerUserId: String, query: String): List<BookEntity>
    @Query("SELECT * FROM books WHERE ownerUserId = :ownerUserId")
    suspend fun getAll(ownerUserId: String): List<BookEntity>
    @Upsert
    suspend fun upsert(book: BookEntity)
    @Query("DELETE FROM books WHERE ownerUserId = :ownerUserId AND id = :bookId")
    suspend fun delete(ownerUserId: String, bookId: String)
}
