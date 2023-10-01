package no.uio.ifi.team16.stim.data.dataLoader

import android.util.Log
import kotlinx.coroutines.runBlocking
import no.uio.ifi.team16.stim.data.NorKyst800
import no.uio.ifi.team16.stim.data.dataLoader.parser.NorKyst800Parser
import no.uio.ifi.team16.stim.util.Options
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.locationtech.proj4j.CRSFactory
import org.locationtech.proj4j.CoordinateTransform
import org.locationtech.proj4j.CoordinateTransformFactory

class NorKyst800AtSiteDataLoaderTest {
    companion object {
        private const val TAG = "NorKyst800AtSiteDataLoaderTest"
    }

    private val dataLoader = NorKyst800AtSiteDataLoader()

    private val fakeResponse = """
        Dataset {
            Float64 depth[depth = 3];
            Grid {
             ARRAY:
                Float64 lat[Y = 2][X = 3];
             MAPS:
                Float64 Y[Y = 2];
                Float64 X[X = 3];
            } lat;
            Grid {
             ARRAY:
                Float64 lon[Y = 2][X = 3];
             MAPS:
                Float64 Y[Y = 2];
                Float64 X[X = 3];
            } lon;
            Grid {
             ARRAY:
                Int16 salinity[time = 3][depth = 2][Y = 2][X = 3];
             MAPS:
                Float64 time[time = 3];
                Float64 depth[depth = 2];
                Float64 Y[Y = 2];
                Float64 X[X = 3];
            } salinity;
            Grid {
             ARRAY:
                Int16 temperature[time = 3][depth = 2][Y = 2][X = 3];
             MAPS:
                Float64 time[time = 3];
                Float64 depth[depth = 2];
                Float64 Y[Y = 2];
                Float64 X[X = 3];
            } temperature;
            Float64 time[time = 2];
            Grid {
             ARRAY:
                Int16 u[time = 3][depth = 2][Y = 2][X = 3];
             MAPS:
                Float64 time[time = 3];
                Float64 depth[depth = 2];
                Float64 Y[Y = 2];
                Float64 X[X = 3];
            } u;
            Grid {
             ARRAY:
                Int16 v[time = 3][depth = 2][Y = 2][X = 3];
             MAPS:
                Float64 time[time = 3];
                Float64 depth[depth = 2];
                Float64 Y[Y = 2];
                Float64 X[X = 3];
            } v;
            Grid {
             ARRAY:
                Float32 w[time = 3][depth = 2][Y = 2][X = 3];
             MAPS:
                Float64 time[time = 3];
                Float64 depth[depth = 2];
                Float64 Y[Y = 2];
                Float64 X[X = 3];
            } w;
        } fou-hi/norkyst800m-1h/NorKyst-800m_ZDEPTHS_his.fc.2022042500.nc;
        ---------------------------------------------
        depth[3]
        0.0, 100.0, 2000.0

        lat.lat[2][3]
        [0], 55.908369929915125, 62.49262602518113, 68.2975145963228
        [1], 58.01408691998771, 65.26772230447494, 72.06046238531032

        lat.Y[2]
        0.0, 560000.0

        lat.X[3]
        0.0, 880000.0, 1760000.0


        lon.lon[2][3]
        [0], 9.194590109686652, 17.645135616058933, 31.23069355646127
        [1], 0.9749537179037423, 7.889075121859453, 20.506316233016715

        lon.Y[2]
        0.0, 560000.0

        lon.X[3]
        0.0, 880000.0, 1760000.0


        salinity.salinity[3][2][2][3]
        [0][0][0], -32767, -32767, -32767
        [0][0][1], 5085, 4591, 4695
        [0][1][0], -32767, -32767, -32767
        [0][1][1], -32767, 4716, 4727
        [1][0][0], -32767, -32767, -32767
        [1][0][1], 5081, 4589, 4698
        [1][1][0], -32767, -32767, -32767
        [1][1][1], -32767, 4721, 4737
        [2][0][0], -32767, -32767, -32767
        [2][0][1], 5086, 4591, 4700
        [2][1][0], -32767, -32767, -32767
        [2][1][1], -32767, 4724, 4733

        salinity.time[3]
        1.6509312E9, 1.6509564E9, 1.6509816E9

        salinity.depth[2]
        0.0, 200.0

        salinity.Y[2]
        0.0, 560000.0

        salinity.X[3]
        0.0, 880000.0, 1760000.0


        temperature.temperature[3][2][2][3]
        [0][0][0], -32767, -32767, -32767
        [0][0][1], 885, 685, 474
        [0][1][0], -32767, -32767, -32767
        [0][1][1], -32767, 679, 456
        [1][0][0], -32767, -32767, -32767
        [1][0][1], 861, 682, 470
        [1][1][0], -32767, -32767, -32767
        [1][1][1], -32767, 680, 464
        [2][0][0], -32767, -32767, -32767
        [2][0][1], 858, 685, 468
        [2][1][0], -32767, -32767, -32767
        [2][1][1], -32767, 679, 456

        temperature.time[3]
        1.6509312E9, 1.6509564E9, 1.6509816E9

        temperature.depth[2]
        0.0, 200.0

        temperature.Y[2]
        0.0, 560000.0

        temperature.X[3]
        0.0, 880000.0, 1760000.0


        time[2]
        1.6509312E9, 1.6509636E9

        u.u[3][2][2][3]
        [0][0][0], -32767, -32767, -32767
        [0][0][1], -32767, -96, -61
        [0][1][0], -32767, -32767, -32767
        [0][1][1], -32767, 24, -77
        [1][0][0], -32767, -32767, -32767
        [1][0][1], -32767, -37, -206
        [1][1][0], -32767, -32767, -32767
        [1][1][1], -32767, 12, 4
        [2][0][0], -32767, -32767, -32767
        [2][0][1], -32767, -57, -229
        [2][1][0], -32767, -32767, -32767
        [2][1][1], -32767, -11, -179

        u.time[3]
        1.6509312E9, 1.6509564E9, 1.6509816E9

        u.depth[2]
        0.0, 200.0

        u.Y[2]
        0.0, 560000.0

        u.X[3]
        0.0, 880000.0, 1760000.0


        v.v[3][2][2][3]
        [0][0][0], -32767, -32767, -32767
        [0][0][1], 19, -25, -134
        [0][1][0], -32767, -32767, -32767
        [0][1][1], -32767, 14, -163
        [1][0][0], -32767, -32767, -32767
        [1][0][1], 6, -4, -129
        [1][1][0], -32767, -32767, -32767
        [1][1][1], -32767, -25, -4
        [2][0][0], -32767, -32767, -32767
        [2][0][1], 129, 25, -38
        [2][1][0], -32767, -32767, -32767
        [2][1][1], -32767, 37, -14

        v.time[3]
        1.6509312E9, 1.6509564E9, 1.6509816E9

        v.depth[2]
        0.0, 200.0

        v.Y[2]
        0.0, 560000.0

        v.X[3]
        0.0, 880000.0, 1760000.0


        w.w[3][2][2][3]
        [0][0][0], 1.0E37, 1.0E37, 1.0E37
        [0][0][1], -6.440923E-5, -2.6763373E-5, -6.496307E-5
        [0][1][0], 1.0E37, 1.0E37, 1.0E37
        [0][1][1], 1.0E37, -5.989377E-4, 4.8309087E-4
        [1][0][0], 1.0E37, 1.0E37, 1.0E37
        [1][0][1], 6.8041554E-5, -1.4682717E-5, 5.5459644E-5
        [1][1][0], 1.0E37, 1.0E37, 1.0E37
        [1][1][1], 1.0E37, 1.4523527E-4, 1.492668E-4
        [2][0][0], 1.0E37, 1.0E37, 1.0E37
        [2][0][1], -4.5285957E-5, -2.0274536E-5, -1.02906655E-4
        [2][1][0], 1.0E37, 1.0E37, 1.0E37
        [2][1][1], 1.0E37, 5.2956515E-4, -0.0013055078

        w.time[3]
        1.6509312E9, 1.6509564E9, 1.6509816E9

        w.depth[2]
        0.0, 200.0

        w.Y[2]
        0.0, 560000.0

        w.X[3]
        0.0, 880000.0, 1760000.0
    """.trimIndent()

    /**
     * Test wether the dataloader is able to load a non-null object
     */
    @Test
    fun testAbleToLoad() {
        val data = runBlocking {
            dataLoader.load(
                Options.fakeSite
            )
        }
        assertNotNull(data)
    }

    /**
     * test if load method works properly
     *
     * repeat the process of parsing the loaded data, and check if done properly
     */
    @Test
    fun testParse() {
        val projection: CoordinateTransform =
            CRSFactory().createFromParameters(null, Options.defaultProj4String).let { stereoCRT ->
                val latLngCRT = stereoCRT.createGeographic()
                val ctFactory = CoordinateTransformFactory()
                ctFactory.createTransform(latLngCRT, stereoCRT)
            }

        val actualNorKyst =
            runBlocking {
                //////////////////////
                // DAS / ATTRIBUTES //
                //////////////////////
                val salinityFSO = Triple(-32767, 0.001f, 30f)
                val temperatureFSO = Triple(-32767, 0.01f, 0f)
                val uFSO = Triple(-32767, 0.001f, 0f)
                val vFSO = Triple(-32767, 0.001f, 0f)
                val wFSO = Triple(1.0E37f, 1f, 0f)

                ////////////////////
                // TIME AND DEPTH //
                ////////////////////

                val (time, depth) = NorKyst800Parser.parseTimeAndDepth(
                    fakeResponse
                ) ?: run {
                    assert(false)
                    return@runBlocking null
                }

                //////////////////
                // GET ALL DATA //
                //////////////////

                //////////////
                // SALINITY //
                //////////////
                val salinity = NorKyst800Parser.parseNullable4DArrayFrom(
                    fakeResponse,
                    salinityFSO,
                    "salinity"
                ) ?: run {
                    assert(false)
                    return@runBlocking null
                }

                /////////////////
                // TEMPERATURE //
                /////////////////
                val temperature = NorKyst800Parser.parseNullable4DArrayFrom(
                    fakeResponse,
                    temperatureFSO,
                    "temperature"
                ) ?: run {
                    assert(false)
                    return@runBlocking null
                }

                //////////////
                // VELOCITY //
                //////////////
                val velocity = NorKyst800Parser.parseVelocity(
                    fakeResponse,
                    uFSO,
                    vFSO,
                    wFSO
                ) ?: run {
                    assert(false)
                    return@runBlocking null
                }

                NorKyst800(
                    depth,
                    salinity,
                    temperature,
                    time,
                    velocity,
                    projection
                )
            }
        ///////////
        // DONE! //
        ///////////

        val expectedNorKyst =
            NorKyst800(
                //depth
                floatArrayOf(0.0f, 100.0f, 2000.0f),
                //salinity
                arrayOf<Array<Array<Array<Float?>>>>(
                    arrayOf(
                        arrayOf(
                            arrayOf(null, null, null),
                            arrayOf(5085f, 4591f, 4695f)
                        ),
                        arrayOf(
                            arrayOf(null, null, null),
                            arrayOf(null, 4716f, 4727f)
                        )
                    ),
                    arrayOf(
                        arrayOf(
                            arrayOf(null, null, null),
                            arrayOf(5081f, 4589f, 4698f)
                        ),
                        arrayOf(
                            arrayOf(null, null, null),
                            arrayOf(null, 4721f, 4737f)
                        )
                    ),
                    arrayOf(
                        arrayOf(
                            arrayOf(null, null, null),
                            arrayOf(5086f, 4591f, 4700f)
                        ),
                        arrayOf(
                            arrayOf(null, null, null),
                            arrayOf(null, 4724f, 4733f)
                        )
                    )
                ).scaleAndOffset(0.001f, 30f),
                //temperature
                arrayOf<Array<Array<Array<Float?>>>>(
                    arrayOf(
                        arrayOf(
                            arrayOf(null, null, null),
                            arrayOf(885f, 685f, 474f)
                        ),
                        arrayOf(
                            arrayOf(null, null, null),
                            arrayOf(null, 679f, 456f)
                        )
                    ),
                    arrayOf(
                        arrayOf(
                            arrayOf(null, null, null),
                            arrayOf(861f, 682f, 470f)
                        ),
                        arrayOf(
                            arrayOf(null, null, null),
                            arrayOf(null, 680f, 464f)
                        )
                    ),
                    arrayOf(
                        arrayOf(
                            arrayOf(null, null, null),
                            arrayOf(858f, 685f, 468f)
                        ),
                        arrayOf(
                            arrayOf(null, null, null),
                            arrayOf(null, 679f, 456f)
                        )
                    )
                ).scaleAndOffset(0.01f, 0f),
                //time
                floatArrayOf(1.6509312E9f, 1.6509564E9f, 1.6509816E9f),
                //velocity
                Triple(
                    //u
                    arrayOf<Array<Array<Array<Float?>>>>(
                        arrayOf(
                            arrayOf(
                                arrayOf(null, null, null),
                                arrayOf(null, -96f, -61f)
                            ),
                            arrayOf(
                                arrayOf(null, null, null),
                                arrayOf(null, 24f, -77f)
                            )
                        ),
                        arrayOf(
                            arrayOf(
                                arrayOf(null, null, null),
                                arrayOf(null, -37f, -206f)
                            ),
                            arrayOf(
                                arrayOf(null, null, null),
                                arrayOf(null, 12f, 4f)
                            )
                        ),
                        arrayOf(
                            arrayOf(
                                arrayOf(null, null, null),
                                arrayOf(null, -57f, -229f)
                            ),
                            arrayOf(
                                arrayOf(null, null, null),
                                arrayOf(null, -11f, -179f)
                            )
                        )
                    ).scaleAndOffset(0.001f, 0f),
                    //v
                    arrayOf<Array<Array<Array<Float?>>>>(
                        arrayOf(
                            arrayOf(
                                arrayOf(null, null, null),
                                arrayOf(19f, -25f, -134f)
                            ),
                            arrayOf(
                                arrayOf(null, null, null),
                                arrayOf(null, 14f, -163f)
                            )
                        ),
                        arrayOf(
                            arrayOf(
                                arrayOf(null, null, null),
                                arrayOf(6f, -4f, -129f)
                            ),
                            arrayOf(
                                arrayOf(null, null, null),
                                arrayOf(null, -25f, -4f)
                            )
                        ),
                        arrayOf(
                            arrayOf(
                                arrayOf(null, null, null),
                                arrayOf(129f, 25f, -38f)
                            ),
                            arrayOf(
                                arrayOf(null, null, null),
                                arrayOf(null, 37f, -14f)
                            )
                        )
                    ).scaleAndOffset(0.001f, 0f),
                    //w
                    arrayOf<Array<Array<Array<Float?>>>>(
                        arrayOf(
                            arrayOf(
                                arrayOf(null, null, null),
                                arrayOf(-6.440923E-5f, -2.6763373E-5f, -6.496307E-5f)
                            ),
                            arrayOf(
                                arrayOf(null, null, null),
                                arrayOf(null, -5.989377E-4f, 4.8309087E-4f)
                            )
                        ),
                        arrayOf(
                            arrayOf(
                                arrayOf(null, null, null),
                                arrayOf(6.8041554E-5f, -1.4682717E-5f, 5.5459644E-5f)
                            ),
                            arrayOf(
                                arrayOf(null, null, null),
                                arrayOf(null, 1.4523527E-4f, 1.492668E-4f)
                            )
                        ),
                        arrayOf(
                            arrayOf(
                                arrayOf(null, null, null),
                                arrayOf(-4.5285957E-5f, -2.0274536E-5f, -1.02906655E-4f)
                            ),
                            arrayOf(
                                arrayOf(null, null, null),
                                arrayOf(null, 5.2956515E-4f, -0.0013055078f)
                            )
                        )
                    ).scaleAndOffset(1f, 0f)
                ),
                projection
            )


        assertArrayEquals(
            expectedNorKyst.salinity,
            actualNorKyst?.salinity
        )
        Log.d(TAG, expectedNorKyst.time.contentToString())
        Log.d(TAG, actualNorKyst?.time?.contentToString() ?: "")
        assert(
            expectedNorKyst.time.contentEquals(actualNorKyst?.time)
        )
        assertArrayEquals(
            expectedNorKyst.temperature,
            actualNorKyst?.temperature
        )
        assert(
            expectedNorKyst.depth.contentEquals(actualNorKyst?.depth)
        )
        assertArrayEquals(
            expectedNorKyst.velocity.first,
            actualNorKyst?.velocity?.first
        )
        assertArrayEquals(
            expectedNorKyst.velocity.second,
            actualNorKyst?.velocity?.second
        )
        assertArrayEquals(
            expectedNorKyst.velocity.third,
            actualNorKyst?.velocity?.third
        )
    }

}