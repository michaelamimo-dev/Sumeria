package com.michaelamimo.sumeria.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michaelamimo.sumeria.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AudioVoiceSettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {
    private val _voiceName = MutableStateFlow("")
    val voiceName: StateFlow<String> = _voiceName.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.observe().collect { _voiceName.value = it.audioVoiceName }
        }
    }

    fun selectVoice(name: String) {
        _voiceName.value = name
        viewModelScope.launch {
            preferencesRepository.save(preferencesRepository.get().copy(audioVoiceName = name))
        }
    }
}
