package com.destinweather.data.api

import com.destinweather.data.model.NoaaAlertResponse
import com.destinweather.data.model.NoaaForecastResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface NoaaApi {

    @GET("points/{latitude},{longitude}")
    suspend fun getPointData(
        @Path("latitude") latitude: String,
        @Path("longitude") longitude: String
    ): PointResponse

    @GET("gridpoints/{wfo}/{x},{y}/forecast")
    suspend fun getForecast(
        @Path("wfo") wfo: String,
        @Path("x") x: Int,
        @Path("y") y: Int
    ): NoaaForecastResponse

    // NEW: Get active alerts
    @GET("alerts/active")
    suspend fun getActiveAlerts(
        @Query("point") point: String  // format: "lat,lon"
    ): NoaaAlertResponse
}

data class PointResponse(
    val properties: PointProperties?
)

data class PointProperties(
    val gridId: String?,
    val gridX: Int?,
    val gridY: Int?
)
