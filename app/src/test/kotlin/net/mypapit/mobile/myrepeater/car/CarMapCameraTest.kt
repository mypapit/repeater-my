package net.mypapit.mobile.myrepeater.car

import net.mypapit.mobile.myrepeater.data.db.Repeater
import net.mypapit.mobile.myrepeater.data.db.RepeaterStatus
import net.mypapit.mobile.myrepeater.data.location.GeoPoint
import net.mypapit.mobile.myrepeater.data.settings.AppSettings
import net.mypapit.mobile.myrepeater.ui.list.RepeaterListItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CarMapCameraTest {

    private fun repeater(id: Long, callsign: String, lat: Double, lon: Double) = Repeater(
        id = id,
        callsign = callsign,
        location = "",
        state = "",
        latitude = lat,
        longitude = lon,
        band = "2m",
        downlinkMhz = 145.625,
        uplinkMhz = null,
        offsetMhz = -0.6,
        tone = "103.5",
        mode = "FM",
        club = "MARTS",
        clubFullName = "",
        linkedNetwork = "",
        status = RepeaterStatus.ON_AIR,
        lastReviewed = "",
        source = "",
        repeaterBookId = "",
        notes = "",
    )

    private val origin = GeoPoint(3.139, 101.687)

    @Test
    fun `zoom clamping respects bounds and handles non-finite values`() {
        assertEquals(CarMapZoom.DEFAULT_ZOOM, CarMapZoom.clampZoom(Double.NaN), 1e-6)
        assertEquals(CarMapZoom.DEFAULT_ZOOM, CarMapZoom.clampZoom(Double.POSITIVE_INFINITY), 1e-6)
        assertEquals(CarMapZoom.MIN_ZOOM, CarMapZoom.clampZoom(2.0), 1e-6)
        assertEquals(CarMapZoom.MAX_ZOOM, CarMapZoom.clampZoom(20.0), 1e-6)
        assertEquals(11.2, CarMapZoom.clampZoom(11.2), 1e-6)
    }

    @Test
    fun `scale factor converts logarithmically and handles double tap and invalid input`() {
        assertEquals(1.0, CarMapZoom.zoomDeltaFromScale(2.0f), 1e-6)
        assertEquals(-1.0, CarMapZoom.zoomDeltaFromScale(0.5f), 1e-6)
        assertEquals(CarMapZoom.DOUBLE_TAP_ZOOM_DELTA, CarMapZoom.zoomDeltaFromScale(10f), 1e-6)
        assertEquals(0.0, CarMapZoom.zoomDeltaFromScale(0f), 1e-6)
        assertEquals(0.0, CarMapZoom.zoomDeltaFromScale(-1f), 1e-6)
        assertEquals(0.0, CarMapZoom.zoomDeltaFromScale(Float.NaN), 1e-6)
    }

    @Test
    fun `default zoom covers a 50 to 60 km radius around the car`() {
        val defaultRadius = CarMapZoom.zoomToRadiusKm(CarMapZoom.DEFAULT_ZOOM, origin.latitude, 180f, 160)
        val closeRadius = CarMapZoom.zoomToRadiusKm(AppSettings.CAR_ZOOM_CLOSE, origin.latitude, 180f, 160)
        val wideRadius = CarMapZoom.zoomToRadiusKm(AppSettings.CAR_ZOOM_WIDE, origin.latitude, 180f, 160)

        assertTrue("Expected default radius in 50..60 km, got $defaultRadius", defaultRadius in 50.0..60.0)
        assertTrue(closeRadius < defaultRadius)
        assertTrue(defaultRadius < wideRadius)
    }

    @Test
    fun `camera follows origin by default and avoids yanking when user panned`() {
        val near = repeater(1, "9M2RKK", 3.18, 101.70)
        val item = RepeaterListItem(near, distanceKm = 5.0, bearingDeg = 20.0)

        val passive = CarMapZoom.computeFocusTarget(
            origin = origin,
            focused = item,
            preferredZoom = 8.0,
            userPanned = false,
            explicitFocus = false,
        )
        assertEquals(CarCameraTarget(origin, 8.0), passive)

        val whilePanned = CarMapZoom.computeFocusTarget(
            origin = origin,
            focused = item,
            preferredZoom = 8.0,
            userPanned = true,
            explicitFocus = false,
        )
        assertNull(whilePanned)
    }

    @Test
    fun `explicit focus frames nearby repeater with origin and centers on distant repeater`() {
        val nearby = repeater(1, "NEAR", 3.38, 101.79)
        val nearTarget = CarMapZoom.computeFocusTarget(
            origin = origin,
            focused = RepeaterListItem(nearby, distanceKm = 30.0, bearingDeg = 23.0),
            preferredZoom = 8.0,
            userPanned = true,
            explicitFocus = true,
        )!!
        assertEquals((origin.latitude + nearby.latitude!!) / 2.0, nearTarget.center.latitude, 1e-6)
        assertEquals(8.0, nearTarget.zoom, 1e-6)

        val far = repeater(2, "FAR", 4.60, 101.09)
        val farTarget = CarMapZoom.computeFocusTarget(
            origin = origin,
            focused = RepeaterListItem(far, distanceKm = 175.0, bearingDeg = 335.0),
            preferredZoom = 9.0,
            userPanned = false,
            explicitFocus = true,
        )!!
        assertEquals(far.latitude!!, farTarget.center.latitude, 1e-6)
        assertEquals(far.longitude!!, farTarget.center.longitude, 1e-6)
        assertEquals(9.0, farTarget.zoom, 1e-6)
    }

    @Test
    fun `pin hit testing picks closest pin within radius and ignores distant taps`() {
        val a = repeater(1, "A", 3.1, 101.6)
        val b = repeater(2, "B", 3.2, 101.7)
        val candidates = listOf(
            a to (100f to 100f),
            b to (120f to 100f),
        )
        assertEquals(a, CarMapZoom.hitTestRepeater(96f, 100f, candidates, emptySet(), 40f))
        assertEquals(b, CarMapZoom.hitTestRepeater(110f, 100f, candidates, setOf(2L), 40f))
        assertNull(CarMapZoom.hitTestRepeater(300f, 300f, candidates, emptySet(), 40f))
    }
}
