package id.andka.justwidget.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

/** Persists all user-configurable app preferences (theme, widget colors, location). */
object ThemeStore {

    private val KEY_DARK             = booleanPreferencesKey("dark_theme")
    private val KEY_ACCENT           = stringPreferencesKey("accent_color")
    private val KEY_WIDGET_TEXT_DARK = booleanPreferencesKey("widget_text_dark")
    private val KEY_WIDGET_BG_STYLE  = stringPreferencesKey("widget_bg_style")
    private val KEY_CUSTOM_LOCATION  = stringPreferencesKey("custom_location")

    // ── App theme ─────────────────────────────────────────────────────────────

    /** true = dark mode, false = light mode. Defaults to dark. */
    fun isDarkFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[KEY_DARK] ?: true }

    suspend fun setDark(context: Context, dark: Boolean) {
        context.dataStore.edit { it[KEY_DARK] = dark }
    }

    // ── Accent color ──────────────────────────────────────────────────────────

    /** Hex color string, e.g. "#C084FC". Defaults to bright purple. */
    fun accentColorFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[KEY_ACCENT] ?: "#C084FC" }

    suspend fun setAccentColor(context: Context, hex: String) {
        context.dataStore.edit { it[KEY_ACCENT] = hex }
    }

    // ── Widget text color ─────────────────────────────────────────────────────

    /** true = dark (black) widget text for light wallpapers; false = white text. */
    fun widgetTextDarkFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[KEY_WIDGET_TEXT_DARK] ?: false }

    suspend fun setWidgetTextDark(context: Context, dark: Boolean) {
        context.dataStore.edit { it[KEY_WIDGET_TEXT_DARK] = dark }
    }

    // ── Custom location ────────────────────────────────────────────────────────

    /** Raw JSON: {"name":"X","lat":0.0,"lon":0.0}. Empty string = use GPS. */
    fun customLocationFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[KEY_CUSTOM_LOCATION] ?: "" }

    suspend fun getCustomLocation(context: Context): Place? {
        val json = customLocationFlow(context).first()
        if (json.isEmpty()) return null
        return try {
            val obj = JSONObject(json)
            Place(
                lat = obj.getDouble("lat"),
                lon = obj.getDouble("lon"),
                name = obj.getString("name")
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun setCustomLocation(context: Context, name: String, lat: Double, lon: Double) {
        context.dataStore.edit {
            it[KEY_CUSTOM_LOCATION] = """{"name":"$name","lat":$lat,"lon":$lon}"""
        }
    }

    suspend fun clearCustomLocation(context: Context) {
        context.dataStore.edit { it.remove(KEY_CUSTOM_LOCATION) }
    }

    // ── Widget Background Style ───────────────────────────────────────────────

    /** "dark" | "light" | "transparent". Defaults to "dark". */
    fun widgetBgStyleFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[KEY_WIDGET_BG_STYLE] ?: "dark" }

    suspend fun setWidgetBgStyle(context: Context, style: String) {
        context.dataStore.edit { it[KEY_WIDGET_BG_STYLE] = style }
    }
}
