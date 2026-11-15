package net.mypapit.mobile.myrepeater.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
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

    private companion object {
        val LIST_NOTICE_DISMISSED = booleanPreferencesKey("list_notice_dismissed")
        val APRS_ENABLED = booleanPreferencesKey("aprs_enabled")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val LOCATION_MODE = stringPreferencesKey("location_mode")
        val PRESET_KEY = stringPreferencesKey("preset_key")
    }
}
