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

/**
 * UI state model for the history screen.
 *
 * This sealed hierarchy makes rendering explicit and exhaustive in Compose:
 * - [Loading]: data fetch in progress
 * - [Success]: history data available
 * - [Error]: unrecoverable error state with user-facing message
 */
sealed interface HistoryUiState {

    /** Indicates that history data is currently being loaded. */
    object Loading : HistoryUiState

    /**
     * Successful history load.
     *
     * @property temperature List of temperature history points
     * @property pressure    List of pressure history points
     */
    data class Success(
        val temperature: List<HistoryPoint>,
        val pressure: List<HistoryPoint>
    ) : HistoryUiState

    /**
     * Error state shown when loading fails.
     *
     * @property message Short, user-facing error description
     */
    data class Error(val message: String) : HistoryUiState
}

/**
 * ViewModel for the history screen.
 *
 * Responsibilities:
 * - Own UI state ([HistoryUiState])
 * - Track selected history range and period offset
 * - Translate UI selections into Flux time expressions
 * - Load history data asynchronously from [InfluxRepository]
 *
 * The ViewModel contains no UI code and no rendering logic.
 */
class HistoryViewModel(
    private val repository: InfluxRepository = InfluxRepository()
) : ViewModel() {

    /**
     * Currently selected time range (24h / week / month / year).
     *
     * Exposed as read-only to the UI; mutations go through intent methods.
     */
    var selectedRange by mutableStateOf(HistoryRange.DAY_24H)
        private set

    /**
     * Offset relative to the current period.
     *
     * Semantics:
     * -  0 → current period (today / this week / etc.)
     * - -1 → previous period (yesterday / last week / etc.)
     * - -2 → two periods back, and so on
     */
    var periodOffset by mutableIntStateOf(0)
        private set

    /**
     * Current UI state consumed by the Compose screen.
     */
    var uiState: HistoryUiState by mutableStateOf(HistoryUiState.Loading)
        private set

    init {
        // Initial load for default range and current period
        refreshHistory()
    }

    /**
     * Handle user selecting a new history range.
     *
     * Resets the period offset to the current period and reloads data.
     */
    fun selectRange(range: HistoryRange) {
        selectedRange = range
        periodOffset = 0
        refreshHistory()
    }

    /**
     * Navigate to the previous time window.
     */
    fun goToPreviousPeriod() {
        periodOffset -= 1
        refreshHistory()
    }

    /**
     * Navigate to the next time window.
     *
     * Forward navigation is only allowed up to the current period (offset = 0).
     */
    fun goToNextPeriod() {
        if (periodOffset < 0) {
            periodOffset += 1
            refreshHistory()
        }
    }

    /**
     * Returns a human-readable label describing the currently selected period.
     *
     * Used purely for UI display; does not affect query logic.
     */
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
     * Converts a ([HistoryRange], periodOffset) pair into Flux `range()`
     * start / stop expressions.
     *
     * Strategy:
     * - offset = 0 → open range ending at now (start only)
     * - offset < 0 → closed range fully in the past
     *
     * Examples (24h range):
     * - offset = 0  → start = "-24h", stop = null
     * - offset = -1 → start = "-48h", stop = "-24h"
     */
    private fun computeRangeExpressions(
        range: HistoryRange,
        offset: Int
    ): Pair<String, String?> {

        /**
         * Helper for ranges expressed as multiples of a unit (hours or days).
         */
        fun forUnit(unit: String, size: Int): Pair<String, String?> {
            val n = -offset // 0 = current, 1 = previous, 2 = two periods back
            return if (n <= 0) {
                // Current period → open-ended range
                "-${size}$unit" to null
            } else {
                // Past periods → explicit start and stop
                val startAmount = size * (n + 1)
                val stopAmount = size * n
                "-${startAmount}$unit" to "-${stopAmount}$unit"
            }
        }

        return when (range) {
            HistoryRange.DAY_24H -> forUnit("h", 24)
            HistoryRange.WEEK    -> forUnit("d", 7)
            HistoryRange.MONTH   -> forUnit("d", 30)   // approximation
            HistoryRange.YEAR    -> forUnit("d", 365)  // approximation
        }
    }

    /**
     * Loads history data for temperature and pressure based on the
     * currently selected range and period offset.
     *
     * Updates [uiState] to reflect loading, success, or error.
     */
    fun refreshHistory() {
        uiState = HistoryUiState.Loading

        val (startExpr, stopExpr) =
            computeRangeExpressions(selectedRange, periodOffset)

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
                // Defensive fallback: repository normally fails closed,
                // but this protects against unexpected runtime errors.
                e.printStackTrace()
                uiState = HistoryUiState.Error("Failed to load history data")
            }
        }
    }
}
