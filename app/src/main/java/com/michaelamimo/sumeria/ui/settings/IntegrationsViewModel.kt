package com.michaelamimo.sumeria.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michaelamimo.sumeria.domain.repository.GoodreadsExportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IntegrationsState(
    val isPreparingExport: Boolean = false,
    val preparedCsv: String? = null,
    val status: String? = null
)

@HiltViewModel
class IntegrationsViewModel @Inject constructor(
    private val exportRepository: GoodreadsExportRepository
) : ViewModel() {
    private val _state = MutableStateFlow(IntegrationsState())
    val state: StateFlow<IntegrationsState> = _state.asStateFlow()

    fun prepareGoodreadsExport() {
        viewModelScope.launch {
            _state.update { it.copy(isPreparingExport = true, preparedCsv = null, status = null) }
            try {
                val csv = exportRepository.createLibraryCsv()
                _state.update { it.copy(isPreparingExport = false, preparedCsv = csv) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update { it.copy(isPreparingExport = false, status = error.localizedMessage ?: "Could not prepare the export.") }
            }
        }
    }

    fun exportSaved() { _state.update { it.copy(preparedCsv = null, status = "Goodreads CSV saved") } }
    fun exportCancelled() { _state.update { it.copy(preparedCsv = null) } }
    fun exportFailed(message: String) { _state.update { it.copy(preparedCsv = null, status = message) } }
    fun dismissStatus() { _state.update { it.copy(status = null) } }
}
