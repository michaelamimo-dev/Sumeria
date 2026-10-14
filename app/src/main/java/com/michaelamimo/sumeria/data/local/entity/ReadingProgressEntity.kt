package com.michaelamimo.sumeria.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.michaelamimo.sumeria.domain.model.ReadingProgress

@Entity(
    tableName = "reading_progress",
    primaryKeys = ["ownerUserId", "bookId"],
    foreignKeys = [ForeignKey(
        entity = BookEntity::class,
        parentColumns = ["ownerUserId", "id"],
        childColumns = ["ownerUserId", "bookId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["ownerUserId", "bookId"])]
)
data class ReadingProgressEntity(
    val ownerUserId: String = "local",
    val bookId: String,
    val locationJson: String?,
    val currentPage: Int?,
    val totalPages: Int?,
    val progression: Float,
    val updatedAtMillis: Long
)

internal fun ReadingProgressEntity.toDomain() = ReadingProgress(bookId, locationJson, currentPage, totalPages, progression, updatedAtMillis)
internal fun ReadingProgress.toEntity(ownerUserId: String = "local") = ReadingProgressEntity(ownerUserId, bookId, locationJson, currentPage, totalPages, progression.coerceIn(0f, 1f), updatedAtMillis)
