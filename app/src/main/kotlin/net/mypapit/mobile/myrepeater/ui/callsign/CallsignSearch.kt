package net.mypapit.mobile.myrepeater.ui.callsign

import net.mypapit.mobile.myrepeater.data.db.CallsignRecord
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

sealed interface CallsignSearchState {
    /** Fewer than [CallsignSearch.MIN_QUERY_LENGTH] characters typed; nothing is listed. */
    data object TooShort : CallsignSearchState

    /** @param truncated more matches exist than are shown */
    data class Results(val records: List<CallsignRecord>, val truncated: Boolean) : CallsignSearchState
}

object CallsignSearch {
    /**
     * The directory is for looking someone up, not for browsing, so a search
     * needs a couple of characters and returns a capped number of rows.
     */
    const val MIN_QUERY_LENGTH = 2
    const val MAX_RESULTS = 100

    private val expiryFormat = DateTimeFormatter.ofPattern("d/M/yyyy")

    /** Trims the text and removes characters that act as wildcards in the database query. */
    fun clean(query: String): String = query.trim().replace(Regex("[%_\\\\]"), "")

    /** True or false when the expiry date can be read as day/month/year, otherwise null. */
    fun isExpired(expiry: String, today: LocalDate): Boolean? =
        try {
            LocalDate.parse(expiry.trim(), expiryFormat).isBefore(today)
        } catch (e: DateTimeParseException) {
            null
        }
}
