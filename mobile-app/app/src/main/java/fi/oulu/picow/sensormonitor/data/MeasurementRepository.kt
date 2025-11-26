package fi.oulu.picow.sensormonitor.data

import fi.oulu.picow.sensormonitor.model.Measurement
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MeasurementRepository {
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    suspend fun getLatestMeasurement(): Measurement {
        // Simulate network delay
        delay(800)

        val now = timeFormat.format(Date())

        // Mock values – later replaced by real API
        return Measurement(
            deviceId = "pico-01",
            temperatureC = 21.5,
            pressureHpa = 1007.3,
            timestamp = now
        )
    }
}