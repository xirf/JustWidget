package id.andka.justwidget.data

/**
 * Maps WMO weather interpretation codes (as returned by Open-Meteo) to emoji,
 * matching the look of the original mockup (⛅ 🌦️ 🌧️ 🌞 …).
 *
 * Reference: https://open-meteo.com/en/docs (WMO Weather interpretation codes)
 */
fun wmoToEmoji(code: Int, isDay: Boolean = true): String = when (code) {
    0 -> if (isDay) "🌞" else "🌙"            // Clear sky
    1 -> if (isDay) "🌤️" else "🌙"           // Mainly clear
    2 -> "⛅"                                  // Partly cloudy
    3 -> "☁️"                                  // Overcast
    45, 48 -> "🌫️"                            // Fog
    51, 53, 55 -> "🌦️"                        // Drizzle
    56, 57 -> "🌧️"                            // Freezing drizzle
    61, 63, 65 -> "🌧️"                        // Rain
    66, 67 -> "🌧️"                            // Freezing rain
    71, 73, 75, 77 -> "🌨️"                    // Snow
    80, 81, 82 -> "🌦️"                        // Rain showers
    85, 86 -> "🌨️"                            // Snow showers
    95 -> "⛈️"                                 // Thunderstorm
    96, 99 -> "⛈️"                             // Thunderstorm with hail
    else -> "❓"
}

/**
 * Converts a wind direction in degrees to an 8-point compass label,
 * e.g. 225° -> "Southwest".
 */
fun degreesToCompass(degrees: Int): String {
    val dirs = arrayOf(
        "North", "Northeast", "East", "Southeast",
        "South", "Southwest", "West", "Northwest",
    )
    val idx = (((degrees % 360) + 360) % 360 + 22) / 45 % 8
    return dirs[idx]
}
