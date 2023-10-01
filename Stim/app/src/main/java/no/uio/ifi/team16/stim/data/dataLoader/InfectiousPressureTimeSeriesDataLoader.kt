package no.uio.ifi.team16.stim.data.dataLoader

import android.util.Log
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.uio.ifi.team16.stim.data.InfectiousPressureTimeSeries
import no.uio.ifi.team16.stim.data.Site
import no.uio.ifi.team16.stim.util.*
import org.locationtech.proj4j.CRSFactory
import org.locationtech.proj4j.CoordinateTransform
import org.locationtech.proj4j.CoordinateTransformFactory
import ucar.ma2.ArrayFloat
import ucar.nc2.Variable
import ucar.nc2.dataset.NetcdfDataset
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Dataloader for infectiouspressure over time.
 *
 * Infectiouspressure is a large grid at one exact time, and will not play well with the representation
 * of infectiouspressure over a small area and several times. So we have to make a separate dataloader
 * for this format.
 *
 * Generally this type of infectiouspressure will be for a specific site, and at a site we are interested
 * in a few squares around that site, so the shape of this data will be something like time.length()x3x3
 */
class InfectiousPressureTimeSeriesDataLoader : InfectiousPressureDataLoader() {
    private val TAG = "InfectiousPressureTimeSeriesDataLoader"

    /////////////
    // LOADERS //
    /////////////
    /**
     * return the infectious pressure at a given site over a given amount of weeks,
     * always starting from the most recent week
     * Note that the weeksrange are weeks from now, and not specific weeks!
     * The amount of grid points around a site to get is specified in Options, through Options.siteRadius
     *
     * The most current dataset is opened, and at the same time a coroutine is launced to load the
     * older datasets.
     *
     * @param site: the site to load infectiousPressure at
     * @param weeksRange: an intprogression of weeks from now, to get. fromClosedRange(2,13,3)
     *                    corresponds to week 2, till week 13 with a stride of 3 FROM NOW, and will
     *                    return the data from week 2, 5, 8 and 11 FROM CURRENT WEEK.
     * @returns InfectiousPressureTimeSeres in the given range at the given site.
     */
    suspend fun load(
        site: Site,
        weeksRange: IntProgression
    ): InfectiousPressureTimeSeries? {
        /*
        first, load the entries in the catalog, then use the first enty in the catalog to get the data
        that is common for all datasets.

         */

        //load name of all entries in catalog
        val catalogEntries = loadEntryUrls()?.toList() ?: run {
            Log.e(TAG, "Failed to open thredds catalog")
            return null
        }

        //get the first entry
        val firstEntry = catalogEntries.firstOrNull() ?: run {
            Log.e(TAG, "Failed to get anything from the catalog after opening, is the url correct?")
            return null
        }

        //attributes common to all files
        var dx = 800f
        var dy = 800f
        var y: Int
        var x: Int
        var minX = 0
        var maxX = 0
        var minY = 0
        var maxY = 0
        var latLngToStereo: CoordinateTransform

        //open the first file, which is used to get the data common for all datasets
        ///////////////////
        // FIRST DATASET //
        ///////////////////
        val currentData = threddsLoad(firstEntry) { firstncfile ->
            //get common data variables
            val gridMapping: Variable = firstncfile.findVariable("grid_mapping")
                ?: run {
                    Log.e(TAG, "Failed to read variable <gridMapping> from infectiousPressure")
                    return null
                }
            dx = gridMapping.findAttribute("dx")?.numericValue?.toFloat()
                ?: run {
                    Log.e(
                        TAG,
                        "Failed to read attribute <dx> from <gridMapping> from infectiousPressure"
                    )
                    return null
                }
            dy = dx
            //make the projection
            val crsFactory = CRSFactory()
            val stereoCRT = crsFactory.createFromParameters(
                null,
                gridMapping.findAttribute("proj4string")?.stringValue
                    ?: run {
                        Log.e(
                            TAG,
                            "Failed to read attribute <proj4string> from <gridMapping> from infectiousPressure"
                        )
                        return null
                    }
            )
            val latLngCRT = stereoCRT.createGeographic()
            val ctFactory = CoordinateTransformFactory()
            latLngToStereo = ctFactory.createTransform(latLngCRT, stereoCRT)

            latLngToStereo.project(site.latLong).let { (xf, yf) ->
                y = (xf / 800).roundToInt()
                x = (yf / 800).roundToInt()
            }

            val radius = Options.siteRadius

            minX = max(x - radius, 0)
            maxX = min(max(x + radius, 0), Options.norKyst800XEnd)
            minY = max(y - radius, 0)
            maxY = min(max(y + radius, 0), Options.norKyst800YEnd)

            getDataFromFile(firstncfile, minX, maxX, minY, maxY)
        } ?: return null

        ////////////////////
        // OLDER DATASETS //
        ////////////////////
        val historicalData = MutableLiveData<Pair<Array<Int>, FloatArray3D>>()
        CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
            historicalData.postValue(
                catalogEntries
                    .takeRange(weeksRange)
                    .mapIndexedNotNull { i, entryUrl ->
                        threddsLoad(entryUrl) { ncfile ->
                            getDataFromFile(ncfile, minX, maxX, minY, maxY)
                        }?.let { data ->
                            Pair(
                                i + 1, //weeks from current week!
                                data.second
                            )
                        }
                    }.unzip().let { (weeks, data) ->
                        Pair(
                            weeks.toTypedArray(),
                            data.toTypedArray()
                        )
                    }
            )
        }

        return InfectiousPressureTimeSeries(
            site.nr,
            currentData.first,
            currentData.second,
            historicalData,
            dx,
            dy
        )
    }

    /**
     * get weeknumber and concentration from a gvien ncfile of infectiouspressure
     *
     * @param ncfile: ncfile of infectiousPressure
     * @param minX minimum x index to load from
     * @param maxX maximum x index to load from
     * @param minY minimum y index to load from
     * @param maxY maximum y index to load from
     * @return week and associated infectiouspressure as a 2D array
     */
    private fun getDataFromFile(
        ncfile: NetcdfDataset,
        minX: Int,
        maxX: Int,
        minY: Int,
        maxY: Int
    ): Pair<Int, FloatArray2D>? {
        val concentrations: Variable = ncfile.findVariable("C10")
            ?: run {
                Log.e(TAG, "Failed to read variable <C10> from infectiousPressure")
                return null
            }
        //take out the arrayfloat(s) for this dataset, return (weeknumber, concnetration)
        return Pair(
            ncfile.findGlobalAttribute("weeknumber")!!.numericValue.toInt(), //global attribute week always exists
            ((concentrations.read("0,${minX}:${maxX},${minY}:${maxY}")
                .reduce(0) as ArrayFloat).to2DFloatArray())
        )
    }
}