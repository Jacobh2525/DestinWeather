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
import java.util.Calendar
import kotlin.math.roundToInt

sealed class WeatherState {
    object Loading : WeatherState()
    data class Success(
        val weather: WeatherResponse,
        val forecast: ForecastResponse,
        val uvIndex: Int? = null,
        val dewPoint: Double? = null
    ) : WeatherState()
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
                val useFahrenheit = PreferencesManager.useFahrenheit
                val units = if (useFahrenheit) "imperial" else "metric"
                val weatherDeferred = async { RetrofitClient.weatherApi.getWeather(city = location, units = units) }
                val forecastDeferred = async { RetrofitClient.weatherApi.getForecast(city = location, units = units) }
                val weather = weatherDeferred.await()
                val forecast = forecastDeferred.await()

                // Real UV index + dew point from Open-Meteo (free, no key).
                // Best-effort: null degrades to "unavailable" in the UI.
                val extras = weather.coord?.let { coord ->
                    runCatching {
                        RetrofitClient.openMeteoApi.getCurrentExtras(
                            latitude = coord.lat,
                            longitude = coord.lon,
                            temperatureUnit = if (useFahrenheit) "fahrenheit" else "celsius"
                        )
                    }.getOrNull()
                }
                val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val uvIndex = extras?.hourly?.uvIndex?.getOrNull(currentHour)?.roundToInt()
                val dewPoint = extras?.current?.dewPoint

                _weatherState.value = WeatherState.Success(weather, forecast, uvIndex, dewPoint)
            } catch (e: Exception) {
                _weatherState.value = WeatherState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun setLocation(location: String) {
        fetchWeather(location)
    }
}
