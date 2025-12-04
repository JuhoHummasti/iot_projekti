package fi.oulu.picow.sensormonitor.ui.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.oulu.picow.sensormonitor.data.HistoryPoint
import fi.oulu.picow.sensormonitor.data.InfluxRepository
import kotlinx.coroutines.launch

// UI state for the history screen
sealed interface HistoryUiState {
    object Loading : HistoryUiState
    data class Success(val points: List<HistoryPoint>) : HistoryUiState
    data class Error(val message: String) : HistoryUiState
}

class HistoryViewModel(
    private val repository: InfluxRepository = InfluxRepository()
) : ViewModel() {

    var selectedRange by mutableStateOf(HistoryRange.DAY_24H)
        private set

    // 0 = current period (today / this week / etc.)
    // -1 = previous period (yesterday / last week / etc.)
    var periodOffset by mutableStateOf(0)
        private set

    var uiState: HistoryUiState by mutableStateOf(HistoryUiState.Loading)
        private set

    init {
        // Load initial history for default range (24h, current period)
        refreshHistory()
    }

    fun selectRange(range: HistoryRange) {
        selectedRange = range
        periodOffset = 0 // reset to current when changing range
        refreshHistory()
    }

    fun goToPreviousPeriod() {
        periodOffset -= 1
        refreshHistory()
    }

    fun goToNextPeriod() {
        if (periodOffset < 0) {
            periodOffset += 1
            refreshHistory()
        }
        // Optionally prevent going into the future: offset must be <= 0
    }

    fun getCurrentPeriodLabel(): String {
        return when (selectedRange) {
            HistoryRange.DAY_24H -> when (periodOffset) {
                0 -> "Today (last 24 h)"
                -1 -> "Yesterday"
                else -> "${-periodOffset} days ago (24 h)"
            }
            HistoryRange.WEEK -> when (periodOffset) {
                0 -> "This week"
                -1 -> "Last week"
                else -> "${-periodOffset} weeks ago"
            }
            HistoryRange.MONTH -> when (periodOffset) {
                0 -> "This month"
                -1 -> "Last month"
                else -> "${-periodOffset} months ago"
            }
            HistoryRange.YEAR -> when (periodOffset) {
                0 -> "This year"
                -1 -> "Last year"
                else -> "${-periodOffset} years ago"
            }
        }
    }

    /**
     * Load history data from InfluxDB using the repository.
     * For now we always call getTemperatureHistory24h(), but you can
     * extend the repository later to support week/month/year + offsets.
     */
    private fun refreshHistory() {
        uiState = HistoryUiState.Loading

        viewModelScope.launch {
            try {
                val points = repository.getTemperatureHistory24h()
                uiState = HistoryUiState.Success(points)
            } catch (e: Exception) {
                e.printStackTrace()
                uiState = HistoryUiState.Error("Failed to load history data")
            }
        }
    }
}
