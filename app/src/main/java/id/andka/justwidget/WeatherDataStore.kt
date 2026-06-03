package id.andka.justwidget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import id.andka.justwidget.data.WeatherData
import id.andka.justwidget.widget.WeatherStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.weatherDs: DataStore<Preferences> by preferencesDataStore("app_weather")

/**
 * Regular (non-Glance) DataStore that mirrors the latest weather JSON.
 * Written by [id.andka.justwidget.widget.WeatherWorker] after every
 * successful fetch so [SettingsActivity] can display live data even without
 * a widget instance pinned to the home screen.
 */
object WeatherDataStore {

    private val KEY = stringPreferencesKey("weather_json")

    fun flow(context: Context): Flow<WeatherData?> =
        context.weatherDs.data.map { WeatherStore.deserialize(it[KEY]) }

    suspend fun save(context: Context, json: String) {
        context.weatherDs.edit { it[KEY] = json }
    }
}
