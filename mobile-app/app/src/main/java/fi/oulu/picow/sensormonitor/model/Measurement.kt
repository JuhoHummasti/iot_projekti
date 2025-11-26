package fi.oulu.picow.sensormonitor.model

data class Measurement(
    val deviceId: String,
    val temperatureC: Double,
    val pressureHpa: Double,
    val timestamp: String
)
