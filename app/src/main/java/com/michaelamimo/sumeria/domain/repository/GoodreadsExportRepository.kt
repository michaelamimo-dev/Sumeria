package com.michaelamimo.sumeria.domain.repository

/** Produces a user-owned export that can be written to any Android document provider. */
interface GoodreadsExportRepository {
    suspend fun createLibraryCsv(): String
}
