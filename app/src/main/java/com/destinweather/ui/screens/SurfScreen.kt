package com.destinweather.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.destinweather.data.model.SurfConditions
import com.destinweather.viewmodel.SurfState
import com.destinweather.viewmodel.SurfViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurfScreen(
    viewModel: SurfViewModel = viewModel(),
    currentLocation: String = "Destin,US",
    onLocationClick: () -> Unit = {}
) {
    val surfState by viewModel.surfState.collectAsState()
    val isRefreshing = surfState is SurfState.Loading
    val pullToRefreshState = rememberPullToRefreshState()

    val locationName = currentLocation.split(",").firstOrNull() ?: "Destin"

    // Get time-based background colors
    val backgroundColors = getSurfBackgroundColors()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(colors = backgroundColors)
            )
    ) {
        when (val state = surfState) {
            is SurfState.Loading -> SurfLoadingContent()
            is SurfState.Success -> {
                PullToRefreshBox(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.fetchSurfData(currentLocation) }
                ) {
                    SurfContent(state.surfList, locationName, onLocationClick)
                }
            }
            is SurfState.Error -> SurfErrorContent(state.message) { viewModel.fetchSurfData(currentLocation) }
        }
    }
}

private fun getSurfBackgroundColors(): List<Color> {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour in 5..7 -> listOf(Color(0xFF1a237e), Color(0xFF283593), Color(0xFF3949ab)) // Early morning
        hour in 8..10 -> listOf(Color(0xFF00ACC1), Color(0xFF26C6DA), Color(0xFF00BCD4)) // Morning
        hour in 11..16 -> listOf(Color(0xFF00ACC1), Color(0xFF00838F), Color(0xFF006064)) // Mid day
        hour in 17..19 -> listOf(Color(0xFF00796b), Color(0xFF004d40), Color(0xFF00251a)) // Evening
        hour in 20..22 -> listOf(Color(0xFF1a1a2e), Color(0xFF16213e), Color(0xFF0f3460)) // Nightfall
        else -> listOf(Color(0xFF0d1b2a), Color(0xFF1b263b), Color(0xFF1e3a5f)) // Late night
    }
}

@Composable
private fun SurfLoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color.White)
    }
}

@Composable
private fun SurfContent(
    surfList: List<SurfConditions>,
    locationName: String,
    onLocationClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Column {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.clickable { onLocationClick() }.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOn, null, tint = Color.White.copy(alpha = 0.8f), modifier =
                        Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("$locationName, FL", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.KeyboardArrowDown, "Change location", tint = Color.White.copy(alpha = 0.8f), modifier
                    = Modifier.size(20.dp))
                }

                Text("Surf Conditions", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        items(surfList) { surf ->
            SurfCard(surf)
        }
    }
}

@Composable
private fun SurfCard(surf: SurfConditions) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment =
                Alignment.CenterVertically) {
                Column {
                    Text(surf.location, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF006064))
                    Text(surf.description, fontSize = 13.sp, color = Color.Gray)
                }
                Surface(
                    color = when (surf.windRating) { "Good" -> Color(0xFF4CAF50); "Fair" -> Color(0xFFFFA000); else ->
                        Color(0xFFF44336) },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(surf.windRating, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp), color =
                        Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SurfStatBox(icon = Icons.Default.Waves, label = "Waves", value = "${surf.waveHeight} ft")
                SurfStatBox(icon = Icons.Default.Timer, label = "Period", value = "${surf.wavePeriod}s")
                SurfStatBox(icon = Icons.Default.Air, label = "Wind", value = "${surf.windSpeed.toInt()} mph")
                SurfStatBox(icon = Icons.Default.Navigation, label = "Swell", value = surf.swellDirection)
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFE0F7FA)), contentAlignment =
                        Alignment.Center) {
                        Icon(Icons.Default.Water, null, tint = Color(0xFF00838F), modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Water", fontSize = 11.sp, color = Color.Gray)
                        Text("${surf.waterTemp}°F", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color =
                            Color(0xFF00838F))
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFE0F7FA)), contentAlignment =
                        Alignment.Center) {
                        Icon(Icons.Default.Waves, null, tint = Color(0xFF00838F), modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Tide", fontSize = 11.sp, color = Color.Gray)
                        Text(surf.tide, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF00838F))
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFE0F7FA)), contentAlignment =
                        Alignment.Center) {
                        Icon(Icons.Default.Explore, null, tint = Color(0xFF00838F), modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Wind Dir", fontSize = 11.sp, color = Color.Gray)
                        Text(surf.windDirection, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color =
                            Color(0xFF00838F))
                    }
                }
            }
        }
    }
}

@Composable
private fun SurfStatBox(icon: ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFE0F7FA)), contentAlignment =
            Alignment.Center) {
            Icon(icon, null, tint = Color(0xFF00838F), modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Text(label, fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
private fun SurfErrorContent(message: String, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Warning, null, modifier = Modifier.size(80.dp), tint = Color.White.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Oops!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Text(message, color = Color.White.copy(alpha = 0.7f))
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Color.White), shape =
            RoundedCornerShape(12.dp)) {
            Text("Try Again", color = Color(0xFF00ACC1))
        }
    }
}
