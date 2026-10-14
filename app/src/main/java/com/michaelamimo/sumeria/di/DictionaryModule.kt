package com.michaelamimo.sumeria.di

import com.michaelamimo.sumeria.data.dictionary.FreeDictionaryRepository
import com.michaelamimo.sumeria.domain.repository.DictionaryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DictionaryModule {
    @Binds
    abstract fun bindDictionaryRepository(implementation: FreeDictionaryRepository): DictionaryRepository
}
