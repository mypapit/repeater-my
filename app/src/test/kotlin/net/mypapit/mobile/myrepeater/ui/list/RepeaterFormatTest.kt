package net.mypapit.mobile.myrepeater.ui.list

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RepeaterFormatTest {

    @Test
    fun `frequency keeps 12_5 kHz steps and pads to kHz`() {
        assertEquals("145.0875", RepeaterFormat.frequency(145.0875))
        assertEquals("144.225", RepeaterFormat.frequency(144.225))
        assertEquals("145.000", RepeaterFormat.frequency(145.0))
        assertEquals("438.500", RepeaterFormat.frequency(438.5))
    }

    @Test
    fun `offset is signed`() {
        assertEquals("+0.6", RepeaterFormat.offset(0.6))
        assertEquals("−0.6", RepeaterFormat.offset(-0.6))
        assertEquals("−5", RepeaterFormat.offset(-5.0))
        assertEquals("0", RepeaterFormat.offset(0.0))
    }

    @Test
    fun `prefix and hue are stable`() {
        assertEquals("9M4", RepeaterFormat.prefix("9M4RLX"))
        assertEquals("9M", RepeaterFormat.prefix("9M"))
        assertEquals(RepeaterFormat.avatarHue("9M4RLX"), RepeaterFormat.avatarHue("9M4RLX"))
        for (call in listOf("9M4RLX", "9M2RKK", "", "ZZZZZZZZZZ")) {
            assertTrue(RepeaterFormat.avatarHue(call) in 0f..359f)
        }
    }
}
