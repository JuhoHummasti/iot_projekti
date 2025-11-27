package fi.oulu.picow.sensormonitor.network

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit interface for querying InfluxDB using Flux.
 *
 * The base URL and auth headers are configured in InfluxClient.
 */
interface InfluxApi {

    @POST("api/v2/query")
    @Headers(
        "Accept: application/csv"
        // Content-Type will come from RequestBody
    )
    suspend fun queryFluxCsv(
        @Query("org") org: String,
        @Body fluxQuery: RequestBody
    ): Response<ResponseBody>
}
