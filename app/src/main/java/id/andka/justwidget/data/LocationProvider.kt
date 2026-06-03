package id.andka.justwidget.data

import android.annotation.SuppressLint
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

/** A resolved place: coordinates + a human-readable name. */
data class Place(val lat: Double, val lon: Double, val name: String)

object LocationProvider {

    /** Fallback used when location permission is denied or no fix is available. */
    val MAGETAN = Place(lat = -7.65, lon = 111.33, name = "Magetan")

    fun hasPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * Returns the device's current place, or [MAGETAN] when permission is missing
     * or no location can be obtained.
     */
    @SuppressLint("MissingPermission") // guarded by hasPermission() above
    suspend fun current(context: Context): Place {
        val custom = ThemeStore.getCustomLocation(context)
        if (custom != null) return custom

        if (!hasPermission(context)) return MAGETAN
        return try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val loc = client
                .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .await()
                ?: client.lastLocation.await()
                ?: return MAGETAN
            val name = reverseGeocode(context, loc.latitude, loc.longitude) ?: MAGETAN.name
            Place(loc.latitude, loc.longitude, name)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            MAGETAN
        }
    }

    suspend fun resolveLocationName(context: Context, name: String): Place? =
        withContext(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                val results = Geocoder(context, Locale.getDefault()).getFromLocationName(name, 1)
                val a = results?.firstOrNull() ?: return@withContext null
                val resolvedName = a.locality ?: a.subAdminArea ?: a.adminArea ?: a.countryName ?: name
                Place(a.latitude, a.longitude, resolvedName)
            } catch (e: Exception) {
                null
            }
        }

    private suspend fun reverseGeocode(context: Context, lat: Double, lon: Double): String? =
        withContext(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                val results = Geocoder(context, Locale.getDefault()).getFromLocation(lat, lon, 1)
                val a = results?.firstOrNull() ?: return@withContext null
                a.locality ?: a.subAdminArea ?: a.adminArea ?: a.countryName
            } catch (e: Exception) {
                null
            }
        }
}
