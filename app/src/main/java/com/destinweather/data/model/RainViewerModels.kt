package com.destinweather.data.model

import com.google.gson.annotations.SerializedName

data class RainViewerResponse(
    @SerializedName("radar") val radar: RadarData,
    @SerializedName("satellite") val satellite: SatelliteData? = null
)

data class RadarData(
    @SerializedName("past") val past: List<RadarFrameData>,
    @SerializedName("nowcast") val nowcast: List<RadarFrameData> = emptyList()
)

data class RadarFrameData(
    @SerializedName("time") val time: Int,
    @SerializedName("path") val path: String
)

data class SatelliteData(
    @SerializedName("infrared") val infrared: List<RadarFrameData> = emptyList()
)
