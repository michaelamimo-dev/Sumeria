package com.michaelamimo.sumeria.di

import com.michaelamimo.sumeria.data.repository.RoomLibraryBookRepository
import com.michaelamimo.sumeria.data.repository.RoomReaderBookmarkRepository
import com.michaelamimo.sumeria.data.repository.RoomReadingProgressRepository
import com.michaelamimo.sumeria.data.repository.RoomReadingSessionRepository
import com.michaelamimo.sumeria.data.repository.RoomUserPreferencesRepository
import com.michaelamimo.sumeria.domain.repository.LibraryBookRepository
import com.michaelamimo.sumeria.domain.repository.ReaderBookmarkRepository
import com.michaelamimo.sumeria.domain.repository.ReadingProgressRepository
import com.michaelamimo.sumeria.domain.repository.ReadingSessionRepository
import com.michaelamimo.sumeria.domain.repository.UserPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindLibraryBookRepository(implementation: RoomLibraryBookRepository): LibraryBookRepository
    @Binds abstract fun bindReadingProgressRepository(implementation: RoomReadingProgressRepository): ReadingProgressRepository
    @Binds abstract fun bindReadingSessionRepository(implementation: RoomReadingSessionRepository): ReadingSessionRepository
    @Binds abstract fun bindReaderBookmarkRepository(implementation: RoomReaderBookmarkRepository): ReaderBookmarkRepository
    @Binds abstract fun bindUserPreferencesRepository(implementation: RoomUserPreferencesRepository): UserPreferencesRepository
}
