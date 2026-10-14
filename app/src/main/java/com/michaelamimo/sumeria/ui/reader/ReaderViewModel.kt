package com.michaelamimo.sumeria.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.os.SystemClock
import com.michaelamimo.sumeria.domain.model.BookFormat
import com.michaelamimo.sumeria.domain.model.DictionaryEntry
import com.michaelamimo.sumeria.domain.model.LibraryBook
import com.michaelamimo.sumeria.domain.model.ReaderBookmark as SavedReaderBookmark
import com.michaelamimo.sumeria.domain.model.ReaderDocument
import com.michaelamimo.sumeria.domain.model.ReadingProgress
import com.michaelamimo.sumeria.domain.model.ReadingSession
import com.michaelamimo.sumeria.domain.model.SessionMode
import com.michaelamimo.sumeria.domain.model.UserPreferences
import com.michaelamimo.sumeria.domain.repository.LibraryBookRepository
import com.michaelamimo.sumeria.domain.repository.DictionaryRepository
import com.michaelamimo.sumeria.domain.repository.ReaderBookmarkRepository
import com.michaelamimo.sumeria.domain.repository.ReaderContentRepository
import com.michaelamimo.sumeria.domain.repository.ReadingProgressRepository
import com.michaelamimo.sumeria.domain.repository.ReadingSessionRepository
import com.michaelamimo.sumeria.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

data class ReaderUiState(
    val book: LibraryBook? = null,
    val document: ReaderDocument? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val preferences: ReaderPreferences = ReaderPreferences(),
    val bookmarks: List<SavedReaderBookmark> = emptyList(),
    val dictionary: DictionaryLookupState? = null,
    val currentBlockIndex: Int = 0,
    val currentSectionIndex: Int = 0,
    val currentScrollOffset: Int = 0,
    val progression: Float = 0f
)

data class DictionaryLookupState(
    val word: String = "",
    val isLoading: Boolean = false,
    val entry: DictionaryEntry? = null,
    val error: String? = null
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookRepository: LibraryBookRepository,
    private val contentRepository: ReaderContentRepository,
    private val dictionaryRepository: DictionaryRepository,
    private val progressRepository: ReadingProgressRepository,
    private val sessionRepository: ReadingSessionRepository,
    private val bookmarkRepository: ReaderBookmarkRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {
    private val bookId: String? = savedStateHandle["bookId"]
    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()
    private var userPreferences = UserPreferences()
    private var progressSaveJob: Job? = null
    private var sessionTimerJob: Job? = null
    private var activeSession: ActiveSession? = null
    private var currentSessionMode: SessionMode = SessionMode.TEXT
    private var dictionaryLookupJob: Job? = null
    private var wordCountPrefix: IntArray = intArrayOf(0)

    private data class ActiveSession(
        val id: String,
        val bookId: String,
        val startedAtMillis: Long,
        val startedAtElapsedMillis: Long,
        val startBlockIndex: Int,
        val startSectionIndex: Int,
        val mode: SessionMode
    )

    init {
        viewModelScope.launch {
            preferencesRepository.observe().collectLatest { preferences ->
                userPreferences = preferences
                _uiState.update { it.copy(preferences = preferences.toReaderPreferences()) }
            }
        }
        if (bookId == null) {
            _uiState.update { it.copy(isLoading = false, error = "Choose a book from your library to start reading.") }
        } else {
            observeBookmarks(bookId)
            loadBook(bookId)
        }
    }

    fun updatePreferences(preferences: ReaderPreferences) {
        _uiState.update { it.copy(preferences = preferences) }
        val updated = userPreferences.copy(
            readerTheme = preferences.theme.name,
            readerFont = preferences.font.name,
            readerFontSize = preferences.fontSize,
            readerLineSpacing = preferences.lineSpacing,
            readerHorizontalMargin = preferences.horizontalMargin,
            readingStyle = preferences.readingStyle.name,
            textAlignment = preferences.textAlignment.name
        )
        userPreferences = updated
        viewModelScope.launch { preferencesRepository.save(updated) }
    }

    fun onPositionChanged(blockIndex: Int, sectionIndex: Int, scrollOffset: Int) {
        val state = _uiState.value
        val sections = state.document?.sections.orEmpty()
        if (sections.isEmpty()) return
        val boundedSection = sectionIndex.coerceIn(0, sections.lastIndex)
        val boundedBlock = blockIndex.coerceAtLeast(0)
        val progression = if (state.book?.format == BookFormat.PDF) {
            (boundedSection + 1).toFloat() / (state.document?.totalPages ?: sections.size).coerceAtLeast(1)
        } else {
            boundedSection.toFloat() / (sections.size - 1).coerceAtLeast(1)
        }.coerceIn(0f, 1f)
        _uiState.update {
            it.copy(
                currentBlockIndex = boundedBlock,
                currentSectionIndex = boundedSection,
                currentScrollOffset = scrollOffset.coerceAtLeast(0),
                progression = progression
            )
        }
        scheduleProgressSave()
    }

    fun toggleBookmark() {
        val state = _uiState.value
        val book = state.book ?: return
        val section = state.document?.sections?.getOrNull(state.currentSectionIndex) ?: return
        val location = bookmarkLocationJson(state.currentBlockIndex, state.currentSectionIndex)
        val existing = state.bookmarks.firstOrNull { it.locationJson == location }
        viewModelScope.launch {
            if (existing != null) {
                bookmarkRepository.delete(existing.id)
            } else {
                bookmarkRepository.save(
                    SavedReaderBookmark(
                        id = UUID.randomUUID().toString(),
                        bookId = book.id,
                        locationJson = location,
                        chapterTitle = section.title,
                        excerpt = section.paragraphs.firstOrNull(),
                        createdAtMillis = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun isCurrentPositionBookmarked(): Boolean {
        val state = _uiState.value
        val location = bookmarkLocationJson(state.currentBlockIndex, state.currentSectionIndex)
        return state.bookmarks.any { it.locationJson == location }
    }

    fun saveProgressNow(onSaved: (() -> Unit)? = null) {
        progressSaveJob?.cancel()
        viewModelScope.launch {
            saveProgress()
            onSaved?.invoke()
        }
    }

    fun showDictionary() {
        dictionaryLookupJob?.cancel()
        _uiState.update { it.copy(dictionary = DictionaryLookupState()) }
    }

    fun lookupWord(word: String) {
        val normalized = word.trim()
        if (normalized.isEmpty()) {
            _uiState.update { it.copy(dictionary = DictionaryLookupState(error = "Enter a word to look up.")) }
            return
        }
        dictionaryLookupJob?.cancel()
        _uiState.update { it.copy(dictionary = DictionaryLookupState(word = normalized, isLoading = true)) }
        dictionaryLookupJob = viewModelScope.launch {
            try {
                val definition = dictionaryRepository.lookup(normalized)
                _uiState.update { it.copy(dictionary = DictionaryLookupState(word = normalized, entry = definition)) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(dictionary = DictionaryLookupState(word = normalized, error = error.message ?: "Dictionary lookup failed."))
                }
            }
        }
    }

    fun dismissDictionary() {
        dictionaryLookupJob?.cancel()
        dictionaryLookupJob = null
        _uiState.update { it.copy(dictionary = null) }
    }

    fun onAudioParagraphChanged(sectionIndex: Int, paragraphIndex: Int) {
        val sections = _uiState.value.document?.sections ?: return
        val section = sections.getOrNull(sectionIndex) ?: return
        if (section.paragraphs.isEmpty()) return
        val blockIndex = sections.take(sectionIndex).sumOf { it.paragraphs.size + 1 } + 1 +
            paragraphIndex.coerceIn(0, section.paragraphs.lastIndex)
        onPositionChanged(blockIndex, sectionIndex, 0)
    }

    fun onReadingModeChanged(mode: SessionMode) {
        if (currentSessionMode == mode) return
        currentSessionMode = mode
        val previousSession = activeSession ?: return
        activeSession = null
        sessionTimerJob?.cancel()
        sessionTimerJob = null
        viewModelScope.launch {
            persistActiveSession(previousSession, ended = true)
            onReaderForeground()
        }
    }

    /** Starts a foreground reading interval. Repeated lifecycle notifications are safe. */
    fun onReaderForeground() {
        if (activeSession != null) return
        val state = _uiState.value
        val book = state.book ?: return
        if (state.document == null || state.isLoading || state.error != null) return
        val now = System.currentTimeMillis()
        val session = ActiveSession(
            id = UUID.randomUUID().toString(),
            bookId = book.id,
            startedAtMillis = now,
            startedAtElapsedMillis = SystemClock.elapsedRealtime(),
            startBlockIndex = state.currentBlockIndex,
            startSectionIndex = state.currentSectionIndex,
            mode = currentSessionMode
        )
        activeSession = session
        sessionTimerJob?.cancel()
        sessionTimerJob = viewModelScope.launch {
            persistActiveSession(session, ended = false)
            while (true) {
                delay(SESSION_CHECKPOINT_INTERVAL_MS)
                persistActiveSession(session, ended = false)
            }
        }
    }

    /** Closes the foreground interval and checkpoints its final duration and progress. */
    fun onReaderBackground() {
        val session = activeSession ?: return
        activeSession = null
        sessionTimerJob?.cancel()
        sessionTimerJob = null
        viewModelScope.launch {
            persistActiveSession(session, ended = true)
            saveProgress()
        }
    }

    private suspend fun persistActiveSession(session: ActiveSession, ended: Boolean) {
        val state = _uiState.value
        if (state.book?.id != session.bookId) return
        val elapsed = (SystemClock.elapsedRealtime() - session.startedAtElapsedMillis).coerceAtLeast(0L)
        // Ignore accidental opens shorter than five seconds; normal periodic checkpoints remain visible.
        if (ended && elapsed < MIN_RECORDED_SESSION_MS) {
            sessionRepository.delete(session.id)
            return
        }
        val currentPosition = if (state.book.format == BookFormat.PDF) {
            state.currentSectionIndex
        } else {
            state.currentBlockIndex
        }
        val initialPosition = if (state.book.format == BookFormat.PDF) {
            session.startSectionIndex
        } else {
            session.startBlockIndex
        }
        val unitsRead = (currentPosition - initialPosition).coerceAtLeast(0)
        val wordsRead = wordsBetween(session.startBlockIndex, state.currentBlockIndex)
        val pagesRead = if (state.book.format == BookFormat.PDF) {
            unitsRead.coerceAtLeast(if (elapsed >= MILLIS_PER_MINUTE) 1 else 0)
        } else if (wordsRead == 0) {
            0
        } else {
            (wordsRead + WORDS_PER_ESTIMATED_PAGE - 1) / WORDS_PER_ESTIMATED_PAGE
        }
        sessionRepository.save(
            ReadingSession(
                id = session.id,
                bookId = session.bookId,
                startedAtMillis = session.startedAtMillis,
                endedAtMillis = if (ended) System.currentTimeMillis() else null,
                durationMillis = elapsed,
                pagesRead = pagesRead,
                wordsRead = wordsRead,
                mode = session.mode
            )
        )
    }

    private fun wordsBetween(startBlock: Int, endBlock: Int): Int {
        val lower = startBlock.coerceIn(0, wordCountPrefix.lastIndex)
        val upper = endBlock.coerceIn(lower, wordCountPrefix.lastIndex)
        return wordCountPrefix[upper] - wordCountPrefix[lower]
    }

    private fun observeBookmarks(bookId: String) {
        viewModelScope.launch {
            bookmarkRepository.observeForBook(bookId).collectLatest { bookmarks ->
                _uiState.update { it.copy(bookmarks = bookmarks) }
            }
        }
    }

    private fun loadBook(bookId: String) {
        viewModelScope.launch {
            try {
                val book = bookRepository.getById(bookId)
                if (book == null) {
                    _uiState.update { it.copy(isLoading = false, error = "This book is no longer in your library.") }
                    return@launch
                }
                val progress = progressRepository.get(bookId)
                val location = progress?.locationJson?.let(::parseLocation)
                _uiState.update {
                    it.copy(
                        book = book,
                        currentBlockIndex = location?.first ?: 0,
                        currentSectionIndex = location?.second ?: 0,
                        currentScrollOffset = location?.third ?: 0,
                        progression = progress?.progression ?: 0f
                    )
                }
                val document = contentRepository.load(book)
                val wordPrefix = withContext(Dispatchers.Default) {
                    buildList {
                        add(0)
                        var total = 0
                        document.sections.forEach { section ->
                            add(total)
                            section.paragraphs.forEach { paragraph ->
                                total += paragraph.trim().split(Regex("\\s+")).count { it.isNotBlank() }
                                add(total)
                            }
                        }
                    }.toIntArray()
                }
                wordCountPrefix = wordPrefix
                _uiState.update {
                    it.copy(
                        document = document,
                        currentBlockIndex = location?.first?.coerceIn(0, document.blockCount - 1) ?: 0,
                        currentSectionIndex = location?.second?.coerceIn(0, document.sections.lastIndex) ?: 0,
                        currentScrollOffset = location?.third?.coerceAtLeast(0) ?: 0,
                        progression = progress?.progression ?: 0f,
                        isLoading = false,
                        error = null
                    )
                }
                bookRepository.upsert(book.copy(lastOpenedAtMillis = System.currentTimeMillis()))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = error.message ?: "This book could not be opened.")
                }
            }
        }
    }

    private fun scheduleProgressSave() {
        progressSaveJob?.cancel()
        progressSaveJob = viewModelScope.launch {
            delay(PROGRESS_SAVE_DELAY_MS)
            saveProgress()
        }
    }

    private suspend fun saveProgress() {
        val state = _uiState.value
        val book = state.book ?: return
        val document = state.document ?: return
        progressRepository.save(
            ReadingProgress(
                bookId = book.id,
                locationJson = locationJson(state.currentBlockIndex, state.currentSectionIndex, state.currentScrollOffset),
                currentPage = if (book.format == BookFormat.PDF) state.currentSectionIndex + 1 else null,
                totalPages = if (book.format == BookFormat.PDF) document.totalPages else null,
                progression = state.progression,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    private fun locationJson(blockIndex: Int, sectionIndex: Int, scrollOffset: Int): String = JSONObject()
        .put("blockIndex", blockIndex)
        .put("sectionIndex", sectionIndex)
        .put("scrollOffset", scrollOffset)
        .toString()

    private fun bookmarkLocationJson(blockIndex: Int, sectionIndex: Int): String = JSONObject()
        .put("blockIndex", blockIndex)
        .put("sectionIndex", sectionIndex)
        .toString()

    private fun parseLocation(json: String): Triple<Int, Int, Int>? = runCatching {
        val location = JSONObject(json)
        Triple(
            location.getInt("blockIndex"),
            location.getInt("sectionIndex"),
            location.optInt("scrollOffset", 0)
        )
    }.getOrNull()

    private fun UserPreferences.toReaderPreferences() = ReaderPreferences(
        theme = runCatching { ReaderTheme.valueOf(readerTheme) }.getOrDefault(ReaderTheme.Light),
        font = runCatching { ReaderFont.valueOf(readerFont) }.getOrDefault(ReaderFont.System),
        fontSize = readerFontSize.coerceIn(14, 32),
        lineSpacing = readerLineSpacing.coerceIn(1.2f, 2.2f),
        horizontalMargin = readerHorizontalMargin.coerceIn(16, 36),
        readingStyle = runCatching { ReadingStyle.valueOf(readingStyle) }.getOrDefault(ReadingStyle.Scroll),
        textAlignment = runCatching { TextAlignment.valueOf(textAlignment) }.getOrDefault(TextAlignment.Justified)
    )

    private val ReaderDocument.blockCount: Int
        get() = sections.sumOf { it.paragraphs.size + 1 }.coerceAtLeast(1)

    private companion object {
        const val PROGRESS_SAVE_DELAY_MS = 900L
        const val SESSION_CHECKPOINT_INTERVAL_MS = 15_000L
        const val MIN_RECORDED_SESSION_MS = 5_000L
        const val WORDS_PER_ESTIMATED_PAGE = 250
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
