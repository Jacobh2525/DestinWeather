package com.destinweather.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Blue200,
    secondary = LightBlue200,
    tertiary = Blue100,
    background = Color(0xFF0D1B2A),
    surface = Color(0xFF1B263B),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color(0xFFE0E1DD),
    onSurface = Color(0xFFE0E1DD),
)

private val LightColorScheme = lightColorScheme(
    primary = Color.White,
    secondary = Color(0xFF81D4FA),
    tertiary = Color(0xFF4FC3F7),
    background = Color(0xFF001E3C), // Deep Dark Navy Blue
    surface = Color.White.copy(alpha = 0.1f),
    onPrimary = Color(0xFF001E3C),
    onSecondary = Color(0xFF001E3C),
    onTertiary = Color.White,
    onBackground = Color.White,    // High contrast white font
    onSurface = Color.White,
    surfaceVariant = Color(0xFF000D1A), // Even darker navy for gradient
    onSurfaceVariant = Color.White,
)

@Composable
fun DestinWeatherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
