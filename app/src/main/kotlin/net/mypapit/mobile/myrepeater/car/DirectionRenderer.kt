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
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import net.mypapit.mobile.myrepeater.R
import net.mypapit.mobile.myrepeater.data.location.Geo
import net.mypapit.mobile.myrepeater.data.location.GeoPoint
import net.mypapit.mobile.myrepeater.ui.list.RepeaterFormat
import net.mypapit.mobile.myrepeater.ui.list.RepeaterListItem
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.min
import kotlin.math.roundToInt

/** What the map side of the car screen shows. */
data class DirectionView(
    val origin: GeoPoint,
    /** Set when distances are measured from a place the user chose, not from the car. */
    val placeName: String?,
    val items: List<RepeaterListItem>,
    val focusedId: Long?,
    val heading: Heading?,
)

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
 * Draws the map side of the car screen: the coastline, the car, the listed
 * repeaters, a line to the one in focus, and a large direction arrow.
 *
 * Nothing here is animated. A new picture is drawn only when what it shows has
 * changed by a visible step (see [key]), which keeps to the rule against moving
 * graphics while driving.
 */
class DirectionRenderer(private val context: Context) : SurfaceCallback {

    private var surface: Surface? = null
    private var width = 0
    private var height = 0
    private var visibleArea: Rect? = null
    private var view: DirectionView? = null
    private var dark = true
    private var outline: List<OutlineRing> = emptyList()
    private var lastKey: Any? = null

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val path = Path()

    fun show(view: DirectionView?) {
        this.view = view
        draw()
    }

    fun setDark(dark: Boolean) {
        this.dark = dark
        draw()
    }

    fun setOutline(rings: List<OutlineRing>) {
        outline = rings
        lastKey = null
        draw()
    }

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        surface = surfaceContainer.surface
        width = surfaceContainer.width
        height = surfaceContainer.height
        lastKey = null
        draw()
    }

    override fun onVisibleAreaChanged(visibleArea: Rect) {
        this.visibleArea = Rect(visibleArea)
        draw()
    }

    override fun onStableAreaChanged(stableArea: Rect) = Unit

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        surface = null
        lastKey = null
    }

    /**
     * Everything that decides what the picture looks like, rounded to the
     * steps the eye can tell apart: about 200 m of travel and five degrees.
     */
    private fun key(area: Rect): Any? {
        val v = view ?: return listOf(dark, area, width, height, outline.size)
        val focused = v.items.firstOrNull { it.repeater.id == v.focusedId }
        return listOf(
            dark, area, width, height, outline.size,
            (v.origin.latitude * GRID_PER_DEGREE).roundToInt(),
            (v.origin.longitude * GRID_PER_DEGREE).roundToInt(),
            v.placeName,
            v.items.map { it.repeater.id },
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
        val area = visibleArea?.takeIf { !it.isEmpty } ?: Rect(0, 0, width, height)
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

        // The panel with the arrow takes the left of a wide area, or the top of a tall one.
        val wide = area.width() >= area.height() * 1.2f
        val panelSize = if (wide) min(area.height() * 0.9f, area.width() * 0.42f) else min(area.width() * 0.9f, area.height() * 0.58f)
        val gap = panelSize * 0.05f
        val panel = if (wide) {
            RectF(area.left + gap, area.exactCenterY() - panelSize / 2, area.left + gap + panelSize, area.exactCenterY() + panelSize / 2)
        } else {
            RectF(area.exactCenterX() - panelSize / 2, area.top + gap, area.exactCenterX() + panelSize / 2, area.top + gap + panelSize)
        }
        val mapArea = if (wide) {
            RectF(panel.right + gap, area.top.toFloat(), area.right.toFloat(), area.bottom.toFloat())
        } else {
            RectF(area.left.toFloat(), panel.bottom + gap, area.right.toFloat(), area.bottom.toFloat())
        }

        val origin = v?.origin ?: DEFAULT_CENTRE
        val frame = MapFrame(
            centre = origin,
            radiusKm = if (v == null) OVERVIEW_RADIUS_KM else MapFrame.radiusFor(v.items),
            centreX = mapArea.centerX(),
            centreY = mapArea.centerY(),
            radiusPx = min(mapArea.width(), mapArea.height()) / 2 * 0.86f,
        )
        // Pins and labels are sized from the whole uncovered area, so a narrow map strip keeps them readable.
        val unit = min(area.width(), area.height()) / 100f * 0.9f

        paintLand(canvas, frame, colours, width, height)
        if (v != null) {
            paintRepeaters(canvas, frame, v, colours, unit, mapArea)
            paintPanel(canvas, panel, v, colours)
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

    private fun paintRepeaters(canvas: Canvas, frame: MapFrame, v: DirectionView, colours: Palette, unit: Float, mapArea: RectF) {
        val carX = frame.centreX
        val carY = frame.centreY
        val focused = v.items.firstOrNull { it.repeater.id == v.focusedId }

        if (focused != null) {
            stroke.color = colours.accent
            stroke.strokeWidth = unit * 1.1f
            canvas.drawLine(carX, carY, frame.x(focused.repeater.longitude!!), frame.y(focused.repeater.latitude!!), stroke)
        }

        for (item in v.items) {
            val x = frame.x(item.repeater.longitude!!)
            val y = frame.y(item.repeater.latitude!!)
            val isFocused = item === focused
            fill.color = colours.pinEdge
            canvas.drawCircle(x, y, unit * (if (isFocused) 3.4f else 2.4f), fill)
            fill.color = if (isFocused) colours.accent else colours.pin
            canvas.drawCircle(x, y, unit * (if (isFocused) 2.6f else 1.7f), fill)
        }

        if (focused != null) {
            text.color = colours.text
            text.typeface = Typeface.DEFAULT_BOLD
            text.textSize = unit * 5.5f
            val x = frame.x(focused.repeater.longitude!!)
            val y = frame.y(focused.repeater.latitude!!)
            // The label sits on the far side of the pin from the car, clear of the line between them.
            val labelY = if (y < frame.centreY) y - unit * 5f else y + unit * 9f
            if (labelY - text.textSize >= mapArea.top && labelY <= mapArea.bottom - unit) {
                canvas.drawText(focused.repeater.callsign, x, labelY, text)
            } else {
                // No room above or below: the label goes beside the pin, on the side away from the car.
                val reach = unit * 5f + text.measureText(focused.repeater.callsign) / 2
                val labelX = if (x < frame.centreX) x - reach else x + reach
                canvas.drawText(focused.repeater.callsign, labelX, y + text.textSize * 0.35f, text)
            }
        }

        // The car, or the chosen place: a ringed dot at the centre of the map.
        fill.color = colours.pinEdge
        canvas.drawCircle(carX, carY, unit * 3f, fill)
        fill.color = colours.car
        canvas.drawCircle(carX, carY, unit * 2.1f, fill)
    }

    private fun paintPanel(canvas: Canvas, panel: RectF, v: DirectionView, colours: Palette) {
        val focused = v.items.firstOrNull { it.repeater.id == v.focusedId } ?: return
        val bearing = focused.bearingDeg ?: return
        val distance = focused.distanceKm ?: return
        val size = panel.width()

        fill.color = colours.panel
        canvas.drawRoundRect(panel, size * 0.08f, size * 0.08f, fill)

        val centreX = panel.centerX()
        val dialY = panel.top + size * 0.36f
        val dialRadius = size * 0.27f
        val near = distance < Geo.COMPASS_MIN_DISTANCE_KM
        // From a chosen place there is no "ahead", and close in the listed position is too rough to point at.
        val showArrow = !near && v.placeName == null

        text.color = colours.text
        if (showArrow) {
            stroke.color = colours.dial
            stroke.strokeWidth = size * 0.02f
            canvas.drawCircle(centreX, dialY, dialRadius, stroke)

            val angle = Headings.step(Headings.arrowAngle(bearing, v.heading)).toFloat()
            canvas.save()
            canvas.rotate(angle, centreX, dialY)
            path.rewind()
            path.moveTo(centreX, dialY - dialRadius * 0.82f)
            path.lineTo(centreX + dialRadius * 0.5f, dialY + dialRadius * 0.6f)
            path.lineTo(centreX, dialY + dialRadius * 0.3f)
            path.lineTo(centreX - dialRadius * 0.5f, dialY + dialRadius * 0.6f)
            path.close()
            fill.color = colours.accent
            canvas.drawPath(path, fill)
            canvas.restore()
        } else if (near) {
            text.typeface = Typeface.DEFAULT_BOLD
            text.textSize = size * 0.11f
            canvas.drawText(context.getString(R.string.car_near_title), centreX, dialY - size * 0.02f, text)
            text.typeface = Typeface.DEFAULT
            text.textSize = size * 0.075f
            canvas.drawText(context.getString(R.string.car_near_note), centreX, dialY + size * 0.08f, text)
        } else {
            text.typeface = Typeface.DEFAULT_BOLD
            text.textSize = size * 0.12f
            canvas.drawText(focused.repeater.callsign, centreX, dialY + size * 0.04f, text)
        }

        text.typeface = Typeface.DEFAULT_BOLD
        text.textSize = size * 0.13f
        canvas.drawText(CarRowText.bearing(bearing), centreX, panel.top + size * 0.78f, text)

        text.typeface = Typeface.DEFAULT
        text.textSize = size * 0.1f
        canvas.drawText("${RepeaterFormat.distance(distance)} km", centreX, panel.top + size * 0.88f, text)

        text.color = colours.faintText
        text.textSize = size * 0.065f
        val note = when {
            v.placeName != null -> context.getString(R.string.car_from_place, v.placeName)
            v.heading != null -> context.getString(R.string.car_ahead_up)
            else -> context.getString(R.string.car_north_up)
        }
        canvas.drawText(note, centreX, panel.top + size * 0.96f, text)
    }

    /** The car says when to use the light map and when the dark one; both must exist. */
    private class Palette(
        val sea: Int, val land: Int, val otherLand: Int, val coast: Int, val panel: Int, val dial: Int,
        val text: Int, val faintText: Int, val accent: Int, val pin: Int, val pinEdge: Int, val car: Int,
    ) {
        companion object {
            val DARK = Palette(
                sea = Color.rgb(0x0E, 0x16, 0x20), land = Color.rgb(0x26, 0x33, 0x40),
                otherLand = Color.rgb(0x1A, 0x23, 0x2D), coast = Color.rgb(0x3A, 0x4B, 0x5C),
                panel = Color.argb(0xE6, 0x12, 0x1B, 0x26), dial = Color.rgb(0x5A, 0x6B, 0x7C),
                text = Color.WHITE, faintText = Color.rgb(0xB4, 0xC0, 0xCC),
                accent = Color.rgb(0xFF, 0xC1, 0x07), pin = Color.rgb(0x8F, 0xA3, 0xB5),
                pinEdge = Color.rgb(0x0E, 0x16, 0x20), car = Color.rgb(0x4F, 0xC3, 0xF7),
            )
            val LIGHT = Palette(
                sea = Color.rgb(0xC9, 0xDE, 0xEE), land = Color.rgb(0xF4, 0xF1, 0xE8),
                otherLand = Color.rgb(0xE2, 0xDF, 0xD5), coast = Color.rgb(0xA9, 0xB7, 0xC2),
                panel = Color.argb(0xEB, 0xFF, 0xFF, 0xFF), dial = Color.rgb(0x8A, 0x97, 0xA3),
                text = Color.rgb(0x15, 0x1C, 0x24), faintText = Color.rgb(0x4A, 0x57, 0x63),
                accent = Color.rgb(0xC6, 0x28, 0x28), pin = Color.rgb(0x5B, 0x6B, 0x7A),
                pinEdge = Color.WHITE, car = Color.rgb(0x15, 0x65, 0xC0),
            )
        }
    }

    private companion object {
        /** About 200 m of latitude per grid step. */
        const val GRID_PER_DEGREE = 500.0

        /** Shown before a position is known: the peninsula and Borneo together. */
        val DEFAULT_CENTRE = GeoPoint(4.2, 109.0)
        const val OVERVIEW_RADIUS_KM = 600.0
    }
}
