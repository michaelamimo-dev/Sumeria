package com.michaelamimo.sumeria.domain.model

data class DictionaryEntry(
    val word: String,
    val phonetic: String?,
    val definitions: List<WordDefinition>
)

data class WordDefinition(
    val partOfSpeech: String?,
    val meaning: String,
    val example: String?
)
