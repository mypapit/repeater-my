package net.mypapit.mobile.myrepeater.data.csv

import net.mypapit.mobile.myrepeater.data.ClubName
import net.mypapit.mobile.myrepeater.data.db.RepeaterStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.io.StringReader

class CsvMappersTest {

    private val repeaterHeader =
        "﻿Callsign,Location (estimated),State (inferred),Latitude,Longitude,Band," +
            "Downlink MHz (repeater TX = your RX),Uplink MHz (repeater RX = your TX),Offset MHz," +
            "Tone (CTCSS Hz or DMR color code),Mode,Club / Sponsor,Affiliate (RepeaterBook),Linked network," +
            "Status,Last reviewed (RepeaterBook),Source,RepeaterBook ID,Notes"

    private fun repeaters(vararg lines: String) =
        RepeaterCsvMapper.map(CsvParser.parse(StringReader((listOf(repeaterHeader) + lines).joinToString("\n"))))

    @Test
    fun `repeater row maps every column`() {
        val r = repeaters(
            "9M4RBZ,Sipitang,Sabah,5.083,115.567,2m,145.0875,145.6875,0.6,203.5,FM,BARC,,," +
                "On-air per RepeaterBook,12/24/2025,RB+HamFreq,13770,\"Listener reports \"\"not heard\"\"\"",
        ).single()

        assertEquals("9M4RBZ", r.callsign)
        assertEquals("Sipitang", r.location)
        assertEquals("Sabah", r.state)
        assertEquals(5.083, r.latitude!!, 1e-9)
        assertEquals(115.567, r.longitude!!, 1e-9)
        assertEquals("2m", r.band)
        assertEquals(145.0875, r.downlinkMhz!!, 1e-9)
        assertEquals(145.6875, r.uplinkMhz!!, 1e-9)
        assertEquals(0.6, r.offsetMhz!!, 1e-9)
        assertEquals("203.5", r.tone)
        assertEquals("FM", r.mode)
        assertEquals("BARC", r.club)
        assertEquals(RepeaterStatus.ON_AIR, r.status)
        assertEquals("13770", r.repeaterBookId)
        assertEquals("Listener reports \"not heard\"", r.notes)
        assertTrue(r.hasCoordinates)
    }

    @Test
    fun `missing or half-missing coordinates leave the repeater off the map`() {
        val rows = repeaters(
            "9M4AAA,Somewhere,Perak,,,2m,145.5,144.9,-0.6,,FM,,,,Unknown per RepeaterBook,,,,",
            "9M4BBB,Somewhere,Perak,4.5,,2m,145.5,144.9,-0.6,,FM,,,,Off-air per RepeaterBook,,,,",
            "9M4CCC,Somewhere,Perak,0,0,2m,145.5,144.9,-0.6,,FM,,,,,,,,",
        )
        assertEquals(3, rows.size)
        rows.forEach {
            assertFalse(it.hasCoordinates)
            assertNull(it.latitude)
            assertNull(it.longitude)
        }
        assertEquals(RepeaterStatus.UNKNOWN, rows[0].status)
        assertEquals(RepeaterStatus.OFF_AIR, rows[1].status)
        assertEquals(RepeaterStatus.UNKNOWN, rows[2].status)
        assertEquals(ClubName.UNKNOWN, rows[0].club)
    }

    @Test
    fun `rows without a callsign are skipped and duplicates kept`() {
        val rows = repeaters(
            ",Nowhere,Perak,4,101,2m,145.5,144.9,-0.6,,FM,,,,,,,,",
            "9M4RKR,Site,Kedah,6,100,2m,145.5,144.9,-0.6,,FM,,,,,,,,",
            "9m4rkr,Site,Kedah,6,100,70cm,438.5,433.5,-5,,FM,,,,,,,,",
        )
        assertEquals(listOf("9M4RKR", "9M4RKR"), rows.map { it.callsign })
    }

    @Test
    fun `column order does not matter`() {
        val csv = "Downlink MHz,Notes,Callsign\n145.5,hello,9M4XYZ\n"
        val r = RepeaterCsvMapper.map(CsvParser.parse(StringReader(csv))).single()
        assertEquals("9M4XYZ", r.callsign)
        assertEquals(145.5, r.downlinkMhz!!, 1e-9)
        assertEquals("hello", r.notes)
        assertEquals("", r.location)
    }

    @Test
    fun `a file that is not the repeater list is rejected`() {
        assertThrows(CsvFormatException::class.java) {
            RepeaterCsvMapper.map(CsvParser.parse(StringReader("<html><body>502 Bad Gateway</body></html>")))
        }
        assertThrows(CsvFormatException::class.java) {
            RepeaterCsvMapper.map(CsvParser.parse(StringReader("")))
        }
    }

    @Test
    fun `callsign rows drop the assignment number and skip blanks`() {
        val csv = "No.,Assignment Holder,Call Sign,Assign No,Expiry Date\n" +
            "1,EXAMPLE HOLDER,9w2abc,0000001,31/12/2031\n" +
            "2,,,,\n" +
            "3,ANOTHER HOLDER,9M2XYZ,0000002,31/12/2029\n"
        val rows = CallsignCsvMapper.map(CsvParser.parse(StringReader(csv)))

        assertEquals(2, rows.size)
        assertEquals("9W2ABC", rows[0].callsign)
        assertEquals("EXAMPLE HOLDER", rows[0].holder)
        assertEquals("31/12/2031", rows[0].expiry)
        assertFalse(rows.any { it.toString().contains("0000001") })
    }

    @Test
    fun `a file that is not the callsign list is rejected`() {
        assertThrows(CsvFormatException::class.java) {
            CallsignCsvMapper.map(CsvParser.parse(StringReader("Callsign,Band\n9M4RLX,2m\n")))
        }
    }

    /** Runs against the unversioned seed files when they are present on the machine. */
    @Test
    fun `seed files parse cleanly`() {
        val repeaterFile = File("../csv/repeaters.csv")
        val callsignFile = File("../csv/callsign.csv")
        assumeTrue(repeaterFile.exists() && callsignFile.exists())

        val repeaters = repeaterFile.bufferedReader().use { RepeaterCsvMapper.map(CsvParser.parse(it)) }
        assertEquals(119, repeaters.size)
        assertEquals(14, repeaters.count { !it.hasCoordinates })
        assertTrue(repeaters.all { it.downlinkMhz != null })
        assertTrue(repeaters.all { it.club.isNotEmpty() && it.club.length <= ClubName.MAX_LENGTH })

        val callsigns = callsignFile.bufferedReader().use { CallsignCsvMapper.map(CsvParser.parse(it)) }
        assertTrue(callsigns.size > 10_000)
        assertTrue(callsigns.all { it.callsign.isNotBlank() })
    }
}
