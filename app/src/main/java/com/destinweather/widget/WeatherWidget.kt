package com.destinweather.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.background
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.destinweather.MainActivity
import com.destinweather.R
import com.destinweather.utils.PreferencesManager

class WeatherWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        PreferencesManager.init(context)
        val weather = runCatching { WidgetWeatherFetcher.fetch(context) }.getOrNull()

        provideContent {
            GlanceTheme {
                val contentModifier = GlanceModifier
                    .fillMaxSize()
                    .appWidgetBackground()
                    .cornerRadius(16.dp)
                    .background(GlanceTheme.colors.surface)
                    .padding(16.dp)
                    .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))

                if (weather != null) {
                    WeatherContent(weather, contentModifier)
                } else {
                    ErrorContent(contentModifier)
                }
            }
        }
    }

    @Composable
    private fun WeatherContent(weather: WidgetWeather, modifier: GlanceModifier) {
        Column(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = weather.locationLabel,
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = GlanceTheme.colors.onSurface
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = weather.updatedText,
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = GlanceTheme.colors.onSurfaceVariant
                        )
                    )
                }
                Image(
                    provider = ImageProvider(R.drawable.ic_refresh),
                    contentDescription = "Refresh",
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                    modifier = GlanceModifier
                        .size(26.dp)
                        .clickable(actionRunCallback<RefreshWeatherAction>())
                )
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = weather.tempText,
                    style = TextStyle(
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface
                    )
                )
                Spacer(modifier = GlanceModifier.width(14.dp))
                Column {
                    Text(
                        text = weather.condition,
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = GlanceTheme.colors.onSurface
                        ),
                        maxLines = 1
                    )
                    weather.highLow?.let {
                        Text(
                            text = it,
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ErrorContent(modifier: GlanceModifier) {
        Column(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Couldn't load weather",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.onSurface
                )
            )
            Spacer(modifier = GlanceModifier.height(6.dp))
            Text(
                text = "Tap to open the app",
                style = TextStyle(
                    fontSize = 12.sp,
                    color = GlanceTheme.colors.onSurfaceVariant
                )
            )
        }
    }
}

class RefreshWeatherAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        WeatherWidget().update(context, glanceId)
    }
}
