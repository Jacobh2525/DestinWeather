package com.destinweather.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * NOAA NDBC buoy observations (free, no key).
 * Water temp comes from the realtime2 text feed; stations frequently report
 * "MM" (missing) so callers pass a fallback chain of nearby buoys.
 */
object NdbcClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /** Water temp in Fahrenheit from the first station with a valid reading, else null. */
    suspend fun getWaterTempF(stationIds: List<String>): Double? = withContext(Dispatchers.IO) {
        for (id in stationIds) {
            try {
                val request = Request.Builder()
                    .url("https://www.ndbc.noaa.gov/data/realtime2/$id.txt")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val body = response.body?.string() ?: return@use
                    val dataLine = body.lines()
                        .firstOrNull { it.isNotBlank() && !it.startsWith("#") }
                        ?: return@use
                    // Columns: YY MM DD hh mm WDIR WSPD GST WVHT DPD APD MWD PRES ATMP WTMP ...
                    val parts = dataLine.trim().split(Regex("\\s+"))
                    if (parts.size > 14 && parts[14] != "MM") {
                        val celsius = parts[14].toDoubleOrNull()
                        if (celsius != null && celsius > -50 && celsius < 50) {
                            return@withContext celsius * 9.0 / 5.0 + 32.0
                        }
                    }
                }
            } catch (_: Exception) { /* try next station */ }
        }
        null
    }
}
