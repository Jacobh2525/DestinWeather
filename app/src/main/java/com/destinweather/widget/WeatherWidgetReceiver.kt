package com.destinweather.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.destinweather.workers.WidgetUpdateWorker

class WeatherWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = WeatherWidget()

    // First widget placed: start the periodic refresh
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetUpdateWorker.schedule(context)
    }

    // Last widget removed: stop the periodic refresh
    override fun onDisabled(context: Context) {
        WidgetUpdateWorker.cancel(context)
        super.onDisabled(context)
    }
}
