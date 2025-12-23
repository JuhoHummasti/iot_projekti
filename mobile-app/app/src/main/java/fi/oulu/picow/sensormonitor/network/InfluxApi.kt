package fi.oulu.picow.sensormonitor.network

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit API definition for querying InfluxDB using the Flux query language.
 *
 * Design notes:
 * - Uses the InfluxDB v2 `/api/v2/query` endpoint.
 * - Responses are returned in CSV format for efficient streaming and parsing.
 * - Authentication, base URL, and common headers are configured in [InfluxClient],
 *   keeping this interface focused purely on request structure.
 */
interface InfluxApi {

    /**
     * Executes a Flux query against InfluxDB and returns the result as CSV.
     *
     * @param org InfluxDB organization name or ID.
     * @param fluxQuery Request body containing the Flux script.
     *                  The `Content-Type` is derived from the provided [RequestBody]
     *                  (typically `application/vnd.flux`).
     *
     * @return Retrofit [Response] wrapping a raw [ResponseBody] containing CSV data.
     *         Callers are responsible for checking `isSuccessful` and parsing the body.
     */
    @POST("api/v2/query")
    @Headers(
        // Explicitly request CSV output; Flux defaults to annotated CSV.
        "Accept: application/csv"
        // Content-Type header is intentionally omitted here and provided by RequestBody
    )
    suspend fun queryFluxCsv(
        @Query("org") org: String,
        @Body fluxQuery: RequestBody
    ): Response<ResponseBody>
}
