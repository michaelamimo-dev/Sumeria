package com.michaelamimo.sumeria.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michaelamimo.sumeria.domain.model.LibraryBook
import com.michaelamimo.sumeria.domain.repository.BookDocumentScanner
import com.michaelamimo.sumeria.domain.repository.LibraryBookRepository
import com.michaelamimo.sumeria.domain.repository.PublicationImporter
import com.michaelamimo.sumeria.domain.repository.UserPreferencesRepository
import com.michaelamimo.sumeria.domain.repository.ReadingProgressRepository
import org.json.JSONArray
import org.json.JSONObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val books: List<LibraryBook> = emptyList(),
    val shelves: List<LibraryShelf> = emptyList(),
    val membership: Map<String, Set<String>> = emptyMap(),
    val completedBookIds: Set<String> = emptySet(),
    val sortOrder: LibrarySortOrder = LibrarySortOrder.DateAdded,
    val isImporting: Boolean = false,
    val message: String? = null
)

data class LibraryShelf(val name: String, val summary: String, val theme: String)
enum class LibrarySortOrder(val label: String) { Alphabetical("Alphabetical"), DateAdded("Date added"), FileSize("File size"), Author("Author") }

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val bookRepository: LibraryBookRepository,
    private val publicationImporter: PublicationImporter,
    private val bookDocumentScanner: BookDocumentScanner,
    private val preferencesRepository: UserPreferencesRepository,
    private val progressRepository: ReadingProgressRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            bookRepository.observeAll().collectLatest { books ->
                _uiState.update { it.copy(books = books) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.observe().collectLatest { preferences ->
                _uiState.update {
                    it.copy(
                        shelves = decodeShelves(preferences.libraryShelvesJson),
                        membership = decodeMembership(preferences.libraryMembershipJson),
                        sortOrder = runCatching { LibrarySortOrder.valueOf(preferences.librarySortOrder) }
                            .getOrDefault(LibrarySortOrder.DateAdded)
                    )
                }
            }
        }
        viewModelScope.launch {
            progressRepository.observeAll().collectLatest { progress ->
                _uiState.update { it.copy(completedBookIds = progress.filter { it.progression >= 0.999f }.map { it.bookId }.toSet()) }
            }
        }
    }

    fun importFile(uri: Uri) {
        if (_uiState.value.isImporting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, message = null) }
            try {
                val book = publicationImporter.import(uri.toString())
                bookRepository.upsert(book)
                _uiState.update { it.copy(isImporting = false, message = "Added ‘${book.title}’ to your library.") }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(isImporting = false, message = error.message ?: "This file could not be added.")
                }
            }
        }
    }

    fun scanFolder(treeUri: Uri) {
        if (_uiState.value.isImporting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, message = null) }
            try {
                val uris = bookDocumentScanner.scan(treeUri.toString())
                if (uris.isEmpty()) {
                    _uiState.update { it.copy(isImporting = false, message = "No EPUB or PDF files were found in that folder.") }
                    return@launch
                }

                var importedCount = 0
                var skippedCount = 0
                for (uri in uris) {
                    try {
                        bookRepository.upsert(publicationImporter.import(uri))
                        importedCount++
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        skippedCount++
                    }
                }

                val result = when {
                    importedCount == 0 -> "No supported books could be imported."
                    skippedCount == 0 -> "Added $importedCount book${if (importedCount == 1) "" else "s"} to your library."
                    else -> "Added $importedCount book${if (importedCount == 1) "" else "s"}; skipped $skippedCount unreadable file${if (skippedCount == 1) "" else "s"}."
                }
                _uiState.update { it.copy(isImporting = false, message = result) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(isImporting = false, message = error.message ?: "The selected folder could not be scanned.")
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun createShelf(name: String, summary: String, theme: String) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        viewModelScope.launch {
            val current = preferencesRepository.get()
            val themeWords = theme.trim().split(Regex("\\s+")).filter(String::isNotBlank).take(3).joinToString(" ")
            val shelves = decodeShelves(current.libraryShelvesJson).filterNot { it.name.equals(cleanName, true) } +
                LibraryShelf(cleanName, summary.trim(), themeWords)
            preferencesRepository.save(current.copy(libraryShelvesJson = encodeShelves(shelves)))
        }
    }

    fun toggleMembership(bookId: String, collection: String) {
        viewModelScope.launch {
            val current = preferencesRepository.get()
            val membership = decodeMembership(current.libraryMembershipJson).toMutableMap()
            val books = membership[collection].orEmpty().toMutableSet()
            if (!books.add(bookId)) books.remove(bookId)
            membership[collection] = books
            preferencesRepository.save(current.copy(libraryMembershipJson = encodeMembership(membership)))
        }
    }

    fun removeBook(bookId: String) {
        viewModelScope.launch {
            bookRepository.delete(bookId)
            val current = preferencesRepository.get()
            val membership = decodeMembership(current.libraryMembershipJson).mapValues { (_, ids) -> ids - bookId }
            preferencesRepository.save(current.copy(libraryMembershipJson = encodeMembership(membership)))
        }
    }

    fun setSortOrder(order: LibrarySortOrder) {
        viewModelScope.launch {
            preferencesRepository.save(preferencesRepository.get().copy(librarySortOrder = order.name))
        }
    }

    private fun decodeShelves(json: String): List<LibraryShelf> = runCatching {
        val array = JSONArray(json)
        List(array.length()) { index ->
            val shelf = array.getJSONObject(index)
            LibraryShelf(shelf.optString("name"), shelf.optString("summary"), shelf.optString("theme"))
        }.filter { it.name.isNotBlank() }
    }.getOrDefault(emptyList())

    private fun encodeShelves(shelves: List<LibraryShelf>) = JSONArray().apply {
        shelves.forEach { put(JSONObject().put("name", it.name).put("summary", it.summary).put("theme", it.theme)) }
    }.toString()

    private fun decodeMembership(json: String): Map<String, Set<String>> = runCatching {
        val objectValue = JSONObject(json)
        buildMap {
            objectValue.keys().forEach { key ->
                val ids = objectValue.getJSONArray(key)
                put(key, List(ids.length()) { ids.getString(it) }.toSet())
            }
        }
    }.getOrDefault(emptyMap())

    private fun encodeMembership(membership: Map<String, Set<String>>) = JSONObject().apply {
        membership.forEach { (key, ids) -> put(key, JSONArray(ids.toList())) }
    }.toString()
}
