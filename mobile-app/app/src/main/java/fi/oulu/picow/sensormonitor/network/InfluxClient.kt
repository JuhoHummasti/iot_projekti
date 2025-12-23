package fi.oulu.picow.sensormonitor.network

import android.util.Log
import fi.oulu.picow.sensormonitor.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

/**
 * Singleton responsible for configuring and exposing the InfluxDB network client.
 *
 * Responsibilities:
 * - Configure Retrofit base URL
 * - Attach authentication and required headers
 * - Enable HTTP logging for debugging
 * - Expose a ready-to-use [InfluxApi] instance
 *
 * All configuration values (URL, token, org, bucket) are provided via [BuildConfig]
 * to keep secrets and environment-specific values out of source code.
 */
object InfluxClient {

    init {
        // Log the base URL once at startup to aid debugging and environment verification.
        Log.d("InfluxClient", "BASE_URL=${BuildConfig.INFLUX_BASE_URL}")
    }

    /**
     * Interceptor responsible for adding mandatory InfluxDB headers to every request.
     *
     * - Authorization uses InfluxDB v2 token-based authentication
     * - Accept is fixed to CSV to match Flux query output expectations
     * - Content-Type is set to Flux query format
     */
    private val authInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Token ${BuildConfig.INFLUX_TOKEN}")
            .addHeader("Accept", "application/csv")
            .addHeader("Content-Type", "application/vnd.flux")
            .build()

        chain.proceed(request)
    }

    /**
     * Logs full request and response bodies.
     *
     * Useful during development and debugging of Flux queries.
     * Should be reduced or disabled in production if sensitive data
     * or performance become a concern.
     */
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    /**
     * Shared OkHttp client configured with authentication and logging interceptors.
     */
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    /**
     * Retrofit instance configured for InfluxDB v2 API.
     *
     * - Uses [ScalarsConverterFactory] because responses are raw CSV strings,
     *   not JSON payloads.
     */
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.INFLUX_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(ScalarsConverterFactory.create())
        .build()

    /**
     * Public API instance used by repositories to execute Flux queries.
     */
    val api: InfluxApi = retrofit.create(InfluxApi::class.java)
}
