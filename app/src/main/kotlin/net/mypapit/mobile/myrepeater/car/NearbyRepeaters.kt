package net.mypapit.mobile.myrepeater.car

import net.mypapit.mobile.myrepeater.data.db.Repeater
import net.mypapit.mobile.myrepeater.data.db.RepeaterStatus
import net.mypapit.mobile.myrepeater.data.location.Geo
import net.mypapit.mobile.myrepeater.data.location.GeoPoint
import net.mypapit.mobile.myrepeater.ui.list.RepeaterFormat
import net.mypapit.mobile.myrepeater.ui.list.RepeaterListItem
import net.mypapit.mobile.myrepeater.ui.list.RepeaterSorter
import java.util.Locale
import kotlin.math.roundToInt

/** Which repeaters the car screen lists, and which of them is in focus. */
object NearbyRepeaters {
    const val MAX_DISTANCE_KM = 100.0
    const val MAX_ROWS = 5

    /** The list is only put in a new order after the car has moved this far, so rows do not jump about. */
    const val REORDER_AFTER_KM = 1.0

    /**
     * The nearest repeaters to [from], nearest first: at most [limit], none
     * further than [MAX_DISTANCE_KM], and none that is off the air or has no
     * known position.
     */
    fun select(repeaters: List<Repeater>, from: GeoPoint, limit: Int = MAX_ROWS): List<RepeaterListItem> =
        RepeaterSorter.arrange(
            repeaters.filter { it.status != RepeaterStatus.OFF_AIR && it.hasCoordinates },
            from,
        )
            .filter { item -> item.distanceKm?.let { it <= MAX_DISTANCE_KM } == true }
            .take(limit.coerceIn(0, MAX_ROWS))

    /** The same repeaters in the same order, measured again from a new position. */
    fun remeasure(items: List<RepeaterListItem>, from: GeoPoint): List<RepeaterListItem> =
        items.map { item ->
            val to = GeoPoint(item.repeater.latitude!!, item.repeater.longitude!!)
            item.copy(distanceKm = Geo.distanceKm(from, to), bearingDeg = Geo.bearingDeg(from, to))
        }

    /** True when the car has moved far enough from where the list was last ordered. */
    fun shouldReorder(orderedAt: GeoPoint?, now: GeoPoint): Boolean =
        orderedAt == null || Geo.distanceKm(orderedAt, now) >= REORDER_AFTER_KM

    /** Keeps the repeater the driver chose while it is still listed; otherwise the nearest. */
    fun focus(previousId: Long?, items: List<RepeaterListItem>): Long? =
        items.firstOrNull { it.repeater.id == previousId }?.repeater?.id ?: items.firstOrNull()?.repeater?.id
}

/** The short texts shown on the car screen. */
object CarRowText {
    /** Callsign and listening frequency. This is the part of a row that must not change on a refresh. */
    fun title(repeater: Repeater): String =
        listOfNotNull(repeater.callsign, repeater.downlinkMhz?.let(RepeaterFormat::frequency)).joinToString("  ")

    /** Offset and tone, for example "−0.6 · Tone 203.5". Empty when the source gives neither. */
    fun tuning(repeater: Repeater, toneLabel: (String) -> String): String =
        listOfNotNull(
            repeater.offsetMhz?.let(RepeaterFormat::offset),
            repeater.tone.trim().takeIf { it.isNotEmpty() }?.let(toneLabel),
        ).joinToString(" · ")

    /** Distance and compass point, for example "42 km NE". */
    fun whereabouts(item: RepeaterListItem): String {
        val km = item.distanceKm ?: return ""
        val point = item.bearingDeg?.let(Geo::compassPoint)
        return listOfNotNull("${RepeaterFormat.distance(km)} km", point).joinToString(" ")
    }

    /** Compass point and three-figure bearing, for example "NE 042°". */
    fun bearing(bearingDeg: Double): String {
        val whole = bearingDeg.roundToInt().mod(360)
        return String.format(Locale.US, "%s %03d°", Geo.compassPoint(bearingDeg), whole)
    }
}

enum class HeadingSource {
    /** The vehicle's own compass. */
    CAR_COMPASS,

    /** The direction the car is moving in, from successive position fixes. */
    TRAVEL,
}

/** Which way the car is facing, in degrees clockwise from north, and how that is known. */
data class Heading(val degrees: Double, val source: HeadingSource)

object Headings {
    /** About 10 km/h. Below this a direction of travel is too unsteady to use. */
    const val MIN_TRAVEL_SPEED_MPS = 3.0f

    /** The arrow moves in steps of this many degrees, never smoothly. */
    const val STEP_DEG = 5

    /**
     * The car's compass when it has one; failing that the direction of travel
     * while moving; otherwise nothing, and the display stays north-up. A
     * phone's magnetic compass is never used: it is unreliable inside a car.
     */
    fun pick(carCompassDeg: Float?, courseDeg: Float?, speedMps: Float?): Heading? = when {
        carCompassDeg != null && carCompassDeg.isFinite() ->
            Heading(normalise(carCompassDeg.toDouble()), HeadingSource.CAR_COMPASS)
        courseDeg != null && courseDeg.isFinite() && speedMps != null && speedMps >= MIN_TRAVEL_SPEED_MPS ->
            Heading(normalise(courseDeg.toDouble()), HeadingSource.TRAVEL)
        else -> null
    }

    /** Where the arrow points on screen: the bearing itself when north is up, else relative to [heading]. */
    fun arrowAngle(bearingDeg: Double, heading: Heading?): Double =
        normalise(bearingDeg - (heading?.degrees ?: 0.0))

    /** [degrees] rounded to the nearest step, in 0 until 360. */
    fun step(degrees: Double, stepDeg: Int = STEP_DEG): Int =
        ((normalise(degrees) / stepDeg).roundToInt() * stepDeg).mod(360)

    fun normalise(degrees: Double): Double = ((degrees % 360.0) + 360.0) % 360.0
}

/**
 * A flat, north-up view of the area round [centre]: [radiusKm] of ground spans
 * [radiusPx] pixels from the screen point ([centreX], [centreY]). Accurate
 * enough over the 100 km the car screen covers.
 */
class MapFrame(
    val centre: GeoPoint,
    val radiusKm: Double,
    val centreX: Float,
    val centreY: Float,
    val radiusPx: Float,
) {
    private val pxPerKm = radiusPx / radiusKm
    private val kmPerDegLon = KM_PER_DEG_LAT * kotlin.math.cos(Math.toRadians(centre.latitude))

    fun x(longitude: Double): Float = (centreX + (longitude - centre.longitude) * kmPerDegLon * pxPerKm).toFloat()
    fun y(latitude: Double): Float = (centreY - (latitude - centre.latitude) * KM_PER_DEG_LAT * pxPerKm).toFloat()

    /** How many degrees of latitude and of longitude [pixels] on screen cover. */
    fun latitudeSpan(pixels: Float): Double = pixels / pxPerKm / KM_PER_DEG_LAT
    fun longitudeSpan(pixels: Float): Double = pixels / pxPerKm / kmPerDegLon

    companion object {
        const val KM_PER_DEG_LAT = 110.574
        private const val MIN_RADIUS_KM = 15.0
        private const val MARGIN = 1.2

        /** Wide enough to show every listed repeater with some room to spare. */
        fun radiusFor(items: List<RepeaterListItem>): Double =
            maxOf(MIN_RADIUS_KM, (items.mapNotNull { it.distanceKm }.maxOrNull() ?: 0.0) * MARGIN)
    }
}
