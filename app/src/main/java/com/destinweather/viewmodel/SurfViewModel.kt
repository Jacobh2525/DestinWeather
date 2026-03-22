package com.destinweather.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.destinweather.data.SurfData
import com.destinweather.data.model.SurfConditions
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

    init {
        fetchSurfData()
    }

    fun fetchSurfData(location: String = "Destin") {
        viewModelScope.launch {
            _surfState.value = SurfState.Loading
            try {
                val surfList = SurfData.getSurfConditions(location)
                _surfState.value = SurfState.Success(surfList)
            } catch (e: Exception) {
                _surfState.value = SurfState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun setLocation(location: String) {
        fetchSurfData(location)
    }
}
