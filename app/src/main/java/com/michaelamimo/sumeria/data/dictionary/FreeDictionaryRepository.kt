package com.michaelamimo.sumeria.data.dictionary

import android.net.Uri
import com.michaelamimo.sumeria.domain.model.DictionaryEntry
import com.michaelamimo.sumeria.domain.model.WordDefinition
import com.michaelamimo.sumeria.domain.repository.DictionaryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FreeDictionaryRepository @Inject constructor() : DictionaryRepository {
    private val cache = ConcurrentHashMap<String, DictionaryEntry>()

    override suspend fun lookup(word: String): DictionaryEntry {
        val normalized = word.trim().lowercase()
        if (!normalized.matches(Regex("[a-z][a-z'-]{0,63}"))) {
            throw IOException("Enter a single English word to look up.")
        }
        cache[normalized]?.let { return it }
        val result = withContext(Dispatchers.IO) {
            val connection = URL("https://api.dictionaryapi.dev/api/v2/entries/en/${Uri.encode(normalized)}")
                .openConnection() as HttpURLConnection
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    if (connection.responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                        throw IOException("No definition was found for ‘$normalized’.")
                    }
                    throw IOException("Dictionary lookup is unavailable. Check your connection and try again.")
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                parseEntry(JSONArray(body), normalized)
            } finally {
                connection.disconnect()
            }
        }
        cache[normalized] = result
        return result
    }

    private fun parseEntry(response: JSONArray, requestedWord: String): DictionaryEntry {
        val entry = (0 until response.length()).asSequence()
            .mapNotNull(response::optJSONObject)
            .firstOrNull()
            ?: throw IOException("No definition was found for ‘$requestedWord’.")
        val definitions = buildList {
            val meanings = entry.optJSONArray("meanings") ?: JSONArray()
            for (meaningIndex in 0 until meanings.length()) {
                val meaning = meanings.optJSONObject(meaningIndex) ?: continue
                val partOfSpeech = meaning.optString("partOfSpeech").takeIf(String::isNotBlank)
                val items = meaning.optJSONArray("definitions") ?: JSONArray()
                for (definitionIndex in 0 until items.length()) {
                    val definition = items.optJSONObject(definitionIndex) ?: continue
                    val text = definition.optString("definition").trim()
                    if (text.isNotEmpty()) {
                        add(
                            WordDefinition(
                                partOfSpeech = partOfSpeech,
                                meaning = text,
                                example = definition.optString("example").takeIf(String::isNotBlank)
                            )
                        )
                    }
                }
            }
        }.take(MAX_DEFINITIONS)
        if (definitions.isEmpty()) throw IOException("No definition was found for ‘$requestedWord’.")
        val phonetic = entry.optString("phonetic").takeIf(String::isNotBlank)
            ?: (0 until entry.optJSONArray("phonetics").orEmptyLength()).asSequence()
                .mapNotNull { entry.optJSONArray("phonetics")?.optJSONObject(it)?.optString("text") }
                .firstOrNull(String::isNotBlank)
        return DictionaryEntry(
            word = entry.optString("word").takeIf(String::isNotBlank) ?: requestedWord,
            phonetic = phonetic,
            definitions = definitions
        )
    }

    private fun JSONArray?.orEmptyLength(): Int = this?.length() ?: 0

    private companion object {
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 15_000
        const val MAX_DEFINITIONS = 8
    }
}
