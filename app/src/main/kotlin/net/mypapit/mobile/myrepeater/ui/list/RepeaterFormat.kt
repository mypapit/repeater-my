package net.mypapit.mobile.myrepeater.ui.list

import java.util.Locale
import kotlin.math.abs

/** Display helpers for repeater values. Pure functions so they can be unit tested. */
object RepeaterFormat {

    /** 145.0875 stays as is, 144.225 is shown as "144.225", 145.0 as "145.000". */
    fun frequency(mhz: Double): String =
        String.format(Locale.US, "%.4f", mhz).removeSuffix("0")

    /** Signed shift with a true minus sign, e.g. "+0.6" or "−0.6". */
    fun offset(mhz: Double): String {
        val magnitude = String.format(Locale.US, "%.3f", abs(mhz)).trimEnd('0').trimEnd('.')
        return when {
            mhz > 0 -> "+$magnitude"
            mhz < 0 -> "−$magnitude"
            else -> "0"
        }
    }

    /** The callsign prefix shown in the avatar when there is no compass, e.g. "9M4". */
    fun prefix(callsign: String): String = callsign.take(3)

    /**
     * A hue in 0..359 that is stable for a given callsign, so the "random"
     * avatar colour does not change between launches or while scrolling.
     */
    fun avatarHue(callsign: String): Float = ((callsign.hashCode() % 360 + 360) % 360).toFloat()
}
