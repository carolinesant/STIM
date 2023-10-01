package no.uio.ifi.team16.stim.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LatLongTest {

    @Test
    fun testEquals() {
        val first = LatLong(60.0, 10.0)
        val second = LatLong(60.0, 10.0)

        assertEquals(first, second)
    }
}