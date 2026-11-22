package net.mypapit.mobile.myrepeater.ui.detail

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import net.mypapit.mobile.myrepeater.data.db.Repeater
import net.mypapit.mobile.myrepeater.ui.list.RepeaterFormat

/**
 * The draft e-mail a user can send to report wrong repeater details.
 *
 * The app never sends anything itself: it only opens the phone's e-mail app
 * with the address, subject and body filled in, for the user to edit and send.
 * The text is in English whatever the app language, because the maintainer
 * reads it.
 */
object CorrectionEmail {
    const val ADDRESS = "mypapit+repeater@gmail.com"
    const val INTRO = "State which repeater details that needs correction"

    private const val NOT_LISTED = "-"

    fun subject(repeater: Repeater): String = "${repeater.callsign} - Repeater Info Corrections"

    fun body(repeater: Repeater): String {
        val frequency = repeater.downlinkMhz?.let { "${RepeaterFormat.frequency(it)} MHz" }
        // A plain hyphen, not the typographic minus used on screen, so it survives any mail app.
        val offset = repeater.offsetMhz?.let { "${RepeaterFormat.offset(it).replace('−', '-')} MHz" }
        val location = listOf(repeater.location, repeater.state).filter { it.isNotBlank() }.joinToString(", ")

        return buildString {
            appendLine(INTRO)
            appendLine()
            appendLine("Repeater callsign: ${repeater.callsign}")
            appendLine("Freq: ${frequency ?: NOT_LISTED}")
            appendLine("Offset: ${offset ?: NOT_LISTED}")
            appendLine("Tone: ${repeater.tone.ifBlank { NOT_LISTED }}")
            appendLine("Club name: ${repeater.clubFullName.ifBlank { NOT_LISTED }}")
            appendLine("Location: ${location.ifBlank { NOT_LISTED }}")
            append("Club: ${repeater.club.ifBlank { NOT_LISTED }}")
        }
    }

    /**
     * Opens an e-mail app on the draft.
     *
     * @return false when the phone has no e-mail app to open
     */
    fun open(context: Context, repeater: Repeater): Boolean {
        val subject = subject(repeater)
        val body = body(repeater)
        // Both the mailto: query and the extras are set: mail apps differ in which they read.
        val uri = Uri.parse("mailto:$ADDRESS?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}")
        val intent = Intent(Intent.ACTION_SENDTO, uri)
            .putExtra(Intent.EXTRA_EMAIL, arrayOf(ADDRESS))
            .putExtra(Intent.EXTRA_SUBJECT, subject)
            .putExtra(Intent.EXTRA_TEXT, body)
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}
