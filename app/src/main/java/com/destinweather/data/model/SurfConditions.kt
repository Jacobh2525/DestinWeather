package com.destinweather.data.model

data class SurfConditions(
    val location: String,
    val waveHeight: Double,      // in feet
    val wavePeriod: Int,         // in seconds
    val swellDirection: String,  // N, NE, E, etc.
    val windSpeed: Double,       // in mph
    val windDirection: String,
    val windRating: String,      // Good, Fair, Poor
    val tide: String,            // Low, High, Rising, Falling
    val waterTemp: Int,          // in Fahrenheit
    val description: String
)
