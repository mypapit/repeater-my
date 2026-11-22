package net.mypapit.mobile.myrepeater.data.sync

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import net.mypapit.mobile.myrepeater.data.csv.CallsignCsvMapper
import net.mypapit.mobile.myrepeater.data.csv.CsvFormatException
import net.mypapit.mobile.myrepeater.data.csv.CsvParser
import net.mypapit.mobile.myrepeater.data.csv.RepeaterCsvMapper
import net.mypapit.mobile.myrepeater.data.db.AppDatabase
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

enum class Dataset(val url: String) {
    REPEATERS("https://api.mypapit.net/repeater/repeaters.csv"),
    CALLSIGNS("https://api.mypapit.net/repeater/callsign.csv"),
}

enum class SyncFailure { NETWORK, BAD_DATA }

sealed interface SyncState {
    data object Idle : SyncState

    /** [fraction] is null when the server did not say how large the file is. */
    data class Downloading(val dataset: Dataset, val fraction: Float?) : SyncState
    data class Processing(val dataset: Dataset) : SyncState
    data object Done : SyncState
    data class Failed(val reason: SyncFailure) : SyncState
}

/**
 * Downloads the two CSV files and replaces the local tables with their content.
 *
 * Requests carry nothing about the user or device beyond a generic client name.
 * Each table is swapped inside one transaction after its file has been fully
 * downloaded and parsed, so a failed or interrupted sync leaves the previous
 * data in place.
 */
class SyncRepository(
    private val http: OkHttpClient,
    private val db: AppDatabase,
    private val prefs: SyncPrefs,
    private val cacheDir: File,
) {

    fun sync(): Flow<SyncState> = flow {
        try {
            for (dataset in Dataset.entries) syncDataset(dataset)
            prefs.setLastSync(System.currentTimeMillis())
            emit(SyncState.Done)
        } catch (e: CsvFormatException) {
            emit(SyncState.Failed(SyncFailure.BAD_DATA))
        } catch (e: IOException) {
            emit(SyncState.Failed(SyncFailure.NETWORK))
        }
    }.conflate().flowOn(Dispatchers.IO)

    private suspend fun FlowCollector<SyncState>.syncDataset(dataset: Dataset) {
        emit(SyncState.Downloading(dataset, null))

        // Only ask "has it changed?" when there is local data to fall back on.
        val knownEtag = if (rowCount(dataset) > 0) prefs.etag(dataset) else null
        val request = Request.Builder()
            .url(dataset.url)
            .header("User-Agent", USER_AGENT)
            .apply { if (knownEtag != null) header("If-None-Match", knownEtag) }
            .build()

        http.newCall(request).execute().use { response ->
            if (response.code == 304) return
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")

            val body = response.body ?: throw IOException("Empty response")
            val total = body.contentLength().takeIf { it > 0 }
            val file = File.createTempFile(dataset.name.lowercase(), ".csv", cacheDir)
            try {
                body.byteStream().use { input ->
                    file.outputStream().use { output ->
                        val buffer = ByteArray(16 * 1024)
                        var done = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            done += read
                            emit(SyncState.Downloading(dataset, total?.let { (done.toFloat() / it).coerceAtMost(1f) }))
                        }
                    }
                }
                emit(SyncState.Processing(dataset))
                store(dataset, file)
                prefs.setEtag(dataset, response.header("ETag"))
            } finally {
                file.delete()
            }
        }
    }

    private suspend fun store(dataset: Dataset, file: File) {
        when (dataset) {
            Dataset.REPEATERS -> {
                val rows = file.bufferedReader().use { RepeaterCsvMapper.map(CsvParser.parse(it)) }
                if (rows.isEmpty()) throw CsvFormatException("No repeaters in file")
                db.withTransaction {
                    db.repeaterDao().deleteAll()
                    rows.chunked(BATCH).forEach { db.repeaterDao().insertAll(it) }
                }
            }
            Dataset.CALLSIGNS -> {
                val rows = file.bufferedReader().use { CallsignCsvMapper.map(CsvParser.parse(it)) }
                if (rows.isEmpty()) throw CsvFormatException("No callsigns in file")
                db.withTransaction {
                    db.callsignDao().deleteAll()
                    rows.chunked(BATCH).forEach { db.callsignDao().insertAll(it) }
                }
            }
        }
    }

    private suspend fun rowCount(dataset: Dataset): Int = when (dataset) {
        Dataset.REPEATERS -> db.repeaterDao().count()
        Dataset.CALLSIGNS -> db.callsignDao().count()
    }

    private companion object {
        const val BATCH = 500
        const val USER_AGENT = "RepeaterMY-Android"
    }
}
