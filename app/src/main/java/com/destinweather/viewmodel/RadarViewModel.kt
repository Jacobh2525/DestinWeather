package com.destinweather.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.destinweather.data.repository.RadarRepository
import com.destinweather.utils.PreferencesManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed class RadarState {
    object Loading : RadarState()
    data class Success(
        val frames: List<RadarFrame>,
        val currentIndex: Int = 0,
        val isPlaying: Boolean = false
    ) : RadarState()
    data class Error(val message: String) : RadarState()
}

data class RadarFrame(
    val time: Int,
    val path: String,
    val isNowcast: Boolean = false
)

class RadarViewModel : ViewModel() {

    private val repository = RadarRepository()

    private val _radarState = MutableStateFlow<RadarState>(RadarState.Loading)
    val radarState: StateFlow<RadarState> = _radarState

    private val _currentLocation = MutableStateFlow(
        Pair(PreferencesManager.lastLat, PreferencesManager.lastLon)
    )
    val currentLocation: StateFlow<Pair<Double, Double>> = _currentLocation

    private var animationJob: Job? = null
    private val animationDelayMs = 500L

    init {
        fetchRadarData()
    }

    fun fetchRadarData() {
        viewModelScope.launch {
            // Keep showing current content during refresh
            if (_radarState.value !is RadarState.Success) {
                _radarState.value = RadarState.Loading
            }

            try {
                val result = repository.fetchRadarTimestamps()

                result.fold(
                    onSuccess = { frames ->
                        if (frames.isEmpty()) {
                            _radarState.value = RadarState.Error("No radar data available")
                            return@launch
                        }

                        // Start at "Now" (last past frame), not the future frames
                        val nowIndex = frames.indexOfLast { !it.isNowcast }.coerceAtLeast(0)

                        _radarState.value = RadarState.Success(
                            frames = frames,
                            currentIndex = nowIndex,
                            isPlaying = false
                        )
                    },
                    onFailure = { error ->
                        _radarState.value = RadarState.Error(error.message ?: "Failed to load radar data")
                    }
                )
            } catch (e: Exception) {
                _radarState.value = RadarState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun setLocation(lat: Double, lon: Double) {
        _currentLocation.value = Pair(lat, lon)
        fetchRadarData()
    }

    fun toggleAnimation() {
        val currentState = _radarState.value
        if (currentState !is RadarState.Success) return

        if (currentState.isPlaying) {
            stopAnimation()
        } else {
            startAnimation()
        }
    }

    private fun startAnimation() {
        val currentState = _radarState.value
        if (currentState !is RadarState.Success) return

        _radarState.value = currentState.copy(isPlaying = true)

        animationJob?.cancel()
        animationJob = viewModelScope.launch {
            var index = currentState.currentIndex

            while (isActive) {
                delay(animationDelayMs)
                index = (index + 1) % currentState.frames.size
                _radarState.value = _radarState.value.let {
                    if (it is RadarState.Success) it.copy(currentIndex = index) else it
                }
            }
        }
    }

    private fun stopAnimation() {
        animationJob?.cancel()
        animationJob = null

        val currentState = _radarState.value
        if (currentState is RadarState.Success) {
            _radarState.value = currentState.copy(isPlaying = false)
        }
    }

    fun seekToFrame(index: Int) {
        val currentState = _radarState.value
        if (currentState !is RadarState.Success) return

        val clampedIndex = index.coerceIn(0, currentState.frames.size - 1)
        _radarState.value = currentState.copy(currentIndex = clampedIndex)
    }

    fun getCurrentTimestamp(): String {
        val currentState = _radarState.value
        if (currentState !is RadarState.Success) return ""

        val timestamp = currentState.frames.getOrNull(currentState.currentIndex)?.time ?: return ""
        return formatTimestamp(timestamp)
    }

    private fun formatTimestamp(timestamp: Int): String {
        val date = java.util.Date(timestamp * 1000L)
        val formatter = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        return formatter.format(date)
    }

    override fun onCleared() {
        super.onCleared()
        stopAnimation()
    }
}
