package com.michaelamimo.sumeria.domain.repository

import com.michaelamimo.sumeria.domain.model.DictionaryEntry

interface DictionaryRepository {
    suspend fun lookup(word: String): DictionaryEntry
}
