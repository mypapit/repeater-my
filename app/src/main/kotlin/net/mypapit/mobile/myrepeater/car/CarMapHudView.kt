package net.mypapit.mobile.myrepeater.car

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View
import androidx.core.graphics.withRotation
import net.mypapit.mobile.myrepeater.R
import net.mypapit.mobile.myrepeater.data.location.Geo
import net.mypapit.mobile.myrepeater.ui.list.RepeaterFormat
import kotlin.math.min

/**
 * Compact corner HUD overlay drawn on top of the interactive car map.
 *
 * Shows the discrete 5-degree stepped direction arrow (hidden within 10 km or when
 * measuring from a preset place), bearing, distance, and focused repeater callsign
 * without stealing half of the map surface.
 */
class CarMapHudView(context: Context) : View(context) {

    private var view: DirectionView? = null
    private var dark = true
    private var offline = false
    private var panMode = false
    private var safeArea: Rect? = null
    private var lastKey: Any? = null

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arrowPath = Path()
    private val cardRect = RectF()
    private val fallbackRect = Rect()

    fun update(view: DirectionView?, dark: Boolean, offline: Boolean, panMode: Boolean) {
        this.view = view
        this.dark = dark
        this.offline = offline
        this.panMode = panMode
        val nextKey = buildKey(view, dark, offline, panMode, safeArea)
        if (nextKey != lastKey) {
            lastKey = nextKey
            invalidate()
        }
    }

    fun setSafeArea(area: Rect?) {
        safeArea = area?.let(::Rect)
        lastKey = null
        invalidate()
    }

    private fun buildKey(
        v: DirectionView?,
        dark: Boolean,
        offline: Boolean,
        panMode: Boolean,
        area: Rect?,
    ): Any? {
        val focused = v?.focusedItem() ?: return listOf(dark, offline, panMode, area)
        return listOf(
            dark,
            offline,
            panMode,
            area,
            focused.repeater.id,
            v.placeName,
            v.heading?.source,
            focused.bearingDeg?.let { Headings.step(Headings.arrowAngle(it, v.heading)) },
            focused.distanceKm?.let(RepeaterFormat::distance),
            focused.bearingDeg?.let(CarRowText::bearing),
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val v = view ?: return
        val focused = v.focusedItem() ?: return
        val bearing = focused.bearingDeg ?: return
        val distance = focused.distanceKm ?: return
        if (width <= 0 || height <= 0) return

        val area = safeArea?.takeIf { !it.isEmpty } ?: fallbackRect.apply { set(0, 0, width, height) }
        val density = resources.displayMetrics.density.coerceAtLeast(1f)
        val colors = if (dark) HudPalette.DARK else HudPalette.LIGHT

        val margin = 8f * density
        val maxCardWidth = (area.width() - margin * 2f).coerceAtLeast(120f * density)
        val cardWidth = min(210f * density, maxCardWidth)
        val cardHeight = min(64f * density, (area.height() - margin * 2f).coerceAtLeast(48f * density))

        val left = area.left + margin
        val top = area.top + margin
        cardRect.set(left, top, left + cardWidth, top + cardHeight)

        val corner = 12f * density
        fill.color = colors.panel
        canvas.drawRoundRect(cardRect, corner, corner, fill)
        stroke.color = colors.border
        stroke.strokeWidth = 1.2f * density
        canvas.drawRoundRect(cardRect, corner, corner, stroke)

        val near = distance < Geo.COMPASS_MIN_DISTANCE_KM
        val showArrow = !near && v.placeName == null
        val dialRadius = cardHeight * 0.34f
        val dialCenterX = cardRect.left + 10f * density + dialRadius
        val dialCenterY = cardRect.centerY()

        if (showArrow) {
            stroke.color = colors.dial
            stroke.strokeWidth = 1.8f * density
            canvas.drawCircle(dialCenterX, dialCenterY, dialRadius, stroke)

            val angle = Headings.step(Headings.arrowAngle(bearing, v.heading)).toFloat()
            canvas.withRotation(angle, dialCenterX, dialCenterY) {
                arrowPath.rewind()
                arrowPath.moveTo(dialCenterX, dialCenterY - dialRadius * 0.78f)
                arrowPath.lineTo(dialCenterX + dialRadius * 0.48f, dialCenterY + dialRadius * 0.56f)
                arrowPath.lineTo(dialCenterX, dialCenterY + dialRadius * 0.28f)
                arrowPath.lineTo(dialCenterX - dialRadius * 0.48f, dialCenterY + dialRadius * 0.56f)
                arrowPath.close()
                fill.color = colors.accent
                drawPath(arrowPath, fill)
            }
        } else if (near) {
            fill.color = colors.nearBg
            canvas.drawCircle(dialCenterX, dialCenterY, dialRadius, fill)
            text.color = colors.accent
            text.textAlign = Paint.Align.CENTER
            text.typeface = Typeface.DEFAULT_BOLD
            text.textSize = 9.5f * density
            canvas.drawText("<10", dialCenterX, dialCenterY + text.textSize * 0.35f, text)
        } else {
            fill.color = colors.nearBg
            canvas.drawCircle(dialCenterX, dialCenterY, dialRadius, fill)
            text.color = colors.accent
            text.textAlign = Paint.Align.CENTER
            text.typeface = Typeface.DEFAULT_BOLD
            text.textSize = 10f * density
            canvas.drawText(Geo.compassPoint(bearing), dialCenterX, dialCenterY + text.textSize * 0.35f, text)
        }

        val textX = dialCenterX + dialRadius + 10f * density
        val maxTextWidth = (cardRect.right - 8f * density - textX).coerceAtLeast(40f)
        text.textAlign = Paint.Align.LEFT

        val title = listOfNotNull(
            focused.repeater.callsign,
            focused.repeater.downlinkMhz?.let(RepeaterFormat::frequency),
        ).joinToString(" · ")
        text.color = colors.text
        text.typeface = Typeface.DEFAULT_BOLD
        text.textSize = 13f * density
        canvas.drawText(ellipsize(title, text, maxTextWidth), textX, cardRect.top + cardHeight * 0.34f, text)

        val primaryInfo = if (near) {
            "${context.getString(R.string.car_near_note)} · ${RepeaterFormat.distance(distance)} km"
        } else {
            "${CarRowText.bearing(bearing)} · ${RepeaterFormat.distance(distance)} km"
        }
        text.color = colors.accent
        text.typeface = Typeface.DEFAULT_BOLD
        text.textSize = 12f * density
        canvas.drawText(ellipsize(primaryInfo, text, maxTextWidth), textX, cardRect.top + cardHeight * 0.62f, text)

        val subNote = buildString {
            append(
                when {
                    v.placeName != null -> context.getString(R.string.car_from_place, v.placeName)
                    v.heading != null -> context.getString(R.string.car_ahead_up)
                    else -> context.getString(R.string.car_north_up)
                },
            )
            if (offline) {
                append(" · ")
                append(context.getString(R.string.car_hud_offline))
            }
        }
        text.color = colors.faintText
        text.typeface = Typeface.DEFAULT
        text.textSize = 10f * density
        canvas.drawText(ellipsize(subNote, text, maxTextWidth), textX, cardRect.top + cardHeight * 0.86f, text)
    }

    private fun ellipsize(value: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(value) <= maxWidth) return value
        val ellipsis = "…"
        var end = value.length
        while (end > 1 && paint.measureText(value, 0, end) + paint.measureText(ellipsis) > maxWidth) {
            end--
        }
        return value.substring(0, end) + ellipsis
    }

    private class HudPalette(
        val panel: Int,
        val border: Int,
        val dial: Int,
        val nearBg: Int,
        val text: Int,
        val faintText: Int,
        val accent: Int,
    ) {
        companion object {
            val DARK = HudPalette(
                panel = Color.argb(0xEB, 0x12, 0x1B, 0x26),
                border = Color.rgb(0x3A, 0x4B, 0x5C),
                dial = Color.rgb(0x5A, 0x6B, 0x7C),
                nearBg = Color.rgb(0x26, 0x33, 0x40),
                text = Color.WHITE,
                faintText = Color.rgb(0xB4, 0xC0, 0xCC),
                accent = Color.rgb(0xFF, 0xC1, 0x07),
            )
            val LIGHT = HudPalette(
                panel = Color.argb(0xF0, 0xFF, 0xFF, 0xFF),
                border = Color.rgb(0xA9, 0xB7, 0xC2),
                dial = Color.rgb(0x8A, 0x97, 0xA3),
                nearBg = Color.rgb(0xE8, 0xEE, 0xF3),
                text = Color.rgb(0x15, 0x1C, 0x24),
                faintText = Color.rgb(0x4A, 0x57, 0x63),
                accent = Color.rgb(0xC6, 0x28, 0x28),
            )
        }
    }
}
