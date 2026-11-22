package net.mypapit.mobile.myrepeater.data.aprs

import net.mypapit.mobile.myrepeater.data.location.GeoPoint
import org.json.JSONException
import org.json.JSONObject
import java.time.Instant
import java.time.format.DateTimeParseException

/**
 * A station recently heard on APRS, the amateur radio position-reporting network.
 *
 * @param name the object name when the report describes an object, otherwise the callsign
 * @param receivedAt when the report was received, or null if the source gave no usable time
 * @param comment free text sent by the station; shown as plain text only
 */
data class AprsStation(
    val callsign: String,
    val name: String,
    val point: GeoPoint,
    val receivedAt: Instant?,
    val comment: String,
)

/** Thrown when the downloaded file is not the GeoJSON we expect. */
class AprsFormatException(message: String) : Exception(message)

/**
 * Reads the stations GeoJSON: a FeatureCollection of Point features whose
 * properties carry callsign, object_name, received_at and comment.
 *
 * The file comes from the network and the comments come from strangers'
 * radios, so everything is bounded and cleaned: features with missing or
 * impossible coordinates are dropped, control characters are removed from
 * text, and lengths and counts are capped.
 */
object AprsParser {
    const val MAX_STATIONS = 2_000
    const val MAX_COMMENT_LENGTH = 200
    private const val MAX_NAME_LENGTH = 16

    fun parse(json: String): List<AprsStation> {
        val features = try {
            val root = JSONObject(json)
            if (root.optString("type") != "FeatureCollection") {
                throw AprsFormatException("Not a FeatureCollection")
            }
            root.optJSONArray("features") ?: throw AprsFormatException("No features")
        } catch (e: JSONException) {
            throw AprsFormatException("Not JSON")
        }

        val stations = ArrayList<AprsStation>()
        for (i in 0 until features.length()) {
            if (stations.size >= MAX_STATIONS) break
            val feature = features.optJSONObject(i) ?: continue
            val geometry = feature.optJSONObject("geometry") ?: continue
            if (geometry.optString("type") != "Point") continue
            val coordinates = geometry.optJSONArray("coordinates") ?: continue
            val longitude = coordinates.optDouble(0, Double.NaN)
            val latitude = coordinates.optDouble(1, Double.NaN)
            if (!isUsable(latitude, longitude)) continue

            val properties = feature.optJSONObject("properties") ?: continue
            val callsign = clean(properties.text("callsign"), MAX_NAME_LENGTH)
            if (callsign.isEmpty()) continue
            val objectName = clean(properties.text("object_name"), MAX_NAME_LENGTH)

            stations += AprsStation(
                callsign = callsign,
                name = objectName.ifEmpty { callsign },
                point = GeoPoint(latitude, longitude),
                receivedAt = parseInstant(properties.text("received_at")),
                comment = clean(properties.text("comment"), MAX_COMMENT_LENGTH),
            )
        }
        return stations
    }

    /** In range, and not a zero coordinate, which is what a station with no fix reports. */
    private fun isUsable(latitude: Double, longitude: Double): Boolean {
        if (latitude.isNaN() || longitude.isNaN()) return false
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return false
        return kotlin.math.abs(latitude) > 1e-4 && kotlin.math.abs(longitude) > 1e-4
    }

    /** A JSON null is read as empty text rather than the word "null". */
    private fun JSONObject.text(key: String): String = if (isNull(key)) "" else optString(key, "")

    private fun clean(text: String, maxLength: Int): String =
        text.filterNot { it.isISOControl() }.trim().take(maxLength)

    private fun parseInstant(text: String): Instant? =
        try {
            if (text.isBlank()) null else Instant.parse(text.trim())
        } catch (e: DateTimeParseException) {
            null
        }
}
