package com.michaelamimo.sumeria.domain.repository

import com.michaelamimo.sumeria.domain.model.ReadingProgress
import kotlinx.coroutines.flow.Flow

interface ReadingProgressRepository {
    fun observe(bookId: String): Flow<ReadingProgress?>
    suspend fun get(bookId: String): ReadingProgress?
    suspend fun save(progress: ReadingProgress)
    suspend fun delete(bookId: String)
}
