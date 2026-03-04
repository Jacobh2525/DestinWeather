package com.destinweather.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.destinweather.data.api.RetrofitClient
import com.destinweather.data.model.ForecastResponse
import com.destinweather.data.model.WeatherResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WeatherViewModel : ViewModel() {

    private val _weatherState = MutableStateFlow<WeatherState>(WeatherState.Loading)
    val weatherState: StateFlow<WeatherState> = _weatherState

    init {
        fetchWeather()
    }

    fun fetchWeather() {
        viewModelScope.launch {
            _weatherState.value = WeatherState.Loading
            try {
                val weather = RetrofitClient.weatherApi.getWeather()
                val forecast = RetrofitClient.weatherApi.getForecast()
                _weatherState.value = WeatherState.Success(weather, forecast)
            } catch (e: Exception) {
                _weatherState.value = WeatherState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed class WeatherState {
    object Loading : WeatherState()
    data class Success(
        val weather: WeatherResponse,
        val forecast: ForecastResponse
    ) : WeatherState()
    data class Error(val message: String) : WeatherState()
}


