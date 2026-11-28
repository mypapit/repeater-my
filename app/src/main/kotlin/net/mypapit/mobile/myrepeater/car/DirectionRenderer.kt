package net.mypapit.mobile.myrepeater.car

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.view.Surface
import androidx.car.app.SurfaceContainer
import androidx.core.graphics.withRotation
import net.mypapit.mobile.myrepeater.R
import net.mypapit.mobile.myrepeater.data.db.Repeater
import net.mypapit.mobile.myrepeater.data.location.Geo
import net.mypapit.mobile.myrepeater.data.location.GeoPoint
import net.mypapit.mobile.myrepeater.ui.list.RepeaterFormat
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.min
import kotlin.math.roundToInt

/** One coastline ring from the bundled outline. */
class OutlineRing(val home: Boolean, val longitudes: FloatArray, val latitudes: FloatArray) {
    val west = longitudes.min()
    val east = longitudes.max()
    val south = latitudes.min()
    val north = latitudes.max()
}

object MapOutline {
    private const val ASSET = "map_outline.geojson"

    /** Reads the coastline bundled with the app, so the car map needs no connection. */
    fun load(context: Context): List<OutlineRing> = try {
        parse(context.assets.open(ASSET).bufferedReader().use { it.readText() })
    } catch (e: Exception) {
        emptyList()
    }

    fun parse(geoJson: String): List<OutlineRing> {
        val rings = mutableListOf<OutlineRing>()
        val features = JSONObject(geoJson).optJSONArray("features") ?: return rings
        for (i in 0 until features.length()) {
            val feature = features.optJSONObject(i) ?: continue
            val home = feature.optJSONObject("properties")?.optBoolean("home", false) ?: false
            val geometry = feature.optJSONObject("geometry") ?: continue
            val coordinates = geometry.optJSONArray("coordinates") ?: continue
            when (geometry.optString("type")) {
                "Polygon" -> addOuterRing(coordinates, home, rings)
                "MultiPolygon" -> for (p in 0 until coordinates.length()) {
                    coordinates.optJSONArray(p)?.let { addOuterRing(it, home, rings) }
                }
            }
        }
        return rings
    }

    /** Only the outer ring of a polygon is kept; lakes are not drawn at this scale. */
    private fun addOuterRing(polygon: JSONArray, home: Boolean, into: MutableList<OutlineRing>) {
        val ring = polygon.optJSONArray(0) ?: return
        val count = ring.length()
        if (count < 3) return
        val longitudes = FloatArray(count)
        val latitudes = FloatArray(count)
        for (i in 0 until count) {
            val point = ring.optJSONArray(i) ?: return
            longitudes[i] = point.optDouble(0).toFloat()
            latitudes[i] = point.optDouble(1).toFloat()
        }
        into += OutlineRing(home, longitudes, latitudes)
    }
}

/**
 * Interactive Canvas fallback renderer for the car screen.
 *
 * Used both as a safety fallback if [CarMapLibreRenderer] cannot open a VirtualDisplay
 * on a head unit and for deterministic Robolectric preview tests.
 *
 * Uses a compact corner compass HUD and Web-Mercator-matched zoom levels so the map
 * stays closer and usable, and handles pan, pinch-zoom, and pin taps.
 */
class DirectionRenderer(private val context: Context) : CarMapSurface {

    private var surface: Surface? = null
    private var width = 0
    private var height = 0
    private var dpi = 160
    private var visibleArea: Rect? = null
    private var stableArea: Rect? = null
    private var view: DirectionView? = null
    private var dark = true
    private var outline: List<OutlineRing> = emptyList()
    private var lastKey: Any? = null

    private var preferredZoom = CarMapZoom.DEFAULT_ZOOM
    private var currentZoom = CarMapZoom.DEFAULT_ZOOM
    private var cameraCenter: GeoPoint? = null
    private var userPanned = false
    private var onZoomChanged: ((Double) -> Unit)? = null
    private var onRepeaterTapped: ((Long) -> Unit)? = null

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val path = Path()
    private val hudRect = RectF()

    override fun show(view: DirectionView?) {
        val prevId = this.view?.focusedId
        this.view = view
        if (view != null && (!userPanned || cameraCenter == null)) {
            cameraCenter = view.origin
            currentZoom = preferredZoom
        } else if (view != null && prevId != view.focusedId && !userPanned) {
            cameraCenter = view.origin
        }
        draw()
    }

    override fun setDark(dark: Boolean) {
        this.dark = dark
        draw()
    }

    fun setOutline(rings: List<OutlineRing>) {
        outline = rings
        lastKey = null
        draw()
    }

    override fun setPreferredZoom(zoom: Double) {
        val clamped = CarMapZoom.clampZoom(zoom)
        preferredZoom = clamped
        currentZoom = clamped
        draw()
    }

    override fun setPanMode(enabled: Boolean) = Unit

    override fun focusRepeater(repeaterId: Long) {
        val v = view ?: return
        val item = v.items.firstOrNull { it.repeater.id == repeaterId }
            ?: v.allRepeaters.firstOrNull { it.id == repeaterId }?.let { r ->
                val lat = r.latitude ?: return@let null
                val lon = r.longitude ?: return@let null
                val to = GeoPoint(lat, lon)
                net.mypapit.mobile.myrepeater.ui.list.RepeaterListItem(
                    repeater = r,
                    distanceKm = Geo.distanceKm(v.origin, to),
                    bearingDeg = Geo.bearingDeg(v.origin, to),
                )
            }
        val target = CarMapZoom.computeFocusTarget(
            origin = v.origin,
            focused = item,
            preferredZoom = preferredZoom,
            userPanned = userPanned,
            explicitFocus = true,
        ) ?: return
        cameraCenter = target.center
        currentZoom = target.zoom
        val dist = item?.distanceKm ?: 0.0
        userPanned = dist > CarMapZoom.sameFrameKm(target.zoom)
        lastKey = null
        draw()
    }

    override fun zoomIn() {
        applyZoom(currentZoom + CarMapZoom.ZOOM_STEP)
    }

    override fun zoomOut() {
        applyZoom(currentZoom - CarMapZoom.ZOOM_STEP)
    }

    override fun recenter() {
        userPanned = false
        currentZoom = preferredZoom
        cameraCenter = view?.origin
        lastKey = null
        draw()
    }

    override fun setCallbacks(onZoomChanged: (Double) -> Unit, onRepeaterTapped: (Long) -> Unit) {
        this.onZoomChanged = onZoomChanged
        this.onRepeaterTapped = onRepeaterTapped
    }

    override fun release() {
        surface = null
        lastKey = null
    }

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        surface = surfaceContainer.surface
        width = surfaceContainer.width
        height = surfaceContainer.height
        dpi = surfaceContainer.dpi.takeIf { it > 0 } ?: context.resources.configuration.densityDpi
        lastKey = null
        draw()
    }

    override fun onVisibleAreaChanged(visibleArea: Rect) {
        this.visibleArea = Rect(visibleArea)
        draw()
    }

    override fun onStableAreaChanged(stableArea: Rect) {
        this.stableArea = Rect(stableArea)
        draw()
    }

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        surface = null
        lastKey = null
    }

    override fun onScroll(distanceX: Float, distanceY: Float) {
        val frame = currentFrame() ?: return
        userPanned = true
        val center = cameraCenter ?: view?.origin ?: CarMapZoom.DEFAULT_CENTRE
        val nextLat = (center.latitude - frame.latitudeSpan(distanceY)).coerceIn(-15.0, 25.0)
        val nextLon = (center.longitude + frame.longitudeSpan(distanceX)).coerceIn(85.0, 130.0)
        cameraCenter = GeoPoint(nextLat, nextLon)
        lastKey = null
        draw()
    }

    override fun onFling(velocityX: Float, velocityY: Float) {
        if (!velocityX.isFinite() || !velocityY.isFinite()) return
        val maxShift = (min(width, height).coerceAtLeast(200) * 0.35f)
        val dx = (-velocityX * 0.08f).coerceIn(-maxShift, maxShift)
        val dy = (-velocityY * 0.08f).coerceIn(-maxShift, maxShift)
        onScroll(dx, dy)
    }

    override fun onScale(focusX: Float, focusY: Float, scaleFactor: Float) {
        val delta = CarMapZoom.zoomDeltaFromScale(scaleFactor)
        if (delta == 0.0) return
        applyZoom(currentZoom + delta)
    }

    override fun onClick(x: Float, y: Float) {
        val v = view ?: return
        val frame = currentFrame() ?: return
        val nearbyIds = v.items.map { it.repeater.id }.toSet()
        val candidates = (v.allRepeaters.ifEmpty { v.items.map { it.repeater } })
            .filter { it.hasCoordinates }
            .map { r -> r to (frame.x(r.longitude!!) to frame.y(r.latitude!!)) }
        val maxTapPx = 44f * (dpi.coerceAtLeast(120) / 160f)
        val hit = CarMapZoom.hitTestRepeater(x, y, candidates, nearbyIds, maxTapPx) ?: return
        onRepeaterTapped?.invoke(hit.id)
    }

    private fun applyZoom(targetZoom: Double) {
        val clamped = CarMapZoom.clampZoom(targetZoom)
        currentZoom = clamped
        preferredZoom = clamped
        lastKey = null
        draw()
        onZoomChanged?.invoke(clamped)
    }

    private fun currentFrame(): MapFrame? {
        if (width <= 0 || height <= 0) return null
        val area = effectiveArea(width, height)
        return buildFrame(area)
    }

    private fun effectiveArea(w: Int, h: Int): Rect =
        stableArea?.takeIf { !it.isEmpty }
            ?: visibleArea?.takeIf { !it.isEmpty }
            ?: Rect(0, 0, w, h)

    private fun buildFrame(area: Rect): MapFrame {
        val v = view
        val center = cameraCenter ?: v?.origin ?: CarMapZoom.DEFAULT_CENTRE
        val zoom = if (v == null) CarMapZoom.OVERVIEW_ZOOM else currentZoom
        val radiusPx = (min(area.width(), area.height()) / 2f * 0.90f).coerceAtLeast(1f)
        val radiusKm = CarMapZoom.zoomToRadiusKm(zoom, center.latitude, radiusPx, dpi)
        return MapFrame(
            centre = center,
            radiusKm = radiusKm,
            centreX = area.exactCenterX(),
            centreY = area.exactCenterY(),
            radiusPx = radiusPx,
        )
    }

    /**
     * Everything that decides what the picture looks like, rounded to the
     * steps the eye can tell apart: about 200 m of travel and five degrees.
     */
    private fun key(area: Rect): Any? {
        val v = view ?: return listOf(dark, area, width, height, outline.size, currentZoom)
        val focused = v.focusedItem()
        val cam = cameraCenter ?: v.origin
        return listOf(
            dark, area, width, height, outline.size,
            (v.origin.latitude * GRID_PER_DEGREE).roundToInt(),
            (v.origin.longitude * GRID_PER_DEGREE).roundToInt(),
            (cam.latitude * GRID_PER_DEGREE).roundToInt(),
            (cam.longitude * GRID_PER_DEGREE).roundToInt(),
            (currentZoom * 10.0).roundToInt(),
            userPanned,
            v.placeName,
            v.items.map { it.repeater.id },
            v.allRepeaters.size,
            v.focusedId,
            v.heading?.source,
            focused?.bearingDeg?.let { Headings.step(Headings.arrowAngle(it, v.heading)) },
            focused?.distanceKm?.let(RepeaterFormat::distance),
            focused?.bearingDeg?.let(CarRowText::bearing),
        )
    }

    private fun draw() {
        val target = surface ?: return
        if (!target.isValid || width <= 0 || height <= 0) return
        val area = effectiveArea(width, height)
        val key = key(area)
        if (key == lastKey) return

        val canvas = try {
            target.lockCanvas(null)
        } catch (e: Exception) {
            return
        }
        try {
            paint(canvas, area, width, height)
            lastKey = key
        } finally {
            target.unlockCanvasAndPost(canvas)
        }
    }

    /** Draws one picture. [area] is the part of the [width] by [height] surface the car leaves uncovered. */
    internal fun paint(canvas: Canvas, area: Rect, width: Int, height: Int) {
        val colours = if (dark) Palette.DARK else Palette.LIGHT
        canvas.drawColor(colours.sea)
        val v = view
        val frame = buildFrame(area)
        val unit = (min(area.width(), area.height()) / 100f * 0.95f).coerceAtLeast(1.5f)
        val mapArea = RectF(area.left.toFloat(), area.top.toFloat(), area.right.toFloat(), area.bottom.toFloat())

        paintLand(canvas, frame, colours, width, height)
        if (v != null) {
            paintRepeaters(canvas, frame, v, colours, unit, mapArea)
            paintHud(canvas, area, v, colours, unit)
        }
    }

    private fun paintLand(canvas: Canvas, frame: MapFrame, colours: Palette, width: Int, height: Int) {
        // Rings wholly outside the picture are skipped.
        val halfLat = frame.latitudeSpan(height.toFloat())
        val halfLon = frame.longitudeSpan(width.toFloat())
        val south = frame.centre.latitude - halfLat
        val north = frame.centre.latitude + halfLat
        val west = frame.centre.longitude - halfLon
        val east = frame.centre.longitude + halfLon

        stroke.strokeWidth = 1.5f
        for (ring in outline) {
            if (ring.north < south || ring.south > north || ring.east < west || ring.west > east) continue
            path.rewind()
            for (i in ring.longitudes.indices) {
                val x = frame.x(ring.longitudes[i].toDouble())
                val y = frame.y(ring.latitudes[i].toDouble())
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            fill.color = if (ring.home) colours.land else colours.otherLand
            canvas.drawPath(path, fill)
            stroke.color = colours.coast
            canvas.drawPath(path, stroke)
        }
    }

    private fun paintRepeaters(
        canvas: Canvas,
        frame: MapFrame,
        v: DirectionView,
        colours: Palette,
        unit: Float,
        mapArea: RectF,
    ) {
        val originX = frame.x(v.origin.longitude)
        val originY = frame.y(v.origin.latitude)
        val focused = v.focusedItem()
        val nearbyIds = v.items.map { it.repeater.id }.toSet()

        if (focused != null && focused.repeater.hasCoordinates) {
            stroke.color = colours.accent
            stroke.strokeWidth = unit * 1.1f
            canvas.drawLine(
                originX,
                originY,
                frame.x(focused.repeater.longitude!!),
                frame.y(focused.repeater.latitude!!),
                stroke,
            )
        }

        val allPoints = if (v.allRepeaters.isNotEmpty()) v.allRepeaters else v.items.map { it.repeater }
        for (repeater in allPoints) {
            if (!repeater.hasCoordinates) continue
            if (repeater.id in nearbyIds || repeater.id == focused?.repeater?.id) continue
            val x = frame.x(repeater.longitude!!)
            val y = frame.y(repeater.latitude!!)
            if (x < -30f || x > width + 30f || y < -30f || y > height + 30f) continue
            fill.color = colours.pinEdge
            canvas.drawCircle(x, y, unit * 1.9f, fill)
            fill.color = bandColor(repeater.band, colours)
            canvas.drawCircle(x, y, unit * 1.3f, fill)
        }

        for (item in v.items) {
            val x = frame.x(item.repeater.longitude!!)
            val y = frame.y(item.repeater.latitude!!)
            val isFocused = item.repeater.id == focused?.repeater?.id
            fill.color = colours.pinEdge
            canvas.drawCircle(x, y, unit * (if (isFocused) 3.4f else 2.5f), fill)
            fill.color = if (isFocused) colours.accent else bandColor(item.repeater.band, colours)
            canvas.drawCircle(x, y, unit * (if (isFocused) 2.6f else 1.8f), fill)
        }

        if (focused != null && focused.repeater.hasCoordinates) {
            val x = frame.x(focused.repeater.longitude!!)
            val y = frame.y(focused.repeater.latitude!!)
            if (focused.repeater.id !in nearbyIds) {
                fill.color = colours.pinEdge
                canvas.drawCircle(x, y, unit * 3.4f, fill)
                fill.color = colours.accent
                canvas.drawCircle(x, y, unit * 2.6f, fill)
            }

            text.color = colours.text
            text.textAlign = Paint.Align.CENTER
            text.typeface = Typeface.DEFAULT_BOLD
            text.textSize = unit * 5.2f
            val labelY = if (y < originY) y - unit * 4.8f else y + unit * 8.2f
            if (labelY - text.textSize >= mapArea.top && labelY <= mapArea.bottom - unit) {
                canvas.drawText(focused.repeater.callsign, x, labelY, text)
            } else {
                val reach = unit * 4.8f + text.measureText(focused.repeater.callsign) / 2
                val labelX = if (x < originX) x - reach else x + reach
                canvas.drawText(focused.repeater.callsign, labelX, y + text.textSize * 0.35f, text)
            }
        }

        // The car, or the chosen place: a ringed dot at the origin.
        fill.color = colours.pinEdge
        canvas.drawCircle(originX, originY, unit * 3.0f, fill)
        fill.color = colours.car
        canvas.drawCircle(originX, originY, unit * 2.1f, fill)
    }

    private fun bandColor(band: String, colours: Palette): Int = when {
        band.equals("2m", ignoreCase = true) -> colours.band2m
        band.equals("70cm", ignoreCase = true) -> colours.band70cm
        else -> colours.pin
    }

    private fun paintHud(canvas: Canvas, area: Rect, v: DirectionView, colours: Palette, unit: Float) {
        val focused = v.focusedItem() ?: return
        val bearing = focused.bearingDeg ?: return
        val distance = focused.distanceKm ?: return

        val margin = unit * 2.5f
        val hudW = min(area.width() - margin * 2f, unit * 48f).coerceAtLeast(unit * 32f)
        val hudH = min(area.height() * 0.3f, unit * 15f).coerceAtLeast(unit * 12f)
        hudRect.set(area.left + margin, area.top + margin, area.left + margin + hudW, area.top + margin + hudH)

        fill.color = colours.panel
        canvas.drawRoundRect(hudRect, hudH * 0.2f, hudH * 0.2f, fill)
        stroke.color = colours.coast
        stroke.strokeWidth = 1.5f
        canvas.drawRoundRect(hudRect, hudH * 0.2f, hudH * 0.2f, stroke)

        val near = distance < Geo.COMPASS_MIN_DISTANCE_KM
        val showArrow = !near && v.placeName == null
        val dialR = hudH * 0.34f
        val dialX = hudRect.left + hudH * 0.5f
        val dialY = hudRect.centerY()

        if (showArrow) {
            stroke.color = colours.dial
            stroke.strokeWidth = unit * 0.55f
            canvas.drawCircle(dialX, dialY, dialR, stroke)
            val angle = Headings.step(Headings.arrowAngle(bearing, v.heading)).toFloat()
            canvas.withRotation(angle, dialX, dialY) {
                path.rewind()
                path.moveTo(dialX, dialY - dialR * 0.8f)
                path.lineTo(dialX + dialR * 0.5f, dialY + dialR * 0.56f)
                path.lineTo(dialX, dialY + dialR * 0.28f)
                path.lineTo(dialX - dialR * 0.5f, dialY + dialR * 0.56f)
                path.close()
                fill.color = colours.accent
                drawPath(path, fill)
            }
        } else {
            fill.color = colours.land
            canvas.drawCircle(dialX, dialY, dialR, fill)
            text.color = colours.accent
            text.textAlign = Paint.Align.CENTER
            text.typeface = Typeface.DEFAULT_BOLD
            text.textSize = hudH * 0.22f
            val glyph = if (near) "<10" else Geo.compassPoint(bearing)
            canvas.drawText(glyph, dialX, dialY + text.textSize * 0.35f, text)
        }

        val textX = dialX + dialR + hudH * 0.18f
        text.textAlign = Paint.Align.LEFT
        text.color = colours.text
        text.typeface = Typeface.DEFAULT_BOLD
        text.textSize = hudH * 0.25f
        canvas.drawText(focused.repeater.callsign, textX, hudRect.top + hudH * 0.36f, text)

        text.color = colours.accent
        text.textSize = hudH * 0.22f
        val info = if (near) {
            context.getString(R.string.car_near_note)
        } else {
            "${CarRowText.bearing(bearing)} · ${RepeaterFormat.distance(distance)} km"
        }
        canvas.drawText(info, textX, hudRect.top + hudH * 0.64f, text)

        text.color = colours.faintText
        text.typeface = Typeface.DEFAULT
        text.textSize = hudH * 0.17f
        val note = when {
            v.placeName != null -> context.getString(R.string.car_from_place, v.placeName)
            v.heading != null -> context.getString(R.string.car_ahead_up)
            else -> context.getString(R.string.car_north_up)
        }
        canvas.drawText(note, textX, hudRect.top + hudH * 0.87f, text)
    }

    /** The car says when to use the light map and when the dark one; both must exist. */
    private class Palette(
        val sea: Int,
        val land: Int,
        val otherLand: Int,
        val coast: Int,
        val panel: Int,
        val dial: Int,
        val text: Int,
        val faintText: Int,
        val accent: Int,
        val pin: Int,
        val band2m: Int,
        val band70cm: Int,
        val pinEdge: Int,
        val car: Int,
    ) {
        companion object {
            val DARK = Palette(
                sea = Color.rgb(0x0E, 0x16, 0x20),
                land = Color.rgb(0x26, 0x33, 0x40),
                otherLand = Color.rgb(0x1A, 0x23, 0x2D),
                coast = Color.rgb(0x3A, 0x4B, 0x5C),
                panel = Color.argb(0xEB, 0x12, 0x1B, 0x26),
                dial = Color.rgb(0x5A, 0x6B, 0x7C),
                text = Color.WHITE,
                faintText = Color.rgb(0xB4, 0xC0, 0xCC),
                accent = Color.rgb(0xFF, 0xC1, 0x07),
                pin = Color.rgb(0xC9, 0xA0, 0xFF),
                band2m = Color.rgb(0xFF, 0x85, 0x00),
                band70cm = Color.rgb(0x33, 0xB5, 0xE5),
                pinEdge = Color.rgb(0x0E, 0x16, 0x20),
                car = Color.rgb(0x4F, 0xC3, 0xF7),
            )
            val LIGHT = Palette(
                sea = Color.rgb(0xC9, 0xDE, 0xEE),
                land = Color.rgb(0xF4, 0xF1, 0xE8),
                otherLand = Color.rgb(0xE2, 0xDF, 0xD5),
                coast = Color.rgb(0xA9, 0xB7, 0xC2),
                panel = Color.argb(0xF0, 0xFF, 0xFF, 0xFF),
                dial = Color.rgb(0x8A, 0x97, 0xA3),
                text = Color.rgb(0x15, 0x1C, 0x24),
                faintText = Color.rgb(0x4A, 0x57, 0x63),
                accent = Color.rgb(0xC6, 0x28, 0x28),
                pin = Color.rgb(0x7E, 0x57, 0xC2),
                band2m = Color.rgb(0xE6, 0x51, 0x00),
                band70cm = Color.rgb(0x02, 0x77, 0xBD),
                pinEdge = Color.WHITE,
                car = Color.rgb(0x15, 0x65, 0xC0),
            )
        }
    }

    private companion object {
        /** About 200 m of latitude per grid step. */
        const val GRID_PER_DEGREE = 500.0
    }
}
