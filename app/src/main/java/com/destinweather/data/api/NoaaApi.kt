package com.destinweather.data.api

import com.destinweather.data.model.AfdListResponse
import com.destinweather.data.model.AfdProductResponse
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

    @GET("alerts/active")
    suspend fun getActiveAlerts(
        @Query("point") point: String  // format: "lat,lon"
    ): NoaaAlertResponse

    // Area Forecast Discussion: list recent AFDs for a Weather Forecast Office
    @GET("products/types/AFD/locations/{wfo}")
    suspend fun getAfdList(
        @Path("wfo") wfo: String
    ): AfdListResponse

    // Fetch a single text product by id
    @GET("products/{id}")
    suspend fun getProduct(
        @Path("id") id: String
    ): AfdProductResponse
}

data class PointResponse(
    val properties: PointProperties?
)

data class PointProperties(
    val gridId: String?,
    val gridX: Int?,
    val gridY: Int?,
    val cwa: String?
)
