package fi.oulu.picow.sensormonitor.ui.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

    data class Success(
        val temperature: List<HistoryPoint>,
        val pressure: List<HistoryPoint>
    ) : HistoryUiState

    data class Error(val message: String) : HistoryUiState
}

class HistoryViewModel(
    private val repository: InfluxRepository = InfluxRepository()
) : ViewModel() {

    var selectedRange by mutableStateOf(HistoryRange.DAY_24H)
        private set

    // 0 = current period (today / this week / etc.)
    // -1 = previous period (yesterday / last week / etc.)
    var periodOffset by mutableIntStateOf(0)
        private set

    var uiState: HistoryUiState by mutableStateOf(HistoryUiState.Loading)
        private set

    init {
        refreshHistory()
    }

    fun selectRange(range: HistoryRange) {
        selectedRange = range
        periodOffset = 0 // reset when changing range
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
     * Convert (range, periodOffset) into Flux `start` / `stop` expressions.
     *
     * Strategy:
     * - offset = 0 → open range from "now - duration" to now
     * - offset = -1 → previous window (e.g. -48h .. -24h, or -14d .. -7d)
     * - offset = -2 → window before that, etc.
     */
    private fun computeRangeExpressions(
        range: HistoryRange,
        offset: Int
    ): Pair<String, String?> {
        fun forUnit(unit: String, size: Int): Pair<String, String?> {
            val n = -offset  // 0 = current, 1 = previous, 2 = two periods back...
            return if (n <= 0) {
                // current period → open range
                "-${size}$unit" to null
            } else {
                // previous periods → [start, stop] both relative to now
                val startAmount = size * (n + 1)
                val stopAmount = size * n
                "-${startAmount}$unit" to "-${stopAmount}$unit"
            }
        }

        return when (range) {
            HistoryRange.DAY_24H -> forUnit("h", 24)
            HistoryRange.WEEK    -> forUnit("d", 7)
            HistoryRange.MONTH   -> forUnit("d", 30)   // simple approx
            HistoryRange.YEAR    -> forUnit("d", 365)  // simple approx
        }
    }

    /**
     * Load history data for both temperature & pressure from InfluxDB.
     * Uses the selected time range and periodOffset.
     */
    private fun refreshHistory() {
        uiState = HistoryUiState.Loading

        val (startExpr, stopExpr) = computeRangeExpressions(selectedRange, periodOffset)

        viewModelScope.launch {
            try {
                val temperature = repository.getMeasurementHistory(
                    measurement = "Laki/temp",
                    startExpr = startExpr,
                    stopExpr = stopExpr
                )

                val pressure = repository.getMeasurementHistory(
                    measurement = "Laki/pressure",
                    startExpr = startExpr,
                    stopExpr = stopExpr
                )

                uiState = HistoryUiState.Success(
                    temperature = temperature,
                    pressure = pressure
                )
            } catch (e: Exception) {
                e.printStackTrace()
                uiState = HistoryUiState.Error("Failed to load history data")
            }
        }
    }
}
