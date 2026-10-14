package com.michaelamimo.sumeria.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.michaelamimo.sumeria.domain.model.ReaderBookmark

@Entity(
    tableName = "reader_bookmarks",
    primaryKeys = ["ownerUserId", "id"],
    foreignKeys = [ForeignKey(
        entity = BookEntity::class,
        parentColumns = ["ownerUserId", "id"],
        childColumns = ["ownerUserId", "bookId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["ownerUserId", "bookId"])]
)
data class ReaderBookmarkEntity(
    val ownerUserId: String = "local",
    val id: String,
    val bookId: String,
    val locationJson: String,
    val chapterTitle: String?,
    val excerpt: String?,
    val createdAtMillis: Long
)

internal fun ReaderBookmarkEntity.toDomain() = ReaderBookmark(id, bookId, locationJson, chapterTitle, excerpt, createdAtMillis)
internal fun ReaderBookmark.toEntity(ownerUserId: String = "local") = ReaderBookmarkEntity(ownerUserId, id, bookId, locationJson, chapterTitle, excerpt, createdAtMillis)
