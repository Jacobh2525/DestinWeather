package com.destinweather.data.model

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
    val detailedForecast: String?
)
