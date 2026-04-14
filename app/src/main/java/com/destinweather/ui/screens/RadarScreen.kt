package com.destinweather.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.network.HttpException
import com.destinweather.utils.PreferencesManager
import com.destinweather.viewmodel.RadarFrame
import com.destinweather.viewmodel.RadarState
import com.destinweather.viewmodel.RadarViewModel
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarScreen(
    viewModel: RadarViewModel = viewModel(),
    onLocationClick: () -> Unit = {}
) {
    val radarState by viewModel.radarState.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()

    // Map state
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val scope = rememberCoroutineScope()

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
            // Header
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

            // Radar Map
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
                        RadarMap(
                            frames = state.frames,
                            currentIndex = state.currentIndex,
                            scale = scale,
                            offset = offset,
                            onScaleChange = { scale = it },
                            onOffsetChange = { offset = it },
                            location = currentLocation
                        )

                        // Frame slider
                        RadarTimeline(
                            frames = state.frames,
                            currentIndex = state.currentIndex,
                            isPlaying = state.isPlaying,
                            onSeek = { viewModel.seekToFrame(it) }
                        )

                        // Zoom controls
                        RadarZoomControls(
                            currentScale = scale,
                            onZoomIn = { scale = min(scale * 1.2f, 5f) },
                            onZoomOut = { scale = max(scale / 1.2f, 0.5f) },
                            onReset = {
                                scale = 1f
                                offset = Offset.Zero
                            }
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
private fun RadarMap(
    frames: List<RadarFrame>,
    currentIndex: Int,
    scale: Float,
    offset: Offset,
    onScaleChange: (Float) -> Unit,
    onOffsetChange: (Offset) -> Unit,
    location: Pair<Double, Double>
) {
    val currentFrame = frames.getOrNull(currentIndex)
    val scope = rememberCoroutineScope()

    // Load the radar image using Coil
    var radarBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    // Build the RainViewer tile URL
    val context = LocalContext.current
    val (lat, lon) = location
    val zoom = 6

    // Convert lat/lon to tile coordinates
    val (tileX, tileY) = remember(lat, lon, zoom) {
        latLonToTileXY(lat, lon, zoom)
    }

    // Build image URL
    val imageUrl = currentFrame?.let { frame ->
        "https://tilecache.rainviewer.com/v2/radar/${frame.timestamp}/256/$zoom/$tileX/$tileY/2/1_1.png"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoomChange, _ ->
                    onScaleChange(max(0.5f, min(5f, scale * zoomChange)))
                    onOffsetChange(offset + pan)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onOffsetChange(offset + dragAmount)
                }
            }
    ) {
        // Base map (CartoDB Positron - free for commercial use, no API key needed)
        // Alternative free tile providers that work without blocking:
        // - CartoDB Voyager: https://basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}.png
        // - CartoDB Positron: https://basemaps.cartocdn.com/rastertiles/light_all/{z}/{x}/{y}.png
        // - CartoDB Dark Matter: https://basemaps.cartocdn.com/rastertiles/dark_all/{z}/{x}/{y}.png
        val mapUrl = "https://basemaps.cartocdn.com/rastertiles/voyager/$zoom/$tileX/$tileY.png"
        
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(mapUrl)
                .crossfade(true)
                .headers(
                    okhttp3.Headers.Builder()
                        .add("User-Agent", "DestinWeather/1.0 (destinweather@app.com)")
                        .build()
                )
                .build(),
            contentDescription = "Base map",
            modifier = Modifier.fillMaxSize(),
            onError = { 
                // If this fails, we still have the radar overlay
            }
        )

        // Radar overlay
        imageUrl?.let { url ->
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = "Radar overlay",
                modifier = Modifier.fillMaxSize(),
                alpha = 0.8f
            )
        }

        // Location marker
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            LocationMarker()
        }
    }
}

@Composable
private fun LocationMarker() {
    Box(
        modifier = Modifier
            .size(20.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulse ring
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )

        Box(
            modifier = Modifier
                .size(20.dp * scale)
                .background(
                    Color(0xFF64B5F6).copy(alpha = 0.3f),
                    shape = CircleShape
                )
        )

        // Center dot
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(Color(0xFF64B5F6), shape = CircleShape)
        )

        // Inner dot
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(Color.White, shape = CircleShape)
        )
    }
}

@Composable
private fun RadarTimeline(
    frames: List<RadarFrame>,
    currentIndex: Int,
    isPlaying: Boolean,
    onSeek: (Int) -> Unit
) {
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
                // Playing indicator
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

        // Frame indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            frames.forEachIndexed { index, frame ->
                val isCurrent = index == currentIndex
                val isPast = index < currentIndex

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            when {
                                isCurrent -> Color(0xFF64B5F6)
                                isPast -> Color(0xFF64B5F6).copy(alpha = 0.5f)
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

        // Time labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "-2h",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp
            )
            Text(
                text = "Now",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun RadarZoomControls(
    currentScale: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Zoom In
        IconButton(
            onClick = onZoomIn,
            modifier = Modifier
                .size(44.dp)
                .background(
                    Color(0xFF1a1a2e).copy(alpha = 0.9f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Zoom In",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        // Zoom Out
        IconButton(
            onClick = onZoomOut,
            modifier = Modifier
                .size(44.dp)
                .background(
                    Color(0xFF1a1a2e).copy(alpha = 0.9f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Zoom Out",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        // Reset
        IconButton(
            onClick = onReset,
            modifier = Modifier
                .size(44.dp)
                .background(
                    Color(0xFF1a1a2e).copy(alpha = 0.9f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Reset View",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
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
