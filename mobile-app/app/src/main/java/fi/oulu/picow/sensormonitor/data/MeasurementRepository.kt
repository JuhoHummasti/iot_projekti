package fi.oulu.picow.sensormonitor.data

import fi.oulu.picow.sensormonitor.model.Measurement

/**
 * Repository responsible for composing a high-level [Measurement] model
 * from lower-level time-series data sources.
 *
 * Responsibilities:
 * - Coordinate calls to [InfluxRepository]
 * - Decide what constitutes the "latest" measurement
 * - Apply safe fallbacks when data is missing
 *
 * This repository intentionally contains simple orchestration logic only.
 * All networking and parsing is delegated to InfluxRepository.
 */
class MeasurementRepository {

    /**
     * Influx-backed repository used to retrieve historical sensor data.
     *
     * Note: Instantiated directly for simplicity. If this grows or becomes
     * shared, consider injecting it (e.g. via constructor or DI framework).
     */
    private val influxRepository = InfluxRepository()

    /**
     * Fetches the most recent temperature and pressure measurements.
     *
     * Strategy:
     * - Query the last 24 hours for both temperature and pressure
     * - Use the *last* datapoint of each series as the latest value
     * - If one series is missing, return NaN for that field
     * - If both are missing, return a clearly invalid placeholder measurement
     *
     * @return [Measurement] representing the most recent known sensor state.
     *         Numeric fields may be NaN if data is unavailable.
     */
    suspend fun getLatestMeasurement(): Measurement {

        // Fetch recent history for both measurements.
        // These calls may return empty lists if no data or an error occurred.
        val tempHistory = influxRepository.getTemperatureHistory24h()
        val presHistory = influxRepository.getPressureHistory24h()

        // Use the last datapoint as the most recent value.
        val latestTemp = tempHistory.lastOrNull()
        val latestPres = presHistory.lastOrNull()

        // If absolutely no data is available, return a sentinel measurement.
        // Using NaN makes missing values explicit and easy to detect downstream.
        if (latestTemp == null && latestPres == null) {
            return Measurement(
                deviceId = "pico-01",
                temperatureC = Double.NaN,
                pressureHpa = Double.NaN,
                timestamp = "No data"
            )
        }

        // Use available values where possible; fall back to NaN otherwise.
        val temperature = latestTemp?.value ?: Double.NaN
        val pressure = latestPres?.value ?: Double.NaN

        // Prefer temperature timestamp if available, otherwise pressure timestamp.
        // This assumes both originate from the same device and clock source.
        val timestamp = latestTemp?.time ?: latestPres?.time ?: "No data"

        return Measurement(
            deviceId = "pico-01",
            temperatureC = temperature,
            pressureHpa = pressure,
            timestamp = timestamp
        )
    }
}
