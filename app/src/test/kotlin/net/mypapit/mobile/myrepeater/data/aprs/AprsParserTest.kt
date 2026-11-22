package net.mypapit.mobile.myrepeater.data.aprs

import net.mypapit.mobile.myrepeater.data.location.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class AprsParserTest {

    private fun feature(
        callsign: String? = "9M2ABC-9",
        lon: Any? = 101.65,
        lat: Any? = 2.87,
        objectName: String? = null,
        receivedAt: String? = "2026-11-14T14:00:02Z",
        comment: String = "hello",
        geometryType: String = "Point",
    ): String {
        fun q(value: String?) = if (value == null) "null" else "\"$value\""
        return """{"type":"Feature","geometry":{"type":"$geometryType","coordinates":[$lon,$lat]},""" +
            """"properties":{"callsign":${q(callsign)},"object_name":${q(objectName)},""" +
            """"received_at":${q(receivedAt)},"comment":${q(comment)},"symbol":">","symbol_table":"/"}}"""
    }

    private fun collection(vararg features: String) =
        """{"type":"FeatureCollection","features":[${features.joinToString(",")}]}"""

    @Test
    fun `a station is read with its position, time and comment`() {
        val station = AprsParser.parse(collection(feature())).single()
        assertEquals("9M2ABC-9", station.callsign)
        assertEquals("9M2ABC-9", station.name)
        assertEquals(GeoPoint(2.87, 101.65), station.point)
        assertEquals(Instant.parse("2026-11-14T14:00:02Z"), station.receivedAt)
        assertEquals("hello", station.comment)
    }

    @Test
    fun `an object is named by its object name and keeps the sender's callsign`() {
        val station = AprsParser.parse(collection(feature(objectName = "BANJIR-1"))).single()
        assertEquals("BANJIR-1", station.name)
        assertEquals("9M2ABC-9", station.callsign)
    }

    @Test
    fun `a null object name is not read as the word null`() {
        assertEquals("9M2ABC-9", AprsParser.parse(collection(feature(objectName = null))).single().name)
    }

    @Test
    fun `stations with missing, zero or impossible coordinates are dropped`() {
        val stations = AprsParser.parse(
            collection(
                feature(callsign = "GOOD"),
                feature(callsign = "ZEROLAT", lat = 0.0000026),
                feature(callsign = "ZEROLON", lon = 0.0),
                feature(callsign = "FARLAT", lat = 95.0),
                feature(callsign = "FARLON", lon = 200.0),
                feature(callsign = "NOLAT", lat = null),
                feature(callsign = "LINE", geometryType = "LineString"),
            ),
        )
        assertEquals(listOf("GOOD"), stations.map { it.callsign })
    }

    @Test
    fun `stations without a callsign are dropped and a bad time becomes null`() {
        val stations = AprsParser.parse(
            collection(feature(callsign = null), feature(callsign = ""), feature(receivedAt = "yesterday")),
        )
        assertEquals(1, stations.size)
        assertNull(stations.single().receivedAt)
    }

    @Test
    fun `comments are stripped of control characters and capped`() {
        val long = "x".repeat(500)
        val stations = AprsParser.parse(collection(feature(comment = "line1\\nline2\\u0007 end"), feature(comment = long)))
        assertEquals("line1line2 end", stations[0].comment)
        assertFalse(stations[0].comment.any { it.isISOControl() })
        assertEquals(AprsParser.MAX_COMMENT_LENGTH, stations[1].comment.length)
    }

    @Test
    fun `the number of stations is capped`() {
        val many = Array(AprsParser.MAX_STATIONS + 50) { feature(callsign = "S$it") }
        assertEquals(AprsParser.MAX_STATIONS, AprsParser.parse(collection(*many)).size)
    }

    @Test
    fun `files that are not the stations list are rejected`() {
        assertThrows(AprsFormatException::class.java) { AprsParser.parse("<html>502 Bad Gateway</html>") }
        assertThrows(AprsFormatException::class.java) { AprsParser.parse("""{"type":"Feature"}""") }
        assertThrows(AprsFormatException::class.java) { AprsParser.parse("""{"type":"FeatureCollection"}""") }
        assertTrue(AprsParser.parse("""{"type":"FeatureCollection","features":[]}""").isEmpty())
    }

    @Test
    fun `a saved copy is fresh for thirty minutes`() {
        val savedAt = 1_000_000_000_000L
        val minute = 60_000L
        assertTrue(AprsRepository.isFresh(savedAt, savedAt))
        assertTrue(AprsRepository.isFresh(savedAt, savedAt + 29 * minute))
        assertFalse(AprsRepository.isFresh(savedAt, savedAt + 30 * minute))
        assertFalse(AprsRepository.isFresh(savedAt, savedAt + 3 * 60 * minute))
        // No saved copy, or a clock that has gone backwards, both mean "download again".
        assertFalse(AprsRepository.isFresh(0, savedAt))
        assertFalse(AprsRepository.isFresh(savedAt, savedAt - minute))
    }
}
