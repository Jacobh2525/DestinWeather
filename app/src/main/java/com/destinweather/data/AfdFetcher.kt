package com.destinweather.data

import com.destinweather.data.api.RetrofitClient

/**
 * Shared fetcher for the latest NWS Area Forecast Discussion,
 * used by the Forecast tab and the morning briefing worker.
 */
object AfdFetcher {

    /** Latest AFD text issued by a Weather Forecast Office (e.g. "MOB"), or null. */
    suspend fun fetchLatestText(cwa: String?): String? {
        if (cwa.isNullOrBlank()) return null
        val list = RetrofitClient.noaaApi.getAfdList(cwa)
        val latestId = list.products?.firstOrNull()?.id ?: return null
        return RetrofitClient.noaaApi.getProduct(latestId).productText
    }

    /** Resolve the responsible WFO from coordinates, then fetch its latest AFD. */
    suspend fun fetchForLocation(lat: Double, lon: Double): String? {
        val point = RetrofitClient.noaaApi.getPointData(lat.toString(), lon.toString())
        return fetchLatestText(point.properties?.cwa)
    }

    /**
     * A short readable excerpt for notifications: strips the WMO/product header
     * lines and returns the opening of the discussion body.
     */
    fun excerpt(fullText: String, maxLen: Int = 320): String {
        val body = fullText.lines()
            .dropWhile { line ->
                val t = line.trim()
                t.isEmpty() ||
                    t.startsWith("0") ||                       // "000" separator
                    Regex("^[A-Z]{4}\\d{2}").containsMatchIn(t) || // WMO header e.g. FXUS64
                    Regex("^[A-Z]{3,8}$").matches(t) ||          // product id line e.g. AFDMOB
                    t.startsWith("...") ||                       // "...New DISCUSSION..." marker
                    t.contains("Area Forecast Discussion", ignoreCase = true) ||
                    t.startsWith("National Weather Service", ignoreCase = true) ||
                    Regex("^\\d{3,4} [AP]M").containsMatchIn(t)    // issuance time line
            }
            .joinToString("\n")
            .trim()
        return if (body.length > maxLen) body.take(maxLen).trimEnd() + "…" else body
    }
}
