package com.destinweather.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.destinweather.data.SurfData
import com.destinweather.data.model.SurfConditions
import com.destinweather.utils.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class SurfState {
    object Loading : SurfState()
    data class Success(val surfList: List<SurfConditions>) : SurfState()
    data class Error(val message: String) : SurfState()
}

class SurfViewModel : ViewModel() {

    private val _surfState = MutableStateFlow<SurfState>(SurfState.Loading)
    val surfState: StateFlow<SurfState> = _surfState

    // GPS mode: fetch by coordinates instead of a preset location key
    private var gpsMode = PreferencesManager.lastLocationGps
    private var gpsLat = PreferencesManager.lastLat
    private var gpsLon = PreferencesManager.lastLon
    private var gpsCity: String? =
        if (gpsMode) PreferencesManager.lastLocation.substringBefore(",") else null

    init {
        fetchSurfData()
    }

    fun fetchSurfData(location: String = PreferencesManager.lastLocation.ifBlank { "Destin,US" }) {
        viewModelScope.launch {
            // Keep showing current content during pull-to-refresh
            if (_surfState.value !is SurfState.Success) {
                _surfState.value = SurfState.Loading
            }
            try {
                val surfList = if (gpsMode) {
                    SurfData.getSurfConditionsAt(gpsLat, gpsLon, gpsCity)
                } else {
                    SurfData.getSurfConditions(location)
                }
                _surfState.value = SurfState.Success(surfList)
            } catch (e: Exception) {
                _surfState.value = SurfState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun setLocation(location: String) {
        gpsMode = false
        fetchSurfData(location)
    }

    fun setGpsLocation(displayName: String, lat: Double, lon: Double) {
        gpsMode = true
        gpsLat = lat
        gpsLon = lon
        gpsCity = displayName.substringBefore(",")
        fetchSurfData()
    }
}
