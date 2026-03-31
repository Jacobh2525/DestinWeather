package com.destinweather.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object PreferencesManager {

    private const val PREFS_NAME = "destin_weather_prefs"

    private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    private const val KEY_USE_FAHRENHEIT = "use_fahrenheit"
    private const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_LAST_LOCATION = "last_location"
    private const val KEY_LAST_LAT = "last_lat"
    private const val KEY_LAST_LON = "last_lon"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_NOTIFICATIONS_ENABLED, value) }

    var useFahrenheit: Boolean
        get() = prefs.getBoolean(KEY_USE_FAHRENHEIT, true)
        set(value) = prefs.edit { putBoolean(KEY_USE_FAHRENHEIT, value) }

    var darkModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) = prefs.edit { putBoolean(KEY_DARK_MODE, value) }

    var lastLocation: String
        get() = prefs.getString(KEY_LAST_LOCATION, "Destin,US") ?: "Destin,US"
        set(value) = prefs.edit { putString(KEY_LAST_LOCATION, value) }

    var lastLat: Double
        get() = prefs.getFloat(KEY_LAST_LAT, 30.3935f).toDouble()
        set(value) = prefs.edit { putFloat(KEY_LAST_LAT, value.toFloat()) }

    var lastLon: Double
        get() = prefs.getFloat(KEY_LAST_LON, -86.4958f).toDouble()
        set(value) = prefs.edit { putFloat(KEY_LAST_LON, value.toFloat()) }
}
