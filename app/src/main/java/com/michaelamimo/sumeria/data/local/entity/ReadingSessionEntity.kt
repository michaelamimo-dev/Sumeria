package com.michaelamimo.sumeria.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.michaelamimo.sumeria.domain.model.ReadingSession
import com.michaelamimo.sumeria.domain.model.SessionMode

@Entity(
    tableName = "reading_sessions",
    primaryKeys = ["ownerUserId", "id"],
    foreignKeys = [ForeignKey(
        entity = BookEntity::class,
        parentColumns = ["ownerUserId", "id"],
        childColumns = ["ownerUserId", "bookId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["ownerUserId", "bookId"]), Index("startedAtMillis")]
)
data class ReadingSessionEntity(
    val ownerUserId: String = "local",
    val id: String,
    val bookId: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long?,
    val durationMillis: Long,
    val pagesRead: Int,
    val wordsRead: Int,
    val mode: SessionMode
)

internal fun ReadingSessionEntity.toDomain() = ReadingSession(id, bookId, startedAtMillis, endedAtMillis, durationMillis, pagesRead, wordsRead, mode)
internal fun ReadingSession.toEntity(ownerUserId: String = "local") = ReadingSessionEntity(ownerUserId, id, bookId, startedAtMillis, endedAtMillis, durationMillis.coerceAtLeast(0), pagesRead.coerceAtLeast(0), wordsRead.coerceAtLeast(0), mode)
