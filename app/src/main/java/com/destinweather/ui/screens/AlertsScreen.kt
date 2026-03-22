package com.destinweather.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.destinweather.data.model.AlertProperties
import com.destinweather.viewmodel.AlertsState
import com.destinweather.viewmodel.AlertsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    viewModel: AlertsViewModel = viewModel(),
    onLocationClick: () -> Unit = {}
) {
    val alertsState by viewModel.alertsState.collectAsState()
    val isRefreshing = alertsState is AlertsState.Loading
    val pullToRefreshState = rememberPullToRefreshState()

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
        PullToRefreshBox(
            state = pullToRefreshState,
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.fetchAlerts() },
            modifier = Modifier.fillMaxSize()
        ) {
            when (val state = alertsState) {
                is AlertsState.Loading -> AlertsLoadingContent()
                is AlertsState.Success -> AlertsContent(state.alerts)
                is AlertsState.Empty -> AlertsEmptyContent()
                is AlertsState.Error -> AlertsErrorContent(state.message) { viewModel.fetchAlerts() }
            }
        }
    }
}

@Composable
private fun AlertsLoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Color.White)
    }
}

@Composable
fun AlertsContent(alerts: List<AlertProperties>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Weather Alerts",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "National Weather Service",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${alerts.size} active alert${if (alerts.size > 1) "s" else ""}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64B5F6)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(alerts) { alert ->
            AlertCard(alert)
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun AlertCard(alert: AlertProperties) {
    val severityColor = when (alert.severity) {
        "Extreme" -> Color(0xFFD32F2F)
        "Severe" -> Color(0xFFF57C00)
        "Moderate" -> Color(0xFFFBC02D)
        else -> Color(0xFF64B5F6)
    }

    val severityIcon = when (alert.event?.lowercase()) {
        "tornado" -> Icons.Default.Warning
        "hurricane" -> Icons.Default.Warning
        "flood", "flash flood" -> Icons.Default.Water
        "rip current" -> Icons.Default.Waves
        "thunderstorm" -> Icons.Default.Bolt
        "wind" -> Icons.Default.Air
        "heat" -> Icons.Default.Thermostat
        else -> Icons.Default.Warning
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = severityColor.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(severityColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = severityIcon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = alert.event ?: "Weather Alert",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = severityColor
                    )
                    Text(
                        text = alert.areaDesc ?: "",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                // Severity badge
                Surface(
                    color = severityColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = alert.severity ?: "",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Headline
            if (!alert.headline.isNullOrBlank()) {
                Text(
                    text = alert.headline,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Description
            if (!alert.description.isNullOrBlank()) {
                Text(
                    text = alert.description,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Instructions
            if (!alert.instruction.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color.Green,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = alert.instruction,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Timing
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Expires: ${alert.expires ?: "Unknown"}",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun AlertsEmptyContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color.Green.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "All Clear!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No active weather alerts for this location.",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun AlertsErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.White.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Unable to load alerts",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            color = Color.White.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF1E88E5))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Try Again", color = Color(0xFF1E88E5))
        }
    }
}
