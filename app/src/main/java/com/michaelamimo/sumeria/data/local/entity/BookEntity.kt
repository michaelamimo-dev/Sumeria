package com.michaelamimo.sumeria.data.local.entity

import androidx.room.Entity
import androidx.room.ColumnInfo
import com.michaelamimo.sumeria.domain.model.BookFormat
import com.michaelamimo.sumeria.domain.model.LibraryBook

@Entity(tableName = "books", primaryKeys = ["ownerUserId", "id"])
data class BookEntity(
    @ColumnInfo(defaultValue = "'local'") val ownerUserId: String = "local",
    val id: String,
    val title: String,
    val authorsJson: String,
    val description: String?,
    val format: BookFormat,
    val sourceUri: String?,
    val coverUri: String?,
    val genre: String?,
    val totalPages: Int?,
    val addedAtMillis: Long,
    val lastOpenedAtMillis: Long?,
    @androidx.room.ColumnInfo(defaultValue = "0") val fileSizeBytes: Long = 0L
)

internal fun BookEntity.toDomain() = LibraryBook(
    id, title, AuthorsJson.decode(authorsJson), description, format, sourceUri, coverUri,
    genre, totalPages, addedAtMillis, lastOpenedAtMillis, fileSizeBytes.takeIf { it > 0L }
)

internal fun LibraryBook.toEntity(ownerUserId: String = "local") = BookEntity(
    ownerUserId, id, title, AuthorsJson.encode(authors), description, format, sourceUri, coverUri,
    genre, totalPages, addedAtMillis, lastOpenedAtMillis, fileSizeBytes ?: 0L
)
