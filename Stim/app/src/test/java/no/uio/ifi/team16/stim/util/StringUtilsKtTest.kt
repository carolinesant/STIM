package no.uio.ifi.team16.stim.util

import org.junit.Assert.assertEquals
import org.junit.Test

class StringUtilsKtTest {

    @Test
    fun capitalizeEachWord() {

        val case1 = "dette er en setning"
        val case2 = "OSLO KOMMUNE"
        val case3 = "FREDRIKSTAD"

        assertEquals("Dette Er En Setning", case1.capitalizeEachWord())
        assertEquals("Oslo Kommune", case2.capitalizeEachWord())
        assertEquals("Fredrikstad", case3.capitalizeEachWord())
    }
}