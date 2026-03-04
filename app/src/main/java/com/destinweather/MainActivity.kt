package com.destinweather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.platform.LocalConfiguration
import com.destinweather.ui.screens.BeachCamsScreen
import com.destinweather.ui.screens.SurfScreen
import com.destinweather.ui.screens.WeatherScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val items = listOf(
                BottomNavItem("Weather", Icons.Default.Cloud),
                BottomNavItem("Surf", Icons.Default.Waves),
                BottomNavItem("Cams", Icons.Default.Videocam)
            )

            val pagerState = rememberPagerState(pageCount = { items.size })
            val coroutineScope = rememberCoroutineScope()

            Scaffold(
                bottomBar = {
                    NavigationBar {
                        items.forEachIndexed { index, item ->
                            NavigationBarItem(
                                icon = { Icon(item.icon, contentDescription = item.title) },
                                label = { Text(item.title) },
                                selected = pagerState.currentPage == index,
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                }
                            )
                        }
                    }
                }
            ) { paddingValues ->
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) { page ->
                    when (page) {
                        0 -> WeatherScreen()
                        1 -> SurfScreen()
                        2 -> BeachCamsScreen()
                    }
                }
            }
        }
    }
}

data class BottomNavItem(
    val title: String,
    val icon: ImageVector
)
