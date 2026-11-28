package net.mypapit.mobile.myrepeater.data.aprs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

/**
 * @param stations what to draw; empty when nothing could be loaded
 * @param fetchedAt epoch millis of the download the stations came from, if any
 * @param failed the latest download attempt failed, so [stations] are older than wanted (or absent)
 */
data class AprsSnapshot(val stations: List<AprsStation>, val fetchedAt: Long?, val failed: Boolean)

/**
 * Downloads the APRS stations file and keeps it on disk.
 *
 * A saved copy younger than [MAX_AGE_MS] is used without touching the network,
 * so the server is asked at most once every 30 minutes however often the map
 * is opened. If a download fails, the last saved copy is used instead.
 * Requests carry nothing about the user or device beyond a generic client name.
 */
class AprsRepository(
    private val http: OkHttpClient,
    cacheDir: File,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val file = File(cacheDir, "aprs_stations.json")

    suspend fun load(): AprsSnapshot = withContext(Dispatchers.IO) {
        val saved = readSaved()
        if (saved != null && isFresh(file.lastModified(), now())) {
            return@withContext AprsSnapshot(saved, file.lastModified(), failed = false)
        }
        try {
            val stations = download()
            AprsSnapshot(stations, file.lastModified(), failed = false)
        } catch (e: IOException) {
            AprsSnapshot(saved.orEmpty(), file.lastModified().takeIf { saved != null }, failed = true)
        } catch (e: AprsFormatException) {
            AprsSnapshot(saved.orEmpty(), file.lastModified().takeIf { saved != null }, failed = true)
        }
    }

    /** Removes the saved copy, for when the user switches the feature off. */
    suspend fun clear() = withContext(Dispatchers.IO) {
        file.delete()
        Unit
    }

    private fun readSaved(): List<AprsStation>? {
        if (!file.isFile) return null
        return try {
            AprsParser.parse(file.readText())
        } catch (e: AprsFormatException) {
            null
        } catch (e: IOException) {
            null
        }
    }

    private fun download(): List<AprsStation> {
        val request = Request.Builder().url(URL).header("User-Agent", USER_AGENT).build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body ?: throw IOException("Empty response")
            if (body.contentLength() > MAX_BYTES) throw IOException("Response too large")

            // Read with a hard ceiling in case the server does not state a length.
            val collected = ByteArrayOutputStream()
            body.byteStream().use { input ->
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    collected.write(buffer, 0, read)
                    if (collected.size() > MAX_BYTES) throw IOException("Response too large")
                }
            }
            val text = collected.toString(Charsets.UTF_8.name())

            // Parse before saving, so a bad download never replaces a good copy.
            val stations = AprsParser.parse(text)
            val temp = File(file.parentFile, file.name + ".tmp")
            temp.writeText(text)
            if (!temp.renameTo(file)) {
                file.delete()
                if (!temp.renameTo(file)) throw IOException("Could not save stations")
            }
            file.setLastModified(now())
            return stations
        }
    }

    companion object {
        const val URL = "https://api.mypapit.net/repeater/stations.json"
        const val MAX_AGE_MS = 30 * 60 * 1000L
        private const val MAX_BYTES = 2 * 1024 * 1024
        private const val USER_AGENT = "RepeaterMY-Android"

        /** True while a copy saved at [savedAt] is still within the 30-minute window. */
        fun isFresh(savedAt: Long, now: Long): Boolean = savedAt > 0 && now - savedAt in 0 until MAX_AGE_MS
    }
}
