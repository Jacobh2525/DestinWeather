package com.destinweather.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.destinweather.ui.theme.AppTheme
import com.destinweather.utils.PreferencesManager
import com.destinweather.workers.AlertCheckWorker
import com.destinweather.workers.BriefingWorker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current

    var useFahrenheit by remember {
        mutableStateOf(PreferencesManager.useFahrenheit)
    }
    var notificationsEnabled by remember {
        mutableStateOf(PreferencesManager.notificationsEnabled)
    }
    var darkModeEnabled by remember {
        mutableStateOf(PreferencesManager.darkModeEnabled)
    }
    var briefingEnabled by remember {
        mutableStateOf(PreferencesManager.briefingEnabled)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = AppTheme.gradient(
                        listOf(Color(0xFF1E88E5), Color(0xFF1565C0), Color(0xFF0D47A1))
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Settings",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.textPrimary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Temperature Units
            SettingsCard(
                title = "Temperature Units",
                subtitle = if (useFahrenheit) "Fahrenheit (°F)" else "Celsius (°C)",
                icon = Icons.Default.Thermostat
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Use Fahrenheit",
                        color = AppTheme.textPrimary,
                        fontSize = 14.sp
                    )
                    Switch(
                        checked = useFahrenheit,
                        onCheckedChange = {
                            useFahrenheit = it
                            PreferencesManager.useFahrenheit = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AppTheme.accent,
                            uncheckedThumbColor = AppTheme.textMuted,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Notifications
            SettingsCard(
                title = "Notifications",
                subtitle = "Get alerts for severe weather",
                icon = Icons.Default.Notifications
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enable Push Notifications",
                        color = AppTheme.textPrimary,
                        fontSize = 14.sp
                    )
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { enabled ->
                            notificationsEnabled = enabled
                            PreferencesManager.notificationsEnabled = enabled
                            if (enabled) {
                                AlertCheckWorker.schedule(
                                    context,
                                    PreferencesManager.lastLat,
                                    PreferencesManager.lastLon
                                )
                            } else {
                                AlertCheckWorker.cancel(context)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AppTheme.accent,
                            uncheckedThumbColor = AppTheme.textMuted,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Morning Briefing
            SettingsCard(
                title = "Morning Briefing",
                subtitle = "Daily 7 AM forecast + NWS discussion",
                icon = Icons.Default.WbTwilight
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daily morning notification",
                        color = AppTheme.textPrimary,
                        fontSize = 14.sp
                    )
                    Switch(
                        checked = briefingEnabled,
                        onCheckedChange = { enabled ->
                            briefingEnabled = enabled
                            PreferencesManager.briefingEnabled = enabled
                            if (enabled) {
                                BriefingWorker.schedule(context)
                                BriefingWorker.sendNow(context) // immediate preview
                            } else {
                                BriefingWorker.cancel(context)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AppTheme.accent,
                            uncheckedThumbColor = AppTheme.textMuted,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dark Mode
            SettingsCard(
                title = "Appearance",
                subtitle = "Change the app theme",
                icon = Icons.Default.DarkMode
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dark Mode",
                        color = AppTheme.textPrimary,
                        fontSize = 14.sp
                    )
                    Switch(
                        checked = darkModeEnabled,
                        onCheckedChange = {
                            darkModeEnabled = it
                            PreferencesManager.darkModeEnabled = it
                            AppTheme.setDark(it) // applies instantly app-wide
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AppTheme.accent,
                            uncheckedThumbColor = AppTheme.textMuted,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // About
            SettingsCard(
                title = "About",
                subtitle = "Version 1.0.0",
                icon = Icons.Default.Info
            ) {
                Column {
                    Text(
                        text = "Destin Weather",
                        color = AppTheme.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your local beach weather app with surf conditions, beach cams, and weather alerts.",
                        color = AppTheme.textSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Data sources: OpenWeatherMap, NOAA",
                        color = AppTheme.textFaint,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.cardSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AppTheme.accent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppTheme.textPrimary
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = AppTheme.textMuted
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
