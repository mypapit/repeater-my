package net.mypapit.mobile.myrepeater.ui

import net.mypapit.mobile.myrepeater.data.db.Repeater
import net.mypapit.mobile.myrepeater.data.db.RepeaterStatus
import net.mypapit.mobile.myrepeater.ui.detail.CorrectionEmail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DateFormatsTest {

    @Test
    fun `dates are written day first with leading zeros`() {
        assertEquals("05/02/2017", DateFormats.date(LocalDate.of(2017, 2, 5)))
        assertEquals("31/12/2031", DateFormats.date(LocalDate.of(2031, 12, 31)))
    }

    @Test
    fun `month-first source dates are turned round`() {
        assertEquals("05/02/2017", DateFormats.fromMonthFirst("2/5/2017"))
        assertEquals("24/12/2025", DateFormats.fromMonthFirst("12/24/2025"))
        assertEquals("28/10/2022", DateFormats.fromMonthFirst(" 10/28/2022 "))
        assertEquals("29/11/2025", DateFormats.fromMonthFirst("2025-11-29"))
    }

    @Test
    fun `day-first source dates are only tidied`() {
        assertEquals("31/12/2031", DateFormats.fromDayFirst("31/12/2031"))
        assertEquals("01/02/2026", DateFormats.fromDayFirst("1/2/2026"))
        assertEquals("29/11/2025", DateFormats.fromDayFirst("2025-11-29"))
    }

    @Test
    fun `text that is not a date is left alone`() {
        assertEquals("unknown", DateFormats.fromMonthFirst("unknown"))
        assertEquals("13/13/2020", DateFormats.fromMonthFirst("13/13/2020"))
        assertEquals("", DateFormats.fromDayFirst(""))
    }

    @Test
    fun `date and time follow the clock setting`() {
        val kualaLumpur = ZoneId.of("Asia/Kuala_Lumpur")
        // 2026-11-14 05:06 in Kuala Lumpur is 2026-11-13 21:06 UTC.
        val millis = 1_794_603_960_000L
        assertEquals("14/11/2026 05:06", DateFormats.dateTime(millis, kualaLumpur, use24Hour = true))
        assertEquals("14/11/2026 5:06 AM", DateFormats.dateTime(millis, kualaLumpur, use24Hour = false))
    }

    @Test
    fun `malaysian time is UTC+8 and marked MYT whatever the phone zone`() {
        // 2026-11-13 21:06 UTC.
        val millis = 1_794_603_960_000L
        assertEquals("14/11/2026 05:06 MYT", DateFormats.malaysiaDateTime(millis, use24Hour = true))
        assertEquals("14/11/2026 5:06 AM MYT", DateFormats.malaysiaDateTime(millis, use24Hour = false))
        // Just before midnight UTC is already the next morning in Malaysia.
        assertEquals("15/11/2026 07:59 MYT", DateFormats.malaysiaDateTime(1_794_700_740_000L, use24Hour = true))
    }

    private val repeater = Repeater(
        callsign = "9M4RXA", location = "Sedim (Gunung Bintang)", state = "Kedah", latitude = 5.4, longitude = 100.8,
        band = "70cm", downlinkMhz = 439.675, uplinkMhz = 434.675, offsetMhz = -5.0, tone = "88.5", mode = "FM",
        club = "MARTS", clubFullName = "Malaysian Amateur Radio Transmitters' Society", linkedNetwork = "",
        status = RepeaterStatus.ON_AIR, lastReviewed = "", source = "", repeaterBookId = "", notes = "",
    )

    @Test
    fun `correction e-mail subject names the repeater`() {
        assertEquals("9M4RXA - Repeater Info Corrections", CorrectionEmail.subject(repeater))
        assertEquals("mypapit+repeater@gmail.com", CorrectionEmail.ADDRESS)
    }

    @Test
    fun `correction e-mail body lists the recorded details`() {
        val expected = """
            State which repeater details that needs correction

            Repeater callsign: 9M4RXA
            Freq: 439.675 MHz
            Offset: -5 MHz
            Tone: 88.5
            Club name: Malaysian Amateur Radio Transmitters' Society
            Location: Sedim (Gunung Bintang), Kedah
            Club: MARTS
        """.trimIndent()
        assertEquals(expected, CorrectionEmail.body(repeater).replace("\r\n", "\n"))
    }

    @Test
    fun `missing details are shown as a dash, never left out`() {
        val bare = repeater.copy(
            downlinkMhz = null, offsetMhz = null, tone = "", clubFullName = "", location = "", state = "",
            club = "UNKNOWN",
        )
        val body = CorrectionEmail.body(bare).replace("\r\n", "\n")
        assertEquals(
            listOf("Freq: -", "Offset: -", "Tone: -", "Club name: -", "Location: -", "Club: UNKNOWN"),
            body.lines().takeLast(6),
        )
        assertFalse(body.contains('−'))
    }
}
