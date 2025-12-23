package fi.oulu.picow.sensormonitor.ui.history

/**
 * Defines selectable time ranges for displaying historical sensor data.
 *
 * This enum is primarily used by the UI layer (e.g. tabs, dropdowns, buttons)
 * to determine which time window should be queried and visualized.
 *
 * The [label] is a user-facing string intended for display purposes only.
 * Query logic should map these values to Flux time expressions elsewhere
 * to keep UI and data layers decoupled.
 */
enum class HistoryRange(

    /**
     * Human-readable label shown in the UI.
     *
     * Example values: "24 h", "Week", "Month", "Year"
     */
    val label: String
) {
    /**
     * Last 24 hours of data.
     */
    DAY_24H("24 h"),

    /**
     * Last 7 days of data.
     */
    WEEK("Week"),

    /**
     * Last 30 days of data.
     */
    MONTH("Month"),

    /**
     * Last 12 months of data.
     */
    YEAR("Year")
}
