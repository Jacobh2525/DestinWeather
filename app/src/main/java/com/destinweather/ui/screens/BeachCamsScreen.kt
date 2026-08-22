package com.destinweather.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.destinweather.data.BeachCam
import com.destinweather.data.beachCams
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

@Composable
fun BeachCamsScreen() {
    // One active player at a time; cams that fail (dead ID or dead thumbnail) get flagged
    var playingCamId by remember { mutableStateOf<String?>(null) }
    val offlineCams = remember { mutableStateMapOf<String, Boolean>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Beach Cams",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Destin, FL - tap a cam to watch live",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(beachCams) { cam ->
                BeachCamCard(
                    cam = cam,
                    isPlaying = playingCamId == cam.videoId,
                    isOffline = offlineCams[cam.videoId] == true,
                    onThumbnailError = { offlineCams[cam.videoId] = true },
                    onPlay = { playingCamId = cam.videoId },
                    onClose = { playingCamId = null },
                    onPlayerError = {
                        offlineCams[cam.videoId] = true
                        playingCamId = null
                    }
                )
            }
        }
    }
}

@Composable
fun BeachCamCard(
    cam: BeachCam,
    isPlaying: Boolean,
    isOffline: Boolean,
    onThumbnailError: () -> Unit,
    onPlay: () -> Unit,
    onClose: () -> Unit,
    onPlayerError: () -> Unit
) {
    val context = LocalContext.current
    val watchUrl = "https://www.youtube.com/watch?v=${cam.videoId}"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Location header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cam.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = cam.location,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Surface(
                    color = if (isOffline) Color.Gray else Color.Red,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (isOffline) "OFFLINE" else "LIVE",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Player / thumbnail area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                when {
                    // Inline live player
                    isPlaying && !isOffline -> {
                        InlinePlayer(videoId = cam.videoId, onPlayerError = onPlayerError)

                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(32.dp)
                                .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close player",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Thumbnail (dimmed if flagged offline)
                    else -> {
                        AsyncImage(
                            model = "https://img.youtube.com/vi/${cam.videoId}/maxresdefault.jpg",
                            contentDescription = "${cam.name} thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            onError = { onThumbnailError() }
                        )

                        if (isOffline) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.45f))
                            )
                        } else {
                            // Play button overlay
                            Surface(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clickable(onClick = onPlay),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxSize(),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Status hint / YouTube fallback
            when {
                isPlaying && !isOffline -> Text(
                    text = "Playing live",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                isOffline -> Text(
                    text = "This cam appears offline - tap to try YouTube",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(watchUrl)))
                        },
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                else -> Text(
                    text = "Tap to watch live",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/** Embedded YouTube live player; plays at the live edge once ready. */
@Composable
private fun InlinePlayer(videoId: String, onPlayerError: () -> Unit) {
    AndroidView(
        factory = { ctx ->
            YouTubePlayerView(ctx).apply {
                addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        youTubePlayer.loadVideo(videoId, 0f)
                    }

                    override fun onError(
                        youTubePlayer: YouTubePlayer,
                        error: PlayerConstants.PlayerError
                    ) {
                        onPlayerError()
                    }
                })
            }
        },
        onRelease = { it.release() },
        modifier = Modifier.fillMaxSize()
    )
}
