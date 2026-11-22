package net.mypapit.mobile.myrepeater.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncPolicyTest {
    private val minute = 60_000L
    private val hour = 60 * minute
    private val day = 24 * hour
    private val now = 1_800_000_000_000L

    @Test
    fun `with no data the app downloads straight away`() {
        assertTrue(SyncPolicy.autoDue(lastSync = null, lastAttempt = null, now = now))
        assertTrue(SyncPolicy.autoDue(lastSync = null, lastAttempt = now - minute, now = now))
        assertTrue(SyncPolicy.manualAllowed(lastSync = null, lastAttempt = null, now = now))
    }

    @Test
    fun `data younger than a week is not refreshed on start`() {
        assertFalse(SyncPolicy.autoDue(now - hour, now - hour, now))
        assertFalse(SyncPolicy.autoDue(now - 6 * day, now - 6 * day, now))
        assertTrue(SyncPolicy.autoDue(now - 7 * day, now - 7 * day, now))
    }

    @Test
    fun `an old copy is not retried on every start after a failure`() {
        assertFalse(SyncPolicy.autoDue(now - 8 * day, lastAttempt = now - minute, now = now))
        assertTrue(SyncPolicy.autoDue(now - 8 * day, lastAttempt = now - 5 * minute, now = now))
    }

    @Test
    fun `update now is off for six hours after a download`() {
        assertFalse(SyncPolicy.manualAllowed(now - minute, now - minute, now))
        assertFalse(SyncPolicy.manualAllowed(now - 6 * hour + 1, now - 6 * hour + 1, now))
        assertTrue(SyncPolicy.manualAllowed(now - 6 * hour, now - 6 * hour, now))
        assertEquals(now + 5 * hour, SyncPolicy.manualAllowedAt(now - hour, now - hour, now))
    }

    @Test
    fun `a failed attempt holds the button for five minutes only`() {
        val lastSync = now - 2 * day
        assertFalse(SyncPolicy.manualAllowed(lastSync, lastAttempt = now - minute, now = now))
        assertEquals(now + 4 * minute, SyncPolicy.manualAllowedAt(lastSync, now - minute, now))
        assertTrue(SyncPolicy.manualAllowed(lastSync, lastAttempt = now - 5 * minute, now = now))
    }

    @Test
    fun `a clock set back does not lock updates out`() {
        val future = now + 30 * day
        assertTrue(SyncPolicy.manualAllowed(future, future, now))
        assertTrue(SyncPolicy.autoDue(future, future, now))
    }
}
