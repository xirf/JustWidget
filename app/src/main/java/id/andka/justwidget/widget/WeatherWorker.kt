package id.andka.justwidget.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import id.andka.justwidget.WeatherDataStore
import id.andka.justwidget.data.LocationProvider
import id.andka.justwidget.data.ThemeStore
import id.andka.justwidget.data.WeatherRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/** Fetches location + weather and writes it into every widget instance. */
class WeatherWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        return try {
            val place = LocationProvider.current(context)
            val data = WeatherRepository.fetch(place.lat, place.lon, place.name)
            val json = WeatherStore.serialize(data)

            val accent = ThemeStore.accentColorFlow(context).first()
            val textDark = ThemeStore.widgetTextDarkFlow(context).first()
            val bgStyle = ThemeStore.widgetBgStyleFlow(context).first()

            val ids = GlanceAppWidgetManager(context).getGlanceIds(WeatherWidget::class.java)
            ids.forEach { id ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[WeatherStore.KEY_JSON] = json
                        this[WeatherStore.KEY_ACCENT] = accent
                        this[WeatherStore.KEY_TEXT_DARK] = textDark
                        this[WeatherStore.KEY_BG_STYLE] = bgStyle
                    }
                }
            }
            WeatherWidget().updateAll(context)
            WeatherDataStore.save(context, json)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val PERIODIC = "weather_periodic"
        private const val ONE_SHOT = "weather_now"

        private fun networkConstraints() = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /** Schedules the ~30-min recurring refresh (kept if already scheduled). */
        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<WeatherWorker>(30, TimeUnit.MINUTES)
                .setConstraints(networkConstraints())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }

        /** Forces an immediate one-off refresh (e.g. widget added / "Refresh now"). */
        fun refreshNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<WeatherWorker>()
                .setConstraints(networkConstraints())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_SHOT, ExistingWorkPolicy.REPLACE, request
            )
        }

        fun cancelAll(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC)
        }
    }
}
