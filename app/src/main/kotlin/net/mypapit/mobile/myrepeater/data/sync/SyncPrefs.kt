package net.mypapit.mobile.myrepeater.data.sync

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.syncStore by preferencesDataStore(name = "sync")

/** Remembers when the data was last fetched and the ETag of each file. */
class SyncPrefs(private val context: Context) {

    /** Epoch millis of the last complete sync, or null before the first one. */
    val lastSync: Flow<Long?> = context.syncStore.data.map { it[LAST_SYNC] }

    /** Epoch millis of the last time a sync was started, whether or not it finished. */
    val lastAttempt: Flow<Long?> = context.syncStore.data.map { it[LAST_ATTEMPT] }

    suspend fun etag(dataset: Dataset): String? =
        context.syncStore.data.first()[etagKey(dataset)]

    suspend fun setEtag(dataset: Dataset, etag: String?) {
        context.syncStore.edit { prefs ->
            if (etag == null) prefs.remove(etagKey(dataset)) else prefs[etagKey(dataset)] = etag
        }
    }

    suspend fun setLastSync(millis: Long) {
        context.syncStore.edit { it[LAST_SYNC] = millis }
    }

    suspend fun setLastAttempt(millis: Long) {
        context.syncStore.edit { it[LAST_ATTEMPT] = millis }
    }

    private fun etagKey(dataset: Dataset) = stringPreferencesKey("etag_${dataset.name.lowercase()}")

    private companion object {
        val LAST_SYNC = longPreferencesKey("last_sync")
        val LAST_ATTEMPT = longPreferencesKey("last_attempt")
    }
}
