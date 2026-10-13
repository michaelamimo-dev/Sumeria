package com.michaelamimo.sumeria.domain.repository

import com.michaelamimo.sumeria.domain.model.LibraryBook

/** Parses a user-selected EPUB/PDF and prepares its metadata for local storage. */
interface PublicationImporter {
    suspend fun import(sourceUri: String): LibraryBook
}

/** Enumerates supported publications within a user-selected SAF tree. */
interface BookDocumentScanner {
    suspend fun scan(treeUri: String): List<String>
}
