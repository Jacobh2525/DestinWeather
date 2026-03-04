package com.destinweather.data.model

import com.google.gson.annotations.SerializedName

data class ForecastResponse(
    @SerializedName("list") val list: List<ForecastItem>,
    @SerializedName("city") val city: City
)

data class ForecastItem(
    @SerializedName("dt") val dt: Long,
    @SerializedName("main") val main: MainWeather,
    @SerializedName("weather") val weather: List<Weather>,
    @SerializedName("wind") val wind: Wind,
    @SerializedName("dt_txt") val dtTxt: String
)

data class City(
    @SerializedName("name") val name: String,
    @SerializedName("country") val country: String
)
