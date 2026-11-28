package net.mypapit.mobile.myrepeater.data.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppSettingsTest {

    @Test
    fun `car map zoom defaults cleanly and persists clamped values`() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        val settings = AppSettings(context)

        assertEquals(AppSettings.DEFAULT_CAR_MAP_ZOOM, settings.settings.first().carMapZoom, 1e-6)

        settings.setCarMapZoom(12.2)
        assertEquals(12.2, settings.settings.first().carMapZoom, 1e-6)

        settings.setCarMapZoom(99.0)
        assertEquals(AppSettings.MAX_CAR_MAP_ZOOM, settings.settings.first().carMapZoom, 1e-6)

        settings.setCarMapZoom(1.0)
        assertEquals(AppSettings.MIN_CAR_MAP_ZOOM, settings.settings.first().carMapZoom, 1e-6)
    }
}
