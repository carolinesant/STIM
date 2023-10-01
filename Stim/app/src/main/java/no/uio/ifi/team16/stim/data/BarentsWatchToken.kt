package no.uio.ifi.team16.stim.data

import java.time.Instant

/**
 * API key for the BarentsWatch API
 * @property key the API key
 * @property validUntil the instant until which the key is no longer usable
 */
data class BarentsWatchToken(val key: String, val validUntil: Instant) {

    /**
     * Check if the key is valid, call this before trying to use it!
     */
    fun isValid(): Boolean {
        return Instant.now() > validUntil
    }
}