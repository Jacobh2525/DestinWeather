package com.destinweather.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.destinweather.data.api.RetrofitClient
import com.destinweather.data.model.NoaaPeriod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class NoaaState {
    object Loading : NoaaState()
    data class Success(val periods: List<NoaaPeriod>) : NoaaState()
    data class Error(val message: String) : NoaaState()
}

class NoaaViewModel : ViewModel() {

    private val _forecastState = MutableStateFlow<NoaaState>(NoaaState.Loading)
    val forecastState: StateFlow<NoaaState> = _forecastState

    private var currentLat = 30.3935  // Default: Destin
    private var currentLon = -86.4958

    init {
        fetchNoaaForecast()
    }

    fun fetchNoaaForecast(latitude: Double = currentLat, longitude: Double = currentLon) {
        viewModelScope.launch {
            _forecastState.value = NoaaState.Loading
            try {
                currentLat = latitude
                currentLon = longitude

                val pointResponse = RetrofitClient.noaaApi.getPointData(
                    latitude.toString(),
                    longitude.toString()
                )

                val gridId = pointResponse.properties?.gridId
                val gridX = pointResponse.properties?.gridX
                val gridY = pointResponse.properties?.gridY

                if (gridId != null && gridX != null && gridY != null) {
                    val forecast = RetrofitClient.noaaApi.getForecast(gridId, gridX, gridY)
                    val periods = forecast.properties?.periods ?: emptyList()
                    _forecastState.value = NoaaState.Success(periods)
                } else {
                    _forecastState.value = NoaaState.Error("Could not get forecast grid")
                }
            } catch (e: Exception) {
                _forecastState.value = NoaaState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun setLocation(lat: Double, lon: Double) {
        fetchNoaaForecast(lat, lon)
    }
}
