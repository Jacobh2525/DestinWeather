package com.destinweather.ui.theme

import androidx.compose.ui.graphics.Color

// Weather condition codes from OpenWeatherMap
object WeatherBackground {

    data class WeatherStyle(
        val colors: List<Color>,
        val isDark: Boolean = false
    )

    fun getWeatherStyle(conditionCode: Int, isNight: Boolean = false): WeatherStyle {
        return when {
            // Thunderstorm
            conditionCode in 200..232 -> WeatherStyle(
                colors = listOf(
                    Color(0xFF2c3e50),
                    Color(0xFF3498db),
                    Color(0xFF1a252f)
                ),
                isDark = true
            )

            // Drizzle
            conditionCode in 300..321 -> WeatherStyle(
                colors = listOf(
                    Color(0xFF546e7a),
                    Color(0xFF78909c),
                    Color(0xFF455a64)
                ),
                isDark = true
            )

            // Rain
            conditionCode in 500..531 -> WeatherStyle(
                colors = listOf(
                    Color(0xFF37474f),
                    Color(0xFF546e7a),
                    Color(0xFF263238)
                ),
                isDark = true
            )

            // Snow
            conditionCode in 600..622 -> WeatherStyle(
                colors = listOf(
                    Color(0xFF90a4ae),
                    Color(0xFFb0bec5),
                    Color(0xFF78909c)
                ),
                isDark = false
            )

            // Atmosphere (fog, mist, etc.)
            conditionCode in 700..781 -> WeatherStyle(
                colors = listOf(
                    Color(0xFF607d8b),
                    Color(0xFF78909c),
                    Color(0xFF455a64)
                ),
                isDark = true
            )

            // Clear
            conditionCode == 800 -> {
                if (isNight) {
                    WeatherStyle(
                        colors = listOf(
                            Color(0xFF0d1b2a),
                            Color(0xFF1b263b),
                            Color(0xFF1e3a5f)
                        ),
                        isDark = true
                    )
                } else {
                    WeatherStyle(
                        colors = listOf(
                            Color(0xFF1E88E5),
                            Color(0xFF1565C0),
                            Color(0xFF0D47A1)
                        ),
                        isDark = false
                    )
                }
            }

            // Clouds
            conditionCode in 801..804 -> {
                if (isNight) {
                    WeatherStyle(
                        colors = listOf(
                            Color(0xFF1a1a2e),
                            Color(0xFF16213e),
                            Color(0xFF0f3460)
                        ),
                        isDark = true
                    )
                } else {
                    WeatherStyle(
                        colors = listOf(
                            Color(0xFF64b5f6),
                            Color(0xFF42a5f5),
                            Color(0xFF1e88e5)
                        ),
                        isDark = false
                    )
                }
            }

            // Default
            else -> WeatherStyle(
                colors = listOf(
                    Color(0xFF1E88E5),
                    Color(0xFF1565C0),
                    Color(0xFF0D47A1)
                ),
                isDark = false
            )
        }
    }

    // Get condition code from weather data
    fun getConditionCode(weather: com.destinweather.data.model.WeatherResponse): Int {
        return weather.weather.firstOrNull()?.id ?: 800
    }

    // Check if it's night time (compare current time with sunset/sunrise)
    fun isNightTime(sunrise: Long, sunset: Long): Boolean {
        val now = System.currentTimeMillis() / 1000
        return now < sunrise || now > sunset
    }
}
