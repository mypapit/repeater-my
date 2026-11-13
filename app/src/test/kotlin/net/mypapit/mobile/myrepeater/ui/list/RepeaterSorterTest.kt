package net.mypapit.mobile.myrepeater.ui.list

import net.mypapit.mobile.myrepeater.data.db.Repeater
import net.mypapit.mobile.myrepeater.data.db.RepeaterStatus
import net.mypapit.mobile.myrepeater.data.location.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepeaterSorterTest {

    private fun repeater(callsign: String, lat: Double?, lon: Double?) = Repeater(
        callsign = callsign, location = "", state = "", latitude = lat, longitude = lon, band = "2m",
        downlinkMhz = 145.0, uplinkMhz = 144.4, offsetMhz = -0.6, tone = "", mode = "FM", club = "UNKNOWN",
        clubFullName = "", linkedNetwork = "", status = RepeaterStatus.UNKNOWN, lastReviewed = "",
        source = "", repeaterBookId = "", notes = "",
    )

    private val far = repeater("9M4FAR", 6.0, 116.0)
    private val unknown = repeater("9M4UNK", null, null)
    private val near = repeater("9M4NEAR", 3.1, 101.7)
    private val mid = repeater("9M4MID", 4.6, 101.1)
    private val all = listOf(far, unknown, near, mid)

    @Test
    fun `without a position the order is kept and nothing is measured`() {
        val items = RepeaterSorter.arrange(all, null)
        assertEquals(all, items.map { it.repeater })
        assertTrue(items.all { it.distanceKm == null && it.bearingDeg == null })
    }

    @Test
    fun `with a position the nearest comes first and unlocated repeaters last`() {
        val items = RepeaterSorter.arrange(all, GeoPoint(3.139, 101.687))
        assertEquals(listOf("9M4NEAR", "9M4MID", "9M4FAR", "9M4UNK"), items.map { it.repeater.callsign })
        assertTrue(items[0].distanceKm!! < 10)
        assertTrue(items[1].distanceKm!! in 150.0..200.0)
        assertNull(items[3].distanceKm)
        assertNull(items[3].bearingDeg)
    }

    @Test
    fun `distance text`() {
        assertEquals("3.4", RepeaterFormat.distance(3.44))
        assertEquals("0.0", RepeaterFormat.distance(0.0))
        assertEquals("42", RepeaterFormat.distance(42.3))
        assertEquals("10", RepeaterFormat.distance(10.0))
    }
}
