package fi.oulu.picow.sensormonitor.data

import fi.oulu.picow.sensormonitor.model.Measurement
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MeasurementRepository {

    private val influxRepository = InfluxRepository()

    suspend fun getLatestMeasurement(): Measurement {
        // Get last 24h history
        val history = influxRepository.getTemperatureHistory24h()

        // Fallback if no data
        val latest = history.lastOrNull()
            ?: return Measurement(
                deviceId = "pico-01",
                temperatureC = Double.NaN,
                pressureHpa = 0.0,
                timestamp = "No data"
            )

        // You can later also fetch pressure etc.
        return Measurement(
            deviceId = "pico-01",
            temperatureC = latest.value,
            pressureHpa = 0.0,
            timestamp = latest.time
        )
    }
}
