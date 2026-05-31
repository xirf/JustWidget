package id.andka.justwidget.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Fetches weather from Open-Meteo (free, no API key, 7-day forecast). */
object WeatherRepository {

    private const val BASE = "https://api.open-meteo.com/v1/forecast"

    suspend fun fetch(lat: Double, lon: Double, placeName: String): WeatherData =
        withContext(Dispatchers.IO) {
            // 3 past days + today + 3 future days = 7 entries, today at index 3.
            val url = URL(
                "$BASE?latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,weather_code,wind_speed_10m,wind_direction_10m,is_day" +
                    "&daily=weather_code,temperature_2m_max,temperature_2m_min" +
                    "&past_days=3&forecast_days=4&timezone=auto&wind_speed_unit=kmh"
            )
            val json = url.readJson()
            parse(json, placeName)
        }

    private fun URL.readJson(): JSONObject {
        val conn = (openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            requestMethod = "GET"
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) error("Open-Meteo HTTP $code")
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            return JSONObject(text)
        } finally {
            conn.disconnect()
        }
    }

    private fun parse(root: JSONObject, placeName: String): WeatherData {
        val current = root.getJSONObject("current")
        val isDay = current.optInt("is_day", 1) == 1
        val currentCode = current.getInt("weather_code")
        val windDir = current.getDouble("wind_direction_10m").toInt()

        val daily = root.getJSONObject("daily")
        val times = daily.getJSONArray("time")
        val codes = daily.getJSONArray("weather_code")
        val maxs = daily.getJSONArray("temperature_2m_max")
        val mins = daily.getJSONArray("temperature_2m_min")

        val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val weekdayFmt = SimpleDateFormat("EEE", Locale.getDefault())
        // Match "today" using the API's own timezone (timezone=auto), so the
        // centered day is correct even across timezone boundaries.
        val tzId = root.optString("timezone", "UTC")
        dayFmt.timeZone = java.util.TimeZone.getTimeZone(tzId)
        weekdayFmt.timeZone = java.util.TimeZone.getTimeZone(tzId)
        val todayStr = dayFmt.format(java.util.Date())
        val cal = Calendar.getInstance()

        var todayIndex = -1
        val days = ArrayList<DayForecast>(times.length())
        for (i in 0 until times.length()) {
            val dateStr = times.getString(i)
            if (dateStr == todayStr) todayIndex = i
            val date = dayFmt.parse(dateStr)
            cal.time = date ?: continue
            days += DayForecast(
                dayOfMonth = cal.get(Calendar.DAY_OF_MONTH),
                weekdayShort = weekdayFmt.format(cal.time),
                emoji = wmoToEmoji(codes.getInt(i), isDay = true),
                minTemp = Math.round(mins.getDouble(i)).toInt(),
                maxTemp = Math.round(maxs.getDouble(i)).toInt(),
            )
        }
        // Fallback: with past_days=3 today is the 4th entry (index 3).
        if (todayIndex < 0) todayIndex = 3.coerceAtMost(days.size - 1)

        return WeatherData(
            placeName = placeName,
            currentTemp = Math.round(current.getDouble("temperature_2m")).toInt(),
            currentEmoji = wmoToEmoji(currentCode, isDay),
            windSpeedKmh = Math.round(current.getDouble("wind_speed_10m")).toInt(),
            windCompass = degreesToCompass(windDir),
            days = days,
            todayIndex = todayIndex,
            updatedAtMillis = System.currentTimeMillis(),
        )
    }
}
