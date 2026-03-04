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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import coil.compose.AsyncImage

// List of Destin/Okaloosa area beach cams (YouTube live stream IDs)
val beachCams = listOf(
    BeachCam(
        name = "Dune Allen Beach Cam",
        location = "Dune Allen Beach 30a",
        videoId = "xNZBPxx8ykg"
    ),
    BeachCam(
        name = "Mid-Bay Bridge Cam",
        location = "Lulu's Destin",
        videoId = "SLnToeuwLhA"
    ),
    BeachCam(
        name = "Inlet Reef Beach Cam",
        location = "Destin, Florida",
        videoId = "DbpGwxabykI"
    ),
    BeachCam(
        name = "Wyndham Garden Beach Cam",
        location = "Ft. Walton Beach, Florida",
        videoId = "QnvxSpZ9HPI"
    )
)

data class BeachCam(
    val name: String,
    val location: String,
    val videoId: String
)

@Composable
fun BeachCamsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1a1a2e))
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Beach Cams",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Text(
            text = "Destin, FL - Tap to watch live",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // List of beach cams
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(beachCams) { cam ->
                BeachCamCard(cam)
            }
        }
    }
}

@Composable
fun BeachCamCard(cam: BeachCam) {
    val context = LocalContext.current

    // YouTube thumbnail URL
    val thumbnailUrl = "https://img.youtube.com/vi/${cam.videoId}/maxresdefault.jpg"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // Open YouTube app when tapped
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${cam.videoId}"))
                context.startActivity(intent)
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
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
                        color = Color(0xFF1a1a2e)
                    )
                    Text(
                        text = cam.location,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                // Live indicator
                Surface(
                    color = Color.Red,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "LIVE",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Thumbnail with play button overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = "${cam.name} thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Play button overlay
                Surface(
                    modifier = Modifier.size(60.dp),
                    shape = CircleShape,
                    color = Color.Red.copy(alpha = 0.9f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxSize(),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tap hint
            Text(
                text = "Tap to open in YouTube",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}
