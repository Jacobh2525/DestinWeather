package com.destinweather.data.model

import com.google.gson.annotations.SerializedName

// Current weather response
data class WeatherResponse(
    @SerializedName("main") val main: MainWeather,
    @SerializedName("weather") val weather: List<Weather>,
    @SerializedName("wind") val wind: Wind,
    @SerializedName("name") val cityName: String,
    @SerializedName("sys") val sys: Sys
)

data class MainWeather(
    @SerializedName("temp") val temp: Double,
    @SerializedName("temp_min") val tempMin: Double?,
    @SerializedName("temp_max") val tempMax: Double?,
    @SerializedName("feels_like") val feelsLike: Double,
    @SerializedName("humidity") val humidity: Int,
    @SerializedName("pressure") val pressure: Int
)

data class Weather(
    @SerializedName("id") val id: Int,
    @SerializedName("main") val main: String,
    @SerializedName("description") val description: String,
    @SerializedName("icon") val icon: String
)

data class Wind(
    @SerializedName("speed") val speed: Double,
    @SerializedName("deg") val deg: Int
)

data class Sys(
    @SerializedName("country") val country: String,
    @SerializedName("sunrise") val sunrise: Long,
    @SerializedName("sunset") val sunset: Long
)
