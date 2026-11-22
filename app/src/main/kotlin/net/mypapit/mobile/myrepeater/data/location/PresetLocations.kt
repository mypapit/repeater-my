package net.mypapit.mobile.myrepeater.data.location

import android.content.Context
import net.mypapit.mobile.myrepeater.data.csv.CsvParser
import java.io.Reader

/** A place the user can pick instead of sharing the device's location. */
data class PresetLocation(val state: String, val name: String, val point: GeoPoint) {
    /** Stable identifier stored in settings. */
    val key: String get() = "$state|$name"
}

/**
 * The bundled list of places, grouped by state. It is derived from the location
 * names in the project's seed repeater list, topped up with towns from the
 * original app for states that had fewer than three.
 */
object PresetLocations {
    private const val ASSET = "preset_locations.csv"

    fun load(context: Context): List<PresetLocation> =
        context.assets.open(ASSET).bufferedReader().use { parse(it) }

    /** Expects a header row followed by state, name, latitude, longitude. */
    fun parse(reader: Reader): List<PresetLocation> =
        CsvParser.parse(reader).drop(1).mapNotNull { row ->
            val lat = row.getOrNull(2)?.toDoubleOrNull()
            val lon = row.getOrNull(3)?.toDoubleOrNull()
            if (row.size < 4 || lat == null || lon == null) null
            else PresetLocation(row[0].trim(), row[1].trim(), GeoPoint(lat, lon))
        }.toList()
}
