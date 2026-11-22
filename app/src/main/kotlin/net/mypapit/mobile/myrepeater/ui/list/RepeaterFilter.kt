package net.mypapit.mobile.myrepeater.ui.list

import net.mypapit.mobile.myrepeater.data.db.Repeater
import net.mypapit.mobile.myrepeater.data.db.RepeaterStatus

/**
 * What the user has typed and ticked above the repeater list.
 *
 * @param query free text; every word must appear somewhere in the repeater's
 *   callsign, place, state, club, network, mode or frequency
 * @param bands bands to keep; empty means all
 * @param state a single state to keep; null means all
 */
data class RepeaterFilter(
    val query: String = "",
    val bands: Set<String> = emptySet(),
    val onAirOnly: Boolean = false,
    val state: String? = null,
) {
    val isActive: Boolean get() = query.isNotBlank() || bands.isNotEmpty() || onAirOnly || state != null

    fun matches(repeater: Repeater): Boolean {
        if (bands.isNotEmpty() && repeater.band !in bands) return false
        if (onAirOnly && repeater.status != RepeaterStatus.ON_AIR) return false
        if (state != null && state !in statesOf(repeater)) return false

        val words = query.trim().split(WHITESPACE).filter { it.isNotEmpty() }
        if (words.isEmpty()) return true
        val haystack = listOfNotNull(
            repeater.callsign,
            repeater.location,
            repeater.state,
            repeater.club,
            repeater.clubFullName,
            repeater.linkedNetwork,
            repeater.mode,
            repeater.downlinkMhz?.let { RepeaterFormat.frequency(it) },
        ).joinToString(" ")
        return words.all { haystack.contains(it, ignoreCase = true) }
    }

    companion object {
        private val WHITESPACE = Regex("\\s+")

        /** A repeater on a border ridge can list two states, written "Selangor/Pahang". */
        fun statesOf(repeater: Repeater): List<String> =
            repeater.state.split('/').map { it.trim() }.filter { it.isNotEmpty() }

        fun statesIn(repeaters: List<Repeater>): List<String> =
            repeaters.flatMap { statesOf(it) }.distinct().sorted()

        fun bandsIn(repeaters: List<Repeater>): List<String> =
            repeaters.map { it.band }.filter { it.isNotBlank() }.distinct().sorted()
    }
}
