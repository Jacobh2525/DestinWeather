package com.destinweather.data.api

import com.destinweather.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val WEATHER_BASE_URL = "https://api.openweathermap.org/data/2.5/"
    private const val NOAA_BASE_URL = "https://api.weather.gov/"
    private const val OPEN_METEO_BASE_URL = "https://api.open-meteo.com/"

    // Appends the OpenWeatherMap API key (from local.properties) to OWM requests,
    // and the required identifying User-Agent to NWS (api.weather.gov) requests.
    private val headerInterceptor = okhttp3.Interceptor { chain ->
        val request = chain.request()
        val newRequest = when {
            request.url.host.contains("openweathermap.org") -> {
                val url = request.url.newBuilder()
                    .addQueryParameter("appid", BuildConfig.OWM_API_KEY)
                    .build()
                request.newBuilder().url(url).build()
            }
            request.url.host.contains("api.weather.gov") -> {
                request.newBuilder()
                    .header("User-Agent", "DestinWeather (github.com/Jacobh2525/DestinWeather)")
                    .build()
            }
            else -> request
        }
        chain.proceed(newRequest)
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                else HttpLoggingInterceptor.Level.NONE
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(headerInterceptor)
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val weatherApi: WeatherApi by lazy {
        Retrofit.Builder()
            .baseUrl(WEATHER_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)
    }

    val noaaApi: NoaaApi by lazy {
        Retrofit.Builder()
            .baseUrl(NOAA_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NoaaApi::class.java)
    }

    val openMeteoApi: OpenMeteoApi by lazy {
        Retrofit.Builder()
            .baseUrl(OPEN_METEO_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenMeteoApi::class.java)
    }
}
