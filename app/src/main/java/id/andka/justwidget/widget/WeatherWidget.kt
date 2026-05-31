package id.andka.justwidget.widget

import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
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
import androidx.glance.appwidget.provideContent
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
            WidgetUi(context, data)
        }
    }
}

private val white = ColorProvider(R.color.widget_text)

@Composable
private fun WidgetUi(context: Context, data: WeatherData?) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_bg))
            .padding(16.dp)
            .clickable(
                actionStartActivity(
                    ComponentName(context, SettingsActivity::class.java)
                )
            ),
    ) {
        // Live clock + date + weekday (self-updating RemoteViews).
        // NOTE: AndroidRemoteViews with no height constraint expands to fill the
        // whole widget and squashes every sibling below it — bound its height.
        AndroidRemoteViews(
            remoteViews = RemoteViews(context.packageName, R.layout.widget_clock),
            modifier = GlanceModifier.fillMaxWidth().height(56.dp),
        )

        Spacer(GlanceModifier.height(8.dp))

        if (data == null || data.days.isEmpty()) {
            Text(
                text = "Tap to load weather…",
                style = TextStyle(color = white, fontSize = 12.sp),
            )
        } else {
            Forecast(data, GlanceModifier.defaultWeight())
        }
    }
}

/**
 * The 7-column forecast. "Today" is centered at position 4: the 3 columns before
 * it are the past 3 days (short cards, label only), and from today onward each
 * column is a full-height band with weather. Wind + place is overlaid at the
 * bottom-left, under the past-day columns.
 */
@Composable
private fun Forecast(data: WeatherData, modifier: GlanceModifier) {
    val days = data.days
    val today = data.todayIndex.coerceIn(0, days.size - 1)

    Box(modifier = modifier.fillMaxWidth()) {
        // Back layer: 7 equal-width columns.
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Top,
        ) {
            days.forEachIndexed { i, day ->
                if (i < today) {
                    PastDayCell(day, GlanceModifier.defaultWeight())
                } else {
                    ForecastColumn(day, accent = i == today, GlanceModifier.defaultWeight())
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
                style = TextStyle(color = white, fontSize = 11.sp),
            )
            Text(
                text = data.placeName,
                style = TextStyle(color = white, fontSize = 13.sp, fontWeight = FontWeight.Medium),
            )
        }
    }
}

/** A past day: short translucent card with just the date label. */
@Composable
private fun PastDayCell(day: DayForecast, modifier: GlanceModifier) {
    Box(modifier = modifier.padding(horizontal = 2.dp)) {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ImageProvider(R.drawable.tile_bg))
                .padding(vertical = 4.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DayLabel(day)
        }
    }
}

/** Today / a future day: full-height band with date label on top, weather below. */
@Composable
private fun ForecastColumn(day: DayForecast, accent: Boolean, modifier: GlanceModifier) {
    val bg = if (accent) R.drawable.tile_bg_accent else R.drawable.tile_bg
    Box(modifier = modifier.fillMaxHeight().padding(horizontal = 2.dp)) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(bg))
                .padding(vertical = 4.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DayLabel(day)
            Spacer(GlanceModifier.defaultWeight())
            Text(
                text = day.emoji,
                style = TextStyle(color = white, fontSize = 16.sp, textAlign = TextAlign.Center),
            )
            Text(
                text = "${day.maxTemp}°",
                style = TextStyle(color = white, fontSize = 11.sp, textAlign = TextAlign.Center),
            )
        }
    }
}

/** Big day-of-month number with the short weekday beneath it. */
@Composable
private fun DayLabel(day: DayForecast) {
    Text(
        text = "${day.dayOfMonth}",
        style = TextStyle(
            color = white,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        ),
    )
    Text(
        text = day.weekdayShort,
        style = TextStyle(color = white, fontSize = 9.sp, textAlign = TextAlign.Center),
    )
}
