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

    suspend fun getTemperatureHistory24h(): List<HistoryPoint> = withContext(Dispatchers.IO) {
        val flux = """
        from(bucket: "${BuildConfig.INFLUX_BUCKET}")
          |> range(start: -24h)
          |> filter(fn: (r) => r._measurement == "Laki/temp")
          |> keep(columns: ["_time", "_value"])
    """.trimIndent()

        // Correct content type
        val body = flux.toRequestBody("application/vnd.flux".toMediaType())

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

        // ---- tolerant CSV parsing ----
        val dataLines = csv.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }

        if (dataLines.isEmpty()) {
            Log.w(TAG, "No non-comment lines in CSV")
            return@withContext emptyList()
        }

        // Find the header line that actually contains _time and _value
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
            .drop(headerIndex + 1) // lines after the header are data rows
            .mapNotNull { line ->
                val cols = line.split(",")
                val time = cols.getOrNull(timeIndex) ?: return@mapNotNull null
                val valueStr = cols.getOrNull(valueIndex) ?: return@mapNotNull null
                val value = valueStr.toDoubleOrNull() ?: return@mapNotNull null
                HistoryPoint(time = time, value = value)
            }

        Log.d(TAG, "Parsed ${result.size} history points")
        result
    }
}
