package net.mypapit.mobile.myrepeater.car

import net.mypapit.mobile.myrepeater.data.db.Repeater
import net.mypapit.mobile.myrepeater.data.db.RepeaterStatus
import net.mypapit.mobile.myrepeater.data.location.GeoPoint
import net.mypapit.mobile.myrepeater.ui.list.RepeaterListItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NearbyRepeatersTest {

    private var nextId = 1L

    private fun repeater(
        callsign: String,
        lat: Double?,
        lon: Double?,
        status: RepeaterStatus = RepeaterStatus.ON_AIR,
        downlink: Double? = 145.0,
        offset: Double? = -0.6,
        tone: String = "203.5",
    ) = Repeater(
        id = nextId++, callsign = callsign, location = "", state = "", latitude = lat, longitude = lon, band = "2m",
        downlinkMhz = downlink, uplinkMhz = null, offsetMhz = offset, tone = tone, mode = "FM", club = "UNKNOWN",
        clubFullName = "", linkedNetwork = "", status = status, lastReviewed = "", source = "",
        repeaterBookId = "", notes = "",
    )

    /** Kuala Lumpur. One hundredth of a degree of latitude is about 1.1 km. */
    private val here = GeoPoint(3.139, 101.687)

    private fun north(km: Double) = here.latitude + km / 110.574

    @Test
    fun `the five nearest are listed, nearest first`() {
        val all = listOf(60.0, 10.0, 90.0, 30.0, 5.0, 45.0, 20.0).map { repeater("R${it.toInt()}", north(it), 101.687) }
        val picked = NearbyRepeaters.select(all, here)
        assertEquals(listOf("R5", "R10", "R20", "R30", "R45"), picked.map { it.repeater.callsign })
        assertTrue(picked.zipWithNext().all { (a, b) -> a.distanceKm!! <= b.distanceKm!! })
    }

    @Test
    fun `nothing beyond 100 km is listed`() {
        val all = listOf(repeater("NEAR", north(40.0), 101.687), repeater("EDGE", north(99.0), 101.687),
            repeater("FAR", north(101.0), 101.687), repeater("SABAH", 6.0, 116.0))
        assertEquals(listOf("NEAR", "EDGE"), NearbyRepeaters.select(all, here).map { it.repeater.callsign })
        assertTrue(NearbyRepeaters.select(listOf(repeater("SABAH", 6.0, 116.0)), here).isEmpty())
    }

    @Test
    fun `off-air and unlocated repeaters are left out, unknown status is kept`() {
        val all = listOf(
            repeater("OFF", north(1.0), 101.687, status = RepeaterStatus.OFF_AIR),
            repeater("NOWHERE", null, null),
            repeater("MAYBE", north(2.0), 101.687, status = RepeaterStatus.UNKNOWN),
            repeater("ON", north(3.0), 101.687),
        )
        assertEquals(listOf("MAYBE", "ON"), NearbyRepeaters.select(all, here).map { it.repeater.callsign })
    }

    @Test
    fun `the car's row limit is respected but never raises the five`() {
        val all = (1..9).map { repeater("R$it", north(it.toDouble()), 101.687) }
        assertEquals(3, NearbyRepeaters.select(all, here, limit = 3).size)
        assertEquals(5, NearbyRepeaters.select(all, here, limit = 40).size)
        assertTrue(NearbyRepeaters.select(all, here, limit = 0).isEmpty())
    }

    @Test
    fun `remeasuring keeps the order and updates the distances`() {
        val picked = NearbyRepeaters.select(
            listOf(repeater("A", north(10.0), 101.687), repeater("B", north(30.0), 101.687)), here,
        )
        val moved = NearbyRepeaters.remeasure(picked, GeoPoint(north(25.0), 101.687))
        assertEquals(listOf("A", "B"), moved.map { it.repeater.callsign })
        assertEquals(15.0, moved[0].distanceKm!!, 0.2)
        assertEquals(5.0, moved[1].distanceKm!!, 0.2)
        assertEquals(180.0, moved[0].bearingDeg!!, 0.5)
    }

    @Test
    fun `the list is reordered only after a kilometre of travel`() {
        assertTrue(NearbyRepeaters.shouldReorder(null, here))
        assertFalse(NearbyRepeaters.shouldReorder(here, GeoPoint(north(0.5), 101.687)))
        assertTrue(NearbyRepeaters.shouldReorder(here, GeoPoint(north(1.2), 101.687)))
    }

    @Test
    fun `focus stays on the chosen repeater while it is listed`() {
        val a = repeater("A", north(1.0), 101.687)
        val b = repeater("B", north(2.0), 101.687)
        val far = repeater("FAR", north(150.0), 101.687)
        val all = listOf(a, b, far)
        val items = NearbyRepeaters.select(all, here)
        assertEquals(b.id, NearbyRepeaters.focus(b.id, items))
        assertEquals(a.id, NearbyRepeaters.focus(null, items))
        assertEquals(a.id, NearbyRepeaters.focus(999L, items))
        assertNull(NearbyRepeaters.focus(b.id, emptyList()))
        assertEquals(far.id, NearbyRepeaters.resolveFocus(far.id, items, all))
        assertEquals(a.id, NearbyRepeaters.resolveFocus(999L, items, all))
    }

    @Test
    fun `row texts are short and complete`() {
        val r = repeater("9M2RKK", north(42.0), 101.687, downlink = 147.98)
        val item = NearbyRepeaters.select(listOf(r), here).single()
        assertEquals("9M2RKK  147.980", CarRowText.title(r))
        assertEquals("−0.6 · Tone 203.5", CarRowText.tuning(r) { "Tone $it" })
        assertEquals("42 km N", CarRowText.whereabouts(item))
        assertEquals("", CarRowText.tuning(r.copy(offsetMhz = null, tone = " ")) { "Tone $it" })
        assertEquals("9M2RKK", CarRowText.title(r.copy(downlinkMhz = null)))
        assertEquals("", CarRowText.whereabouts(RepeaterListItem(r, null, null)))
    }

    @Test
    fun `bearings are written with a compass point and three figures`() {
        assertEquals("NE 042°", CarRowText.bearing(42.2))
        assertEquals("N 000°", CarRowText.bearing(359.7))
        assertEquals("W 270°", CarRowText.bearing(270.0))
    }

    @Test
    fun `the car compass is preferred, then travel direction above walking pace`() {
        assertEquals(Heading(90.0, HeadingSource.CAR_COMPASS), Headings.pick(90f, 200f, 20f))
        assertEquals(Heading(200.0, HeadingSource.TRAVEL), Headings.pick(null, 200f, 20f))
        assertNull(Headings.pick(null, 200f, 1f))
        assertNull(Headings.pick(null, 200f, null))
        assertNull(Headings.pick(null, null, 20f))
        assertNull(Headings.pick(Float.NaN, null, null))
        assertEquals(Heading(350.0, HeadingSource.CAR_COMPASS), Headings.pick(-10f, null, null))
    }

    @Test
    fun `the arrow is relative to the heading, or to north without one`() {
        assertEquals(45.0, Headings.arrowAngle(45.0, null), 1e-9)
        assertEquals(0.0, Headings.arrowAngle(90.0, Heading(90.0, HeadingSource.TRAVEL)), 1e-9)
        assertEquals(270.0, Headings.arrowAngle(10.0, Heading(100.0, HeadingSource.CAR_COMPASS)), 1e-9)
    }

    @Test
    fun `the arrow moves in five degree steps`() {
        assertEquals(40, Headings.step(42.4))
        assertEquals(45, Headings.step(42.6))
        assertEquals(0, Headings.step(358.0))
        assertEquals(355, Headings.step(-4.0))
    }

    @Test
    fun `the map is north-up and to scale`() {
        val frame = MapFrame(here, radiusKm = 50.0, centreX = 400f, centreY = 300f, radiusPx = 200f)
        assertEquals(400f, frame.x(here.longitude), 0.01f)
        assertEquals(300f, frame.y(here.latitude), 0.01f)
        // 50 km north is one radius up the screen; east is to the right.
        assertEquals(100f, frame.y(north(50.0)), 0.5f)
        assertTrue(frame.x(here.longitude + 0.1) > 400f)
        assertTrue(frame.y(here.latitude - 0.1) > 300f)
        assertEquals(50.0 / MapFrame.KM_PER_DEG_LAT, frame.latitudeSpan(200f), 1e-6)
    }

    @Test
    fun `the map is wide enough for every listed repeater`() {
        val items = NearbyRepeaters.select(
            listOf(repeater("A", north(10.0), 101.687), repeater("B", north(80.0), 101.687)), here,
        )
        // 80 km plus a fifth; the fixture's "km north" is good to about half a percent.
        assertEquals(96.0, MapFrame.radiusFor(items), 1.0)
        assertEquals(15.0, MapFrame.radiusFor(emptyList()), 1e-9)
        assertEquals(15.0, MapFrame.radiusFor(items.take(1)), 1e-9)
    }
}
