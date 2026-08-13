package com.destinweather.data

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * NOAA CO-OPS tide predictions (free, no key).
 * Returns a human string like "Falling · Low 10:19 PM" from hi/lo predictions.
 */
object TideClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val eventTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val displayTimeFormat = DateTimeFormatter.ofPattern("h:mm a")

    private data class TidePredictions(
        @SerializedName("predictions") val predictions: List<TideEvent>?
    )

    private data class TideEvent(
        @SerializedName("t") val time: String,
        @SerializedName("type") val type: String  // "H" or "L"
    )

    /** Tide state string for the given CO-OPS station, or null if unavailable. */
    suspend fun getTideState(stationId: String): String? = withContext(Dispatchers.IO) {
        if (stationId.isBlank()) return@withContext null
        try {
            val today = LocalDate.now()
            val fmt = DateTimeFormatter.ofPattern("yyyyMMdd")
            val url = "https://api.tidesandcurrents.noaa.gov/api/prod/datagetter" +
                "?product=predictions&application=DestinWeather" +
                "&begin_date=${today.minusDays(1).format(fmt)}" +
                "&end_date=${today.plusDays(1).format(fmt)}" +
                "&datum=MLLW&station=$stationId&time_zone=lst_ldt" +
                "&units=english&interval=hilo&format=json"

            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val events = gson.fromJson(body, TidePredictions::class.java)
                    .predictions
                    ?.mapNotNull { e ->
                        runCatching {
                            LocalDateTime.parse(e.time, eventTimeFormat) to e.type
                        }.getOrNull()
                    }
                    ?.sortedBy { it.first }
                    ?: return@withContext null

                val now = LocalDateTime.now()
                val last = events.lastOrNull { it.first <= now } ?: return@withContext null
                val next = events.firstOrNull { it.first > now } ?: return@withContext null

                val state = if (last.second == "H") "Falling" else "Rising"
                val nextName = if (next.second == "H") "High" else "Low"
                "$state · $nextName ${next.first.format(displayTimeFormat)}"
            }
        } catch (_: Exception) {
            null
        }
    }
}
