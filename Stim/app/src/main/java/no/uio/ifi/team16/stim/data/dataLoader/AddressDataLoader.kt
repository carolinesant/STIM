package no.uio.ifi.team16.stim.data.dataLoader

import android.util.Log
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitString
import no.uio.ifi.team16.stim.util.LatLong
import org.json.JSONObject

class AddressDataLoader {

    companion object {
        private const val TAG = "AddressDataLoader"
        private const val POINT_SEARCH_URL = "https://ws.geonorge.no/adresser/v1/punktsok"
        private const val SEARCH_URL = "https://ws.geonorge.no/adresser/v1/sok"
        private const val RADIUS = 1000
    }

    /**
     * Load the municipalityNr at the given latLong
     * @param latLong latLong to find MunicipalityNr of
     * @return pair of municipality-number and name
     */
    suspend fun loadMunicipalityNr(latLong: LatLong): Pair<String, String>? {

        val side = "side" to 0
        val radius = "radius" to RADIUS
        val utkoordsys = "utkoordsys" to 4258
        val koordsys = "koordsys" to 4258
        val lat = "lat" to latLong.lat
        val lon = "lon" to latLong.lng

        val params = listOf<Pair<String, Any>>(side, radius, utkoordsys, koordsys, lat, lon)

        var result = ""
        try {
            result = Fuel.get(POINT_SEARCH_URL, params).awaitString()
        } catch (e: Exception) {
            Log.e(TAG, "Kunne ikke hente kommune for latlng: $latLong", e)
        }

        if (result.isBlank()) {
            Log.e(TAG, "Empty response from Kartverket API")
            return null
        }

        val response = JSONObject(result)
        val addresses = response.getJSONArray("adresser")

        if (addresses.length() > 0) {
            val first = addresses.getJSONObject(0)
            return Pair(first.getString("kommunenummer"), first.getString("kommunenavn"))
        }

        return null
    }

    /**
     * Load the municipalitynumber of a municipalityname
     */
    suspend fun loadMunicipalityNr(name: String): String? {

        val side = "side" to 0
        val utkoordsys = "utkoordsys" to 4258
        val treffPerSide = "treffPerSide" to 1
        val kommunenavn = "kommunenavn" to name

        val params = listOf<Pair<String, Any>>(side, utkoordsys, treffPerSide, kommunenavn)

        var result = ""
        try {
            result = Fuel.get(SEARCH_URL, params).awaitString()
        } catch (e: Exception) {
            Log.e(TAG, "Kunne ikke hente kommunenummer for kommunenavn: $name", e)
        }

        if (result.isBlank()) {
            Log.e(TAG, "Empty response from Kartverket API")
            return null
        }

        val response = JSONObject(result)
        val adresser = response.getJSONArray("adresser")

        if (adresser.length() > 0) {
            val first = adresser.getJSONObject(0)
            return first.getString("kommunenummer")
        }

        return null
    }
}