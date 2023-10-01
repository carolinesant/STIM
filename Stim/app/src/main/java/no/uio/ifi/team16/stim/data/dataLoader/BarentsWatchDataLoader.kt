package no.uio.ifi.team16.stim.data.dataLoader

import android.util.Log
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.coroutines.awaitStringResult
import com.github.kittinunf.result.getOrElse
import com.github.kittinunf.result.onError
import no.uio.ifi.team16.stim.data.BarentsWatchAtSite
import no.uio.ifi.team16.stim.data.BarentsWatchToken
import no.uio.ifi.team16.stim.data.Site
import org.json.JSONObject
import java.net.URLEncoder
import java.time.Instant

class BarentsWatchDataLoader {

    companion object {
        private const val TAG = "BarentsWatchDataLoader"
        private const val BASE_URL = "https://id.barentswatch.no/connect/token"
        private const val CLIENT_SECRET = "YXA8wDV&SmUqdo"
        private const val CLIENT_ID = "andreaav@uio.no:andreaav@uio.no"
    }

    /**
     * Retrieves an authentication token to be used in API requests to the BW API
     */
    suspend fun getToken(): BarentsWatchToken? {

        val bodyParams = hashMapOf(
            "grant_type" to "client_credentials",
            "client_secret" to CLIENT_SECRET,
            "scope" to "api",
            "client_id" to CLIENT_ID
        )

        val body = getDataString(bodyParams)

        val (request) = Fuel.post(BASE_URL)
            .header(Headers.CONTENT_TYPE, "application/x-www-form-urlencoded")
            .header(Headers.ACCEPT, "*/*")
            .header(Headers.ACCEPT_ENCODING, "gzip, deflate, br")
            .header("Connection", "keep-alive")
            .body(body)
            .responseString()

        //await result and handle error if any
        val tokenStr: String = request.awaitStringResult().onError { error ->
            Log.e(TAG, "Failed to post to BarentsWatch API due to: ", error)
            return null
        }.getOrElse { err ->
            Log.e(TAG, "Failed to get response from BarentsWatch API due to: ", err)
            return null
        }

        val jsonObject = JSONObject(tokenStr)
        val validityTime: Long
        val token: String
        try {
            token = jsonObject.getString("access_token")
            validityTime = jsonObject.getString("expires_in").toLong()
        } catch (e: Error) {
            Log.e(TAG, "Failed to read token from response due to $e")
            return null
        }
        return BarentsWatchToken(token, Instant.now().plusSeconds(validityTime))
    }

    private fun getDataString(params: HashMap<String, String>): String {
        val result = StringBuilder()
        var first = true
        for ((key, value) in params) {
            if (first) first = false else result.append("&")
            result.append(URLEncoder.encode(key, "UTF-8"))
            result.append("=")
            result.append(URLEncoder.encode(value, "UTF-8"))
        }
        return result.toString()
    }

    /**
     * load data at the given site with a token
     *
     * @param site site to load data at
     * @param token token from the barentswatch API to use in authentication
     * @see getToken
     * @return Disease data from the barentswatch API at the given site
     */
    suspend fun loadData(site: Site, token: BarentsWatchToken): BarentsWatchAtSite? {

        val listPD = getPdList(site, token) ?: return null
        val listILA = getIlaList(site, token) ?: return null

        return BarentsWatchAtSite(listPD, listILA)
    }

    private suspend fun getIlaList(site: Site, token: BarentsWatchToken): HashMap<String, String>? {
        val listILA = HashMap<String, String>()
        val baseUrlILA = "https://www.barentswatch.no/bwapi/v1/geodata/download/localitieswithila"
        val lon = site.latLong.lng
        val lat = site.latLong.lat
        val distance = 1.0

        val responseStr = Fuel.get(
            baseUrlILA,
            listOf("format" to "JSON", "lon" to lon, "lat" to lat, "distance" to distance)
        )
            .authentication()
            .bearer(token.key)
            .awaitStringResult()
            .onError { error ->
                Log.e(TAG, "Failed to read ILA list from BarentsWatch API due to: ", error)
                return null
            }.getOrElse { err ->
                Log.e(TAG, "Failed to get ILA list from from BarentsWatch API due to: ", err)
                return null
            }

        val response = JSONObject(responseStr)
        try {
            val totalFeatures = response.getString("totalFeatures")
            if (totalFeatures == "0") {
                return HashMap()
            }

            val features = response.getJSONArray("features")
            val jsonObject = features.getJSONObject(0)
            val properties = jsonObject.getJSONObject("properties")
            listILA["mistankedato"] = properties.getString("mistankedato")
            listILA["paavistdato"] = properties.getString("paavistdato")
        } catch (e: Error) {
            Log.e(TAG, "Failed to parse ILA from response due to: ", e)
            return null
        }

        return listILA
    }

    private suspend fun getPdList(site: Site, token: BarentsWatchToken): HashMap<String, String>? {
        val listPD = HashMap<String, String>()
        val baseUrlPD = "https://www.barentswatch.no/bwapi/v1/geodata/download/localitieswithpd"
        val lon = site.latLong.lng
        val lat = site.latLong.lat
        val distance = 1.0

        val responseStr = Fuel.get(
            baseUrlPD,
            listOf("format" to "JSON", "lon" to lon, "lat" to lat, "distance" to distance)
        )
            .authentication()
            .bearer(token.key)
            .awaitStringResult()
            .onError { error ->
                Log.e(TAG, "Failed to read PD list from BarentsWatch API due to: ", error)
                return null
            }.getOrElse { err ->
                Log.e(
                    TAG,
                    "Failed to get PD list from from BarentsWatch API due to: ", err
                )
                return null
            }

        val response = JSONObject(responseStr)
        try {
            val totalFeatures = response.getString("totalFeatures")
            if (totalFeatures == "0") {
                return HashMap()
            }

            val features = response.getJSONArray("features")
            val jsonObject = features.getJSONObject(0)
            val properties = jsonObject.getJSONObject("properties")
            listPD["pd_ukjent_mistankedato"] = properties.getString("pd_ukjent_mistankedato")
            listPD["pd_sav2_mistankedato"] = properties.getString("pd_sav2_mistankedato")
            listPD["pd_sav3_mistankedato"] = properties.getString("pd_sav3_mistankedato")
            listPD["pd_ukjent_paavistdato"] = properties.getString("pd_ukjent_paavistdato")
            listPD["pd_sav2_paavistdato"] = properties.getString("pd_sav2_paavistdato")
            listPD["pd_sav3_paavistdato"] = properties.getString("pd_sav3_paavistdato")
        } catch (e: Error) {
            Log.e(TAG, "Failed to parse PD from response due to $e")
            return null
        }

        return listPD
    }
}