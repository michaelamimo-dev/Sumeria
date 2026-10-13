package com.michaelamimo.sumeria.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.michaelamimo.sumeria.domain.model.BookFormat
import com.michaelamimo.sumeria.domain.model.LibraryBook

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val authorsJson: String,
    val description: String?,
    val format: BookFormat,
    val sourceUri: String?,
    val coverUri: String?,
    val genre: String?,
    val totalPages: Int?,
    val addedAtMillis: Long,
    val lastOpenedAtMillis: Long?
)

internal fun BookEntity.toDomain() = LibraryBook(
    id, title, AuthorsJson.decode(authorsJson), description, format, sourceUri, coverUri,
    genre, totalPages, addedAtMillis, lastOpenedAtMillis
)

internal fun LibraryBook.toEntity() = BookEntity(
    id, title, AuthorsJson.encode(authors), description, format, sourceUri, coverUri,
    genre, totalPages, addedAtMillis, lastOpenedAtMillis
)
