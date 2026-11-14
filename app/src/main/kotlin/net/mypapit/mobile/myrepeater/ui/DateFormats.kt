package net.mypapit.mobile.myrepeater.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Every date the app shows is written day first, DD/MM/YYYY, as is usual in
 * Malaysia, whatever the phone's language is set to.
 */
object DateFormats {
    private val dayFirst = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH)
    private val time24 = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
    private val time12 = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

    private val looseDayFirst = DateTimeFormatter.ofPattern("d/M/yyyy", Locale.ENGLISH)
    private val looseMonthFirst = DateTimeFormatter.ofPattern("M/d/yyyy", Locale.ENGLISH)

    fun date(date: LocalDate): String = date.format(dayFirst)

    /**
     * Rewrites a date the source gives month first (the repeater list's
     * "last reviewed" column, for example 12/24/2025) as 24/12/2025. ISO dates
     * are accepted too. Text that is not a date is returned unchanged.
     */
    fun fromMonthFirst(text: String): String =
        (parse(text, looseMonthFirst) ?: parse(text, DateTimeFormatter.ISO_LOCAL_DATE))?.let(::date) ?: text.trim()

    /**
     * Tidies a date the source already gives day first (the callsign list's
     * expiry column), so 1/2/2026 becomes 01/02/2026. Text that is not a date
     * is returned unchanged.
     */
    fun fromDayFirst(text: String): String =
        (parse(text, looseDayFirst) ?: parse(text, DateTimeFormatter.ISO_LOCAL_DATE))?.let(::date) ?: text.trim()

    /** A moment in time as DD/MM/YYYY followed by the time, in 24-hour or 12-hour form. */
    fun dateTime(epochMillis: Long, zone: ZoneId, use24Hour: Boolean): String {
        val local = Instant.ofEpochMilli(epochMillis).atZone(zone)
        return local.format(dayFirst) + " " + local.format(if (use24Hour) time24 else time12)
    }

    private fun parse(text: String, format: DateTimeFormatter): LocalDate? =
        try {
            LocalDate.parse(text.trim(), format)
        } catch (e: DateTimeParseException) {
            null
        }
}
