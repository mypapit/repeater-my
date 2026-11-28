package net.mypapit.mobile.myrepeater.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClubNameTest {

    @Test
    fun `affiliate wins over sponsor`() {
        assertEquals("ASTRA", ClubName.derive("ASTRA", "MARTS"))
    }

    @Test
    fun `sponsor is used when affiliate is empty`() {
        assertEquals("BARC", ClubName.derive("", "BARC"))
        assertEquals("BARC", ClubName.derive("  ", "BARC"))
        assertEquals("BARC", ClubName.derive(null, "BARC"))
    }

    @Test
    fun `unknown when both are empty or placeholders`() {
        assertEquals("UNKNOWN", ClubName.derive("", ""))
        assertEquals("UNKNOWN", ClubName.derive(null, null))
        assertEquals("UNKNOWN", ClubName.derive("-", "N/A"))
        assertEquals("UNKNOWN", ClubName.derive("??", "none"))
    }

    @Test
    fun `short names are kept and upper-cased`() {
        assertEquals("RES'K", ClubName.derive("RES'K", ""))
        assertEquals("PEMANCAR", ClubName.derive("Pemancar", ""))
    }

    @Test
    fun `bracketed acronym is preferred for long names`() {
        assertEquals("ARCS", ClubName.derive("", "AMATEUR RADIO CLUB SARAWAK (ARCS)"))
    }

    @Test
    fun `leading acronym of a mixed-case name is kept`() {
        assertEquals("ASTRA", ClubName.derive("ASTRA Local Besut", ""))
        assertEquals("AKRAB", ClubName.derive("AKRAB Bachok", ""))
        assertEquals("MOTOROLA", ClubName.derive("", "MOTOROLA Amateur Radio Club"))
    }

    @Test
    fun `long all-caps names become initials`() {
        assertEquals("KRATS", ClubName.derive("KELAB RADIO AMATUR TAWAU, SABAH", ""))
        assertEquals("SARS", ClubName.derive("", "SABAH AMATEUR RADIO SOCIETY"))
        assertEquals("KRAS", ClubName.derive("KELAB RADIO AMATUR SEGAMAT", ""))
    }

    @Test
    fun `a single long word is cut`() {
        assertEquals("SUPERCALIF", ClubName.derive("Supercalifragilistic", ""))
    }

    @Test
    fun `result never exceeds the limit`() {
        val names = listOf(
            "A B C D E F G H I J K L M N O P",
            "KELAB RADIO AMATUR UTARA SELATAN SABAH",
            "TUARAN RADIO AMATEUR TRANSMITTING SOCIETY",
            "x".repeat(50),
        )
        for (name in names) {
            val club = ClubName.derive(name, "")
            assertTrue("$name -> $club", club.length in 1..ClubName.MAX_LENGTH)
        }
    }

    @Test
    fun `full name prefers the sponsor`() {
        assertEquals("SABAH AMATEUR RADIO SOCIETY", ClubName.fullName("SARS", "SABAH AMATEUR RADIO SOCIETY"))
        assertEquals("SARS", ClubName.fullName("SARS", ""))
        assertEquals("", ClubName.fullName("", "-"))
    }
}
