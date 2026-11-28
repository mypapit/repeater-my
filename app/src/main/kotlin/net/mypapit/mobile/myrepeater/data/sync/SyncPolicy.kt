package net.mypapit.mobile.myrepeater.data.sync

/**
 * Decides when the app may ask the server for data, so that neither the app
 * nor an impatient thumb can hammer it. The repeater list changes rarely:
 * a copy is treated as good for a week.
 */
object SyncPolicy {
    private const val MINUTE_MS = 60_000L
    private const val HOUR_MS = 60 * MINUTE_MS

    /** The app refreshes by itself only when its copy is older than this. */
    const val AUTO_REFRESH_MS = 7 * 24 * HOUR_MS

    /** After a successful download, "Update now" stays off for this long. */
    const val MANUAL_COOLDOWN_MS = 6 * HOUR_MS

    /** After any attempt, including a failed one, the next has to wait this long. */
    const val RETRY_COOLDOWN_MS = 5 * MINUTE_MS

    /**
     * Whether the app should refresh on its own when it starts.
     *
     * @param lastSync when data was last downloaded in full, or null if never
     * @param lastAttempt when a download was last started, whether or not it worked
     */
    fun autoDue(lastSync: Long?, lastAttempt: Long?, now: Long): Boolean {
        if (lastSync == null) return true
        if (elapsed(lastSync, now) < AUTO_REFRESH_MS) return false
        return lastAttempt == null || elapsed(lastAttempt, now) >= RETRY_COOLDOWN_MS
    }

    /**
     * The moment from which the user may ask for an update by hand. A value
     * at or before [now] means it is allowed already.
     */
    fun manualAllowedAt(lastSync: Long?, lastAttempt: Long?, now: Long): Long {
        val afterSuccess = lastSync?.takeIf { it <= now }?.plus(MANUAL_COOLDOWN_MS) ?: 0L
        val afterAttempt = lastAttempt?.takeIf { it <= now }?.plus(RETRY_COOLDOWN_MS) ?: 0L
        return maxOf(afterSuccess, afterAttempt)
    }

    fun manualAllowed(lastSync: Long?, lastAttempt: Long?, now: Long): Boolean =
        manualAllowedAt(lastSync, lastAttempt, now) <= now

    /**
     * Time since [then]. A time in the future means the phone's clock was set
     * back; that counts as long ago, so a wrong clock can never lock updates out.
     */
    private fun elapsed(then: Long, now: Long): Long = if (then > now) Long.MAX_VALUE else now - then
}
