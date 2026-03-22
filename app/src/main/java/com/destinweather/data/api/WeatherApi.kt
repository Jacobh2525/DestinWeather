package com.destinweather.data.api

import com.destinweather.data.model.ForecastResponse
import com.destinweather.data.model.MarineResponse
import com.destinweather.data.model.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {

    @GET("weather")
    suspend fun getWeather(
        @Query("q") city: String = "Destin,US",
        @Query("appid") apiKey: String = "4eb1e079aaf05aa0e8741ea74097d961",
        @Query("units") units: String = "imperial"
    ): WeatherResponse

    @GET("forecast")
    suspend fun getForecast(
        @Query("q") city: String = "Destin,US",
        @Query("appid") apiKey: String = "4eb1e079aaf05aa0e8741ea74097d961",
        @Query("units") units: String = "imperial"
    ): ForecastResponse

    @GET("forecast/marine")
    suspend fun getMarineWeather(
        @Query("q") city: String = "Destin,US",
        @Query("appid") apiKey: String = "4eb1e079aaf05aa0e8741ea74097d961",
        @Query("units") units: String = "imperial"
    ): MarineResponse

}
