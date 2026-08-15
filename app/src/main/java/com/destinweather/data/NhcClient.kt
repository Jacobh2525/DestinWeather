package com.destinweather.data

import com.destinweather.data.model.ActiveStormsResponse
import com.destinweather.data.model.NhcStorm
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * NOAA National Hurricane Center active-storm feed (free, no key).
 * A thin OkHttp+Gson client in the same style as [TideClient]/[NdbcClient].
 */
object NhcClient {

    private const val CURRENT_STORMS_URL = "https://www.nhc.noaa.gov/CurrentStorms.json"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    /**
     * Active tropical cyclones worldwide (Atlantic/East/Central Pacific).
     * Returns an empty list when the tropics are quiet; throws on failure
     * so the caller can surface an error state.
     */
    suspend fun getActiveStorms(): List<NhcStorm> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(CURRENT_STORMS_URL).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("NHC HTTP ${response.code}")
            val body = response.body?.string() ?: throw IOException("Empty NHC response")
            gson.fromJson(body, ActiveStormsResponse::class.java).activeStorms ?: emptyList()
        }
    }
}
