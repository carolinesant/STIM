package no.uio.ifi.team16.stim.data.dataLoader

import kotlinx.coroutines.runBlocking
import no.uio.ifi.team16.stim.util.Options
import org.junit.Assert.assertNotNull
import org.junit.Test
import kotlin.ranges.IntProgression.Companion.fromClosedRange

class InfectiousPressureTimeSeriesDataLoaderTest {

    private val dataLoader = InfectiousPressureTimeSeriesDataLoader()

    /**
     * Test wether the dataloader is able to load a non-null object
     */
    @Test
    fun testAbleToLoad() {
        val data = runBlocking {
            dataLoader.load(
                Options.fakeSite,
                fromClosedRange(0, 3, 1)
            )
        }
        assertNotNull(data)
    }
}