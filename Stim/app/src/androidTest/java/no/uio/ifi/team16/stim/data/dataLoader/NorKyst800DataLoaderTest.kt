package no.uio.ifi.team16.stim.data.dataLoader

import kotlinx.coroutines.runBlocking
import no.uio.ifi.team16.stim.util.NullableFloatArray4D
import org.junit.Assert.assertNotNull
import org.junit.Test
import kotlin.ranges.IntProgression.Companion.fromClosedRange

fun NullableFloatArray4D.scaleAndOffset(factor: Float, offset: Float): NullableFloatArray4D =
    map { arr3d ->
        arr3d.map { arr2d ->
            arr2d.map { arr1d ->
                arr1d.map { element ->
                    if (element == null) null else element * factor + offset
                }.toTypedArray()
            }.toTypedArray()
        }.toTypedArray()
    }.toTypedArray()

class NorKyst800DataLoaderTest {

    private val dataLoader = NorKyst800DataLoader()

    /**
     * Test wether the dataloader is able to load a non-null object
     */
    @Test
    fun testAbleToLoad() {
        val data = runBlocking {
            dataLoader.load(
                fromClosedRange(200, 210, 4),
                fromClosedRange(200, 212, 7),
                fromClosedRange(0, 10, 2)
            )
        }
        assertNotNull(data)
    }
}