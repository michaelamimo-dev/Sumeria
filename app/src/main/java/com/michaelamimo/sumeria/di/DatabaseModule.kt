package com.michaelamimo.sumeria.di

import android.content.Context
import androidx.room.Room
import com.michaelamimo.sumeria.data.local.SumeriaDatabase
import com.michaelamimo.sumeria.data.local.MIGRATION_1_2
import com.michaelamimo.sumeria.data.local.MIGRATION_2_3
import com.michaelamimo.sumeria.data.local.MIGRATION_3_4
import com.michaelamimo.sumeria.data.local.dao.BookDao
import com.michaelamimo.sumeria.data.local.dao.ReaderBookmarkDao
import com.michaelamimo.sumeria.data.local.dao.ReadingProgressDao
import com.michaelamimo.sumeria.data.local.dao.ReadingSessionDao
import com.michaelamimo.sumeria.data.local.dao.UserPreferencesDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SumeriaDatabase =
        Room.databaseBuilder(context, SumeriaDatabase::class.java, "sumeria.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()

    @Provides fun provideBookDao(database: SumeriaDatabase): BookDao = database.bookDao()
    @Provides fun provideReadingProgressDao(database: SumeriaDatabase): ReadingProgressDao = database.readingProgressDao()
    @Provides fun provideReadingSessionDao(database: SumeriaDatabase): ReadingSessionDao = database.readingSessionDao()
    @Provides fun provideReaderBookmarkDao(database: SumeriaDatabase): ReaderBookmarkDao = database.readerBookmarkDao()
    @Provides fun provideUserPreferencesDao(database: SumeriaDatabase): UserPreferencesDao = database.userPreferencesDao()
}
