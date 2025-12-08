package fi.oulu.picow.sensormonitor.data

import android.util.Log
import fi.oulu.picow.sensormonitor.BuildConfig
import fi.oulu.picow.sensormonitor.network.InfluxClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

data class HistoryPoint(
    val time: String,
    val value: Double
)

/**
 * Repository for querying InfluxDB Cloud with Flux.
 */
class InfluxRepository {

    companion object {
        private const val TAG = "InfluxRepo"
        private const val FLUX_CONTENT_TYPE = "application/vnd.flux"
    }

    /**
     * Generic helper: query one measurement with a given start/stop range.
     *
     * @param measurement e.g. "Laki/temp" or "Laki/pressure"
     * @param startExpr   Flux duration or timestamp, e.g. "-24h", "-7d"
     * @param stopExpr    optional Flux stop, e.g. "-24h". If null → open range.
     */
    suspend fun getMeasurementHistory(
        measurement: String,
        startExpr: String,
        stopExpr: String? = null
    ): List<HistoryPoint> = withContext(Dispatchers.IO) {

        val stopPart = stopExpr?.let { ", stop: $it" } ?: ""

        val flux = """
            from(bucket: "${BuildConfig.INFLUX_BUCKET}")
              |> range(start: $startExpr$stopPart)
              |> filter(fn: (r) => r._measurement == "$measurement")
              |> keep(columns: ["_time", "_value"])
        """.trimIndent()

        val body = flux.toRequestBody(FLUX_CONTENT_TYPE.toMediaType())

        val response = InfluxClient.api.queryFluxCsv(
            org = BuildConfig.INFLUX_ORG,
            fluxQuery = body
        )

        Log.d(TAG, "HTTP status: ${response.code()}")

        if (!response.isSuccessful) {
            Log.e(TAG, "Influx ERROR:\n${response.errorBody()?.string()}")
            return@withContext emptyList()
        }

        val csv = response.body()?.string().orEmpty()
        Log.d(TAG, "CSV response:\n$csv")

        // Tolerant CSV parsing
        val dataLines = csv.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }

        if (dataLines.isEmpty()) {
            Log.w(TAG, "No non-comment lines in CSV")
            return@withContext emptyList()
        }

        // Find header with _time + _value
        val headerIndex = dataLines.indexOfFirst { line ->
            line.contains("_time") && line.contains("_value")
        }

        if (headerIndex == -1) {
            Log.e(TAG, "Could not find header with _time / _value in CSV")
            return@withContext emptyList()
        }

        val headerCols = dataLines[headerIndex].split(",")
        val timeIndex = headerCols.indexOf("_time")
        val valueIndex = headerCols.indexOf("_value")

        if (timeIndex == -1 || valueIndex == -1) {
            Log.e(TAG, "Header does not contain _time / _value: $headerCols")
            return@withContext emptyList()
        }

        val result = dataLines
            .drop(headerIndex + 1)
            .mapNotNull { line ->
                val cols = line.split(",")
                val time = cols.getOrNull(timeIndex) ?: return@mapNotNull null
                val valueStr = cols.getOrNull(valueIndex) ?: return@mapNotNull null
                val value = valueStr.toDoubleOrNull() ?: return@mapNotNull null
                HistoryPoint(time = time, value = value)
            }

        Log.d(TAG, "Parsed ${result.size} history points for $measurement")
        result
    }

    // Optional convenience wrappers so old callers still compile
    suspend fun getTemperatureHistory24h(): List<HistoryPoint> =
        getMeasurementHistory("Laki/temp", startExpr = "-24h", stopExpr = null)

    suspend fun getPressureHistory24h(): List<HistoryPoint> =
        getMeasurementHistory("Laki/pressure", startExpr = "-24h", stopExpr = null)
}
