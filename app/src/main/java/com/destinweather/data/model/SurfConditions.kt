package com.destinweather.data.model

data class SurfConditions(
    val location: String,
    val waveHeight: Double,      // in feet
    val wavePeriod: Int,         // in seconds
    val swellDirection: String,  // N, NE, E, etc.
    val windSpeed: Double,       // in mph
    val windDirection: String,
    val windRating: String,      // Good, Fair, Poor
    val tide: String?,           // e.g. "Falling · Low 10:19 PM"; null if unavailable
    val waterTemp: Int?,         // in Fahrenheit; null if no buoy reading
    val description: String
)
