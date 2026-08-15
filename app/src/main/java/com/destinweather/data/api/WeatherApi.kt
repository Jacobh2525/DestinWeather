package com.destinweather.data.api

import com.destinweather.data.model.ForecastResponse
import com.destinweather.data.model.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {

    // appid is appended automatically by RetrofitClient's interceptor
    // (key lives in local.properties -> BuildConfig.OWM_API_KEY)

    @GET("weather")
    suspend fun getWeather(
        @Query("q") city: String = "Destin,US",
        @Query("units") units: String = "imperial"
    ): WeatherResponse

    @GET("forecast")
    suspend fun getForecast(
        @Query("q") city: String = "Destin,US",
        @Query("units") units: String = "imperial"
    ): ForecastResponse

}
