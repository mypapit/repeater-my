package net.mypapit.mobile.myrepeater.car

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Row
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.car.app.testing.TestCarContext
import androidx.test.core.app.ApplicationProvider
import net.mypapit.mobile.myrepeater.data.db.Repeater
import net.mypapit.mobile.myrepeater.data.db.RepeaterStatus
import net.mypapit.mobile.myrepeater.data.location.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Builds the car templates and draws the car map without a car. The library
 * checks its own rules when a template is built, so a template that breaks
 * them fails here. The pictures are saved under build/car-preview to look at.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CarScreenTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val carContext = TestCarContext.createCarContext(context)
    private val templates = NearbyTemplates(carContext)

    private fun repeater(id: Long, callsign: String, lat: Double, lon: Double, mhz: Double, tone: String) = Repeater(
        id = id, callsign = callsign, location = "", state = "", latitude = lat, longitude = lon, band = "2m",
        downlinkMhz = mhz, uplinkMhz = null, offsetMhz = -0.6, tone = tone, mode = "FM", club = "UNKNOWN",
        clubFullName = "", linkedNetwork = "", status = RepeaterStatus.ON_AIR, lastReviewed = "", source = "",
        repeaterBookId = "", notes = "",
    )

    private val kualaLumpur = GeoPoint(3.139, 101.687)
    private val repeaters = listOf(
        repeater(1, "9M2RKK", 3.21, 101.70, 147.98, "203.5"),
        repeater(2, "9M2RUK", 3.42, 101.79, 145.625, "103.5"),
        repeater(3, "9M2RGT", 3.05, 101.40, 145.75, ""),
        repeater(4, "9M2RSB", 2.72, 101.94, 147.76, "203.5"),
        repeater(5, "9M2RTM", 3.71, 101.74, 145.675, "123.0"),
        repeater(6, "9M2RIP", 4.60, 101.09, 146.72, "203.5"),
    )
    private val items = NearbyRepeaters.select(repeaters, kualaLumpur)

    private fun content(template: Any) = (template as MapWithContentTemplate).contentTemplate

    @Test
    fun `the list is five rows beside a map, each with two short lines`() {
        val list = content(templates.list(items, focusedId = 2, onFocus = {})) as ListTemplate
        val rows = list.singleList!!.items.map { it as Row }
        assertEquals(5, rows.size)
        assertEquals("9M2RKK  147.980", rows[0].title.toString())
        assertEquals(listOf("−0.6 · Tone 203.5", "8.0 km N"), rows[0].texts.map { it.toString() })
        // No tone in the source: the line carries the offset alone.
        assertEquals("−0.6", rows.first { it.title.toString().startsWith("9M2RGT") }.texts[0].toString())
        assertTrue(rows.all { it.onClickDelegate != null && it.image != null })
        assertEquals("Nearby repeaters", list.header!!.title.toString())
    }

    @Test
    fun `tapping a row reports that repeater`() {
        var chosen: Long? = null
        val list = content(templates.list(items, focusedId = 1, onFocus = { chosen = it })) as ListTemplate
        val third = list.singleList!!.items[2] as Row
        third.onClickDelegate!!.sendClick(object : androidx.car.app.OnDoneCallback {})
        assertEquals(items[2].repeater.id, chosen)
    }

    @Test
    fun `an empty list says nothing is in range`() {
        val list = content(templates.list(emptyList(), null) {}) as ListTemplate
        assertTrue(list.singleList!!.items.isEmpty())
        assertEquals("No repeater within 100 km.", list.singleList!!.noItemsMessage.toString())
    }

    @Test
    fun `the other states build as the same kind of template`() {
        assertTrue((content(templates.loading()) as ListTemplate).isLoading)
        val noData = content(templates.noData()) as MessageTemplate
        assertTrue(noData.message.toString().contains("on your phone"))
        val needLocation = content(templates.needLocation {}) as MessageTemplate
        assertTrue(needLocation.message.toString().contains("only when it is safe"))
        assertEquals(1, needLocation.actions.size)
        assertTrue(needLocation.actions[0].onClickDelegate!!.isParkedOnly)
    }

    private fun picture(name: String, view: DirectionView?, dark: Boolean, width: Int, height: Int, area: Rect): Bitmap {
        val renderer = DirectionRenderer(context)
        renderer.setOutline(MapOutline.load(context))
        renderer.setDark(dark)
        renderer.show(view)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        renderer.paint(Canvas(bitmap), area, width, height)
        val folder = File("build/car-preview").apply { mkdirs() }
        File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return bitmap
    }

    private fun view(focusedId: Long, heading: Heading? = null, place: String? = null, from: GeoPoint = kualaLumpur) =
        DirectionView(from, place, NearbyRepeaters.select(repeaters, from), focusedId, heading)

    @Test
    fun `the bundled coastline is read`() {
        val rings = MapOutline.load(context)
        assertTrue(rings.size > 10)
        assertTrue(rings.any { it.home })
    }

    @Test
    fun `the map is drawn in dark and light, wide and tall, without a crash`() {
        // A wide screen with the list covering the right-hand side, as in a right-hand-drive car.
        val wide = Rect(24, 24, 760, 456)
        val dark = picture("wide-dark", view(5), dark = true, width = 1280, height = 480, area = wide)
        val light = picture("wide-light", view(5), dark = false, width = 1280, height = 480, area = wide)
        assertNotEquals(dark.getPixel(2, 2), light.getPixel(2, 2))

        picture("wide-ahead-up", view(4, Heading(40.0, HeadingSource.TRAVEL)), true, 1280, 480, wide)
        picture("wide-near", view(1), true, 1280, 480, wide)
        picture("wide-preset", view(2, place = "Kuala Lumpur"), true, 1280, 480, wide)
        picture("wide-no-position", null, true, 1280, 480, wide)
        picture("tall-dark", view(3), true, 800, 1280, Rect(20, 20, 780, 760))
        picture("small-light", view(2), false, 800, 480, Rect(330, 10, 790, 470))
        // The shape the Desktop Head Unit gives at 800 by 480: the list on the left, a bar below.
        picture("head-unit", view(4), true, 800, 400, Rect(404, 8, 792, 392))
    }
}
