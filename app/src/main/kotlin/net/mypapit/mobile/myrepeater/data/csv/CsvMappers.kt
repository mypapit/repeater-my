package net.mypapit.mobile.myrepeater.data.csv

import net.mypapit.mobile.myrepeater.data.ClubName
import net.mypapit.mobile.myrepeater.data.db.CallsignRecord
import net.mypapit.mobile.myrepeater.data.db.Repeater
import net.mypapit.mobile.myrepeater.data.db.RepeaterStatus

/** Turns the rows of repeaters.csv into [Repeater]s. The first row must be the header. */
object RepeaterCsvMapper {

    fun map(rows: Sequence<List<String>>): List<Repeater> {
        val iterator = rows.iterator()
        if (!iterator.hasNext()) throw CsvFormatException("Empty repeater file")
        val header = CsvHeader(iterator.next())

        val callsign = header.require("callsign")
        val downlink = header.require("downlink")
        val location = header.find("location")
        val state = header.find("state")
        val latitude = header.find("latitude")
        val longitude = header.find("longitude")
        val band = header.find("band")
        val uplink = header.find("uplink")
        val offset = header.find("offset")
        val tone = header.find("tone")
        val mode = header.find("mode")
        val sponsor = header.find("club")
        val affiliate = header.find("affiliate")
        val linked = header.find("linked")
        val status = header.find("status")
        val reviewed = header.find("last reviewed")
        val source = header.find("source")
        val repeaterBookId = header.find("repeaterbook id")
        val notes = header.find("notes")

        val result = ArrayList<Repeater>()
        for (row in iterator) {
            val call = row.cell(callsign).uppercase()
            if (call.isEmpty()) continue

            val lat = row.cell(latitude).toDoubleOrNull()?.takeIf { it in -90.0..90.0 }
            val lon = row.cell(longitude).toDoubleOrNull()?.takeIf { it in -180.0..180.0 }
            val hasPosition = lat != null && lon != null && !(lat == 0.0 && lon == 0.0)

            result += Repeater(
                callsign = call,
                location = row.cell(location),
                state = row.cell(state),
                latitude = lat.takeIf { hasPosition },
                longitude = lon.takeIf { hasPosition },
                band = row.cell(band),
                downlinkMhz = row.cell(downlink).toDoubleOrNull(),
                uplinkMhz = row.cell(uplink).toDoubleOrNull(),
                offsetMhz = row.cell(offset).toDoubleOrNull(),
                tone = row.cell(tone),
                mode = row.cell(mode),
                club = ClubName.derive(row.cell(affiliate), row.cell(sponsor)),
                clubFullName = ClubName.fullName(row.cell(affiliate), row.cell(sponsor)),
                linkedNetwork = row.cell(linked),
                status = parseStatus(row.cell(status)),
                lastReviewed = row.cell(reviewed),
                source = row.cell(source),
                repeaterBookId = row.cell(repeaterBookId),
                notes = row.cell(notes),
            )
        }
        return result
    }

    internal fun parseStatus(raw: String): RepeaterStatus {
        val text = raw.lowercase()
        return when {
            text.startsWith("on-air") || text.startsWith("on air") -> RepeaterStatus.ON_AIR
            text.startsWith("off-air") || text.startsWith("off air") -> RepeaterStatus.OFF_AIR
            else -> RepeaterStatus.UNKNOWN
        }
    }
}

/** Turns the rows of callsign.csv into [CallsignRecord]s, dropping the assignment number. */
object CallsignCsvMapper {

    fun map(rows: Sequence<List<String>>): List<CallsignRecord> {
        val iterator = rows.iterator()
        if (!iterator.hasNext()) throw CsvFormatException("Empty callsign file")
        val header = CsvHeader(iterator.next())

        val callsign = header.require("call sign")
        val holder = header.require("assignment holder")
        val expiry = header.find("expiry")

        val result = ArrayList<CallsignRecord>()
        for (row in iterator) {
            val call = row.cell(callsign).uppercase()
            if (call.isEmpty()) continue
            result += CallsignRecord(
                callsign = call,
                holder = row.cell(holder),
                expiry = row.cell(expiry),
            )
        }
        return result
    }
}
