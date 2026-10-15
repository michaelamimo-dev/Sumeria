package com.michaelamimo.sumeria.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michaelamimo.sumeria.auth.AuthRepository
import com.michaelamimo.sumeria.domain.model.UserPreferences
import com.michaelamimo.sumeria.domain.repository.CloudSyncRepository
import com.michaelamimo.sumeria.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CloudSyncUiState(
    val autoSyncEnabled: Boolean = true,
    val isSyncing: Boolean = false,
    val status: String = "Ready to sync",
    val preferences: UserPreferences = UserPreferences()
)

@HiltViewModel
class CloudSyncViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val cloudSyncRepository: CloudSyncRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {
    private val _state = MutableStateFlow(CloudSyncUiState())
    val state: StateFlow<CloudSyncUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.observe().collect { preferences ->
                _state.update { it.copy(autoSyncEnabled = preferences.autoCloudSyncEnabled, preferences = preferences) }
            }
        }
    }

    fun setAutoSync(enabled: Boolean) {
        viewModelScope.launch {
            val current = preferencesRepository.get()
            preferencesRepository.save(current.copy(autoCloudSyncEnabled = enabled))
            _state.update { it.copy(autoSyncEnabled = enabled, status = if (enabled) "Automatic sync enabled" else "Automatic sync paused") }
        }
    }

    fun updateDailyReminder(enabled: Boolean, minutesAfterMidnight: Int? = null) {
        viewModelScope.launch {
            val current = preferencesRepository.get()
            preferencesRepository.save(
                current.copy(
                    dailyRemindersEnabled = enabled,
                    reminderMinutesAfterMidnight = minutesAfterMidnight ?: current.reminderMinutesAfterMidnight
                )
            )
        }
    }

    fun updateAppTheme(mode: String) {
        viewModelScope.launch {
            preferencesRepository.save(preferencesRepository.get().copy(appThemeMode = mode))
        }
    }

    fun updateReaderTheme(theme: String) {
        viewModelScope.launch {
            preferencesRepository.save(preferencesRepository.get().copy(readerTheme = theme))
        }
    }

    fun updateDailyGoal(minutes: Int) {
        if (minutes <= 0) return
        viewModelScope.launch {
            preferencesRepository.save(preferencesRepository.get().copy(dailyGoalMinutes = minutes))
        }
    }

    fun updateYearlyBookGoal(books: Int) {
        if (books <= 0) return
        viewModelScope.launch {
            preferencesRepository.save(preferencesRepository.get().copy(yearlyBookGoal = books))
        }
    }

    fun syncNow() {
        val user = authRepository.currentUser
        if (user == null) {
            _state.update { it.copy(status = "Sign in to sync across devices") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true, status = "Syncing with cloud…") }
            try {
                cloudSyncRepository.syncUser(user.id)
                _state.update { it.copy(isSyncing = false, status = "Synced just now") }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update { it.copy(isSyncing = false, status = error.localizedMessage ?: "Sync failed. Try again.") }
            }
        }
    }
}
