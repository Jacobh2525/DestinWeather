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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.destinweather.data.model.NhcStorm
import com.destinweather.utils.PreferencesManager
import com.destinweather.viewmodel.HurricaneState
import com.destinweather.viewmodel.HurricaneViewModel
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HurricaneScreen(viewModel: HurricaneViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val isRefreshing = state is HurricaneState.Loading
    val pullToRefreshState = rememberPullToRefreshState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1c2833), Color(0xFF2e4053), Color(0xFF1a252f))
                )
            )
    ) {
        when (val s = state) {
            is HurricaneState.Loading -> HurricaneLoadingContent()
            is HurricaneState.Success -> {
                PullToRefreshBox(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.fetch() }
                ) {
                    HurricaneContent(s.storms, s.outlook)
                }
            }
            is HurricaneState.Error -> HurricaneErrorContent(s.message) { viewModel.fetch() }
        }
    }
}

@Composable
private fun HurricaneLoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color.White)
    }
}

@Composable
private fun HurricaneContent(storms: List<NhcStorm>, outlook: String?) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Cyclone, null,
                        tint = Color.White, modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Hurricane Center", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Text(
                    "NOAA National Hurricane Center",
                    fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        item {
            Text("Active Tropical Cyclones", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }

        if (storms.isEmpty()) {
            item { QuietTropicsCard() }
        } else {
            items(storms) { storm -> StormCard(storm) }
        }

        if (!outlook.isNullOrBlank()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Atlantic Tropical Outlook", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
            item { OutlookCard(outlook) }
        }

        item {
            Text(
                "Live data: NOAA / National Hurricane Center",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun QuietTropicsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckCircle, null,
                tint = Color(0xFF4CAF50), modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text("No active tropical cyclones", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(
                    "The Atlantic and Pacific basins are quiet right now.",
                    fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun StormCard(storm: NhcStorm) {
    val classification = storm.classification?.uppercase() ?: "??"
    val windsKt = storm.intensityKt?.toIntOrNull()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        storm.name ?: "Unnamed",
                        fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF006064)
                    )
                    Text(
                        "${basinLabel(storm.id)} · ${classificationLabel(classification, windsKt)}",
                        fontSize = 13.sp, color = Color.Gray
                    )
                }
                Surface(color = classificationColor(classification), shape = RoundedCornerShape(20.dp)) {
                    Text(
                        classification,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StormStatBox(icon = Icons.Default.Air, label = "Winds", value = formatWind(windsKt))
                StormStatBox(icon = Icons.Default.Speed, label = "Pressure", value = storm.pressureMb?.let { "$it mb" } ?: "—")
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StormStatBox(
                    icon = Icons.Default.Navigation, label = "Movement",
                    value = formatMovement(storm.movementDir, storm.movementSpeedKt)
                )
                StormStatBox(
                    icon = Icons.Default.Place, label = "Position",
                    value = formatPosition(storm.latitude, storm.longitude)
                )
            }

            formatAdvisoryTime(storm.lastUpdate)?.let { updated ->
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    "Advisory updated: $updated",
                    fontSize = 11.sp, color = Color.Gray,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun StormStatBox(icon: ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(140.dp)) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFE0F7FA)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color(0xFF00838F), modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Text(label, fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
private fun OutlookCard(outlook: String) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Tropical Weather Outlook",
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White
                    )
                    Text(
                        "NWS National Hurricane Center · Atlantic basin",
                        fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f)
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.clickable { expanded = !expanded }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (expanded) outlook else outlook.take(600).trimEnd() + "  …",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.White.copy(alpha = 0.9f),
                lineHeight = 16.sp
            )

            Text(
                text = if (expanded) "Show less" else "Read full outlook",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF64B5F6),
                modifier = Modifier
                    .padding(top = 10.dp)
                    .clickable { expanded = !expanded }
            )
        }
    }
}

@Composable
private fun HurricaneErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Warning, null, modifier = Modifier.size(80.dp), tint = Color.White.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Oops!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Text(message, color = Color.White.copy(alpha = 0.7f))
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Color.White), shape =
            RoundedCornerShape(12.dp)) {
            Text("Try Again", color = Color(0xFF2e4053))
        }
    }
}

// ---------- Formatting helpers ----------

private fun classificationLabel(code: String, windsKt: Int?): String = when (code) {
    "TD" -> "Tropical Depression"
    "TS" -> "Tropical Storm"
    "HU" -> hurricaneCategory(windsKt)?.let { "Category $it Hurricane" } ?: "Hurricane"
    "ST" -> "Subtropical Storm"
    "SD" -> "Subtropical Depression"
    "EX" -> "Post-Tropical Cyclone"
    "PTC" -> "Potential Tropical Cyclone"
    "LO" -> "Tropical Low"
    "DB" -> "Tropical Disturbance"
    "WV" -> "Tropical Wave"
    else -> "Tropical Cyclone"
}

// Saffir-Simpson category from sustained winds in knots
private fun hurricaneCategory(kt: Int?): Int? = when {
    kt == null -> null
    kt in 64..82 -> 1
    kt in 83..95 -> 2
    kt in 96..112 -> 3
    kt in 113..136 -> 4
    kt >= 137 -> 5
    else -> null
}

private fun classificationColor(code: String): Color = when (code) {
    "HU" -> Color(0xFFF44336)
    "TS", "ST" -> Color(0xFFFFA000)
    "TD", "SD" -> Color(0xFF1E88E5)
    else -> Color(0xFF757575)
}

private fun basinLabel(id: String?): String = when (id?.take(2)?.lowercase()) {
    "al" -> "Atlantic"
    "ep" -> "East Pacific"
    "cp" -> "Central Pacific"
    "wp" -> "West Pacific"
    "io" -> "Indian Ocean"
    "sh" -> "Southern Hemisphere"
    else -> "Tropics"
}

private fun formatSpeed(kt: Int?): String? {
    if (kt == null) return null
    return if (PreferencesManager.useFahrenheit) "${(kt * 1.15078).roundToInt()} mph"
    else "${(kt * 1.852).roundToInt()} km/h"
}

private fun formatWind(kt: Int?): String = formatSpeed(kt) ?: "—"

private fun formatMovement(dir: Int?, speedKt: Int?): String {
    if (dir == null) return "—"
    val cardinal = degreesToCardinal(dir.toDouble())
    val speed = formatSpeed(speedKt)
    return if (speed != null) "$cardinal · $speed" else cardinal
}

private fun formatPosition(lat: Double?, lon: Double?): String {
    if (lat == null || lon == null) return "—"
    val latStr = "%.1f°%s".format(abs(lat), if (lat >= 0) "N" else "S")
    val lonStr = "%.1f°%s".format(abs(lon), if (lon >= 0) "E" else "W")
    return "$latStr, $lonStr"
}

private fun formatAdvisoryTime(isoUtc: String?): String? = isoUtc?.let {
    runCatching {
        val instant = java.time.Instant.parse(it)
        java.time.format.DateTimeFormatter.ofPattern("MMM d, h:mm a")
            .withZone(java.time.ZoneId.systemDefault())
            .format(instant)
    }.getOrNull()
}

private fun degreesToCardinal(deg: Double): String = when {
    deg < 22.5 || deg >= 337.5 -> "N"
    deg < 67.5 -> "NE"
    deg < 112.5 -> "E"
    deg < 157.5 -> "SE"
    deg < 202.5 -> "S"
    deg < 247.5 -> "SW"
    deg < 292.5 -> "W"
    else -> "NW"
}
