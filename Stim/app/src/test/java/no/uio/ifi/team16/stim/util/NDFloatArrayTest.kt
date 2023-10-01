package no.uio.ifi.team16.stim.util

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class NDFloatArrayTest {

    @Test
    fun getSorrounding() {
        val arr: NullableFloatArray2D =
            arrayOf(
                arrayOf(null, 2f, 3f, 4f),
                arrayOf(null, 6f, 7f, null),
                arrayOf(null, null, 10f, 11f),
                arrayOf(null, 13f, 14f, 15f),
                arrayOf(16f, null, 18f, 19f)
            )

        var actualSlice = arr.getSorrounding(2, 2, 1)
        var expectedSlice =
            arrayOf(
                arrayOf(6f, 7f, null),
                arrayOf(null, 10f, 11f),
                arrayOf(13f, 14f, 15f),
            )
        assertArrayEquals(expectedSlice, actualSlice)

        actualSlice = arr.getSorrounding(4, 3, 1)
        expectedSlice =
            arrayOf(
                arrayOf(14f, 15f),
                arrayOf(18f, 19f)
            )
        assertArrayEquals(expectedSlice, actualSlice)

    }
}