package com.michaelamimo.sumeria.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michaelamimo.sumeria.domain.model.LibraryBook
import com.michaelamimo.sumeria.domain.model.ReadingProgress
import com.michaelamimo.sumeria.domain.model.ReadingSession
import com.michaelamimo.sumeria.domain.model.UserPreferences
import com.michaelamimo.sumeria.domain.repository.LibraryBookRepository
import com.michaelamimo.sumeria.domain.repository.ReadingProgressRepository
import com.michaelamimo.sumeria.domain.repository.ReadingSessionRepository
import com.michaelamimo.sumeria.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class StatsDataState(
    val sessions: List<ReadingSession> = emptyList(),
    val books: List<LibraryBook> = emptyList(),
    val progress: List<ReadingProgress> = emptyList(),
    val dailyGoalMinutes: Int = UserPreferences().dailyGoalMinutes,
    val yearlyBookGoal: Int = UserPreferences().yearlyBookGoal,
    val isLoading: Boolean = true
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    sessionRepository: ReadingSessionRepository,
    bookRepository: LibraryBookRepository,
    progressRepository: ReadingProgressRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {
    private val zone = ZoneId.systemDefault()
    private val historyStart = LocalDate.now(zone).minusYears(5).atStartOfDay(zone).toInstant().toEpochMilli()
    private val historyEnd = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

    val uiState: StateFlow<StatsDataState> = combine(
        sessionRepository.observeBetween(historyStart, historyEnd),
        bookRepository.observeAll(),
        progressRepository.observeAll(),
        preferencesRepository.observe()
    ) { sessions, books, progress, preferences ->
        StatsDataState(
            sessions = sessions,
            books = books,
            progress = progress,
            dailyGoalMinutes = preferences.dailyGoalMinutes.coerceAtLeast(1),
            yearlyBookGoal = preferences.yearlyBookGoal.coerceAtLeast(1),
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsDataState())

    fun updateGoals(dailyGoalMinutes: Int, yearlyBookGoal: Int) {
        if (dailyGoalMinutes <= 0 || yearlyBookGoal <= 0) return
        viewModelScope.launch {
            val current = preferencesRepository.get()
            preferencesRepository.save(
                current.copy(
                    dailyGoalMinutes = dailyGoalMinutes,
                    yearlyBookGoal = yearlyBookGoal
                )
            )
        }
    }
}
