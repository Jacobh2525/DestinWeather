package com.destinweather.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.glance.appwidget.updateAll
import com.destinweather.widget.WeatherWidget
import java.util.concurrent.TimeUnit

/** Periodic home-screen widget refresh; runs only while a widget is placed. */
class WidgetUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        WeatherWidget().updateAll(applicationContext)
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }

    companion object {
        const val WORK_NAME = "weather_widget_update"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<WidgetUpdateWorker>(30, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build()
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
