package fi.oulu.picow.sensormonitor.model

/**
 * Immutable domain model representing a single sensor measurement snapshot.
 *
 * This model is used at the application/domain layer and is typically
 * composed from one or more lower-level data sources (e.g. time-series
 * measurements retrieved from InfluxDB).
 *
 * Conventions:
 * - Numeric values may be [Double.NaN] if the measurement is unavailable.
 * - The timestamp is provided as a formatted string to keep the model
 *   UI-friendly; parsing to a date/time object is left to the caller if needed.
 */
data class Measurement(

    /**
     * Unique identifier of the device that produced the measurement.
     *
     * Example: `"pico-01"`
     */
    val deviceId: String,

    /**
     * Measured temperature in degrees Celsius (°C).
     *
     * May be [Double.NaN] if temperature data is missing or unavailable.
     */
    val temperatureC: Double,

    /**
     * Measured atmospheric pressure in hectopascals (hPa).
     *
     * May be [Double.NaN] if pressure data is missing or unavailable.
     */
    val pressureHpa: Double,

    /**
     * Timestamp associated with the measurement.
     *
     * Expected to be an RFC3339 / ISO-8601 formatted string when originating
     * from InfluxDB. A placeholder value (e.g. `"No data"`) may be used when
     * no valid measurement exists.
     */
    val timestamp: String
)
