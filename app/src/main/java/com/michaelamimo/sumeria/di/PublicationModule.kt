package com.michaelamimo.sumeria.di

import com.michaelamimo.sumeria.data.importer.ReadiumPublicationImporter
import com.michaelamimo.sumeria.data.importer.SafBookDocumentScanner
import com.michaelamimo.sumeria.domain.repository.BookDocumentScanner
import com.michaelamimo.sumeria.domain.repository.PublicationImporter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class PublicationModule {
    @Binds abstract fun bindPublicationImporter(implementation: ReadiumPublicationImporter): PublicationImporter
    @Binds abstract fun bindBookDocumentScanner(implementation: SafBookDocumentScanner): BookDocumentScanner
}
