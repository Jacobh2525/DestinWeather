package com.destinweather.data.api

import com.destinweather.data.model.MarineEntry
import com.destinweather.data.model.WindResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApi {

    // Free, no API key. Absolute URL: marine API is a separate host.
    // Multiple locations can be batched via comma-separated lat/lon lists.
    @GET("https://marine-api.open-meteo.com/v1/marine")
    suspend fun getMarine(
        @Query("latitude") latitudes: String,
        @Query("longitude") longitudes: String,
        @Query("current") current: String = "wave_height,wave_direction,wave_period,swell_wave_height,swell_wave_direction,swell_wave_period",
        @Query("length_unit") lengthUnit: String = "imperial"
    ): List<MarineEntry>

    @GET("v1/forecast")
    suspend fun getWind(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "wind_speed_10m,wind_direction_10m",
        @Query("wind_speed_unit") windSpeedUnit: String = "mph"
    ): WindResponse
}
