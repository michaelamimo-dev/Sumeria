package com.michaelamimo.sumeria.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.michaelamimo.sumeria.domain.model.ReaderBookmark

@Entity(tableName = "reader_bookmarks", foreignKeys = [ForeignKey(
    entity = BookEntity::class, parentColumns = ["id"], childColumns = ["bookId"], onDelete = ForeignKey.CASCADE
)], indices = [Index("bookId")])
data class ReaderBookmarkEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val locationJson: String,
    val chapterTitle: String?,
    val excerpt: String?,
    val createdAtMillis: Long
)

internal fun ReaderBookmarkEntity.toDomain() = ReaderBookmark(id, bookId, locationJson, chapterTitle, excerpt, createdAtMillis)
internal fun ReaderBookmark.toEntity() = ReaderBookmarkEntity(id, bookId, locationJson, chapterTitle, excerpt, createdAtMillis)
