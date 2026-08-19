package com.destinweather.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.location.LocationListener
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Framework-only GPS access (no Play Services dependency).
 * Coarse accuracy is plenty for city-level weather.
 */
object GpsLocationHelper {

    fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Best-effort current position: recent cached fix first, then a single fresh
     * update with a 12s timeout. Returns null if denied/unavailable/timed out.
     */
    suspend fun getCurrentLocation(context: Context): Pair<Double, Double>? {
        if (!hasLocationPermission(context)) return null
        val lm = context.getSystemService(LocationManager::class.java) ?: return null

        // Fast path: a cached fix under 10 minutes old
        val now = System.currentTimeMillis()
        for (provider in listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)) {
            try {
                val cached = lm.getLastKnownLocation(provider)
                if (cached != null && now - cached.time < 10 * 60 * 1000L) {
                    return cached.latitude to cached.longitude
                }
            } catch (_: SecurityException) { }
        }

        val provider = when {
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> return null
        }

        // requestSingleUpdate delivers on the given looper's thread
        return withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine { cont ->
                var delivered = false
                val listener = LocationListener { loc ->
                    if (!delivered) {
                        delivered = true
                        cont.resume(loc.latitude to loc.longitude)
                    }
                }
                val handler = Handler(Looper.getMainLooper())
                val timeout = Runnable {
                    if (!delivered) {
                        delivered = true
                        lm.removeUpdates(listener)
                        cont.resume(null)
                    }
                }
                cont.invokeOnCancellation {
                    delivered = true
                    handler.removeCallbacks(timeout)
                    lm.removeUpdates(listener)
                }
                try {
                    lm.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                    handler.postDelayed(timeout, 12_000)
                } catch (_: SecurityException) {
                    if (!delivered) {
                        delivered = true
                        cont.resume(null)
                    }
                }
            }
        }
    }

    /** "City, State" for display, or null when geocoding is unavailable. */
    @Suppress("DEPRECATION") // sync variant works on all supported API levels
    suspend fun reverseGeocodeCity(context: Context, lat: Double, lon: Double): String? =
        withContext(Dispatchers.IO) {
            try {
                if (!Geocoder.isPresent()) return@withContext null
                val address = Geocoder(context, Locale.getDefault())
                    .getFromLocation(lat, lon, 1)
                    ?.firstOrNull() ?: return@withContext null
                val city = address.locality ?: address.subAdminArea
                val state = address.adminArea
                when {
                    city != null && state != null -> "$city, $state"
                    city != null -> city
                    else -> null
                }
            } catch (_: Exception) {
                null
            }
        }
}
