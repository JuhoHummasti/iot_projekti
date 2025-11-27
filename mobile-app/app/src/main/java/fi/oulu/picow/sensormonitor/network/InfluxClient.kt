package fi.oulu.picow.sensormonitor.network

import android.util.Log
import fi.oulu.picow.sensormonitor.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

object InfluxClient {
    init {
        Log.d("InfluxClient", "BASE_URL=${BuildConfig.INFLUX_BASE_URL}")
    }
    private val authInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Token ${BuildConfig.INFLUX_TOKEN}")
            .addHeader("Accept", "application/csv")
            .addHeader("Content-Type", "application/vnd.flux")
            .build()
        chain.proceed(request)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.INFLUX_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(ScalarsConverterFactory.create())
        .build()

    val api: InfluxApi = retrofit.create(InfluxApi::class.java)
}
