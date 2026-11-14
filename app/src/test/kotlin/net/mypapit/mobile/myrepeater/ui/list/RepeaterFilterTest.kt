package net.mypapit.mobile.myrepeater.ui.list

import net.mypapit.mobile.myrepeater.data.db.Repeater
import net.mypapit.mobile.myrepeater.data.db.RepeaterStatus
import net.mypapit.mobile.myrepeater.ui.callsign.CallsignSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RepeaterFilterTest {

    private fun repeater(
        callsign: String,
        location: String = "Bukit Lanjan",
        state: String = "Selangor",
        band: String = "2m",
        downlink: Double = 147.98,
        club: String = "MARTS",
        status: RepeaterStatus = RepeaterStatus.ON_AIR,
    ) = Repeater(
        callsign = callsign, location = location, state = state, latitude = null, longitude = null, band = band,
        downlinkMhz = downlink, uplinkMhz = null, offsetMhz = null, tone = "", mode = "FM", club = club,
        clubFullName = "", linkedNetwork = "", status = status, lastReviewed = "", source = "",
        repeaterBookId = "", notes = "",
    )

    private val lanjan = repeater("9M2RKK")
    private val genting = repeater(
        "9M2RUK", location = "Gunung Ulu Kali (Genting)", state = "Selangor/Pahang", downlink = 146.9875,
    )
    private val uhf = repeater("9M2RKL", band = "70cm", downlink = 439.6)
    private val offAir = repeater(
        "9M4RLX", location = "Kota Marudu", state = "Sabah", downlink = 144.225, club = "BARC",
        status = RepeaterStatus.OFF_AIR,
    )
    private val all = listOf(lanjan, genting, uhf, offAir)

    private fun RepeaterFilter.pick() = all.filter(::matches).map { it.callsign }

    @Test
    fun `an empty filter keeps everything and is inactive`() {
        assertFalse(RepeaterFilter().isActive)
        assertEquals(4, RepeaterFilter().pick().size)
        assertFalse(RepeaterFilter(query = "   ").isActive)
    }

    @Test
    fun `text matches callsign, place, club and frequency, ignoring case`() {
        assertEquals(listOf("9M2RKK"), RepeaterFilter(query = "rkk").pick())
        assertEquals(listOf("9M2RUK"), RepeaterFilter(query = "genting").pick())
        assertEquals(listOf("9M4RLX"), RepeaterFilter(query = "barc").pick())
        assertEquals(listOf("9M2RKL"), RepeaterFilter(query = "439.6").pick())
        assertEquals(listOf("9M2RKK"), RepeaterFilter(query = "147.98").pick())
    }

    @Test
    fun `every word must match`() {
        assertEquals(listOf("9M2RKK", "9M2RKL"), RepeaterFilter(query = "lanjan marts").pick())
        assertEquals(emptyList<String>(), RepeaterFilter(query = "lanjan sabah").pick())
    }

    @Test
    fun `band, on-air and state narrow the list together`() {
        assertEquals(listOf("9M2RKL"), RepeaterFilter(bands = setOf("70cm")).pick())
        assertEquals(listOf("9M2RKK", "9M2RUK", "9M2RKL"), RepeaterFilter(onAirOnly = true).pick())
        assertEquals(listOf("9M4RLX"), RepeaterFilter(state = "Sabah").pick())
        assertEquals(emptyList<String>(), RepeaterFilter(state = "Sabah", onAirOnly = true).pick())
        assertTrue(RepeaterFilter(state = "Sabah").isActive)
    }

    @Test
    fun `a repeater on a state border is found under either state`() {
        assertEquals(listOf("9M2RUK"), RepeaterFilter(state = "Pahang").pick())
        assertTrue("9M2RUK" in RepeaterFilter(state = "Selangor").pick())
        assertEquals(listOf("Pahang", "Sabah", "Selangor"), RepeaterFilter.statesIn(all))
        assertEquals(listOf("2m", "70cm"), RepeaterFilter.bandsIn(all))
    }

    @Test
    fun `callsign queries lose wildcard characters`() {
        assertEquals("9W2", CallsignSearch.clean("  9W2%  "))
        assertEquals("AB", CallsignSearch.clean("A_B\\"))
        assertEquals("", CallsignSearch.clean("%%"))
    }

    @Test
    fun `expiry is read as day-month-year`() {
        val today = LocalDate.of(2026, 11, 14)
        assertEquals(false, CallsignSearch.isExpired("31/12/2031", today))
        assertEquals(true, CallsignSearch.isExpired("31/12/2025", today))
        assertEquals(false, CallsignSearch.isExpired("14/11/2026", today))
        assertEquals(true, CallsignSearch.isExpired("1/2/2026", today))
        assertNull(CallsignSearch.isExpired("", today))
        assertNull(CallsignSearch.isExpired("sometime", today))
    }
}
