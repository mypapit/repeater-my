package net.mypapit.mobile.myrepeater.data.location

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.Surface
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Reads which way the top of the screen is facing, using the rotation-vector sensor. */
class CompassSource(private val context: Context) {

    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val sensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    val isAvailable: Boolean get() = sensor != null

    /** Smoothed heading in degrees clockwise from magnetic north (0 until 360). */
    fun magneticHeadings(): Flow<Float> = callbackFlow {
        val manager = sensorManager
        val rotationSensor = sensor
        if (manager == null || rotationSensor == null) {
            close()
            return@callbackFlow
        }
        val display = context.getSystemService(DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY)

        val rotation = FloatArray(9)
        val remapped = FloatArray(9)
        val orientation = FloatArray(3)
        var smoothSin = 0.0
        var smoothCos = 0.0
        var primed = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotation, event.values)
                // Keep "up the screen" as forward whichever way the display is rotated.
                val (axisX, axisY) = when (display?.rotation) {
                    Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
                    Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
                    Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
                    else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
                }
                SensorManager.remapCoordinateSystem(rotation, axisX, axisY, remapped)
                SensorManager.getOrientation(remapped, orientation)

                // Average on the circle so the needle does not spin when crossing north.
                val azimuth = orientation[0].toDouble()
                if (primed) {
                    smoothSin += SMOOTHING * (sin(azimuth) - smoothSin)
                    smoothCos += SMOOTHING * (cos(azimuth) - smoothCos)
                } else {
                    smoothSin = sin(azimuth)
                    smoothCos = cos(azimuth)
                    primed = true
                }
                val degrees = Math.toDegrees(atan2(smoothSin, smoothCos))
                trySend(((degrees + 360.0) % 360.0).toFloat())
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        manager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        awaitClose { manager.unregisterListener(listener) }
    }.conflate()

    companion object {
        private const val SMOOTHING = 0.15

        /** Degrees to add to a magnetic heading at [point] to get a true heading. */
        fun declination(point: GeoPoint, timeMillis: Long = System.currentTimeMillis()): Float =
            GeomagneticField(point.latitude.toFloat(), point.longitude.toFloat(), 0f, timeMillis).declination
    }
}
