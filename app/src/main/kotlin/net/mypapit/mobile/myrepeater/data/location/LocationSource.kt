package net.mypapit.mobile.myrepeater.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map

/** A position fix, with the direction and speed of travel when the device reports them. */
data class LocationFix(val point: GeoPoint, val courseDeg: Float? = null, val speedMps: Float? = null)

/**
 * The device's position, read from the platform location service while the
 * app is on screen. Positions stay on the device: they are only used to sort
 * the repeater list and aim the compass.
 */
class LocationSource(private val context: Context) {

    fun hasPermission(): Boolean = PERMISSIONS.any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    /** Emits the last known position at once (if any), then updates. Needs location permission. */
    fun updates(): Flow<GeoPoint> = fixes().map { it.point }

    /** As [updates], with the direction and speed of travel. Used by the car screen. */
    @SuppressLint("MissingPermission")
    fun fixes(): Flow<LocationFix> = callbackFlow {
        val manager = context.getSystemService(LocationManager::class.java)
        if (manager == null || !hasPermission()) {
            close()
            return@callbackFlow
        }

        var best: Location? = null
        fun offer(location: Location) {
            if (isBetter(location, best)) {
                best = location
                trySend(
                    LocationFix(
                        point = GeoPoint(location.latitude, location.longitude),
                        courseDeg = if (location.hasBearing()) location.bearing else null,
                        speedMps = if (location.hasSpeed()) location.speed else null,
                    ),
                )
            }
        }

        val providers = providers(manager)
        val listener = LocationListenerCompat { offer(it) }
        val request = LocationRequestCompat.Builder(UPDATE_INTERVAL_MS)
            .setMinUpdateDistanceMeters(MIN_DISTANCE_M)
            .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY)
            .build()
        try {
            for (provider in providers) {
                manager.getLastKnownLocation(provider)?.let(::offer)
                LocationManagerCompat.requestLocationUpdates(
                    manager, provider, request, ContextCompat.getMainExecutor(context), listener,
                )
            }
        } catch (e: SecurityException) {
            close()
        } catch (e: IllegalArgumentException) {
            // A provider disappeared between listing and registering; carry on with the rest.
        }

        awaitClose { LocationManagerCompat.removeUpdates(manager, listener) }
    }.conflate()

    private fun providers(manager: LocationManager): List<String> {
        val all = manager.allProviders
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && LocationManager.FUSED_PROVIDER in all) {
            return listOf(LocationManager.FUSED_PROVIDER)
        }
        return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).filter { it in all }
    }

    /** Prefers fresher fixes, and among fixes of similar age the more accurate one. */
    private fun isBetter(candidate: Location, current: Location?): Boolean {
        if (current == null) return true
        val newerBy = candidate.time - current.time
        if (newerBy > STALE_AFTER_MS) return true
        if (newerBy < -STALE_AFTER_MS) return false
        return candidate.accuracy <= current.accuracy
    }

    companion object {
        val PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
        private const val UPDATE_INTERVAL_MS = 10_000L
        private const val MIN_DISTANCE_M = 25f
        private const val STALE_AFTER_MS = 30_000L
    }
}
