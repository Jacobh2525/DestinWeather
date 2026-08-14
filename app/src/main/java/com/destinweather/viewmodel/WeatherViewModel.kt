package com.destinweather.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.destinweather.data.api.RetrofitClient
import com.destinweather.data.model.ForecastResponse
import com.destinweather.data.model.WeatherResponse
import com.destinweather.utils.PreferencesManager
import kotlinx.coroutines.async
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

    private val _currentLocation = MutableStateFlow(PreferencesManager.lastLocation)
    val currentLocation: StateFlow<String> = _currentLocation

    init {
        fetchWeather()
    }

    fun fetchWeather(location: String = _currentLocation.value) {
        viewModelScope.launch {
            // Keep showing current content during pull-to-refresh
            if (_weatherState.value !is WeatherState.Success) {
                _weatherState.value = WeatherState.Loading
            }
            _currentLocation.value = location
            try {
                val units = if (PreferencesManager.useFahrenheit) "imperial" else "metric"
                val weatherDeferred = async { RetrofitClient.weatherApi.getWeather(city = location, units = units) }
                val forecastDeferred = async { RetrofitClient.weatherApi.getForecast(city = location, units = units) }
                _weatherState.value = WeatherState.Success(weatherDeferred.await(), forecastDeferred.await())
            } catch (e: Exception) {
                _weatherState.value = WeatherState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun setLocation(location: String) {
        fetchWeather(location)
    }
}
