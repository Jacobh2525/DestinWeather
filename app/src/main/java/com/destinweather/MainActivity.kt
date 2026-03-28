package com.destinweather

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.destinweather.ui.screens.AlertsScreen
import com.destinweather.ui.screens.BeachCamsScreen
import com.destinweather.ui.screens.LocationPickerSheet
import com.destinweather.ui.screens.NoaaForecastScreen
import com.destinweather.ui.screens.RadarScreen
import com.destinweather.ui.screens.SettingsScreen
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

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        NotificationHelper.createNotificationChannel(this)
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
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            var lastLat = 30.3935
            var lastLon = -86.4958

            val items = listOf(
                BottomNavItem("Weather", Icons.Default.Cloud, Icons.Outlined.Cloud),
                BottomNavItem("Surf", Icons.Default.Waves, Icons.Outlined.Waves),
                BottomNavItem("Alerts", Icons.Default.Warning, Icons.Outlined.Warning),
                BottomNavItem("Radar", Icons.Default.Radar, Icons.Outlined.Radar),
                BottomNavItem("Forecast", Icons.AutoMirrored.Filled.Article, Icons.AutoMirrored.Outlined.Article),
                BottomNavItem("Cams", Icons.Default.Videocam, Icons.Outlined.Videocam)
            )

            // Page count = screens + settings
            val pageCount = items.size + 1
            val pagerState = rememberPagerState(pageCount = { pageCount })
            val coroutineScope = rememberCoroutineScope()

            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = Color(0xFF1a1a2e)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Destin Weather",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(top = 32.dp, bottom = 8.dp)
                            )
                            Text(
                                text = "Your beach weather companion",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            // Settings in drawer
                            DrawerItem(
                                icon = Icons.Default.Settings,
                                title = "Settings",
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(items.size) // Last page = Settings
                                        drawerState.close()
                                    }
                                }
                            )

                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.padding(vertical = 16.dp)
                            )

                            // Navigation items
                            items.forEachIndexed { index, item ->
                                DrawerItem(
                                    icon = item.selectedIcon,
                                    title = item.title,
                                    onClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index)
                                            drawerState.close()
                                        }
                                    }
                                )
                            }
                        }
                    }
                },
                gesturesEnabled = true
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = if (pagerState.currentPage < items.size)
                                        items[pagerState.currentPage].title
                                    else "Settings",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { 
                                    coroutineScope.launch { drawerState.open() }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Menu",
                                        tint = Color.White
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color(0xFF1a1a2e),
                                titleContentColor = Color.White
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = Color(0xFF1a1a2e),
                            contentColor = Color.White
                        ) {
                            // Show only main items in bottom nav (not settings)
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
                                3 -> RadarScreen(
                                    onLocationClick = { showLocationPicker = true }
                                )
                                4 -> NoaaForecastScreen(
                                    viewModel = noaaViewModel,
                                    onLocationClick = { showLocationPicker = true }
                                )
                                5 -> BeachCamsScreen()
                                6 -> SettingsScreen() // Settings page!
                            }
                        }
                    }
                }
            }

            if (showLocationPicker) {
                LocationPickerSheet(
                    currentLocation = currentLocation,
                    onLocationSelected = { location, lat, lon ->
                        weatherViewModel.setLocation(location)
                        surfViewModel.setLocation(location)
                        noaaViewModel.setLocation(lat, lon)
                        alertsViewModel.setLocation(lat, lon)
                        lastLat = lat
                        lastLon = lon
                        AlertCheckWorker.schedule(this, lat, lon)
                    },
                    onDismiss = { showLocationPicker = false }
                )
            }

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
            AlertCheckWorker.schedule(this, 30.3935, -86.4958)
        }
    }
}

data class BottomNavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun DrawerItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}
