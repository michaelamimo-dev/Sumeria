package com.michaelamimo.sumeria.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michaelamimo.sumeria.domain.model.LibraryBook
import com.michaelamimo.sumeria.domain.model.ReadingProgress
import com.michaelamimo.sumeria.domain.model.ReadingSession
import com.michaelamimo.sumeria.domain.repository.LibraryBookRepository
import com.michaelamimo.sumeria.domain.repository.ReadingProgressRepository
import com.michaelamimo.sumeria.domain.repository.ReadingSessionRepository
import com.michaelamimo.sumeria.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class HomeUiState(
    val books: List<LibraryBook> = emptyList(),
    val progress: List<ReadingProgress> = emptyList(),
    val sessions: List<ReadingSession> = emptyList(),
    val dailyGoalMinutes: Int = 15,
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    books: LibraryBookRepository,
    progress: ReadingProgressRepository,
    sessions: ReadingSessionRepository,
    preferences: UserPreferencesRepository
) : ViewModel() {
    private val zone = ZoneId.systemDefault()
    private val from = LocalDate.now(zone).minusDays(30).atStartOfDay(zone).toInstant().toEpochMilli()
    private val until = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

    val state: StateFlow<HomeUiState> = combine(
        books.observeAll(), progress.observeAll(), sessions.observeBetween(from, until), preferences.observe()
    ) { bookRows, progressRows, sessionRows, prefs ->
        HomeUiState(bookRows, progressRows, sessionRows, prefs.dailyGoalMinutes.coerceAtLeast(1), false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
