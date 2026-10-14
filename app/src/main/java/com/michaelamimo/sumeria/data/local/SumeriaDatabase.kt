package com.michaelamimo.sumeria.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.michaelamimo.sumeria.data.local.dao.BookDao
import com.michaelamimo.sumeria.data.local.dao.ReaderBookmarkDao
import com.michaelamimo.sumeria.data.local.dao.ReadingProgressDao
import com.michaelamimo.sumeria.data.local.dao.ReadingSessionDao
import com.michaelamimo.sumeria.data.local.dao.UserPreferencesDao
import com.michaelamimo.sumeria.data.local.entity.BookEntity
import com.michaelamimo.sumeria.data.local.entity.ReaderBookmarkEntity
import com.michaelamimo.sumeria.data.local.entity.ReadingProgressEntity
import com.michaelamimo.sumeria.data.local.entity.ReadingSessionEntity
import com.michaelamimo.sumeria.data.local.entity.UserPreferencesEntity

@Database(entities = [
    BookEntity::class, ReadingProgressEntity::class, ReadingSessionEntity::class,
    ReaderBookmarkEntity::class, UserPreferencesEntity::class
], version = 4, exportSchema = true)
@TypeConverters(Converters::class)
abstract class SumeriaDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun readingSessionDao(): ReadingSessionDao
    abstract fun readerBookmarkDao(): ReaderBookmarkDao
    abstract fun userPreferencesDao(): UserPreferencesDao
}
