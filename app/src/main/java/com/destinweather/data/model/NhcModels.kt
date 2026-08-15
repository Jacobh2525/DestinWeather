package com.destinweather.data.model

import com.google.gson.annotations.SerializedName

// NHC CurrentStorms.json (https://www.nhc.noaa.gov/CurrentStorms.json)
// Returns an empty activeStorms list when the tropics are quiet.
data class ActiveStormsResponse(
    @SerializedName("activeStorms") val activeStorms: List<NhcStorm>?
)

data class NhcStorm(
    @SerializedName("id") val id: String?,                    // e.g. "al092026" (al=Atlantic, ep=E Pacific, cp=C Pacific)
    @SerializedName("name") val name: String?,
    @SerializedName("classification") val classification: String?,  // TD, TS, HU, EX, PTC, LO, DB, ST, SD, WV
    @SerializedName("intensity") val intensityKt: String?,          // sustained winds, knots, as string
    @SerializedName("pressure") val pressureMb: String?,            // central pressure, mb
    @SerializedName("latitudeNumeric") val latitude: Double?,
    @SerializedName("longitudeNumeric") val longitude: Double?,
    @SerializedName("movementDir") val movementDir: Int?,           // degrees
    @SerializedName("movementSpeed") val movementSpeedKt: Int?,     // knots
    @SerializedName("lastUpdate") val lastUpdate: String?           // ISO-8601 UTC
)
