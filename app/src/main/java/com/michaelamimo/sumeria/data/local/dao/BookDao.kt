package com.michaelamimo.sumeria.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.michaelamimo.sumeria.data.local.entity.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY COALESCE(lastOpenedAtMillis, addedAtMillis) DESC, title COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<BookEntity>>
    @Query("SELECT * FROM books WHERE id = :bookId LIMIT 1")
    fun observeById(bookId: String): Flow<BookEntity?>
    @Query("SELECT * FROM books WHERE id = :bookId LIMIT 1")
    suspend fun getById(bookId: String): BookEntity?
    @Query("SELECT * FROM books WHERE title LIKE '%' || :query || '%' OR authorsJson LIKE '%' || :query || '%' ORDER BY title COLLATE NOCASE ASC")
    suspend fun search(query: String): List<BookEntity>
    @Upsert
    suspend fun upsert(book: BookEntity)
    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun delete(bookId: String)
}
