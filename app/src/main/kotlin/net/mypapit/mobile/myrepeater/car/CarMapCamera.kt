package net.mypapit.mobile.myrepeater.car

import androidx.car.app.SurfaceCallback
import net.mypapit.mobile.myrepeater.data.db.Repeater
import net.mypapit.mobile.myrepeater.data.location.Geo
import net.mypapit.mobile.myrepeater.data.location.GeoPoint
import net.mypapit.mobile.myrepeater.data.settings.AppSettings
import net.mypapit.mobile.myrepeater.ui.list.RepeaterListItem
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.pow

/** What the map side of the car screen shows. */
data class DirectionView(
    val origin: GeoPoint,
    /** Set when distances are measured from a place the user chose, not from the car. */
    val placeName: String?,
    val items: List<RepeaterListItem>,
    val focusedId: Long?,
    val heading: Heading?,
    /** All active repeaters with coordinates so panning reveals sites beyond the top 5 list. */
    val allRepeaters: List<Repeater> = items.map { it.repeater },
) {
    /** Looks up the focused repeater in the nearby list first, then across all known sites. */
    fun focusedItem(): RepeaterListItem? {
        val id = focusedId ?: return null
        items.firstOrNull { it.repeater.id == id }?.let { return it }
        val repeater = allRepeaters.firstOrNull { it.id == id } ?: return null
        val lat = repeater.latitude ?: return null
        val lon = repeater.longitude ?: return null
        val to = GeoPoint(lat, lon)
        return RepeaterListItem(
            repeater = repeater,
            distanceKm = Geo.distanceKm(origin, to),
            bearingDeg = Geo.bearingDeg(origin, to),
        )
    }
}

/** Target camera centre and zoom for the car map. */
data class CarCameraTarget(val center: GeoPoint, val zoom: Double)

/** Contract implemented by both the MapLibre car renderer and the Canvas fallback renderer. */
interface CarMapSurface : SurfaceCallback {
    fun show(view: DirectionView?)
    fun setDark(dark: Boolean)
    fun setPreferredZoom(zoom: Double)
    fun setPanMode(enabled: Boolean)
    fun focusRepeater(repeaterId: Long)
    fun zoomIn()
    fun zoomOut()
    fun recenter()
    fun setCallbacks(onZoomChanged: (Double) -> Unit, onRepeaterTapped: (Long) -> Unit)
    fun release()
}

/** Zoom bounds, scale-factor conversion, smart focus targeting, and pin hit-testing. */
object CarMapZoom {
    const val DEFAULT_ZOOM = AppSettings.DEFAULT_CAR_MAP_ZOOM
    const val MIN_ZOOM = AppSettings.MIN_CAR_MAP_ZOOM
    const val MAX_ZOOM = AppSettings.MAX_CAR_MAP_ZOOM
    const val ZOOM_STEP = 1.0
    const val DOUBLE_TAP_ZOOM_DELTA = 1.0
    const val OVERVIEW_ZOOM = 5.5
    val DEFAULT_CENTRE = GeoPoint(4.2, 109.0)

    private const val DOUBLE_TAP_SENTINEL = 10f
    private const val MAX_GESTURE_ZOOM_DELTA = 1.5
    private const val TILE_WORLD_DP = 512.0
    private const val EQUATOR_METERS = 40_075_016.686

    fun clampZoom(zoom: Double): Double = AppSettings.clampCarZoom(zoom)

    /** Converts Android Auto's [SurfaceCallback.onScale] factor into a Web Mercator zoom delta. */
    fun zoomDeltaFromScale(scaleFactor: Float): Double {
        if (!scaleFactor.isFinite() || scaleFactor <= 0f) return 0.0
        if (scaleFactor >= DOUBLE_TAP_SENTINEL) return DOUBLE_TAP_ZOOM_DELTA
        return (ln(scaleFactor.toDouble()) / ln(2.0)).coerceIn(-MAX_GESTURE_ZOOM_DELTA, MAX_GESTURE_ZOOM_DELTA)
    }

    /**
     * Approximate ground radius (in km) spanned by [radiusPx] at [zoom] and [latitude].
     * Uses MapLibre's 512dp tile convention so the Canvas fallback matches MapLibre zoom levels.
     */
    fun zoomToRadiusKm(zoom: Double, latitude: Double, radiusPx: Float, dpi: Int = 160): Double {
        val safeDpi = dpi.coerceAtLeast(120)
        val radiusDp = (radiusPx.coerceAtLeast(1f) * 160.0) / safeDpi
        val latCos = cos(Math.toRadians(latitude)).coerceAtLeast(0.1)
        val metersPerDp = (EQUATOR_METERS * latCos) / (TILE_WORLD_DP * 2.0.pow(zoom))
        return (metersPerDp * radiusDp / 1000.0).coerceIn(1.0, 2_000.0)
    }

    fun sameFrameKm(zoom: Double): Double =
        (50.0 * 2.0.pow(DEFAULT_ZOOM - clampZoom(zoom))).coerceIn(5.0, 160.0)

    /**
     * Picks where the car map camera should point.
     *
     * While [userPanned] is true, passive GPS/data refreshes return `null` so the camera never
     * yanks away while the driver is panning. An [explicitFocus] (tapping a list row) frames
     * both origin and repeater when close, or centres on the repeater at [preferredZoom] when far.
     */
    fun computeFocusTarget(
        origin: GeoPoint,
        focused: RepeaterListItem?,
        preferredZoom: Double,
        userPanned: Boolean,
        explicitFocus: Boolean,
    ): CarCameraTarget? {
        if (userPanned && !explicitFocus) return null
        val zoom = clampZoom(preferredZoom)
        if (explicitFocus && focused?.repeater?.hasCoordinates == true) {
            val targetPoint = GeoPoint(focused.repeater.latitude!!, focused.repeater.longitude!!)
            val distKm = focused.distanceKm ?: Geo.distanceKm(origin, targetPoint)
            val center = if (distKm <= sameFrameKm(zoom)) {
                GeoPoint(
                    (origin.latitude + targetPoint.latitude) / 2.0,
                    (origin.longitude + targetPoint.longitude) / 2.0,
                )
            } else {
                targetPoint
            }
            return CarCameraTarget(center = center, zoom = zoom)
        }
        return CarCameraTarget(center = origin, zoom = zoom)
    }

    /** Finds the closest repeater pin within [maxDistancePx] of a tap, favouring [nearbyIds] on close pins. */
    fun hitTestRepeater(
        tapX: Float,
        tapY: Float,
        candidates: List<Pair<Repeater, Pair<Float, Float>>>,
        nearbyIds: Set<Long>,
        maxDistancePx: Float,
    ): Repeater? {
        var best: Repeater? = null
        var bestScore = Float.MAX_VALUE
        for ((repeater, point) in candidates) {
            val dist = hypot(point.first - tapX, point.second - tapY)
            if (dist > maxDistancePx) continue
            // Slight tie-breaker preference for listed nearby pins when markers overlap closely.
            val score = if (repeater.id in nearbyIds) dist * 0.85f else dist
            if (score < bestScore) {
                bestScore = score
                best = repeater
            }
        }
        return best
    }
}
