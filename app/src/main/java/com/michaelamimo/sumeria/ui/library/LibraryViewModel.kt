package com.michaelamimo.sumeria.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michaelamimo.sumeria.domain.model.LibraryBook
import com.michaelamimo.sumeria.domain.repository.BookDocumentScanner
import com.michaelamimo.sumeria.domain.repository.LibraryBookRepository
import com.michaelamimo.sumeria.domain.repository.PublicationImporter
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
    val isImporting: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val bookRepository: LibraryBookRepository,
    private val publicationImporter: PublicationImporter,
    private val bookDocumentScanner: BookDocumentScanner
) : ViewModel() {
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            bookRepository.observeAll().collectLatest { books ->
                _uiState.update { it.copy(books = books) }
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
}
