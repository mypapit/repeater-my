package net.mypapit.mobile.myrepeater.ui.map

import net.mypapit.mobile.myrepeater.data.db.Repeater
import net.mypapit.mobile.myrepeater.data.db.RepeaterStatus
import net.mypapit.mobile.myrepeater.data.location.GeoPoint

/** One pin on the map. Repeaters that share a site are grouped under a single pin. */
data class MapSite(val point: GeoPoint, val repeaters: List<Repeater>) {
    /** Colour key for the pin: the band if all machines share it, otherwise "mixed". */
    val band: String get() = repeaters.map { it.band }.distinct().singleOrNull() ?: BAND_MIXED

    /** A site is drawn dimmed only when nothing on it is known to be on air. */
    val onAir: Boolean get() = repeaters.any { it.status == RepeaterStatus.ON_AIR }

    /** Stable identifier used to find the site again when its pin is tapped. */
    val id: String get() = "${point.latitude},${point.longitude}"

    companion object {
        const val BAND_MIXED = "mixed"
    }
}

object MapSites {
    /** Malaysia, peninsula to Sabah: south-west and north-east corners. */
    val MALAYSIA_SOUTH_WEST = GeoPoint(0.8, 99.5)
    val MALAYSIA_NORTH_EAST = GeoPoint(7.5, 119.5)

    /** Groups repeaters by exact coordinates and leaves out those without any. */
    fun from(repeaters: List<Repeater>): List<MapSite> =
        repeaters.filter { it.hasCoordinates }
            .groupBy { GeoPoint(it.latitude!!, it.longitude!!) }
            .map { (point, group) -> MapSite(point, group.sortedBy { it.downlinkMhz }) }
}
