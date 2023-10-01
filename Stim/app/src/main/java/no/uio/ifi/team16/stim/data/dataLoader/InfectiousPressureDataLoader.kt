package no.uio.ifi.team16.stim.data.dataLoader

import android.util.Log
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitString
import no.uio.ifi.team16.stim.data.InfectiousPressure
import no.uio.ifi.team16.stim.util.Options
import no.uio.ifi.team16.stim.util.reformatFLS
import no.uio.ifi.team16.stim.util.to2DFloatArray
import ucar.ma2.ArrayFloat
import ucar.nc2.Variable
import kotlin.ranges.IntProgression.Companion.fromClosedRange

/**
 * DataLoader for infectious pressure data.
 *
 * Used to load InfectiousPressure Objects, which are used to show heatMap data.
 **/
open class InfectiousPressureDataLoader : THREDDSDataLoader() {
    private val TAG = "InfectiousPressureDataLoader"

    /*the catalogentries are the datasets in the catalog. There are a lot so it should be loaded only
    once per run. However the catalog updates at wednesdays, so if the application is started 5 min before
    and caches the result, the cache will be incorrect when the catalog updates and will be unable to
    check the catalog.
    */
    private var dirtyCatalogCache = true

    //sequence of urls in the catalog, format AGG_OPR_<YEAR>_<WEEK>.nc
    private var catalogCache: Sequence<String> = sequenceOf()

    private val baseUrl = "http://thredds.nodc.no:8080/thredds/fileServer/smittepress_new2018/"

    private val catalogUrl =
        "http://thredds.nodc.no:8080/thredds/catalog/smittepress_new2018/catalog.html"

    /////////////
    // LOADERS //
    /////////////
    /**
     * return data in the given range.
     * Usually the data is in a 2602x902 yx-grid
     *
     * @param xRange range of x-coordinates to get
     * @param yRange range of y-coordinates to get
     * @return data of infectious pressure in the prescribed data range.
     *
     * @see THREDDSDataLoader.threddsLoad()
     */
    suspend fun load(
        xRange: IntProgression,
        yRange: IntProgression
    ): InfectiousPressure? {
        val catalogEntries = loadEntryUrls() //fills catalogCache, blocks the current coroutine
        val firstEntry = catalogEntries?.firstOrNull() ?: run {
            Log.e(
                TAG,
                "failed to load a single entry from the catalog, is the url correct? Are you connected to the internet?"
            )
            return null
        }

        return load(firstEntry, xRange, yRange)
    }

    /**
     * load the default dataset, as specified by Options
     */
    suspend fun loadDefault(): InfectiousPressure? =
        try {
            load(
                fromClosedRange(0, 901, Options.infectiousPressureStepX),
                fromClosedRange(0, 2601, Options.infectiousPressureStepY)
            )
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Failed to load InfectiousPressure global data due to outOfMemoryError + $e")
            null
        }

    /**
     * load from a specified url.
     * Also used in testing.
     *
     * @param url range of x-coordinates to get
     * @param xRange range of x-coordinates to get
     * @param yRange range of y-coordinates to get
     * @return data of infectious pressure in the prescribed data range.
     *
     * @see THREDDSDataLoader.threddsLoad()
     */
    fun load(
        url: String,
        xRange: IntProgression,
        yRange: IntProgression
    ): InfectiousPressure? {
        return threddsLoad(url) { ncfile ->
            //make some extra ranges to access data
            val range2 = "${xRange.reformatFLS()},${yRange.reformatFLS()}"
            val range3 = "0,$range2"
            Log.d(TAG, range3)
            //make the projection
            val gridMapping: Variable = ncfile.findVariable("grid_mapping")
                ?: throw NullPointerException("Failed to read variable <gridMapping> from infectiousPressure") //caught by THREDDSLOAD
            val latLngToStereo =
                readAndMakeProjectionFromGridMapping(gridMapping) //can throw NullpointerException, caught by THREDDSLOAD
            //lets make some infectious pressure
            //Variables are data that are NOT READ YET. findVariable() is not null-safe
            val concentrations: Variable = ncfile.findVariable("C10")
                ?: throw NullPointerException("Failed to read variable <C10> from infectiousPressure") //caught by THREDDSLOAD
            val time: Variable = ncfile.findVariable("time")
                ?: throw NullPointerException("Failed to read variable <time> from infectiousPressure") //caught by THREDDSLOAD

            //make the infectiousPressure
            Log.d("LOADER", "MADE!")
            InfectiousPressure(
                (concentrations.read(range3).reduce(0) as ArrayFloat).to2DFloatArray(),
                time.readScalarFloat(),
                latLngToStereo
            )
        }
    }


    ///////////////
    // UTILITIES //
    ///////////////
    /**
     * Make a list of all url-entries in the catalog, sorted by date(newer first)
     *
     * regex out all entries in the catalog, also parsing out year and week of the entries,
     * for each triple of year, week entryName,
     * associate year*52+week to entryName, giving a map where the keys are weeks from year 0(excluding some leap stuff, but should work(?))
     * return a list where values(catalog-entry names) are sorted by keys(weeks from year 0),
     * so that catalog-entries are sorted by time
     *
     * CACHES ON FIRST CALL
     *
     * Sorting ruins the sequencing, ie all entries are loaded greedily.
     * Probably not possible to make lazy since entries are sorted weirdly in catalog
     */
    //get the name, year and week of an entry in the catalog, used to get url.
    //group1: entry name
    //group2: entry year
    //group3: entry week
    private val catalogEntryRegex =
        Regex("""'catalog\.html\?dataset=smittepress_new2018/(agg_OPR_(.*?)_(.*?)\.nc)'""")
    protected suspend fun loadEntryUrls(): Sequence<String>? =
        if (dirtyCatalogCache) {
            try {
                Fuel.get(catalogUrl).awaitString()
            } catch (e: Exception) {
                Log.e(TAG, "Unable to retrieve smittepress catalog due to", e)
                null
            }?.let { responseStr ->
                catalogEntryRegex.findAll(responseStr)
                    .map { catalogEntryMatch -> //for every match(catalog entry)
                        //the following groups are guaranteed to exist, from the way the regex is constructed
                        val name = catalogEntryMatch.groupValues[1]
                        val year = catalogEntryMatch.groupValues[2].toInt()
                        val week = catalogEntryMatch.groupValues[3].toInt()
                        //associate weeks from year 0 to name
                        //the first entry of the year is labeled as week 52, for some reason, map to 0
                        year * 52 + (if (week == 52) 0 else week) to name
                    }.sortedByDescending { (weeksFromYear0, _) -> //sort by weeks from year 0
                        weeksFromYear0
                    }.map { (_, name) -> //reduce to only urls
                        baseUrl + name
                    } //we have a list of urls, sorted by date!
            }?.let { result -> //if everything succeeded, store in cache and return it
                Log.d(TAG, "loaded catalog ${result.toList()}")
                catalogCache = result
                dirtyCatalogCache = false
                catalogCache
            } ?: run { //"catch" unsucssessfull parse
                Log.e(TAG, "Failed to get infectiouspressure catalog: $catalogUrl")
                null
            }
        } else { //if cache available
            catalogCache
        }
}