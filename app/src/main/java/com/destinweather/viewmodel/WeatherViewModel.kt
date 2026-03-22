package com.destinweather.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.destinweather.data.api.RetrofitClient
import com.destinweather.data.model.ForecastResponse
import com.destinweather.data.model.WeatherResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class WeatherState {
    object Loading : WeatherState()
    data class Success(val weather: WeatherResponse, val forecast: ForecastResponse) : WeatherState()
    data class Error(val message: String) : WeatherState()
}

class WeatherViewModel : ViewModel() {

    private val _weatherState = MutableStateFlow<WeatherState>(WeatherState.Loading)
    val weatherState: StateFlow<WeatherState> = _weatherState

    private val _currentLocation = MutableStateFlow("Destin,US")
    val currentLocation: StateFlow<String> = _currentLocation

    init {
        fetchWeather()
    }

    fun fetchWeather(location: String = _currentLocation.value) {
        viewModelScope.launch {
            _weatherState.value = WeatherState.Loading
            _currentLocation.value = location
            try {
                val weather = RetrofitClient.weatherApi.getWeather(city = location)
                val forecast = RetrofitClient.weatherApi.getForecast(city = location)
                _weatherState.value = WeatherState.Success(weather, forecast)
            } catch (e: Exception) {
                _weatherState.value = WeatherState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun setLocation(location: String) {
        fetchWeather(location)
    }
}



