package fi.oulu.picow.sensormonitor.data

import fi.oulu.picow.sensormonitor.model.Measurement

class MeasurementRepository {

    private val influxRepository = InfluxRepository()

    suspend fun getLatestMeasurement(): Measurement {
        val tempHistory = influxRepository.getTemperatureHistory24h()
        val presHistory = influxRepository.getPressureHistory24h()

        val latestTemp = tempHistory.lastOrNull()
        val latestPres = presHistory.lastOrNull()

        if (latestTemp == null && latestPres == null) {
            return Measurement(
                deviceId = "pico-01",
                temperatureC = Double.NaN,
                pressureHpa = Double.NaN,
                timestamp = "No data"
            )
        }

        val temperature = latestTemp?.value ?: Double.NaN
        val pressure = latestPres?.value ?: Double.NaN
        val timestamp = latestTemp?.time ?: latestPres?.time ?: "No data"

        return Measurement(
            deviceId = "pico-01",
            temperatureC = temperature,
            pressureHpa = pressure,
            timestamp = timestamp
        )
    }
}
