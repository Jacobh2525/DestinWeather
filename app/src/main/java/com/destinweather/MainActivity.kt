package com.destinweather

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.destinweather.ui.screens.AlertsScreen
import com.destinweather.ui.screens.BeachCamsScreen
import com.destinweather.ui.screens.LocationPickerSheet
import com.destinweather.ui.screens.NoaaForecastScreen
import com.destinweather.ui.screens.SurfScreen
import com.destinweather.ui.screens.WeatherScreen
import com.destinweather.utils.NotificationHelper
import com.destinweather.viewmodel.AlertsViewModel
import com.destinweather.viewmodel.NoaaViewModel
import com.destinweather.viewmodel.SurfViewModel
import com.destinweather.viewmodel.WeatherViewModel
import com.destinweather.workers.AlertCheckWorker
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            AlertCheckWorker.schedule(this, 30.3935, -86.4958)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create notification channel
        NotificationHelper.createNotificationChannel(this)

        // Request notification permission on Android 13+
        requestNotificationPermission()

        setContent {
            var keepSplashScreen by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(1500)
                keepSplashScreen = false
            }

            splashScreen.setKeepOnScreenCondition { keepSplashScreen }

            val weatherViewModel: WeatherViewModel = viewModel()
            val surfViewModel: SurfViewModel = viewModel()
            val noaaViewModel: NoaaViewModel = viewModel()
            val alertsViewModel: AlertsViewModel = viewModel()
            val currentLocation by weatherViewModel.currentLocation.collectAsState()

            var showLocationPicker by remember { mutableStateOf(false) }

            // Track location for alert checks
            var lastLat = 30.3935
            var lastLon = -86.4958

            val items = listOf(
                BottomNavItem("Weather", Icons.Default.Cloud, Icons.Outlined.Cloud),
                BottomNavItem("Surf", Icons.Default.Waves, Icons.Outlined.Waves),
                BottomNavItem("Alerts", Icons.Default.Warning, Icons.Outlined.Warning),
                BottomNavItem("Forecast", Icons.Default.Article, Icons.Outlined.Article),
                BottomNavItem("Cams", Icons.Default.Videocam, Icons.Outlined.Videocam)
            )

            val pagerState = rememberPagerState(pageCount = { items.size })
            val coroutineScope = rememberCoroutineScope()

            Scaffold(
                bottomBar = {
                    NavigationBar(
                        containerColor = Color(0xFF1a1a2e),
                        contentColor = Color.White
                    ) {
                        items.forEachIndexed { index, item ->
                            val selected = pagerState.currentPage == index

                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.title
                                    )
                                },
                                label = { Text(item.title) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF64B5F6),
                                    selectedTextColor = Color(0xFF64B5F6),
                                    unselectedIconColor = Color.White.copy(alpha = 0.6f),
                                    unselectedTextColor = Color.White.copy(alpha = 0.6f),
                                    indicatorColor = Color(0xFF64B5F6).copy(alpha = 0.2f)
                                )
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (page) {
                            0 -> WeatherScreen(
                                viewModel = weatherViewModel,
                                onLocationClick = { showLocationPicker = true }
                            )
                            1 -> SurfScreen(
                                viewModel = surfViewModel,
                                currentLocation = currentLocation,
                                onLocationClick = { showLocationPicker = true }
                            )
                            2 -> AlertsScreen(
                                viewModel = alertsViewModel,
                                onLocationClick = { showLocationPicker = true }
                            )
                            3 -> NoaaForecastScreen(
                                viewModel = noaaViewModel,
                                onLocationClick = { showLocationPicker = true }
                            )
                            4 -> BeachCamsScreen()
                        }
                    }
                }
            }

            // Location Picker Sheet
            if (showLocationPicker) {
                LocationPickerSheet(
                    currentLocation = currentLocation,
                    onLocationSelected = { location, lat, lon ->
                        weatherViewModel.setLocation(location)
                        surfViewModel.setLocation(location)
                        noaaViewModel.setLocation(lat, lon)
                        alertsViewModel.setLocation(lat, lon)

                        // Update alert checks with new location
                        lastLat = lat
                        lastLon = lon
                        AlertCheckWorker.schedule(this, lat, lon)
                    },
                    onDismiss = { showLocationPicker = false }
                )
            }

            // Schedule initial alert check
            LaunchedEffect(Unit) {
                AlertCheckWorker.schedule(this@MainActivity, lastLat, lastLon)
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    AlertCheckWorker.schedule(this, 30.3935, -86.4958)
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // For older Android versions, just schedule the work
            AlertCheckWorker.schedule(this, 30.3935, -86.4958)
        }
    }
}

data class BottomNavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)
