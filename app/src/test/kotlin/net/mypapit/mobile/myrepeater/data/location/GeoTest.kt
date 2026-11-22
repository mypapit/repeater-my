package net.mypapit.mobile.myrepeater.data.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.StringReader

class GeoTest {

    private val origin = GeoPoint(3.0, 101.0)

    @Test
    fun `distance to the same point is zero`() {
        assertEquals(0.0, Geo.distanceKm(origin, origin), 1e-9)
    }

    @Test
    fun `one degree of latitude is about 111 km`() {
        assertEquals(111.19, Geo.distanceKm(origin, GeoPoint(4.0, 101.0)), 0.05)
    }

    @Test
    fun `distance is symmetric`() {
        val other = GeoPoint(5.4141, 100.3288)
        assertEquals(Geo.distanceKm(origin, other), Geo.distanceKm(other, origin), 1e-9)
    }

    @Test
    fun `bearings follow the compass`() {
        assertEquals(0.0, Geo.bearingDeg(origin, GeoPoint(4.0, 101.0)), 0.01)
        assertEquals(180.0, Geo.bearingDeg(origin, GeoPoint(2.0, 101.0)), 0.01)
        assertEquals(90.0, Geo.bearingDeg(GeoPoint(0.0, 101.0), GeoPoint(0.0, 102.0)), 0.01)
        assertEquals(270.0, Geo.bearingDeg(GeoPoint(0.0, 101.0), GeoPoint(0.0, 100.0)), 0.01)
    }

    @Test
    fun `bearing stays within 0 until 360`() {
        for (lat in listOf(-1.0, 2.5, 7.0)) for (lon in listOf(99.0, 101.0, 119.0)) {
            val bearing = Geo.bearingDeg(origin, GeoPoint(lat, lon))
            assertTrue("$lat,$lon -> $bearing", bearing >= 0.0 && bearing < 360.0)
        }
    }

    @Test
    fun `preset rows parse and bad rows are dropped`() {
        val csv = "state,name,latitude,longitude\n" +
            "Kedah,\"Gunung Raya, Langkawi\",6.383,99.817\n" +
            "Perlis,Kangar,6.44,100.2\n" +
            "Perlis,Broken,not-a-number,100.2\n" +
            "short,row\n"
        val presets = PresetLocations.parse(StringReader(csv))

        assertEquals(2, presets.size)
        assertEquals("Gunung Raya, Langkawi", presets[0].name)
        assertEquals("Kedah|Gunung Raya, Langkawi", presets[0].key)
        assertEquals(GeoPoint(6.44, 100.2), presets[1].point)
    }

    @Test
    fun `bundled presets cover every state with at least three places`() {
        val presets = File("src/main/assets/preset_locations.csv").bufferedReader().use { PresetLocations.parse(it) }
        val byState = presets.groupBy { it.state }
        val states = listOf(
            "Johor", "Kedah", "Kelantan", "Melaka", "Negeri Sembilan", "Pahang", "Penang",
            "Perak", "Perlis", "Sabah", "Sarawak", "Selangor", "Terengganu",
        )
        for (state in states) {
            assertTrue("$state has ${byState[state]?.size ?: 0}", (byState[state]?.size ?: 0) >= 3)
        }
        for (territory in listOf("Kuala Lumpur", "Putrajaya", "Labuan")) {
            assertTrue(territory, byState.containsKey(territory))
        }
        assertEquals(presets.size, presets.map { it.key }.toSet().size)
        // Everything should sit inside Malaysia's bounding box.
        assertTrue(presets.all { it.point.latitude in 0.8..7.5 && it.point.longitude in 99.5..119.5 })
    }
}
