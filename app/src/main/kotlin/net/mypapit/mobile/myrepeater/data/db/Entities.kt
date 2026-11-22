package net.mypapit.mobile.myrepeater.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class RepeaterStatus { ON_AIR, OFF_AIR, UNKNOWN }

/**
 * One row of repeaters.csv. Callsigns are not unique in the source (a site can
 * carry a 2m and a 70cm machine under one callsign), so the key is generated.
 */
@Entity(tableName = "repeaters", indices = [Index("callsign")])
data class Repeater(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val callsign: String,
    val location: String,
    val state: String,
    /** Null when the source has no usable coordinates; such rows stay off the map. */
    val latitude: Double?,
    val longitude: Double?,
    val band: String,
    /** Repeater transmit frequency: what the user listens on. */
    val downlinkMhz: Double?,
    /** Repeater receive frequency: what the user transmits on. */
    val uplinkMhz: Double?,
    val offsetMhz: Double?,
    /** CTCSS tone in Hz or a DMR colour code, as given by the source. */
    val tone: String,
    val mode: String,
    /** Short label, at most 10 characters. See [net.mypapit.mobile.myrepeater.data.ClubName]. */
    val club: String,
    val clubFullName: String,
    val linkedNetwork: String,
    val status: RepeaterStatus,
    val lastReviewed: String,
    val source: String,
    val repeaterBookId: String,
    val notes: String,
) {
    val hasCoordinates: Boolean get() = latitude != null && longitude != null
}

/**
 * One row of callsign.csv. The assignment number in the source is deliberately
 * not stored: the app only needs callsign, holder and expiry for lookups.
 */
@Entity(tableName = "callsigns", indices = [Index("callsign")])
data class CallsignRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val callsign: String,
    val holder: String,
    val expiry: String,
)
