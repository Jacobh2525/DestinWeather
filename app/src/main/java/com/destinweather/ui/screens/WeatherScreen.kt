package com.destinweather.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.destinweather.data.model.ForecastItem
import com.destinweather.data.model.ForecastResponse
import com.destinweather.data.model.WeatherResponse
import com.destinweather.viewmodel.WeatherState
import com.destinweather.viewmodel.WeatherViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel = viewModel()
) {
    val weatherState by viewModel.weatherState.collectAsState()
    val isRefreshing = weatherState is WeatherState.Loading

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E88E5),
                        Color(0xFF64B5F6)
                    )
                )
            )
    ) {
        when (val state = weatherState) {
            is WeatherState.Loading -> {
                // Initial loading shows the CircularProgressIndicator
                // PullToRefreshBox will show its own indicator during refresh
                LoadingContent()
            }
            is WeatherState.Success -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        viewModel.fetchWeather()
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    WeatherContent(state.weather, state.forecast)
                }
            }
            is WeatherState.Error -> ErrorContent(state.message) {
                viewModel.fetchWeather()
            }
        }
    }
}

@Composable
fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Color.White)
    }
}

@Composable
fun WeatherContent(weather: WeatherResponse, forecast: ForecastResponse) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // City Name
        Text(
            text = "${weather.cityName}, ${weather.sys.country}",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Weather Icon
        val iconCode = weather.weather.firstOrNull()?.icon ?: "01d"
        val iconUrl = "https://openweathermap.org/img/wn/${iconCode}@4x.png"

        AsyncImage(
            model = iconUrl,
            contentDescription = "Weather icon",
            modifier = Modifier.size(120.dp),
            contentScale = ContentScale.Fit
        )

        // Temperature
        Text(
            text = "${weather.main.temp.toInt()}°F",
            fontSize = 72.sp,
            fontWeight = FontWeight.Light,
            color = Color.White
        )

        // Condition
        Text(
            text = weather.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "",
            fontSize = 18.sp,
            color = Color.White.copy(alpha = 0.9f)
        )

        // High/Low
        val highTemp = forecast.list.maxOfOrNull { it.main.temp }?.toInt() ?: 0
        val lowTemp = forecast.list.minOfOrNull { it.main.temp }?.toInt() ?: 0
        Text(
            text = "H: ${highTemp}°  L: ${lowTemp}°",
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Current Details
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DetailCard(
                modifier = Modifier.weight(1f),
                title = "Feels Like",
                value = "${weather.main.feelsLike.toInt()}°F"
            )
            DetailCard(
                modifier = Modifier.weight(1f),
                title = "Humidity",
                value = "${weather.main.humidity}%"
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DetailCard(
                modifier = Modifier.weight(1f),
                title = "Wind",
                value = "${weather.wind.speed.toInt()} mph"
            )
            DetailCard(
                modifier = Modifier.weight(1f),
                title = "Pressure",
                value = "${weather.main.pressure} hPa"
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 5-Day Forecast Header
        Text(
            text = "5-Day Forecast",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 5-Day Forecast
        ForecastSection(forecast)

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ForecastSection(forecast: ForecastResponse) {
    // Group by day and get one entry per day
    val dailyForecasts = forecast.list
        .groupBy { item ->
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(item.dt * 1000))
        }
        .map { (_, items) -> items.first() }
        .take(5)

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(dailyForecasts) { item ->
            ForecastCard(item)
        }
    }
}

@Composable
fun ForecastCard(item: ForecastItem) {
    val dayName = SimpleDateFormat("EEE", Locale.getDefault()).format(Date(item.dt * 1000))
    val date = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(item.dt * 1000))
    val iconCode = item.weather.firstOrNull()?.icon ?: "01d"
    val iconUrl = "https://openweathermap.org/img/wn/${iconCode}.png"
    val temp = item.main.temp.toInt()
    val description = item.weather.firstOrNull()?.main ?: ""

    Card(
        modifier = Modifier
            .width(100.dp)
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = dayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = date,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            AsyncImage(
                model = iconUrl,
                contentDescription = description,
                modifier = Modifier.size(50.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${temp}°",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = description,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun DetailCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Oops!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            color = Color.White.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White
            )
        ) {
            Text("Retry", color = Color(0xFF1E88E5))
        }
    }
}
