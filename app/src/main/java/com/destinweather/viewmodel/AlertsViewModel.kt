package com.destinweather.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.destinweather.data.api.RetrofitClient
import com.destinweather.data.model.AlertProperties
import com.destinweather.utils.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AlertsState {
    object Loading : AlertsState()
    data class Success(val alerts: List<AlertProperties>) : AlertsState()
    data class Error(val message: String) : AlertsState()
    object Empty : AlertsState()
}

class AlertsViewModel : ViewModel() {

    private val _alertsState = MutableStateFlow<AlertsState>(AlertsState.Loading)
    val alertsState: StateFlow<AlertsState> = _alertsState

    private var currentLat = PreferencesManager.lastLat
    private var currentLon = PreferencesManager.lastLon

    init {
        fetchAlerts()
    }

    fun fetchAlerts(latitude: Double = currentLat, longitude: Double = currentLon) {
        viewModelScope.launch {
            // Keep showing current content during pull-to-refresh
            if (_alertsState.value !is AlertsState.Success) {
                _alertsState.value = AlertsState.Loading
            }
            try {
                currentLat = latitude
                currentLon = longitude

                val response = RetrofitClient.noaaApi.getActiveAlerts("${latitude},${longitude}")
                val alerts = response.features?.mapNotNull { it.properties } ?: emptyList()

                _alertsState.value = if (alerts.isEmpty()) {
                    AlertsState.Empty
                } else {
                    AlertsState.Success(alerts)
                }
            } catch (e: Exception) {
                _alertsState.value = AlertsState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun setLocation(latitude: Double, longitude: Double) {
        fetchAlerts(latitude, longitude)
    }
}
