package com.destinweather.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.destinweather.data.AfdFetcher
import com.destinweather.data.api.RetrofitClient
import com.destinweather.utils.NotificationHelper
import com.destinweather.utils.PreferencesManager
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Daily 7 AM briefing: current conditions + today's NWS forecast + an
 * excerpt of the NWS forecaster discussion. Opt-in via Settings.
 */
class BriefingWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Prefs may not be initialized if the process started just for this worker
        PreferencesManager.init(applicationContext)

        // Respect the toggle even if work somehow remains enqueued
        if (!PreferencesManager.briefingEnabled) return Result.success()

        val lat = PreferencesManager.lastLat
        val lon = PreferencesManager.lastLon
        val units = if (PreferencesManager.useFahrenheit) "imperial" else "metric"

        return try {
            coroutineScope {
                val weatherDeferred = async {
                    if (PreferencesManager.lastLocationGps) {
                        RetrofitClient.weatherApi.getWeatherByCoords(lat, lon, units)
                    } else {
                        RetrofitClient.weatherApi.getWeather(
                            city = PreferencesManager.lastLocation,
                            units = units
                        )
                    }
                }
                // NWS forecast + discussion are best-effort garnish
                val todayDeferred = async {
                    runCatching {
                        val point = RetrofitClient.noaaApi.getPointData(lat.toString(), lon.toString())
                        val p = point.properties
                        if (p?.gridId != null && p.gridX != null && p.gridY != null) {
                            RetrofitClient.noaaApi.getForecast(p.gridId, p.gridX, p.gridY)
                                .properties?.periods?.firstOrNull()
                        } else null
                    }.getOrNull()
                }
                val afdDeferred = async {
                    runCatching { AfdFetcher.fetchForLocation(lat, lon) }.getOrNull()
                }

                val weather = weatherDeferred.await()   // essential
                val today = todayDeferred.await()
                val afd = afdDeferred.await()

                val temp = weather.main.temp.roundToInt()
                val condition = weather.weather.firstOrNull()?.main ?: "Weather"
                val city = weather.cityName

                val title = "$temp° $condition — $city"
                val text = today?.shortForecast ?: "Tap for today's forecast"

                val bigText = buildString {
                    today?.detailedForecast?.let { append(it) }
                    if (!afd.isNullOrBlank()) {
                        if (isNotEmpty()) append("\n\n")
                        append("— Forecast Discussion (excerpt) —\n")
                        append(AfdFetcher.excerpt(afd))
                    }
                }.ifBlank { "Tap to open Destin Weather" }

                // Store the FULL briefing (entire discussion) for the tap-to-read popup
                val fullBody = buildString {
                    today?.detailedForecast?.let {
                        append("— Today's Forecast —\n\n")
                        append(it)
                    }
                    if (!afd.isNullOrBlank()) {
                        if (isNotEmpty()) append("\n\n")
                        append("— Forecast Discussion —\n\n")
                        append(afd.trim())
                    }
                }
                PreferencesManager.briefingTitle = title
                PreferencesManager.briefingBody = fullBody
                PreferencesManager.briefingTime = System.currentTimeMillis()

                NotificationHelper.showMorningBriefing(applicationContext, title, text, bigText)
                Result.success()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "morning_briefing"
        private const val BRIEFING_HOUR = 7

        fun schedule(
            context: Context,
            policy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.UPDATE
        ) {
            val now = System.currentTimeMillis()
            val next = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, BRIEFING_HOUR)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                policy,
                PeriodicWorkRequestBuilder<BriefingWorker>(1, TimeUnit.DAYS)
                    .setInitialDelay(next.timeInMillis - now, TimeUnit.MILLISECONDS)
                    .setConstraints(constraints)
                    .build()
            )
        }

        /** Immediate one-off run so the toggle can be tested without waiting for morning. */
        fun sendNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            WorkManager.getInstance(context).enqueue(
                OneTimeWorkRequestBuilder<BriefingWorker>()
                    .setConstraints(constraints)
                    .build()
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
