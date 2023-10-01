package no.uio.ifi.team16.stim.data.dataLoader

import android.util.Log
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Parameters
import com.github.kittinunf.fuel.coroutines.awaitString
import no.uio.ifi.team16.stim.data.AreaPlacement
import no.uio.ifi.team16.stim.data.Capacity
import no.uio.ifi.team16.stim.data.Municipality
import no.uio.ifi.team16.stim.data.Site
import no.uio.ifi.team16.stim.util.LatLong
import no.uio.ifi.team16.stim.util.Options
import no.uio.ifi.team16.stim.util.capitalizeEachWord
import org.json.JSONArray

/**
 * Load data from kartverkets API
 */
class SitesDataLoader {
    companion object {
        private const val TAG = "SitesDataLoader"
        private const val BASE_URL: String = "https://api.fiskeridir.no/pub-aqua/api/v1/sites"
    }

    /////////////
    // LOADERS //
    /////////////
    /**
     * Loads the 100 first municipality from the url, with the given parameters in the query
     *
     * See the municipality-API parameters banner for all parameters.
     * https://api.fiskeridir.no/pub-aqua/api/swagger-ui/index.html?configUrl=/pub-aqua/api/api-docs/swagger-config#/site-resource/municipality
     *
     * @param parameters list of parameters to the query, can be in the form of List<Pair<String,Any?>>,
     * or using Fules own syntax, listof(param1string to param1, ...)
     * @return a list of sites returned from the query with given parameters, or null if fetching or parsing fails
     */
    private suspend fun loadWithParameters(parameters: Parameters): List<Site>? {
        var responseStr = ""
        try {
            responseStr = Fuel.get(BASE_URL, parameters).awaitString()
        } catch (e: Exception) {
            Log.e(TAG, "Kunne ikke hente municipality med params: $parameters", e)
        }

        if (responseStr.isEmpty()) {
            Log.w(TAG, "empty response")
            return null
        }

        val sites = JSONArray(responseStr)
        val out: MutableList<Site> = mutableListOf()

        if (sites.length() == 0) {
            return null
        }

        for (i in 0 until sites.length()) {
            //try to parse, if succesfull add to out, otherwise just try next
            sites.getJSONObject(i)?.runCatching {
                out.add(
                    Site(
                        getInt("siteNr"),
                        getString("name"),
                        LatLong(getDouble("latitude"), getDouble("longitude")),
                        // a site might not have an areaplacement
                        getJSONObject("placement")?.let { ap ->
                            AreaPlacement(
                                ap.getInt("municipalityCode"),
                                ap.getString("municipalityName").capitalizeEachWord(),
                                ap.getInt("countyCode"),
                            )
                        },
                        Capacity(getDouble("capacity"), getString("capacityUnitType")),
                        getString("placementType"),
                        getString("waterType")
                    )
                )
            }?.onFailure { failure ->
                Log.e(TAG, "Failed to create a site due to: $failure")
            }
        }

        //the API only returns 100 sites at a time, if we got 100 sites, load the next 100
        if (out.size == 100) {
            //get the used range parameters, and load with the next ones
            val nextParameters: MutableList<Pair<String, Any?>> = mutableListOf()
            parameters
                .firstOrNull { (param, _) -> param == "range" }
                ?.second
                ?.toString()
                ?.split("-")
                ?.getOrNull(1)
                ?.toIntOrNull()
                ?.let { previousRangeEnd ->
                    val parametersWithoutRange =
                        parameters.filter { (param, _) -> param != "range" }
                    val nextRangeEnd = previousRangeEnd + 100
                    nextParameters.add("range" to "${previousRangeEnd + 1}-$nextRangeEnd")
                    nextParameters.addAll(parametersWithoutRange)
                } ?: run { //no range parameter, implicit 0-100
                nextParameters.add("range" to "100-199")
                nextParameters.addAll(parameters)
            }
            loadWithParameters(
                nextParameters
            )?.let { nextList ->
                out.addAll(
                    nextList
                )
            }
        }
        Log.d(TAG, "loaded ${out.size} sites!")
        return out.toList()
    }

    /**
     * Load sites with a given municipality code
     * @param municipalityCode municipalitycode to find sites in
     * @return municipality object with sites in the specified municipality
     * @see loadWithParameters
     */
    suspend fun loadDataByMunicipalityCode(municipalityCode: String): Municipality? =
        loadWithParameters(
            listOf(
                "range" to Options.sitesRange,
                "municipality-code" to municipalityCode
            )
        )?.let { sites ->
            Municipality(
                municipalityCode,
                sites
            )
        }

    /**
     * Load sites with a given name
     * @param name name of a site
     * @return a list of matching sites or null if none found
     */
    suspend fun loadSitesByName(name: String): List<Site>? {
        return loadWithParameters(
            listOf(
                "range" to Options.sitesRange,
                "name" to name
            )
        )
    }

    /**
     * Load a site with the given number/unique ID
     * @param nr site number
     * @return site or null if none exists
     */
    suspend fun loadDataByNr(nr: String): Site? {
        val sites = loadWithParameters(
            listOf(
                "range" to Options.sitesRange,
                "nr" to nr
            )
        )
        return sites?.getOrNull(0)
    }
}

