package net.mypapit.mobile.myrepeater.car

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import androidx.car.app.AppManager
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.mypapit.mobile.myrepeater.R
import net.mypapit.mobile.myrepeater.RepeaterApp

/**
 * The Android Auto side of the app. The car connects to this service and draws
 * the templates it is given; the app has no screens of its own in the car.
 */
class RepeaterCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator =
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            // Debug builds also accept the desktop head unit used for testing.
            HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        } else {
            // Released builds draw only for Google's own car hosts, which covers every Android Auto car.
            HostValidator.Builder(applicationContext)
                .addAllowedHosts(R.array.car_hosts_allowlist)
                .build()
        }

    override fun onCreateSession(): Session = RepeaterSession()
}

class RepeaterSession : Session() {
    private var renderer: DirectionRenderer? = null

    override fun onCreateScreen(intent: Intent): Screen {
        val container = (carContext.applicationContext as RepeaterApp).container
        val renderer = DirectionRenderer(carContext).also { this.renderer = it }
        renderer.setDark(carContext.isDarkMode)
        carContext.getCarService(AppManager::class.java).setSurfaceCallback(renderer)

        lifecycleScope.launch {
            renderer.setOutline(withContext(Dispatchers.Default) { MapOutline.load(carContext) })
        }
        return NearbyScreen(carContext, container, renderer)
    }

    /** The car switches between its day and night looks; the map follows. */
    override fun onCarConfigurationChanged(newConfiguration: Configuration) {
        renderer?.setDark(carContext.isDarkMode)
    }
}
