package com.destinweather.data

import com.destinweather.data.api.RetrofitClient
import com.destinweather.data.model.SurfConditions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SurfData {

    // Map queryName to actual API city name
    private val cityNameMapping = mapOf(
        "Destin,US" to "Destin,FL",
        "Panama City Beach,US" to "Panama City Beach,FL",
        "Pensacola,US" to "Pensacola,FL",
        "Fort Walton Beach,US" to "Fort Walton Beach,FL",
        "Gulf Shores,US" to "Gulf Shores,AL",
        "Orange Beach,US" to "Orange Beach,AL",
        "Myrtle Beach,US" to "Myrtle Beach,SC",
        "Miami,US" to "Miami,FL",
        "Tampa,US" to "Tampa,FL",
        "Jacksonville,US" to "Jacksonville,FL",
        "Key West,US" to "Key West,FL",
        "Cocoa Beach,US" to "Cocoa Beach,FL",
        "Santa Rosa Beach,US" to "Santa Rosa Beach,FL",
        "Seaside,US" to "Seaside,FL",
        "Alys Beach,US" to "Alys Beach,FL"
    )

    // Surf spots - keys match queryName from LocationPicker EXACTLY
    private val locationSurfSpots = mapOf(
        "Destin,US" to listOf("Destin Harbor", "Okaloosa Island", "Crystal Beach", "Miramar Beach", "Henderson Beach"),
        "Panama City Beach,US" to listOf("St. Andrews SP", "Carillon Beach", "Camp Helen", "Edgewater Beach", "Pier Park"),
        "Pensacola,US" to listOf("Pensacola Beach", "Perdido Key", "Fort Pickens", "Quietwater Beach", "Big Lagoon"),
        "Fort Walton Beach,US" to listOf("Okaloosa Pier", "Fort Walton Beach", "Island Coast", "The Gulf", "Wynnewood"),
        "Gulf Shores,US" to listOf("Gulf Shores Beach", "Gulf State Park", "Bon Secour", "Little Lagoon", "Gulf Pier"),
        "Orange Beach,US" to listOf("Orange Beach", "Gulf State Park", "The Pass", "Cotton Bayou", "Romar Beach"),
        "Myrtle Beach,US" to listOf("Myrtle Beach", "North Myrtle Beach", "Surfside Beach", "Garden City", "Litchfield"),
        "Miami,US" to listOf("South Beach", "Key Biscayne", "Haulover", "Surfside", "Hollywood Beach"),
        "Tampa,US" to listOf("Clearwater Beach", "St. Pete Beach", "Indian Rocks", "Treasure Island", "Sand Key"),
        "Jacksonville,US" to listOf("Jacksonville Beach", "Atlantic Beach", "Neptune Beach", "Ponte Vedra", "Mickler's"),
        "Key West,US" to listOf("Smathers Beach", "Fort Zachary", "South Beach", "Higgs Beach", "Rest Beach"),
        "Cocoa Beach,US" to listOf("Cocoa Beach", "Cape Canaveral", "Patrick AFB", "Indialantic", "Sebastian Inlet"),
        "Santa Rosa Beach,US" to listOf("Santa Rosa Beach", "Grayton Beach", "WaterColor", "Blue Mountain", "Seagrove"),
        "Seaside,US" to listOf("Seaside", "WaterColor", "Grayton Beach", "Seagrove", "Pt Washington"),
        "Alys Beach,US" to listOf("Alys Beach", "Rosemary Beach", "Inlet Beach", "Camp Gulf", "Pier Park")
    )

    suspend fun getSurfConditions(location: String): List<SurfConditions> = withContext(Dispatchers.IO) {
        // First check if we have preset spots
        val hasPresetSpots = locationSurfSpots.containsKey(location)

        // Get spots - if we have them, use them; otherwise generate from location name
        val spotNames = if (hasPresetSpots) {
            locationSurfSpots[location]!!
        } else {
            // Generate from location name
            val cityName = location.split(",").firstOrNull() ?: "Beach"
            listOf("$cityName Beach", "$cityName Pier", "$cityName Point", "North $cityName", "South $cityName")
        }

        try {
            // Get API city name
            val apiCity = cityNameMapping[location] ?: location

            val weather = RetrofitClient.weatherApi.getWeather(city = apiCity)
            val marine = RetrofitClient.weatherApi.getMarineWeather(city = apiCity)

            val currentWave = marine.list?.firstOrNull()
            val waveHeight = currentWave?.waveHeight ?: 2.0
            val wavePeriod = currentWave?.wavePeriod?.toInt() ?: 7

            val waterTempKelvin = currentWave?.temperature?.sea
            val waterTemp = if (waterTempKelvin != null && waterTempKelvin > 0) {
                ((waterTempKelvin - 273.15) * 9/5 + 32).toInt()
            } else {
                (weather.main.temp - 2).toInt()
            }

            val windSpeed = weather.wind.speed

            // Create surf conditions for each spot
            spotNames.mapIndexed { index, spot ->
                val variation = index * 0.3
                val spotWind = windSpeed * (0.8 + (index * 0.1))

                SurfConditions(
                    location = spot,
                    waveHeight = waveHeight + variation - 0.3,
                    wavePeriod = wavePeriod + (index % 3) - 1,
                    swellDirection = getSwellDirection(weather.wind.deg),
                    windSpeed = spotWind,
                    windDirection = getCardinalDirection(weather.wind.deg),
                    windRating = getRating(spotWind),
                    tide = getTide(index),
                    waterTemp = waterTemp - (index % 2),
                    description = getDescription(waveHeight + variation, spotWind)
                )
            }
        } catch (e: Exception) {
            // Fallback with real spot names
            spotNames.mapIndexed { index, spot ->
                SurfConditions(
                    location = spot,
                    waveHeight = 2.5 + (index * 0.3),
                    wavePeriod = 7 + (index % 3),
                    swellDirection = "SE",
                    windSpeed = 12.0 + (index * 2),
                    windDirection = "NE",
                    windRating = if (index < 2) "Good" else "Fair",
                    tide = getTide(index),
                    waterTemp = 78 - (index % 2),
                    description = getDescription(2.5, 12.0)
                )
            }
        }
    }

    private fun getSwellDirection(windDeg: Int): String = when { windDeg < 45 || windDeg >= 315 -> "N"; windDeg < 90 -> "NE";
        windDeg < 135 -> "E"; windDeg < 180 -> "SE"; windDeg < 225 -> "S"; windDeg < 270 -> "SW"; else -> "W" }

    private fun getCardinalDirection(degrees: Int): String = when { degrees < 23 || degrees >= 338 -> "N"; degrees < 68 ->
        "NE"; degrees < 113 -> "E"; degrees < 158 -> "SE"; degrees < 203 -> "S"; degrees < 248 -> "SW"; degrees < 293 -> "W"; else ->
        "NW" }

    private fun getRating(windSpeed: Double): String = when { windSpeed < 10 -> "Good"; windSpeed < 20 -> "Fair"; else ->
        "Poor" }

    private fun getTide(index: Int): String = listOf("Rising", "High", "Falling", "Low", "Rising")[index % 5]

    private fun getDescription(waveHeight: Double, windSpeed: Double): String = when { windSpeed < 10 && waveHeight > 2 ->
        "Clean conditions!"; windSpeed < 15 -> "Decent for all"; windSpeed < 20 -> "Choppy"; else -> "Not recommended" }
}
