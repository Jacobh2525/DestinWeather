package com.destinweather.data.model

import com.google.gson.annotations.SerializedName

// Open-Meteo Marine API (batched coords return a JSON array of these)
data class MarineEntry(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("current") val current: MarineCurrent?
)

data class MarineCurrent(
    @SerializedName("wave_height") val waveHeight: Double?,
    @SerializedName("wave_direction") val waveDirection: Double?,
    @SerializedName("wave_period") val wavePeriod: Double?,
    @SerializedName("swell_wave_height") val swellWaveHeight: Double?,
    @SerializedName("swell_wave_direction") val swellWaveDirection: Double?,
    @SerializedName("swell_wave_period") val swellWavePeriod: Double?
)

// Open-Meteo Forecast API (wind)
data class WindResponse(
    @SerializedName("current") val current: WindCurrent?
)

data class WindCurrent(
    @SerializedName("wind_speed_10m") val windSpeed: Double?,
    @SerializedName("wind_direction_10m") val windDirection: Double?
)

// Open-Meteo Forecast API (dew point + UV index for the weather screen)
data class CurrentExtrasResponse(
    @SerializedName("current") val current: CurrentExtras?,
    @SerializedName("hourly") val hourly: HourlyExtras?
)

data class CurrentExtras(
    @SerializedName("dewpoint_2m") val dewPoint: Double?
)

data class HourlyExtras(
    @SerializedName("uv_index") val uvIndex: List<Double>?
)
