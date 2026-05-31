package id.andka.justwidget.widget

import androidx.datastore.preferences.core.stringPreferencesKey
import id.andka.justwidget.data.DayForecast
import id.andka.justwidget.data.WeatherData
import org.json.JSONArray
import org.json.JSONObject

/** Serializes [WeatherData] to/from JSON so it can live in Glance preferences. */
object WeatherStore {

    val KEY_JSON = stringPreferencesKey("weather_json")

    fun serialize(data: WeatherData): String {
        val days = JSONArray()
        data.days.forEach { d ->
            days.put(
                JSONObject()
                    .put("dom", d.dayOfMonth)
                    .put("wd", d.weekdayShort)
                    .put("emoji", d.emoji)
                    .put("min", d.minTemp)
                    .put("max", d.maxTemp)
            )
        }
        return JSONObject()
            .put("place", data.placeName)
            .put("temp", data.currentTemp)
            .put("emoji", data.currentEmoji)
            .put("wind", data.windSpeedKmh)
            .put("compass", data.windCompass)
            .put("today", data.todayIndex)
            .put("updated", data.updatedAtMillis)
            .put("days", days)
            .toString()
    }

    fun deserialize(json: String?): WeatherData? {
        if (json.isNullOrBlank()) return null
        return try {
            val o = JSONObject(json)
            val arr = o.getJSONArray("days")
            val days = ArrayList<DayForecast>(arr.length())
            for (i in 0 until arr.length()) {
                val d = arr.getJSONObject(i)
                days += DayForecast(
                    dayOfMonth = d.getInt("dom"),
                    weekdayShort = d.getString("wd"),
                    emoji = d.getString("emoji"),
                    minTemp = d.getInt("min"),
                    maxTemp = d.getInt("max"),
                )
            }
            WeatherData(
                placeName = o.getString("place"),
                currentTemp = o.getInt("temp"),
                currentEmoji = o.getString("emoji"),
                windSpeedKmh = o.getInt("wind"),
                windCompass = o.getString("compass"),
                days = days,
                todayIndex = o.optInt("today", 3),
                updatedAtMillis = o.getLong("updated"),
            )
        } catch (e: Exception) {
            null
        }
    }
}
