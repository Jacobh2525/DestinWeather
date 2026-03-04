package com.destinweather.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.destinweather.data.SurfData
import com.destinweather.data.model.SurfConditions
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurfScreen() {
    var isRefreshing by remember { mutableStateOf(false) }
    var surfList by remember { mutableStateOf<List<SurfConditions>>(emptyList()) }
    val pullToRefreshState = rememberPullToRefreshState()

    // Initial load
    LaunchedEffect(Unit) {
        surfList = SurfData.getSurfConditions()
    }

    // Handle pull to refresh
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            surfList = SurfData.getSurfConditions()
            delay(1000) // Simulate network delay
            isRefreshing = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF00ACC1),
                        Color(0xFF26C6DA)
                    )
                )
            )
    ) {
        PullToRefreshBox(
            state = pullToRefreshState,
            isRefreshing = isRefreshing,
            onRefresh = { isRefreshing = true }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Text(
                    text = "Surf Conditions",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "Destin, FL",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Surf Spots List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(surfList) { surf ->
                        SurfCard(surf)
                    }
                }
            }
        }
    }
}

@Composable
fun SurfCard(surf: SurfConditions) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = surf.location,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF006064)
                )
                Surface(
                    color = when (surf.windRating) {
                        "Good" -> Color(0xFF4CAF50)
                        "Fair" -> Color(0xFFFFA000)
                        else -> Color(0xFFF44336)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = surf.windRating,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SurfStat(
                    icon = Icons.Default.Waves,
                    label = "Waves",
                    value = "${surf.waveHeight} ft"
                )
                SurfStat(
                    icon = Icons.Default.Timer,
                    label = "Period",
                    value = "${surf.wavePeriod}s"
                )
                SurfStat(
                    icon = Icons.Default.Air,
                    label = "Wind",
                    value = "${surf.windSpeed.toInt()} mph"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = surf.description,
                fontSize = 14.sp,
                color = Color.DarkGray
            )
        }
    }
}

@Composable
fun SurfStat(icon: ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF00838F),
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}
