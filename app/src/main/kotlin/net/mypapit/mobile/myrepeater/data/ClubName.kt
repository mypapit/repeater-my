package net.mypapit.mobile.myrepeater.data

/**
 * Derives the short club label shown on repeater cards.
 *
 * Source order: RepeaterBook affiliate, then club/sponsor, then [UNKNOWN].
 * Labels longer than [MAX_LENGTH] are shortened, in order of preference, to
 * a bracketed acronym ("... (ARCS)"), a leading all-caps word in a mixed-case
 * name ("ASTRA Local Besut"), the initials of a multi-word name, or a plain cut.
 */
object ClubName {
    const val MAX_LENGTH = 10
    const val UNKNOWN = "UNKNOWN"

    private val placeholders = setOf("N/A", "NA", "NONE", "NIL", "UNKNOWN", "TBA", "TBC")
    private val whitespace = Regex("\\s+")
    private val bracketedAcronym = Regex("\\(([\\p{L}\\p{N}]{2,$MAX_LENGTH})\\)")
    private val wordBreak = Regex("[^\\p{L}\\p{N}']+")

    fun derive(affiliate: String?, sponsor: String?): String {
        val source = sequenceOf(affiliate, sponsor).firstOrNull { isValid(it) } ?: return UNKNOWN
        return shorten(source!!)
    }

    /** The untruncated name for the detail screen; empty when neither field is usable. */
    fun fullName(affiliate: String?, sponsor: String?): String =
        sequenceOf(sponsor, affiliate).firstOrNull { isValid(it) }?.clean().orEmpty()

    fun isValid(value: String?): Boolean {
        if (value == null || value.none { it.isLetterOrDigit() }) return false
        return value.trim().uppercase() !in placeholders
    }

    fun shorten(raw: String): String {
        val name = raw.clean()
        if (name.length <= MAX_LENGTH) return name.uppercase()

        bracketedAcronym.find(name)?.let { return it.groupValues[1].uppercase() }

        val words = name.split(wordBreak).filter { it.isNotEmpty() }
        if (words.size >= 2) {
            val first = words.first()
            val restIsMixedCase = words.drop(1).any { word -> word.any { it.isLowerCase() } }
            val firstIsAcronym = first.length in 2..MAX_LENGTH &&
                first.any { it.isLetter() } && first == first.uppercase()
            if (firstIsAcronym && restIsMixedCase) return first

            return words.joinToString("") { it.first().uppercase() }.take(MAX_LENGTH)
        }
        return name.take(MAX_LENGTH).uppercase()
    }

    private fun String.clean() = trim().replace(whitespace, " ")
}
