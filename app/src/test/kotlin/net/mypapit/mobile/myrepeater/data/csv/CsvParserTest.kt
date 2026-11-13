package net.mypapit.mobile.myrepeater.data.csv

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.StringReader

class CsvParserTest {

    private fun parse(text: String) = CsvParser.parse(StringReader(text)).toList()

    @Test
    fun `plain rows split on commas`() {
        assertEquals(listOf(listOf("a", "b", "c"), listOf("1", "2", "3")), parse("a,b,c\n1,2,3\n"))
    }

    @Test
    fun `byte order mark is dropped from the first cell`() {
        assertEquals(listOf(listOf("Callsign", "Band")), parse("﻿Callsign,Band\n"))
    }

    @Test
    fun `quoted cells keep commas, quotes and newlines`() {
        val rows = parse("x,\"KELAB RADIO AMATUR TAWAU, SABAH\",y\nz,\"reports \"\"not heard\"\"\",\"line1\nline2\"\n")
        assertEquals("KELAB RADIO AMATUR TAWAU, SABAH", rows[0][1])
        assertEquals("reports \"not heard\"", rows[1][1])
        assertEquals("line1\nline2", rows[1][2])
    }

    @Test
    fun `windows line endings and a missing final newline are handled`() {
        assertEquals(listOf(listOf("a", "b"), listOf("c", "d")), parse("a,b\r\nc,d"))
    }

    @Test
    fun `empty cells are kept and blank lines skipped`() {
        assertEquals(listOf(listOf("a", "", ""), listOf("", "b", "")), parse("a,,\n\n,,\n,b,\n"))
    }

    @Test
    fun `empty quoted cell is an empty string`() {
        assertEquals(listOf(listOf("a", "", "b")), parse("a,\"\",b\n"))
    }

    @Test
    fun `empty input yields no rows`() {
        assertEquals(emptyList<List<String>>(), parse(""))
    }
}
