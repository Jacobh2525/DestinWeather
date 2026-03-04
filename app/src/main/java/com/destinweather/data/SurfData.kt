package com.destinweather.data

import com.destinweather.data.model.SurfConditions
import kotlinx.coroutines.delay

object SurfData {

    // Made this a suspend function so we can refresh it
    suspend fun getSurfConditions(): List<SurfConditions> {
        // Simulate network delay (remove this in production)
        delay(1000)

        // Return the data - in production, this would be an API call
        return getStaticSurfData()
    }

    // Keep your static data in a separate function
    private fun getStaticSurfData(): List<SurfConditions> {
        return listOf(
            SurfConditions(
                location = "Destin Harbor",
                waveHeight = 2.5,
                wavePeriod = 8,
                swellDirection = "S",
                windSpeed = 12.0,
                windDirection = "NE",
                windRating = "Fair",
                tide = "Rising",
                waterTemp = 78,
                description = "Small south swell with offshore winds"
            ),
            SurfConditions(
                location = "Okaloosa Island",
                waveHeight = 3.0,
                wavePeriod = 7,
                swellDirection = "SE",
                windSpeed = 15.0,
                windDirection = "E",
                windRating = "Good",
                tide = "High",
                waterTemp = 78,
                description = "Fun size waves, good conditions"
            ),
            SurfConditions(
                location = "Crystal Beach",
                waveHeight = 2.0,
                wavePeriod = 6,
                swellDirection = "S",
                windSpeed = 8.0,
                windDirection = "N",
                windRating = "Good",
                tide = "Low",
                waterTemp = 78,
                description = "Clean morning conditions"
            ),
            SurfConditions(
                location = "Miramar Beach",
                waveHeight = 1.5,
                wavePeriod = 5,
                swellDirection = "SW",
                windSpeed = 18.0,
                windDirection = "SW",
                windRating = "Poor",
                tide = "Falling",
                waterTemp = 77,
                description = "Choppy conditions, wind swell only"
            ),
            SurfConditions(
                location = "Henderson Beach",
                waveHeight = 2.0,
                wavePeriod = 7,
                swellDirection = "S",
                windSpeed = 10.0,
                windDirection = "NW",
                windRating = "Good",
                tide = "Rising",
                waterTemp = 78,
                description = "Decent swell with offshore winds"
            )
        )
    }
}
