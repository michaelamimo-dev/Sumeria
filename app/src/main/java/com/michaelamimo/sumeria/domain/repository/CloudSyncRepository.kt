package com.michaelamimo.sumeria.domain.repository

interface CloudSyncRepository {
    suspend fun syncUser(userId: String)
    suspend fun uploadLocalSnapshot(userId: String)
    suspend fun deleteBook(userId: String, bookId: String)
    suspend fun deleteBookmark(userId: String, bookmarkId: String)
}
