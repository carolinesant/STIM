package no.uio.ifi.team16.stim.data.dataLoader

import android.util.Log
import kotlinx.coroutines.runBlocking
import no.uio.ifi.team16.stim.util.*
import org.junit.Assert.*
import org.junit.Test
import org.locationtech.proj4j.CRSFactory
import org.locationtech.proj4j.CoordinateTransform
import org.locationtech.proj4j.CoordinateTransformFactory
import ucar.ma2.ArrayFloat
import ucar.nc2.Variable
import ucar.nc2.dataset.NetcdfDataset
import kotlin.math.roundToInt
import kotlin.ranges.IntProgression.Companion.fromClosedRange

class InfectiousPressureDataLoaderTest {
    companion object {
        private const val TAG = "InfectiousPressureDataLoaderTest"
        private const val TEST_URL =
            "http://thredds.nodc.no:8080/thredds/fileServer/smittepress_new2018/agg_OPR_2022_9.nc"
    }

    private val dataLoader = InfectiousPressureDataLoader()

    /**
     * Test wether the dataloader is able to load a non-null object
     */
    @Test
    fun testAbleToLoad() {
        val data = runBlocking {
            dataLoader.load(
                fromClosedRange(200, 210, 4),
                fromClosedRange(200, 212, 7)
            )
        }
        assertNotNull(data)
    }

    /**
     * Test wether the Infectiouspressure-object that is made from the parsing has correctly
     * parsed out the concentration. It might be oriented the wrong way, or get the wrong indexes.
     *
     * WARNING: this test is time-dependent; the dataset might be deleted a few years from now!
     * As of now we are not able to hard-code the response here, as it is streamed and sliced
     * by the THREDDS server.
     */
    @Test
    fun testCorrectParse() {
        val data = runBlocking {
            dataLoader.load(
                TEST_URL,
                fromClosedRange(200, 210, 4),
                fromClosedRange(200, 212, 7)
            )
        }

        val expectedConcentration =
            floatArrayOf(
                5.066191E-6f, 0.0f,
                1.1897087E-5f, 1.3255544E-5f,
                1.9028561E-5f, 8.851041E-6f
            )
        Log.d(TAG, data?.concentration?.prettyPrint() ?: "")
        assertArrayEquals(
            expectedConcentration,
            data?.concentration?.reduce { acc, floats -> acc.plus(floats) },
            0.000000000001f
        )
    }

    /**
     * Test if the projection that is parsed out from the data correctly maps between latlongs and
     * eta/xi coordinates(grid coordinates)
     *
     * WARNING: this test is time-dependent; the dataset might be deleted a few years from now!
     * As of now we are not able to hard-code the response here, as it is streamed and sliced
     * by the THREDDS server.
     */
    @Test
    fun testProjection() {
        testProjectionOf(
            fromClosedRange(200, 210, 4),
            fromClosedRange(200, 212, 7)
        )
        testProjectionOf(
            fromClosedRange(100, 341, 13),
            fromClosedRange(500, 872, 17)
        )
        testProjectionOf(
            fromClosedRange(340, 891, 3),
            fromClosedRange(5, 498, 23)
        )
    }

    private fun testProjectionOf(
        xRange: IntProgression,
        yRange: IntProgression
    ) {
        var projection: CoordinateTransform
        var latitudes: FloatArray2D
        var longitudes: FloatArray2D

        runBlocking {
            NetcdfDataset.openDataset(TEST_URL).let { ncfile ->
                val range2 = "${xRange.reformatFLS()},${yRange.reformatFLS()}"

                val gridMapping: Variable = ncfile.findVariable("grid_mapping")
                    ?: throw NullPointerException("Failed to read variable <gridMapping> from infectiousPressure")

                val crsFactory = CRSFactory()
                val stereoCRT = crsFactory.createFromParameters(
                    null,
                    gridMapping.findAttribute("proj4string")?.stringValue
                        ?: throw NullPointerException("Failed to read attribute <proj4string> from <gridMapping> from infectiousPressure")
                )
                val latLngCRT = stereoCRT.createGeographic()
                val ctFactory = CoordinateTransformFactory()

                projection = ctFactory.createTransform(latLngCRT, stereoCRT)

                latitudes = (ncfile.findVariable("lat").read(range2) as ArrayFloat).to2DFloatArray()
                longitudes =
                    (ncfile.findVariable("lon").read(range2) as ArrayFloat).to2DFloatArray()
            }
        }

        /*
        with the specified ranges, the projection should map the latlongs we got to the coordinate
        ranges we used to read
        */

        //make a grid of the eta/xi coordinates corresponding to the indexes
        val expectedYXGrid = xRange.map { x ->
            yRange.map { y ->
                Pair(y * 800, x * 800)
            }
        }
        Log.d(TAG, expectedYXGrid.toString())

        //the latitudes and longitudes are organized as XY,
        val latLongs = latitudes.zip(longitudes).map { (latitudeRow, longitudeRow) ->
            latitudeRow.zip(longitudeRow).map { (lat, lng) ->
                LatLong(lat.toDouble(), lng.toDouble())
            }
        }
        Log.d(TAG, latLongs.toString())
        Log.d(TAG, latitudes.prettyPrint())

        val actualYXGrid = latLongs.map { latLongRow ->
            latLongRow.map { latLong ->
                projection.project(latLong).let { (yFloat, xFloat) ->
                    Pair((yFloat).roundToInt(), (xFloat).roundToInt())
                }
            }
        }
        Log.d(TAG, actualYXGrid.toString())

        //assert for each element in grid
        expectedYXGrid.zip(actualYXGrid).map { (expectedYXRow, actualYXRow) ->
            expectedYXRow.zip(actualYXRow).map { (expectedYX, actualYX) ->
                assertEquals(expectedYX, actualYX)
            }
        }
    }
}