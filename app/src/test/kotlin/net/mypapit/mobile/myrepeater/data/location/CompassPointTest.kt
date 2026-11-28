package net.mypapit.mobile.myrepeater.data.location

import org.junit.Assert.assertEquals
import org.junit.Test

class CompassPointTest {

    @Test
    fun `bearings map to the nearest of eight compass points`() {
        assertEquals("N", Geo.compassPoint(0.0))
        assertEquals("N", Geo.compassPoint(22.4))
        assertEquals("NE", Geo.compassPoint(22.5))
        assertEquals("E", Geo.compassPoint(90.0))
        assertEquals("SE", Geo.compassPoint(135.0))
        assertEquals("S", Geo.compassPoint(180.0))
        assertEquals("SW", Geo.compassPoint(225.0))
        assertEquals("W", Geo.compassPoint(270.0))
        assertEquals("NW", Geo.compassPoint(315.0))
        assertEquals("N", Geo.compassPoint(337.5))
        assertEquals("N", Geo.compassPoint(359.9))
    }

    @Test
    fun `bearings outside 0 to 360 wrap around`() {
        assertEquals("N", Geo.compassPoint(360.0))
        assertEquals("E", Geo.compassPoint(450.0))
        assertEquals("W", Geo.compassPoint(-90.0))
    }
}
