package com.destinweather.widget

import android.content.Context
import com.destinweather.data.api.RetrofitClient
import com.destinweather.utils.PreferencesManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class WidgetWeather(
    val locationLabel: String,
    val tempText: String,
    val condition: String,
    val highLow: String?,
    val updatedText: String
)

/**
 * Fetches current conditions for the widget using the same saved location
 * (preset city or GPS) and units as the app. Throws on failure so the
 * widget can render its error state.
 */
object WidgetWeatherFetcher {

    suspend fun fetch(context: Context): WidgetWeather {
        // Widget may run in a fresh process where prefs were never initialized
        PreferencesManager.init(context)

        val units = if (PreferencesManager.useFahrenheit) "imperial" else "metric"
        val weather = if (PreferencesManager.lastLocationGps) {
            RetrofitClient.weatherApi.getWeatherByCoords(
                PreferencesManager.lastLat,
                PreferencesManager.lastLon,
                units
            )
        } else {
            RetrofitClient.weatherApi.getWeather(
                city = PreferencesManager.lastLocation,
                units = units
            )
        }

        val high = weather.main.tempMax?.roundToInt()
        val low = weather.main.tempMin?.roundToInt()

        return WidgetWeather(
            locationLabel = "${weather.cityName}, ${weather.sys.country}",
            tempText = "${weather.main.temp.roundToInt()}°",
            condition = weather.weather.firstOrNull()?.description
                ?.replaceFirstChar { it.uppercase() } ?: "",
            highLow = if (high != null && low != null) "H $high°  L $low°" else null,
            updatedText = "Updated " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        )
    }
}
