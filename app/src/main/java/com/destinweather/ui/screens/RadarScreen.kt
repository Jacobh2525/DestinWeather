package com.destinweather.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.destinweather.viewmodel.RadarFrame
import com.destinweather.viewmodel.RadarState
import com.destinweather.viewmodel.RadarViewModel
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet
import org.ramani.compose.*

private const val RADAR_COLOR_SCHEME = "2"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarScreen(
    viewModel: RadarViewModel = viewModel(),
    onLocationClick: () -> Unit = {}
) {
    val radarState by viewModel.radarState.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E88E5),
                        Color(0xFF1565C0),
                        Color(0xFF0D47A1)
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            RadarHeader(
                timestamp = if (radarState is RadarState.Success) {
                    viewModel.getCurrentTimestamp()
                } else "",
                isPlaying = if (radarState is RadarState.Success) {
                    (radarState as RadarState.Success).isPlaying
                } else false,
                onPlayPause = { viewModel.toggleAnimation() },
                onRefresh = { viewModel.fetchRadarData() },
                onLocationClick = onLocationClick
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1a1a2e))
            ) {
                when (val state = radarState) {
                    is RadarState.Loading -> {
                        RadarLoadingContent()
                    }
                    is RadarState.Success -> {
                        RadarMapContent(
                            frames = state.frames,
                            currentIndex = state.currentIndex,
                            isPlaying = state.isPlaying,
                            location = currentLocation,
                            onSeek = { viewModel.seekToFrame(it) }
                        )
                    }
                    is RadarState.Error -> {
                        RadarErrorContent(
                            message = state.message,
                            onRetry = { viewModel.fetchRadarData() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RadarMapContent(
    frames: List<RadarFrame>,
    currentIndex: Int,
    isPlaying: Boolean,
    location: Pair<Double, Double>,
    onSeek: (Int) -> Unit
) {
    val (lat, lon) = location

    val cameraPositionState = rememberCameraPositionState(
        CameraPosition(LatLng(lat, lon), 8.0)
    )

    LaunchedEffect(lat, lon) {
        cameraPositionState.position = CameraPosition(LatLng(lat, lon), 8.0)
    }

    // Build tile URL per frame (RainViewer radar only supports zoom 0-7;
    // above that they return an error IMAGE, so TileSet must cap maxZoom at 7.
    // 512px tiles = 2x sharper radar, matching MapLibre's default raster tileSize)
    fun radarUrl(frame: RadarFrame) =
        "https://tilecache.rainviewer.com${frame.path}/512/{z}/{x}/{y}/$RADAR_COLOR_SCHEME/1_1.png"

    // Track loaded style and which frame set is applied
    var loadedStyle by remember { mutableStateOf<Style?>(null) }
    var appliedFrameKey by remember { mutableStateOf<String?>(null) }
    var lastVisibleIndex by remember { mutableIntStateOf(-1) }

    val frameKey = frames.firstOrNull()?.path

    // Add one hidden source+layer per frame; recreate when frames refresh
    LaunchedEffect(frameKey, loadedStyle) {
        val style = loadedStyle ?: return@LaunchedEffect
        val key = frameKey ?: return@LaunchedEffect
        if (key == appliedFrameKey) return@LaunchedEffect

        // Remove previous radar layers/sources
        for (i in 0 until 20) {
            try { style.removeLayer("radar-layer-$i") } catch (_: Exception) {}
            try { style.removeSource("radar-source-$i") } catch (_: Exception) {}
        }

        frames.forEachIndexed { index, frame ->
            val tileSet = TileSet("2.2.0", radarUrl(frame))
            tileSet.maxZoom = 7f
            val source = RasterSource("radar-source-$index", tileSet)
            style.addSource(source)
            val layer = RasterLayer("radar-layer-$index", "radar-source-$index")
            layer.setProperties(
                PropertyFactory.rasterOpacity(0.8f),
                PropertyFactory.visibility(Property.NONE)
            )
            style.addLayer(layer)
        }

        appliedFrameKey = key
        lastVisibleIndex = -1
    }

    // Toggle visibility on frame change (instant - tiles stay cached per source)
    LaunchedEffect(currentIndex, appliedFrameKey) {
        val style = loadedStyle ?: return@LaunchedEffect
        if (appliedFrameKey == null) return@LaunchedEffect

        if (lastVisibleIndex >= 0 && lastVisibleIndex != currentIndex) {
            try {
                style.getLayer("radar-layer-$lastVisibleIndex")
                    ?.setProperties(PropertyFactory.visibility(Property.NONE))
            } catch (_: Exception) {}
        }
        try {
            style.getLayer("radar-layer-$currentIndex")
                ?.setProperties(PropertyFactory.visibility(Property.VISIBLE))
        } catch (_: Exception) {}
        lastVisibleIndex = currentIndex
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapLibre(
            modifier = Modifier.fillMaxSize(),
            style = MapStyle.Uri("https://basemaps.cartocdn.com/gl/dark-matter-gl-style/style.json"),
            cameraPositionState = cameraPositionState,
            uiSettings = UiSettings(
                zoomGesturesEnabled = true,
                scrollGesturesEnabled = true,
                rotateGesturesEnabled = false,
                tiltGesturesEnabled = false,
                isAttributionEnabled = false,
                isLogoEnabled = false
            ),
            properties = MapProperties(
                minZoom = 2.0,
                maxZoom = 12.0
            ),
            onStyleLoaded = { style ->
                loadedStyle = style
            }
        )

        RadarTimeline(
            frames = frames,
            currentIndex = currentIndex,
            isPlaying = isPlaying,
            onSeek = onSeek
        )
    }
}

@Composable
private fun RadarHeader(
    timestamp: String,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onRefresh: () -> Unit,
    onLocationClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Live Radar",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = if (timestamp.isNotEmpty()) "As of $timestamp" else "Loading...",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .background(
                        Color.White.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White
                )
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .background(
                        Color.White.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = Color.White
                )
            }

            IconButton(
                onClick = onLocationClick,
                modifier = Modifier
                    .background(
                        Color.White.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Change Location",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun RadarTimeline(
    frames: List<RadarFrame>,
    currentIndex: Int,
    isPlaying: Boolean,
    onSeek: (Int) -> Unit
) {
    val timeFormat = remember { java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()) }
    val lastPastIndex = frames.indexOfLast { !it.isNowcast }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(
                Color(0xFF1a1a2e).copy(alpha = 0.9f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Radar Timeline",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            if (isPlaying) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(3) { index ->
                        val delay = index * 150
                        val infiniteTransition = rememberInfiniteTransition(label = "dot_$index")
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(500, delayMillis = delay),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "alpha"
                        )

                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    Color(0xFF64B5F6).copy(alpha = alpha),
                                    shape = CircleShape
                                )
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Playing",
                        color = Color(0xFF64B5F6),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Frame indicators (past = blue, future/nowcast = purple)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            frames.forEachIndexed { index, frame ->
                val isCurrent = index == currentIndex
                val isPast = index < currentIndex
                val frameColor = if (frame.isNowcast) Color(0xFFCE93D8) else Color(0xFF64B5F6)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            when {
                                isCurrent -> frameColor
                                isPast -> frameColor.copy(alpha = 0.5f)
                                else -> Color.White.copy(alpha = 0.2f)
                            },
                            shape = RoundedCornerShape(2.dp)
                        )
                        .clickable { onSeek(index) }
                )

                if (index < frames.size - 1) {
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Per-frame time labels (every 3rd tick, plus "Now" and the final tick)
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            frames.forEachIndexed { index, frame ->
                Box(modifier = Modifier.weight(1f)) {
                    if (index == lastPastIndex) {
                        Text(
                            text = "Now",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    } else if (index % 3 == 0 || index == frames.lastIndex) {
                        Text(
                            text = timeFormat.format(java.util.Date(frame.time * 1000L)),
                            color = if (frame.isNowcast) Color(0xFFCE93D8)
                                    else Color.White.copy(alpha = 0.6f),
                            fontSize = 9.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RadarLoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = Color(0xFF64B5F6),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading radar...",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun RadarErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Unable to load radar",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF64B5F6)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Try Again")
        }
    }
}
