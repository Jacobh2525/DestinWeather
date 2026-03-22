package com.destinweather.data.model

import com.google.gson.annotations.SerializedName

data class MarineResponse(
    @SerializedName("list")
    val list: List<MarineItem>?
)

data class MarineItem(
    @SerializedName("dt")
    val dt: Long?,
    @SerializedName("main")
    val main: MarineMain?,
    @SerializedName("waveHeight")
    val waveHeight: Double?,
    @SerializedName("wavePeriod")
    val wavePeriod: Double?,
    @SerializedName("temperature")
    val temperature: MarineTemperature?
)

data class MarineMain(
    @SerializedName("temp")
    val temp: Double?
)

data class MarineTemperature(
    @SerializedName("sea")
    val sea: Double?  // Water temperature in Kelvin/Celsius
)
