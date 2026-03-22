package com.destinweather.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.destinweather.data.model.ForecastItem
import com.destinweather.data.model.ForecastResponse
import com.destinweather.data.model.WeatherResponse
import com.destinweather.ui.theme.WeatherBackground
import com.destinweather.viewmodel.WeatherState
import com.destinweather.viewmodel.WeatherViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel = viewModel(),
    onLocationClick: () -> Unit = {}
) {
    val weatherState by viewModel.weatherState.collectAsState()
    val isRefreshing = weatherState is WeatherState.Loading
    val pullToRefreshState = rememberPullToRefreshState()

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = weatherState) {
            is WeatherState.Loading -> WeatherLoadingContent()
            is WeatherState.Success -> {
                val conditionCode = WeatherBackground.getConditionCode(state.weather)
                val isNight = WeatherBackground.isNightTime(state.weather.sys.sunrise, state.weather.sys.sunset)
                val weatherStyle = WeatherBackground.getWeatherStyle(conditionCode, isNight)

                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = weatherStyle.colors))) {
                    PullToRefreshBox(
                        state = pullToRefreshState,
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.fetchWeather() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        WeatherContent(state.weather, state.forecast, onLocationClick)
                    }
                }
            }
            is WeatherState.Error -> {
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color(0xFF1E88E5),
                    Color(0xFF1565C0), Color(0xFF0D47A1))))) {
                    WeatherErrorContent(state.message) { viewModel.fetchWeather() }
                }
            }
        }
    }
}

@Composable
private fun WeatherLoadingContent() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Cloud, null, modifier = Modifier.size(80.dp).scale(scale), tint = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Loading weather...", color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable
fun WeatherContent(weather: WeatherResponse, forecast: ForecastResponse, onLocationClick: () -> Unit) {
    val uvIndex = calculateUVIndex(forecast)
    val sunriseTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(weather.sys.sunrise * 1000))
    val sunsetTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(weather.sys.sunset * 1000))
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val hourlyToday = forecast.list.filter { 
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.dt * 1000)) == today 
    }.take(12)
    val currentPrecip = ((forecast.list.firstOrNull()?.pop ?: 0.0) * 100).toInt()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment =
        Alignment.CenterHorizontally) {
        // City Name
        Row(modifier = Modifier.clickable { onLocationClick() }.padding(8.dp), verticalAlignment =
            Alignment.CenterVertically) {
            Icon(Icons.Default.LocationOn, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("${weather.cityName}, ${weather.sys.country}", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color =
                Color.White)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Default.KeyboardArrowDown, "Change location", tint = Color.White.copy(alpha = 0.8f), modifier =
                Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        val iconCode = weather.weather.firstOrNull()?.icon ?: "01d"
        AsyncImage(
            model = "https://openweathermap.org/img/wn/${iconCode}@4x.png",
            contentDescription = "Weather icon",
            modifier = Modifier.size(140.dp)
        )

        Text("${weather.main.temp.toInt()}°", fontSize = 80.sp, fontWeight = FontWeight.Light, color = Color.White)
        Text(weather.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "", fontSize = 20.sp, color =
            Color.White.copy(alpha = 0.9f))

        val highTemp = forecast.list.maxOfOrNull { it.main.temp }?.toInt() ?: 0
        val lowTemp = forecast.list.minOfOrNull { it.main.temp }?.toInt() ?: 0
        Text("H: ${highTemp}°  •  L: ${lowTemp}°", fontSize = 16.sp, color = Color.White.copy(alpha = 0.8f))

        Spacer(modifier = Modifier.height(28.dp))

        // Row 1: Feels Like + Humidity
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ExpandableDetailCard(
                modifier = Modifier.weight(1f),
                title = "Feels Like",
                value = "${weather.main.feelsLike.toInt()}°",
                icon = Icons.Default.Thermostat,
                details = listOf(
                    "Actual: ${weather.main.temp.toInt()}°F" to "",
                    "Diff: ${(weather.main.feelsLike - weather.main.temp).toInt()}°" to ""
                )
            )
            ExpandableDetailCard(
                modifier = Modifier.weight(1f),
                title = "Humidity",
                value = "${weather.main.humidity}%",
                icon = Icons.Default.WaterDrop,
                details = listOf(
                    "Level: ${if (weather.main.humidity > 70) "High" else if (weather.main.humidity > 40) "Normal" else "Low"}" to "",
                    "Dew Point: ${(weather.main.temp - (100 - weather.main.humidity) / 5).toInt()}°F" to ""
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 2: Wind + Pressure
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ExpandableDetailCard(
                modifier = Modifier.weight(1f),
                title = "Wind",
                value = "${weather.wind.speed.toInt()} mph",
                icon = Icons.Default.Air,
                details = listOf(
                    "Direction: ${getWindDirection(weather.wind.deg)}" to "",
                    "Speed: ${getWindDescription(weather.wind.speed)}" to ""
                )
            )
            ExpandableDetailCard(
                modifier = Modifier.weight(1f),
                title = "Pressure",
                value = "${weather.main.pressure}",
                icon = Icons.Default.Speed,
                details = listOf(
                    "Value: ${weather.main.pressure} hPa" to "",
                    "Status: ${getPressureStatus(weather.main.pressure)}" to ""
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 3: UV Index (full width)
        ExpandableDetailCard(
            modifier = Modifier.fillMaxWidth(),
            title = "UV Index",
            value = "$uvIndex - ${getUVLevel(uvIndex)}",
            icon = Icons.Default.WbSunny,
            details = listOf(
                "Level: ${getUVLevel(uvIndex)}" to "",
                "Protection: ${getUVProtection(uvIndex)}" to "",
                "Tip: ${getUVTip(uvIndex)}" to ""
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Row 4: Rain + Sunrise (side by side)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ExpandableDetailCard(
                modifier = Modifier.weight(1f),
                title = "Rain Chance",
                value = "$currentPrecip%",
                icon = Icons.Default.Umbrella,
                details = listOf(
                    "Chance: $currentPrecip%" to "",
                    "Clouds: ${forecast.list.firstOrNull()?.clouds?.all ?: 0}%" to ""
                )
            )
            ExpandableDetailCard(
                modifier = Modifier.weight(1f),
                title = "Sunrise",
                value = sunriseTime,
                icon = Icons.Default.WbTwilight,
                details = listOf(
                    "Sunrise: $sunriseTime" to "",
                    "Sunset: $sunsetTime" to "",
                    "Day: ${getDayLength(weather.sys.sunrise, weather.sys.sunset)}" to ""
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 5: Sunset (full width)
        ExpandableDetailCard(
            modifier = Modifier.fillMaxWidth(),
            title = "Sunset & Evening",
            value = sunsetTime,
            icon = Icons.Default.NightsStay,
            details = listOf(
                "Sunset: $sunsetTime" to "",
                "Time Left: ${getTimeUntilSunset(weather.sys.sunset)}" to "",
                "UV After Dark: 0" to ""
            )
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text("Today", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White, modifier =
            Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) { items(hourlyToday) { HourlyCard(it) } }

        Spacer(modifier = Modifier.height(24.dp))

        Text("5-Day Forecast", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White, modifier =
            Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(12.dp))
        ForecastSection(forecast)

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun ExpandableDetailCard(modifier: Modifier = Modifier, title: String, value: String, icon: ImageVector, details:
List<Pair<String, String>>) {
    var expanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(if (expanded) 180f else 0f, label = "rotation")

    Card(modifier = modifier.clickable { expanded = !expanded }, shape = RoundedCornerShape(16.dp), colors =
        CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                    Text(value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
                Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White.copy(alpha = 0.7f), modifier =
                    Modifier.rotate(rotationAngle))
            }

            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))
                    details.forEach { (label, extra) ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                            if (extra.isNotEmpty()) Text(extra, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }
        }
    }
}

private fun getWindDirection(deg: Int): String = when { 
    deg < 23 || deg >= 338 -> "N"
    deg < 68 -> "NE"
    deg < 113 -> "E"
    deg < 158 -> "SE"
    deg < 203 -> "S"
    deg < 248 -> "SW"
    deg < 293 -> "W"
    else -> "NW" 
}

private fun getWindDescription(speed: Double): String = when { 
    speed < 5 -> "Calm"
    speed < 15 -> "Light"
    speed < 25 -> "Moderate"
    speed < 35 -> "Strong"
    else -> "Very Strong" 
}

private fun getPressureStatus(pressure: Int): String = when { 
    pressure < 1000 -> "Low"
    pressure < 1020 -> "Normal"
    else -> "High" 
}

private fun getUVLevel(uv: Int): String = when { 
    uv <= 2 -> "Low"
    uv <= 5 -> "Moderate"
    uv <= 7 -> "High"
    uv <= 10 -> "Very High"
    else -> "Extreme" 
}

private fun getUVProtection(uv: Int): String = when { 
    uv <= 2 -> "None"
    uv <= 5 -> "Sunscreen"
    uv <= 7 -> "Seek shade"
    uv <= 10 -> "Extra protection"
    else -> "Avoid sun" 
}

private fun getUVTip(uv: Int): String = when { 
    uv <= 2 -> "Safe outdoors"
    uv <= 5 -> "Wear sunscreen"
    uv <= 7 -> "Seek shade midday"
    uv <= 10 -> "Limit exposure"
    else -> "Stay indoors" 
}

private fun getDayLength(sunrise: Long, sunset: Long): String {
    val length = sunset - sunrise
    val hours = length / 3600
    val minutes = (length % 3600) / 60
    return "${hours}h ${minutes}m"
}

private fun getTimeUntilSunset(sunset: Long): String {
    val now = System.currentTimeMillis() / 1000
    val diff = sunset - now
    if (diff < 0) return "Passed"
    val hours = diff / 3600
    val minutes = (diff % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

@Composable
fun HourlyCard(item: ForecastItem) {
    val time = SimpleDateFormat("h a", Locale.getDefault()).format(Date(item.dt * 1000))
    val iconUrl = "https://openweathermap.org/img/wn/${item.weather.firstOrNull()?.icon ?: "01d"}.png"
    val precipChance = ((item.pop ?: 0.0) * 100).toInt()

    Card(modifier = Modifier.width(72.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor
    = Color.White.copy(alpha = 0.15f))) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(time, fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
            Spacer(modifier = Modifier.height(6.dp))
            AsyncImage(model = iconUrl, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text("${item.main.temp.toInt()}°", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            if (precipChance > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WaterDrop, null, tint = Color(0xFF64B5F6), modifier = Modifier.size(10.dp))
                    Text("$precipChance%", fontSize = 10.sp, color = Color(0xFF64B5F6))
                }
            }
        }
    }
}

private fun calculateUVIndex(forecast: ForecastResponse): Int {
    val cloudCover = forecast.list.firstOrNull()?.clouds?.all ?: 50
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val baseUV = when (hour) { in 6..8 -> 2; in 9..10 -> 5; in 11..14 -> 8; in 15..16 -> 6; in 17..18 -> 3; else -> 0 }
    return (baseUV * (1 - cloudCover / 200.0)).toInt().coerceAtLeast(0)
}

@Composable
fun ForecastSection(forecast: ForecastResponse) {
    val dailyForecasts = forecast.list.groupBy { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.dt *
            1000)) }.map { it.value.first() }.take(5)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { dailyForecasts.forEach { ForecastRowCard(it) } }
}

@Composable
fun ForecastRowCard(item: ForecastItem) {
    val dayName = if (SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(item.dt * 1000)) ==
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) "Today" else SimpleDateFormat("EEE",
        Locale.getDefault()).format(Date(item.dt * 1000))
    val iconUrl = "https://openweathermap.org/img/wn/${item.weather.firstOrNull()?.icon ?: "01d"}.png"
    val precipChance = ((item.pop ?: 0.0) * 100).toInt()

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors =
        CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment =
            Alignment.CenterVertically) {
            Column(modifier = Modifier.width(70.dp)) {
                Text(dayName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(item.dt * 1000)), fontSize = 12.sp, color =
                    Color.White.copy(alpha = 0.7f))
            }
            Spacer(modifier = Modifier.width(8.dp))
            AsyncImage(model = iconUrl, contentDescription = null, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(item.weather.firstOrNull()?.main ?: "", modifier = Modifier.weight(1f), fontSize = 13.sp, color =
                Color.White.copy(alpha = 0.8f))
            if (precipChance > 20) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(45.dp)) {
                    Icon(Icons.Default.WaterDrop, null, tint = Color(0xFF64B5F6), modifier = Modifier.size(14.dp))
                    Text("$precipChance%", fontSize = 12.sp, color = Color(0xFF64B5F6))
                }
            } else { Spacer(modifier = Modifier.width(45.dp)) }
            Text("${item.main.temp.toInt()}°", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
                modifier = Modifier.width(40.dp))
            Text("${(item.main.temp - 5).toInt()}°", fontSize = 14.sp, color = Color.White.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun WeatherErrorContent(message: String, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.CloudOff, null, modifier = Modifier.size(80.dp), tint = Color.White.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Oops!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Text(message, color = Color.White.copy(alpha = 0.7f))
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Color.White), shape =
            RoundedCornerShape(12.dp)) {
            Icon(Icons.Default.Refresh, null, tint = Color(0xFF1E88E5))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Try Again", color = Color(0xFF1E88E5))
        }
    }
}
