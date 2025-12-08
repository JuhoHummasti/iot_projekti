package fi.oulu.picow.sensormonitor.data

import android.util.Log
import fi.oulu.picow.sensormonitor.BuildConfig
import fi.oulu.picow.sensormonitor.network.InfluxClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody

data class HistoryPoint(
    val time: String,
    val value: Double
)

class InfluxRepository {

    companion object {
        private const val TAG = "InfluxRepo"
    }
    suspend fun getTemperatureHistory24h() =
        queryHistory(measurement = "Laki/temp", hours = 24)

    suspend fun getPressureHistory24h() =
        queryHistory(measurement = "Laki/pressure", hours = 24)

    private suspend fun queryHistory(
        measurement: String,
        hours: Int = 24
    ): List<HistoryPoint> = withContext(Dispatchers.IO) {

        val flux = """
        from(bucket: "${BuildConfig.INFLUX_BUCKET}")
          |> range(start: -${hours}h)
          |> filter(fn: (r) => r._measurement == "$measurement")
          |> keep(columns: ["_time", "_value"])
    """.trimIndent()

        val body = flux.toRequestBody("application/vnd.flux".toMediaType())
        val response = InfluxClient.api.queryFluxCsv(
            org = BuildConfig.INFLUX_ORG,
            fluxQuery = body
        )

        if (!response.isSuccessful) return@withContext emptyList()

        val csv = response.body()?.string().orEmpty()
        val lines = csv.lines().map { it.trim() }.filter { it.isNotBlank() && !it.startsWith("#") }
        if (lines.isEmpty()) return@withContext emptyList()

        val headerIndex = lines.indexOfFirst { it.contains("_time") && it.contains("_value") }
        if (headerIndex == -1) return@withContext emptyList()

        val headers = lines[headerIndex].split(",")
        val timeIdx = headers.indexOf("_time")
        val valueIdx = headers.indexOf("_value")

        lines.drop(headerIndex + 1).mapNotNull { line ->
            val c = line.split(",")
            val time = c.getOrNull(timeIdx) ?: return@mapNotNull null
            val value = c.getOrNull(valueIdx)?.toDoubleOrNull() ?: return@mapNotNull null
            HistoryPoint(time, value)
        }
    }
}
