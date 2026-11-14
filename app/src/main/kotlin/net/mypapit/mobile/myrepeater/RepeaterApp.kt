package net.mypapit.mobile.myrepeater

import android.app.Application
import android.content.Context
import java.util.concurrent.TimeUnit
import net.mypapit.mobile.myrepeater.data.aprs.AprsRepository
import net.mypapit.mobile.myrepeater.data.db.AppDatabase
import net.mypapit.mobile.myrepeater.data.location.CompassSource
import net.mypapit.mobile.myrepeater.data.location.LocationSource
import net.mypapit.mobile.myrepeater.data.location.PresetLocation
import net.mypapit.mobile.myrepeater.data.location.PresetLocations
import net.mypapit.mobile.myrepeater.data.settings.AppSettings
import net.mypapit.mobile.myrepeater.data.sync.SyncPrefs
import net.mypapit.mobile.myrepeater.data.sync.SyncRepository
import okhttp3.OkHttpClient

class RepeaterApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

/** App-wide singletons, built by hand; the app is small enough not to need a DI framework. */
class AppContainer(context: Context) {
    val database: AppDatabase = AppDatabase.create(context)
    val syncPrefs = SyncPrefs(context)
    val appSettings = AppSettings(context)
    val locationSource = LocationSource(context)
    val compassSource = CompassSource(context)
    val presets: List<PresetLocation> by lazy { PresetLocations.load(context) }

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val syncRepository = SyncRepository(http, database, syncPrefs, context.cacheDir)
    val aprsRepository = AprsRepository(http, context.cacheDir)
}
