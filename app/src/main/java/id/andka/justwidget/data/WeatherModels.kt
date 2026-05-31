package id.andka.justwidget.data

/** A single day in the forecast strip. */
data class DayForecast(
    val dayOfMonth: Int,   // e.g. 10
    val weekdayShort: String, // e.g. "Tue"
    val emoji: String,     // weather emoji for that day
    val minTemp: Int,      // °C
    val maxTemp: Int,      // °C
)

/** Everything the widget needs to render. Serialized into Glance preferences. */
data class WeatherData(
    val placeName: String,
    val currentTemp: Int,      // °C
    val currentEmoji: String,
    val windSpeedKmh: Int,
    val windCompass: String,   // e.g. "Southwest"
    val days: List<DayForecast>,
    val todayIndex: Int,       // index into [days] that is "today" (centered at pos 4)
    val updatedAtMillis: Long,
)
