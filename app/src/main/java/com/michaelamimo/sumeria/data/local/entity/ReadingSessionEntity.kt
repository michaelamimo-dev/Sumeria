package com.michaelamimo.sumeria.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.michaelamimo.sumeria.domain.model.ReadingSession
import com.michaelamimo.sumeria.domain.model.SessionMode

@Entity(tableName = "reading_sessions", foreignKeys = [ForeignKey(
    entity = BookEntity::class, parentColumns = ["id"], childColumns = ["bookId"], onDelete = ForeignKey.CASCADE
)], indices = [Index("bookId"), Index("startedAtMillis")])
data class ReadingSessionEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long?,
    val durationMillis: Long,
    val pagesRead: Int,
    val wordsRead: Int,
    val mode: SessionMode
)

internal fun ReadingSessionEntity.toDomain() = ReadingSession(id, bookId, startedAtMillis, endedAtMillis, durationMillis, pagesRead, wordsRead, mode)
internal fun ReadingSession.toEntity() = ReadingSessionEntity(id, bookId, startedAtMillis, endedAtMillis, durationMillis.coerceAtLeast(0), pagesRead.coerceAtLeast(0), wordsRead.coerceAtLeast(0), mode)
