package net.mypapit.mobile.myrepeater.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(name = "settings")

enum class LocationMode {
    /** Use the device's own position (needs the location permission). */
    DEVICE,

    /** Use a place the user picked from the bundled list. */
    PRESET,
}

data class Settings(
    val onboardingDone: Boolean = false,
    val locationMode: LocationMode = LocationMode.DEVICE,
    /** [net.mypapit.mobile.myrepeater.data.location.PresetLocation.key] of the chosen place, if any. */
    val presetKey: String? = null,
    /** Experimental: show recently heard APRS stations on the map. Off unless the user turns it on. */
    val aprsEnabled: Boolean = false,
    /** The one-line note above the repeater list has been closed by the user. */
    val listNoticeDismissed: Boolean = false,
    /** Preferred zoom level on the Android Auto map. */
    val carMapZoom: Double = AppSettings.DEFAULT_CAR_MAP_ZOOM,
)

class AppSettings(private val context: Context) {

    val settings: Flow<Settings> = context.settingsStore.data.map { prefs ->
        Settings(
            onboardingDone = prefs[ONBOARDING_DONE] ?: false,
            locationMode = prefs[LOCATION_MODE]
                ?.let { name -> LocationMode.entries.firstOrNull { it.name == name } }
                ?: LocationMode.DEVICE,
            presetKey = prefs[PRESET_KEY],
            aprsEnabled = prefs[APRS_ENABLED] ?: false,
            listNoticeDismissed = prefs[LIST_NOTICE_DISMISSED] ?: false,
            carMapZoom = clampCarZoom(prefs[CAR_MAP_ZOOM] ?: DEFAULT_CAR_MAP_ZOOM),
        )
    }

    suspend fun setOnboardingDone() {
        context.settingsStore.edit { it[ONBOARDING_DONE] = true }
    }

    suspend fun useDeviceLocation() {
        context.settingsStore.edit { it[LOCATION_MODE] = LocationMode.DEVICE.name }
    }

    suspend fun usePreset(key: String) {
        context.settingsStore.edit {
            it[LOCATION_MODE] = LocationMode.PRESET.name
            it[PRESET_KEY] = key
        }
    }

    suspend fun setAprsEnabled(enabled: Boolean) {
        context.settingsStore.edit { it[APRS_ENABLED] = enabled }
    }

    suspend fun dismissListNotice() {
        context.settingsStore.edit { it[LIST_NOTICE_DISMISSED] = true }
    }

    suspend fun setCarMapZoom(zoom: Double) {
        val clamped = clampCarZoom(zoom)
        context.settingsStore.edit { it[CAR_MAP_ZOOM] = clamped }
    }

    companion object {
        const val DEFAULT_CAR_MAP_ZOOM = 8.0
        const val MIN_CAR_MAP_ZOOM = 5.5
        const val MAX_CAR_MAP_ZOOM = 15.0
        const val CAR_ZOOM_CLOSE = 9.0
        const val CAR_ZOOM_STANDARD = 8.0
        const val CAR_ZOOM_WIDE = 7.0

        fun clampCarZoom(zoom: Double): Double =
            if (zoom.isFinite()) zoom.coerceIn(MIN_CAR_MAP_ZOOM, MAX_CAR_MAP_ZOOM) else DEFAULT_CAR_MAP_ZOOM

        private val LIST_NOTICE_DISMISSED = booleanPreferencesKey("list_notice_dismissed")
        private val APRS_ENABLED = booleanPreferencesKey("aprs_enabled")
        private val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        private val LOCATION_MODE = stringPreferencesKey("location_mode")
        private val PRESET_KEY = stringPreferencesKey("preset_key")
        private val CAR_MAP_ZOOM = doublePreferencesKey("car_map_zoom")
    }
}
