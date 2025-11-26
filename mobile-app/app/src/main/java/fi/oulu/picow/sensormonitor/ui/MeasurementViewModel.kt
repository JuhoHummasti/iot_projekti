package fi.oulu.picow.sensormonitor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.oulu.picow.sensormonitor.data.MeasurementRepository
import fi.oulu.picow.sensormonitor.model.Measurement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface MeasurementUiState {
    object Loading : MeasurementUiState
    data class Success(val measurement: Measurement) : MeasurementUiState
    data class Error(val message: String) : MeasurementUiState
}

class MeasurementViewModel(
    private val repository: MeasurementRepository = MeasurementRepository()
) : ViewModel() {

    private val _uiState: MutableStateFlow<MeasurementUiState> =
        MutableStateFlow(MeasurementUiState.Loading)
    val uiState: StateFlow<MeasurementUiState> = _uiState

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = MeasurementUiState.Loading
        viewModelScope.launch {
            try {
                val measurement = repository.getLatestMeasurement()
                _uiState.value = MeasurementUiState.Success(measurement)
            } catch (e: Exception) {
                _uiState.value = MeasurementUiState.Error("Failed to load data")
            }
        }
    }
}
