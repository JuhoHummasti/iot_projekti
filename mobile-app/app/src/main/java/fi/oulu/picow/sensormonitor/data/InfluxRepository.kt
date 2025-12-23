package fi.oulu.picow.sensormonitor.data

import android.util.Log
import fi.oulu.picow.sensormonitor.BuildConfig
import fi.oulu.picow.sensormonitor.network.InfluxClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Single time-series datapoint returned from InfluxDB.
 *
 * @property time  RFC3339 timestamp string from Influx (`_time` column).
 * @property value Numeric sample value (`_value` column).
 */
data class HistoryPoint(
    val time: String,
    val value: Double
)

/**
 * Repository responsible for querying InfluxDB (Flux) and mapping responses into domain models.
 *
 * Notes / conventions:
 * - Uses `Dispatchers.IO` because the call performs network I/O and parsing.
 * - Returns `emptyList()` on errors to keep callers simple (no exceptions bubbling up).
 *   If you later want error-specific UI, consider returning a sealed result type instead.
 */
class InfluxRepository {

    companion object {
        /** Log tag used for repository-level diagnostics. */
        private const val TAG = "InfluxRepo"

        /**
         * InfluxDB Flux endpoint content type.
         * Influx expects the Flux script in the request body with this media type.
         */
        private const val FLUX_CONTENT_TYPE = "application/vnd.flux"
    }

    /**
     * Queries a single measurement from InfluxDB within a Flux `range()`.
     *
     * @param measurement Influx measurement name, e.g. `"Laki/temp"` or `"Laki/pressure"`.
     * @param startExpr Flux start expression (duration or timestamp), e.g. `"-24h"`, `"-7d"`.
     * @param stopExpr Optional Flux stop expression. If null, the range is open-ended on the stop side.
     *
     * @return A list of parsed [HistoryPoint] values. Returns empty list if the request fails,
     *         the response is malformed, or no datapoints are found.
     */
    suspend fun getMeasurementHistory(
        measurement: String,
        startExpr: String,
        stopExpr: String? = null
    ): List<HistoryPoint> = withContext(Dispatchers.IO) {

        // Flux `range()` supports an optional stop argument. We append it only when provided.
        val stopPart = stopExpr?.let { ", stop: $it" } ?: ""

        // Keep the query minimal: select bucket, time range, measurement, and only the needed columns.
        // `keep()` reduces the CSV payload size and makes parsing more predictable.
        val flux = """
            from(bucket: "${BuildConfig.INFLUX_BUCKET}")
              |> range(start: $startExpr$stopPart)
              |> filter(fn: (r) => r._measurement == "$measurement")
              |> keep(columns: ["_time", "_value"])
        """.trimIndent()

        // Influx query endpoint expects the Flux script in the body.
        val body = flux.toRequestBody(FLUX_CONTENT_TYPE.toMediaType())

        val response = InfluxClient.api.queryFluxCsv(
            org = BuildConfig.INFLUX_ORG,
            fluxQuery = body
        )

        Log.d(TAG, "HTTP status: ${response.code()}")

        // Fail closed: if the request didn't succeed, log the server message and return no data.
        if (!response.isSuccessful) {
            Log.e(TAG, "Influx ERROR:\n${response.errorBody()?.string()}")
            return@withContext emptyList()
        }

        // Response is CSV (with comment lines starting with '#').
        val csv = response.body()?.string().orEmpty()
        Log.d(TAG, "CSV response:\n$csv")

        // Tolerant CSV parsing:
        // - trim whitespace
        // - ignore empty lines
        // - ignore Influx metadata/comment rows (#...)
        val dataLines = csv.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }

        if (dataLines.isEmpty()) {
            Log.w(TAG, "No non-comment lines in CSV")
            return@withContext emptyList()
        }

        // Find the first header line that contains both required columns.
        // Influx can emit multiple tables/headers depending on query shape.
        val headerIndex = dataLines.indexOfFirst { line ->
            line.contains("_time") && line.contains("_value")
        }

        if (headerIndex == -1) {
            Log.e(TAG, "Could not find header with _time / _value in CSV")
            return@withContext emptyList()
        }

        // Determine column indices dynamically to avoid depending on column ordering.
        val headerCols = dataLines[headerIndex].split(",")
        val timeIndex = headerCols.indexOf("_time")
        val valueIndex = headerCols.indexOf("_value")

        if (timeIndex == -1 || valueIndex == -1) {
            Log.e(TAG, "Header does not contain _time / _value: $headerCols")
            return@withContext emptyList()
        }

        // Parse each subsequent data row. Rows with missing or non-numeric values are skipped.
        // (This keeps the parsing robust to partial/malformed rows without failing the whole query.)
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

    /**
     * Convenience wrapper for callers that want the last 24h of temperature values.
     * Kept to preserve backwards compatibility with older call sites.
     */
    suspend fun getTemperatureHistory24h(): List<HistoryPoint> =
        getMeasurementHistory("Laki/temp", startExpr = "-24h", stopExpr = null)

    /**
     * Convenience wrapper for callers that want the last 24h of pressure values.
     * Kept to preserve backwards compatibility with older call sites.
     */
    suspend fun getPressureHistory24h(): List<HistoryPoint> =
        getMeasurementHistory("Laki/pressure", startExpr = "-24h", stopExpr = null)
}
