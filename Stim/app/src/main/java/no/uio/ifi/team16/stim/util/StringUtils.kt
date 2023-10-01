package no.uio.ifi.team16.stim.util

/**
 * Endrer "dette er en setning" til "Dette Er En Setning"
 */
fun String.capitalizeEachWord(): String {
    return this.split(" ").joinToString(" ") { it.lowercase().replaceFirstChar(Char::titlecase) }
}