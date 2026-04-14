package com.destinweather.viewmodel

import android.graphics.Bitmap
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
    val timestamp: Int,
    val bitmap: Bitmap? = null,
    val isLoaded: Boolean = false
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
    private val animationDelayMs = 500L // 500ms between frames

    init {
        fetchRadarData()
    }

    fun fetchRadarData() {
        viewModelScope.launch {
            _radarState.value = RadarState.Loading

            try {
                // Fetch available timestamps from RainViewer
                val timestampsResult = repository.fetchRadarTimestamps()

                timestampsResult.fold(
                    onSuccess = { timestamps ->
                        if (timestamps.isEmpty()) {
                            _radarState.value = RadarState.Error("No radar data available")
                            return@launch
                        }

                        // Create frames from timestamps (use last 10 frames)
                        val recentTimestamps = timestamps.takeLast(10)
                        val frames = recentTimestamps.map { RadarFrame(timestamp = it) }

                        _radarState.value = RadarState.Success(
                            frames = frames,
                            currentIndex = frames.size - 1,
                            isPlaying = false
                        )

                        // Preload the current frame
                        loadRadarFrame(frames.size - 1)
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

    private fun loadRadarFrame(index: Int) {
        val currentState = _radarState.value
        if (currentState !is RadarState.Success) return

        val frames = currentState.frames.toMutableList()
        if (index >= frames.size || frames[index].isLoaded) return

        viewModelScope.launch {
            val (lat, lon) = _currentLocation.value
            val timestamp = frames[index].timestamp

            // Calculate tile coordinates for current zoom level (let's use zoom level 6)
            val zoom = 6
            val (x, y) = latLonToTileXY(lat, lon, zoom)

            val result = repository.fetchRainViewerTile(zoom, x, y, timestamp)

            result.fold(
                onSuccess = { bitmap ->
                    frames[index] = frames[index].copy(
                        bitmap = bitmap,
                        isLoaded = true
                    )
                    _radarState.value = currentState.copy(frames = frames)
                },
                onFailure = { /* Silently fail - image will be null */ }
            )
        }
    }

    fun setLocation(lat: Double, lon: Double) {
        _currentLocation.value = Pair(lat, lon)
        // Reload radar data for new location
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
                _radarState.value = currentState.copy(currentIndex = index)

                // Preload next frame if not loaded
                loadRadarFrame((index + 1) % currentState.frames.size)
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

        // Load this frame if not already loaded
        loadRadarFrame(clampedIndex)
    }

    fun getCurrentTimestamp(): String {
        val currentState = _radarState.value
        if (currentState !is RadarState.Success) return ""

        val timestamp = currentState.frames.getOrNull(currentState.currentIndex)?.timestamp ?: return ""
        return formatTimestamp(timestamp)
    }

    private fun formatTimestamp(timestamp: Int): String {
        val date = java.util.Date(timestamp * 1000L)
        val formatter = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        return formatter.format(date)
    }

    /**
     * Convert latitude/longitude to tile coordinates
     */
    private fun latLonToTileXY(lat: Double, lon: Double, zoom: Int): Pair<Int, Int> {
        val n = 1 shl zoom
        val x = ((lon + 180.0) / 360.0 * n).toInt()
        val latRad = Math.toRadians(lat)
        val y = ((1.0 - kotlin.math.ln(kotlin.math.tan(latRad) + 1 / kotlin.math.cos(latRad)) / kotlin.math.PI) / 2.0 * n).toInt()
        return Pair(x, y)
    }

    override fun onCleared() {
        super.onCleared()
        stopAnimation()
    }
}
