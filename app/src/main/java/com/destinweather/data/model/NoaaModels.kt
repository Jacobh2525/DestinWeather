package com.destinweather.data.model

import com.google.gson.annotations.SerializedName

data class NoaaForecastResponse(
    val properties: NoaaProperties?
)

data class NoaaProperties(
    val periods: List<NoaaPeriod>?
)

data class NoaaPeriod(
    val number: Int?,
    val name: String?,
    val temperature: Int?,
    val temperatureUnit: String?,
    val windSpeed: String?,
    val windDirection: String?,
    val shortForecast: String?,
    val detailedForecast: String?,
    val probabilityOfPrecipitation: NoaaValue?
)

data class NoaaValue(
    val value: Int?
)

// Area Forecast Discussion (NWS text product)
data class AfdListResponse(
    @SerializedName("@graph") val products: List<AfdProductRef>?
)

data class AfdProductRef(
    val id: String?,
    val issuanceTime: String?
)

data class AfdProductResponse(
    val productText: String?,
    val issuanceTime: String?
)
