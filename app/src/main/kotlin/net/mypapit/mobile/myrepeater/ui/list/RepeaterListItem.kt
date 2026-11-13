package net.mypapit.mobile.myrepeater.ui.list

import net.mypapit.mobile.myrepeater.data.db.Repeater
import net.mypapit.mobile.myrepeater.data.location.Geo
import net.mypapit.mobile.myrepeater.data.location.GeoPoint

/** A repeater plus where it lies from the user. Both are null without a position or coordinates. */
data class RepeaterListItem(
    val repeater: Repeater,
    val distanceKm: Double?,
    val bearingDeg: Double?,
)

object RepeaterSorter {

    /**
     * With a position: nearest first, and repeaters without coordinates at the
     * end in callsign order. Without one: the given order, no distances.
     */
    fun arrange(repeaters: List<Repeater>, from: GeoPoint?): List<RepeaterListItem> {
        if (from == null) return repeaters.map { RepeaterListItem(it, null, null) }

        return repeaters.map { repeater ->
            val lat = repeater.latitude
            val lon = repeater.longitude
            if (lat == null || lon == null) {
                RepeaterListItem(repeater, null, null)
            } else {
                val to = GeoPoint(lat, lon)
                RepeaterListItem(repeater, Geo.distanceKm(from, to), Geo.bearingDeg(from, to))
            }
        }.sortedWith(compareBy(nullsLast()) { it.distanceKm })
    }
}
