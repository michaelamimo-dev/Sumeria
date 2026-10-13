package com.michaelamimo.sumeria.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.michaelamimo.sumeria.domain.model.ReadingProgress

@Entity(tableName = "reading_progress", foreignKeys = [ForeignKey(
    entity = BookEntity::class, parentColumns = ["id"], childColumns = ["bookId"], onDelete = ForeignKey.CASCADE
)], indices = [Index("bookId")])
data class ReadingProgressEntity(
    @PrimaryKey val bookId: String,
    val locationJson: String?,
    val currentPage: Int?,
    val totalPages: Int?,
    val progression: Float,
    val updatedAtMillis: Long
)

internal fun ReadingProgressEntity.toDomain() = ReadingProgress(bookId, locationJson, currentPage, totalPages, progression, updatedAtMillis)
internal fun ReadingProgress.toEntity() = ReadingProgressEntity(bookId, locationJson, currentPage, totalPages, progression.coerceIn(0f, 1f), updatedAtMillis)
