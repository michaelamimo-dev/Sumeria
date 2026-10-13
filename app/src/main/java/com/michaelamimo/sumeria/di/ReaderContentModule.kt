package com.michaelamimo.sumeria.di

import com.michaelamimo.sumeria.data.reader.PublicationReaderContentRepository
import com.michaelamimo.sumeria.domain.repository.ReaderContentRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ReaderContentModule {
    @Binds
    abstract fun bindReaderContentRepository(
        implementation: PublicationReaderContentRepository
    ): ReaderContentRepository
}
