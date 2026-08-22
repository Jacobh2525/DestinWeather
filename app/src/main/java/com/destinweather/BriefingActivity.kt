package com.destinweather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.destinweather.ui.theme.AppTheme
import com.destinweather.ui.theme.DestinWeatherTheme
import com.destinweather.utils.PreferencesManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dialog-style popup showing the full morning briefing (today's forecast +
 * the complete NWS forecaster discussion). Opened by tapping the briefing
 * notification; content is read from the prefs the worker stored.
 */
class BriefingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferencesManager.init(this)
        AppTheme.setDark(PreferencesManager.darkModeEnabled)
        setContent {
            DestinWeatherTheme(darkTheme = AppTheme.isDark) {
                BriefingDialog(
                    title = PreferencesManager.briefingTitle,
                    body = PreferencesManager.briefingBody,
                    time = PreferencesManager.briefingTime,
                    onDismiss = { finish() }
                )
            }
        }
    }
}

@Composable
private fun BriefingDialog(title: String, body: String, time: Long, onDismiss: () -> Unit) {
    // Scrim: tap outside the card to dismiss
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(24.dp)
                // Consume taps so the card itself doesn't dismiss
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                ),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = AppTheme.shellBackground)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Text(
                    text = title.ifBlank { "Morning Briefing" },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.shellText
                )
                Text(
                    text = if (time > 0)
                        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(time))
                    else "",
                    fontSize = 12.sp,
                    color = AppTheme.textMuted
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = AppTheme.innerDivider)
                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.weight(1f)) {
                    Text(
                        text = body.ifBlank { "No briefing stored yet. Enable Morning Briefing in Settings." },
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp,
                        color = AppTheme.shellText,
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close", color = AppTheme.accent, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
