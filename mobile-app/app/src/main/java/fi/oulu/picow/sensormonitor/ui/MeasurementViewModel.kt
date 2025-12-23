package fi.oulu.picow.sensormonitor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.oulu.picow.sensormonitor.data.MeasurementRepository
import fi.oulu.picow.sensormonitor.model.Measurement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * UI state model for the main dashboard screen.
 *
 * Represents the lifecycle of loading a single, latest sensor measurement.
 */
sealed interface MeasurementUiState {

    /** Indicates that the latest measurement is currently being loaded. */
    object Loading : MeasurementUiState

    /**
     * Successful load of the latest measurement.
     *
     * @property measurement Most recent sensor data snapshot
     */
    data class Success(val measurement: Measurement) : MeasurementUiState

    /**
     * Error state shown when the measurement could not be loaded.
     *
     * @property message Short, user-facing error description
     */
    data class Error(val message: String) : MeasurementUiState
}

/**
 * ViewModel backing the main dashboard screen.
 *
 * Responsibilities:
 * - Load the most recent sensor measurement
 * - Expose UI state via a cold [StateFlow]
 * - Handle refresh requests from the UI
 *
 * All business and networking logic is delegated to [MeasurementRepository].
 */
class MeasurementViewModel(
    private val repository: MeasurementRepository = MeasurementRepository()
) : ViewModel() {

    /**
     * Internal mutable UI state.
     *
     * Kept private to enforce unidirectional data flow.
     */
    private val _uiState: MutableStateFlow<MeasurementUiState> =
        MutableStateFlow(MeasurementUiState.Loading)

    /**
     * Public, read-only UI state observed by the Compose UI.
     */
    val uiState: StateFlow<MeasurementUiState> = _uiState

    init {
        // Load initial measurement when ViewModel is created
        refresh()
    }

    /**
     * Triggers a reload of the latest measurement.
     *
     * Updates [uiState] to reflect loading, success, or error.
     */
    fun refresh() {
        _uiState.value = MeasurementUiState.Loading

        viewModelScope.launch {
            try {
                val measurement = repository.getLatestMeasurement()
                _uiState.value = MeasurementUiState.Success(measurement)
            } catch (_: Exception) {
                // Defensive fallback: repository normally handles errors,
                // but this ensures UI stability in unexpected cases.
                _uiState.value = MeasurementUiState.Error("Failed to load data")
            }
        }
    }
}
