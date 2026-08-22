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
    private const val KEY_LAST_LOCATION_GPS = "last_location_gps"
    private const val KEY_NOTIFIED_ALERTS = "notified_alerts"
    private const val KEY_BRIEFING_ENABLED = "briefing_enabled"

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
        // Default dark: preserves the app's long-standing appearance for
        // existing installs that never touched the toggle
        get() = prefs.getBoolean(KEY_DARK_MODE, true)
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

    // True when the saved location came from GPS rather than a preset city
    var lastLocationGps: Boolean
        get() = prefs.getBoolean(KEY_LAST_LOCATION_GPS, false)
        set(value) = prefs.edit { putBoolean(KEY_LAST_LOCATION_GPS, value) }

    // Daily 7 AM briefing notification (opt-in)
    var briefingEnabled: Boolean
        get() = prefs.getBoolean(KEY_BRIEFING_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_BRIEFING_ENABLED, value) }

    // IDs of severe alerts already notified (dedup background worker)
    var notifiedAlertIds: Set<String>
        get() = prefs.getStringSet(KEY_NOTIFIED_ALERTS, emptySet()) ?: emptySet()
        set(value) = prefs.edit { putStringSet(KEY_NOTIFIED_ALERTS, value) }
}
