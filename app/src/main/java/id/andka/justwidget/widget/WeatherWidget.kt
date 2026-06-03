package id.andka.justwidget.widget

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import id.andka.justwidget.R
import id.andka.justwidget.SettingsActivity
import id.andka.justwidget.data.DayForecast
import id.andka.justwidget.data.WeatherData

class WeatherWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val data = WeatherStore.deserialize(prefs[WeatherStore.KEY_JSON])
            val accentHex = prefs[WeatherStore.KEY_ACCENT] ?: "#C084FC"
            val textDark = prefs[WeatherStore.KEY_TEXT_DARK] ?: false
            val bgStyle = prefs[WeatherStore.KEY_BG_STYLE] ?: "dark"
            WidgetUi(context, data, accentHex, textDark, bgStyle)
        }
    }

    companion object {
        suspend fun updateSettings(
            context: Context,
            accent: String,
            textDark: Boolean,
            bgStyle: String
        ) {
            val manager = GlanceAppWidgetManager(context)
            val ids = manager.getGlanceIds(WeatherWidget::class.java)
            ids.forEach { id ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[WeatherStore.KEY_ACCENT] = accent
                        this[WeatherStore.KEY_TEXT_DARK] = textDark
                        this[WeatherStore.KEY_BG_STYLE] = bgStyle
                    }
                }
            }
            WeatherWidget().updateAll(context)
        }
    }
}

@Composable
private fun WidgetUi(
    context: Context,
    data: WeatherData?,
    accentHex: String,
    textDark: Boolean,
    bgStyle: String
) {
    val textColor = if (textDark) Color(0xFF1A1A2E) else Color.White
    val textColorProvider = ColorProvider(textColor)

    // RemoteViews for Clock (need to set custom text color)
    val rv = RemoteViews(context.packageName, R.layout.widget_clock).apply {
        val clockColor = if (textDark) 0xFF1A1A2E.toInt() else android.graphics.Color.WHITE
        setTextColor(R.id.widget_time, clockColor)
        setTextColor(R.id.widget_date, clockColor)
        setTextColor(R.id.widget_weekday, clockColor)
    }

    // Determine root widget background style
    val rootBackgroundMod = when (bgStyle) {
        "light" -> GlanceModifier.background(Color(0xB3F0F4F8)).cornerRadius(20.dp)
        "transparent" -> GlanceModifier.background(Color.Transparent)
        else -> GlanceModifier.background(Color(0xB3101018)).cornerRadius(20.dp) // "dark"
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .then(rootBackgroundMod)
            .padding(16.dp)
            .clickable(
                actionStartActivity(
                    ComponentName(context, SettingsActivity::class.java)
                )
            ),
    ) {
        AndroidRemoteViews(
            remoteViews = rv,
            modifier = GlanceModifier.fillMaxWidth().height(62.dp),
        )

        Spacer(GlanceModifier.height(8.dp))

        if (data == null || data.days.isEmpty()) {
            Text(
                text = "Tap to load weather…",
                style = TextStyle(color = textColorProvider, fontSize = 12.sp),
            )
        } else {
            Forecast(data, accentHex, textDark, textColorProvider, GlanceModifier.defaultWeight())
        }
    }
}

/**
 * The 7-column forecast.
 */
@Composable
private fun Forecast(
    data: WeatherData,
    accentHex: String,
    textDark: Boolean,
    textColorProvider: ColorProvider,
    modifier: GlanceModifier
) {
    val days = data.days
    val today = data.todayIndex.coerceIn(0, days.size - 1)

    val accentColor = try {
        Color(android.graphics.Color.parseColor(accentHex))
    } catch (e: Exception) {
        Color(0xFFC084FC)
    }

    val tileBgColor = if (textDark) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.12f)
    val accentBgColor = accentColor.copy(alpha = 0.22f)

    Box(modifier = modifier.fillMaxWidth()) {
        // Back layer: 7 equal-width columns.
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Top,
        ) {
            days.forEachIndexed { i, day ->
                if (i < today) {
                    PastDayCell(day, tileBgColor, textColorProvider, GlanceModifier.defaultWeight())
                } else {
                    val isToday = (i == today)
                    val bg = if (isToday) accentBgColor else tileBgColor
                    ForecastColumn(day, bg, textColorProvider, GlanceModifier.defaultWeight())
                }
            }
        }

        // Front layer: wind + place pinned to the bottom-left.
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Bottom,
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = "${data.windCompass}, ${data.windSpeedKmh} km/h",
                style = TextStyle(color = textColorProvider, fontSize = 11.sp),
            )
            Text(
                text = data.placeName,
                style = TextStyle(color = textColorProvider, fontSize = 13.sp, fontWeight = FontWeight.Medium),
            )
        }
    }
}

/** A past day: short translucent card with just the date label. */
@Composable
private fun PastDayCell(
    day: DayForecast,
    bgColor: Color,
    textColorProvider: ColorProvider,
    modifier: GlanceModifier
) {
    Box(modifier = modifier.padding(horizontal = 4.dp)) {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(bgColor)
                .cornerRadius(12.dp)
                .padding(vertical = 4.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DayLabel(day, textColorProvider)
        }
    }
}

/** Today / a future day: full-height band with date label on top, weather below. */
@Composable
private fun ForecastColumn(
    day: DayForecast,
    bgColor: Color,
    textColorProvider: ColorProvider,
    modifier: GlanceModifier
) {
    Box(modifier = modifier.fillMaxHeight().padding(horizontal = 4.dp)) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(bgColor)
                .cornerRadius(12.dp)
                .padding(vertical = 4.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DayLabel(day, textColorProvider)
            Spacer(GlanceModifier.defaultWeight())
            Text(
                text = day.emoji,
                style = TextStyle(color = textColorProvider, fontSize = 16.sp, textAlign = TextAlign.Center),
            )
            Text(
                text = "${day.maxTemp}°",
                style = TextStyle(color = textColorProvider, fontSize = 11.sp, textAlign = TextAlign.Center),
            )
        }
    }
}

/** Big day-of-month number with the short weekday beneath it. */
@Composable
private fun DayLabel(day: DayForecast, textColorProvider: ColorProvider) {
    Text(
        text = "${day.dayOfMonth}",
        style = TextStyle(
            color = textColorProvider,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        ),
    )
    Spacer(GlanceModifier.height(4.dp))
    Text(
        text = day.weekdayShort,
        style = TextStyle(color = textColorProvider, fontSize = 9.sp, textAlign = TextAlign.Center),
    )
}
