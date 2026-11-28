package net.mypapit.mobile.myrepeater.ui.map

import net.mypapit.mobile.myrepeater.data.db.Repeater
import net.mypapit.mobile.myrepeater.data.db.RepeaterStatus
import net.mypapit.mobile.myrepeater.data.location.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapSitesTest {

    private fun repeater(
        callsign: String,
        lat: Double?,
        lon: Double?,
        band: String = "2m",
        downlink: Double = 145.0,
        status: RepeaterStatus = RepeaterStatus.ON_AIR,
    ) = Repeater(
        callsign = callsign, location = "", state = "", latitude = lat, longitude = lon, band = band,
        downlinkMhz = downlink, uplinkMhz = null, offsetMhz = null, tone = "", mode = "FM", club = "UNKNOWN",
        clubFullName = "", linkedNetwork = "", status = status, lastReviewed = "", source = "",
        repeaterBookId = "", notes = "",
    )

    @Test
    fun `repeaters sharing coordinates become one site, ordered by frequency`() {
        val sites = MapSites.from(
            listOf(
                repeater("9M2RKL", 3.18, 101.61, band = "70cm", downlink = 439.6),
                repeater("9M2RKK", 3.18, 101.61, band = "2m", downlink = 147.98),
                repeater("9M4RDF", 4.88, 101.97),
            ),
        )
        assertEquals(2, sites.size)
        val lanjan = sites.first { it.point == GeoPoint(3.18, 101.61) }
        assertEquals(listOf("9M2RKK", "9M2RKL"), lanjan.repeaters.map { it.callsign })
        assertEquals(MapSite.BAND_MIXED, lanjan.band)
        assertEquals("2m", sites.first { it !== lanjan }.band)
    }

    @Test
    fun `repeaters without coordinates are left off the map`() {
        val sites = MapSites.from(listOf(repeater("9M4AAA", null, null), repeater("9M4BBB", 4.0, null)))
        assertTrue(sites.isEmpty())
    }

    @Test
    fun `a site is on air when any of its repeaters is`() {
        val mixed = MapSites.from(
            listOf(
                repeater("A", 1.0, 100.0, status = RepeaterStatus.OFF_AIR),
                repeater("B", 1.0, 100.0, status = RepeaterStatus.ON_AIR),
            ),
        ).single()
        val dead = MapSites.from(
            listOf(
                repeater("C", 2.0, 100.0, status = RepeaterStatus.OFF_AIR),
                repeater("D", 2.0, 100.0, status = RepeaterStatus.UNKNOWN),
            ),
        ).single()
        assertTrue(mixed.onAir)
        assertFalse(dead.onAir)
    }

    @Test
    fun `site ids are unique per location`() {
        val sites = MapSites.from(listOf(repeater("A", 1.0, 100.0), repeater("B", 1.0, 100.5), repeater("C", 1.5, 100.0)))
        assertEquals(3, sites.map { it.id }.toSet().size)
    }
}
