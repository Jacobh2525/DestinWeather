package com.destinweather.workers

import android.content.Context
import androidx.work.*
import com.destinweather.data.api.RetrofitClient
import com.destinweather.utils.NotificationHelper
import com.destinweather.utils.PreferencesManager
import java.util.concurrent.TimeUnit

class AlertCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Prefs may not be initialized if the process started just for this worker
        PreferencesManager.init(applicationContext)

        // Respect the settings toggle even if the work somehow remains enqueued
        if (!PreferencesManager.notificationsEnabled) return Result.success()

        val lat = inputData.getDouble(KEY_LAT, 30.3935)
        val lon = inputData.getDouble(KEY_LON, -86.4958)

        return try {
            val response = RetrofitClient.noaaApi.getActiveAlerts("${lat},${lon}")
            val alerts = response.features?.mapNotNull { it.properties } ?: emptyList()

            // Find severe or extreme alerts
            val severeAlerts = alerts.filter {
                it.severity == "Extreme" || it.severity == "Severe"
            }

            // Only notify for alerts we haven't already notified about
            val alreadyNotified = PreferencesManager.notifiedAlertIds
            val newAlerts = severeAlerts.filter { (it.id ?: "") !in alreadyNotified }

            if (newAlerts.isNotEmpty()) {
                val worstAlert = newAlerts.first()
                NotificationHelper.showSevereAlertNotification(
                    context = applicationContext,
                    alertId = worstAlert.id ?: "alert",
                    event = worstAlert.event ?: "Weather Alert",
                    severity = worstAlert.severity ?: "Severe",
                    headline = worstAlert.headline
                )
                PreferencesManager.notifiedAlertIds =
                    alreadyNotified + newAlerts.mapNotNull { it.id }
            }

            Result.success()
        } catch (e: retrofit2.HttpException) {
            // 4xx responses are deterministic - retrying won't help
            if (e.code() in 400..499) Result.success() else Result.retry()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val KEY_LAT = "latitude"
        const val KEY_LON = "longitude"
        const val WORK_NAME = "alert_check_work"

        fun schedule(context: Context, lat: Double, lon: Double) {
            val inputData = Data.Builder()
                .putDouble(KEY_LAT, lat)
                .putDouble(KEY_LON, lon)
                .build()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<AlertCheckWorker>(
                1, TimeUnit.HOURS
            )
                .setInputData(inputData)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
