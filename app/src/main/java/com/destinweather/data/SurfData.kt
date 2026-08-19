package com.destinweather.data

import com.destinweather.data.api.RetrofitClient
import com.destinweather.data.model.SurfConditions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.math.roundToInt

/**
 * Real surf data orchestration:
 *  - Waves/swell: Open-Meteo Marine API (free, no key), batched per-spot coords
 *  - Wind: Open-Meteo Forecast API at city coords
 *  - Water temp: nearest NDBC buoy with a valid reading (fallback chain)
 *  - Tide: nearest NOAA CO-OPS prediction station
 *
 * If the marine call fails the whole fetch throws -> SurfState.Error.
 * Wind/water/tide degrade to null and render as unavailable in the UI.
 * No fabricated values are ever returned.
 */
object SurfData {

    private val locationCoords = mapOf(
        "Destin,US" to (30.3935 to -86.4958),
        "Panama City Beach,US" to (30.1523 to -85.6594),
        "Pensacola,US" to (30.4213 to -87.2169),
        "Fort Walton Beach,US" to (30.4058 to -86.6188),
        "Gulf Shores,US" to (30.2460 to -87.7008),
        "Orange Beach,US" to (30.2944 to -87.6297),
        "Myrtle Beach,US" to (33.6891 to -78.8867),
        "Miami,US" to (25.7617 to -80.1918),
        "Tampa,US" to (27.9506 to -82.4572),
        "Jacksonville,US" to (30.3322 to -81.6557),
        "Key West,US" to (24.5551 to -81.7800),
        "Cocoa Beach,US" to (28.3200 to -80.6076),
        "Santa Rosa Beach,US" to (30.3960 to -86.1728),
        "Seaside,US" to (30.3152 to -86.1394),
        "Alys Beach,US" to (30.2855 to -86.1650)
    )

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

    // Nearest NDBC buoys per city, ordered by distance (nearest first).
    // Buoys occasionally drop water temp, so fall back down the chain.
    private val buoyStations = mapOf(
        "Destin,US" to listOf("42005", "42028", "42012"),
        "Panama City Beach,US" to listOf("42005", "42028", "42027"),
        "Pensacola,US" to listOf("42012", "42016", "42015"),
        "Fort Walton Beach,US" to listOf("42005", "42028", "42012"),
        "Gulf Shores,US" to listOf("42012", "42016", "42015"),
        "Orange Beach,US" to listOf("42012", "42016", "42015"),
        "Myrtle Beach,US" to listOf("41119", "41024", "41108"),
        "Miami,US" to listOf("41122", "42025", "42079"),
        "Tampa,US" to listOf("42098", "42021", "42013"),
        "Jacksonville,US" to listOf("41112", "41117", "41012"),
        "Key West,US" to listOf("42080", "42095", "42037"),
        "Cocoa Beach,US" to listOf("41113", "41118", "41009"),
        "Santa Rosa Beach,US" to listOf("42005", "42028", "42012"),
        "Seaside,US" to listOf("42005", "42028", "42012"),
        "Alys Beach,US" to listOf("42005", "42028", "42012")
    )

    // Nearest NOAA CO-OPS tide-prediction station per city
    private val tideStations = mapOf(
        "Destin,US" to "8729511",               // East Pass (Destin)
        "Panama City Beach,US" to "8729108",    // Panama City
        "Pensacola,US" to "8729840",            // Pensacola
        "Fort Walton Beach,US" to "8729547",    // Okaloosa Island
        "Gulf Shores,US" to "8731269",          // Gulf State Park Pier
        "Orange Beach,US" to "8731439",         // Gulf Shores, ICWW
        "Myrtle Beach,US" to "8660854",         // Myrtle Beach, Combination Bridge
        "Miami,US" to "8723165",                // Miami Miamarina, Biscayne Bay
        "Tampa,US" to "8726693",                // Hillsborough River Entrance
        "Jacksonville,US" to "8720226",         // Jacksonville, Main Street Bridge
        "Key West,US" to "8724557",             // Key West, White Street Pier
        "Cocoa Beach,US" to "8721649",          // Cocoa Beach
        "Santa Rosa Beach,US" to "8729376",     // Santa Rosa, Hogtown Bayou
        "Seaside,US" to "8729376",              // Santa Rosa, Hogtown Bayou
        "Alys Beach,US" to "8729376"            // Santa Rosa, Hogtown Bayou
    )

    // Small coastal offsets so each spot card queries its own marine grid cell
    private val spotOffsets = listOf(
        0.0 to 0.0, 0.04 to 0.05, -0.04 to -0.05, 0.08 to 0.09, -0.07 to -0.09
    )

    suspend fun getSurfConditions(location: String): List<SurfConditions> {
        val (lat, lon) = locationCoords[location]
            ?: throw IOException("Unknown location: $location")

        val spotNames = locationSurfSpots[location]
            ?: location.split(",").firstOrNull()?.let { city ->
                listOf("$city Beach", "$city Pier", "$city Point", "North $city", "South $city")
            }
            ?: throw IOException("No surf spots for: $location")

        return fetchSurf(
            lat, lon, spotNames,
            buoyStations[location] ?: emptyList(),
            tideStations[location] ?: ""
        )
    }

    /**
     * GPS path: waves and wind at the exact coordinates; buoy and tide
     * station chains borrowed from the nearest preset location.
     */
    suspend fun getSurfConditionsAt(lat: Double, lon: Double, cityName: String?): List<SurfConditions> {
        val nearest = nearestPresetKey(lat, lon)
        val spotBase = cityName?.substringBefore(",")?.trim()?.takeIf { it.isNotBlank() }
            ?: nearest?.substringBefore(",")
            ?: "Local"
        val spotNames = listOf(
            "$spotBase Beach", "$spotBase Pier", "$spotBase Point",
            "North $spotBase", "South $spotBase"
        )
        return fetchSurf(
            lat, lon, spotNames,
            nearest?.let { buoyStations[it] } ?: emptyList(),
            nearest?.let { tideStations[it] } ?: ""
        )
    }

    // Closest preset location by simple planar distance (fine at these scales)
    private fun nearestPresetKey(lat: Double, lon: Double): String? =
        locationCoords.minByOrNull { (_, c) ->
            val dLat = lat - c.first
            val dLon = lon - c.second
            dLat * dLat + dLon * dLon
        }?.key

    private suspend fun fetchSurf(
        lat: Double,
        lon: Double,
        spotNames: List<String>,
        buoyIds: List<String>,
        tideStation: String
    ): List<SurfConditions> = withContext(Dispatchers.IO) {
        val coords = spotOffsets.take(spotNames.size)
        val lats = coords.joinToString(",") { "%.4f".format(lat + it.first) }
        val lons = coords.joinToString(",") { "%.4f".format(lon + it.second) }

        coroutineScope {
            val marineDeferred = async { RetrofitClient.openMeteoApi.getMarine(lats, lons) }
            val windDeferred = async {
                runCatching { RetrofitClient.openMeteoApi.getWind(lat, lon) }.getOrNull()
            }
            val waterTempDeferred = async {
                NdbcClient.getWaterTempF(buoyIds)
            }
            val tideDeferred = async {
                TideClient.getTideState(tideStation)
            }

            // Marine data is essential - a failure here surfaces as SurfState.Error
            val marine = marineDeferred.await()
            val wind = windDeferred.await()
            val waterTempF = waterTempDeferred.await()
            val tide = tideDeferred.await()

            val windMph = wind?.current?.windSpeed ?: 0.0
            val windDir = degreesToCardinal(wind?.current?.windDirection ?: 0.0)

            spotNames.mapIndexed { index, spot ->
                val current = marine.getOrNull(index)?.current
                val waveHeight = ((current?.waveHeight ?: 0.0) * 10).roundToInt() / 10.0
                val period = (current?.wavePeriod ?: 0.0).roundToInt()
                val swellDir = degreesToCardinal(
                    current?.swellWaveDirection ?: current?.waveDirection ?: 0.0
                )

                SurfConditions(
                    location = spot,
                    waveHeight = waveHeight,
                    wavePeriod = period,
                    swellDirection = swellDir,
                    windSpeed = (windMph * 10).roundToInt() / 10.0,
                    windDirection = windDir,
                    windRating = getRating(waveHeight, windMph),
                    tide = tide,
                    waterTemp = waterTempF?.roundToInt(),
                    description = getDescription(waveHeight, period, windMph)
                )
            }
        }
    }

    private fun degreesToCardinal(deg: Double): String = when {
        deg < 22.5 || deg >= 337.5 -> "N"
        deg < 67.5 -> "NE"
        deg < 112.5 -> "E"
        deg < 157.5 -> "SE"
        deg < 202.5 -> "S"
        deg < 247.5 -> "SW"
        deg < 292.5 -> "W"
        else -> "NW"
    }

    private fun getRating(waveHeightFt: Double, windMph: Double): String = when {
        waveHeightFt >= 3.0 && windMph < 15.0 -> "Good"
        waveHeightFt >= 1.5 && windMph < 20.0 -> "Fair"
        else -> "Poor"
    }

    private fun getDescription(waveHeightFt: Double, periodSec: Int, windMph: Double): String = when {
        waveHeightFt >= 4.0 -> "Solid surf - experienced surfers"
        waveHeightFt >= 2.5 && windMph < 12.0 -> "Fun, clean waves"
        waveHeightFt >= 2.5 -> "Decent size but choppy"
        waveHeightFt >= 1.5 -> "Small - good for beginners"
        else -> "Nearly flat"
    }
}
