package com.destinweather.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * App-wide light/dark appearance.
 *
 * The app's identity is hand-drawn gradient backgrounds with translucent
 * cards, so instead of swapping Material color schemes we keep each
 * screen's signature gradient and derive a pastel variant in light mode,
 * with theme-aware text/card colors. [isDark] is a Compose state: flipping
 * it (from the Settings toggle) recomposes the whole app instantly.
 *
 * Initialized from PreferencesManager in MainActivity.onCreate.
 */
object AppTheme {

    private var _isDark by mutableStateOf(true)

    val isDark: Boolean get() = _isDark

    fun setDark(dark: Boolean) {
        _isDark = dark
    }

    /** A screen's signature gradient, pastel-ized in light mode. */
    fun gradient(colors: List<Color>): List<Color> =
        if (isDark) colors else colors.map { lerp(it, Color.White, 0.55f) }

    // Text on gradient backgrounds
    val textPrimary get() = if (isDark) Color.White else Color(0xFF1B2531)
    val textSecondary get() = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF4A5568)
    val textMuted get() = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF6B7280)
    val textFaint get() = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF8A94A6)

    // Translucent surfaces on gradients
    val cardSurface get() = if (isDark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.75f)
    val cardInner get() = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)
    val iconCircle get() = if (isDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.08f)
    val innerDivider get() = if (isDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.12f)

    // App chrome (drawer, top bar, bottom nav, bottom sheets, radar frame)
    val shellBackground get() = if (isDark) Color(0xFF1a1a2e) else Color(0xFFF5F7FA)
    val shellText get() = if (isDark) Color.White else Color(0xFF1B2531)

    // Accent readable on both dark gradients and light pastels
    val accent get() = if (isDark) Color(0xFF64B5F6) else Color(0xFF1565C0)

    // Progress indicators on gradients
    val spinner get() = if (isDark) Color.White else Color(0xFF1565C0)
}
