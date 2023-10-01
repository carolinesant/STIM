package no.uio.ifi.team16.stim.data.repository

import no.uio.ifi.team16.stim.data.dataLoader.AddressDataLoader
import no.uio.ifi.team16.stim.util.LatLong

class AddressRepository {

    private val dataSource = AddressDataLoader()

    /**
     * Key = latlng, value = municipality nr
     */
    private val municipalityNrCache: MutableMap<LatLong, String?> = mutableMapOf()

    /**
     * Key = municipality name (in uppercase!!), value = municipality nr
     */
    private val municipalityNameCache: MutableMap<String, String?> = mutableMapOf()

    /**
     * Get municipality number for a given coordinate
     */
    suspend fun getMunicipalityNr(latLong: LatLong): String? {
        val cachedNr = municipalityNrCache[latLong]
        if (cachedNr != null) {
            return cachedNr
        }

        val data = dataSource.loadMunicipalityNr(latLong)
        if (data != null) {
            municipalityNrCache[latLong] = data.first
            municipalityNameCache[data.first] = data.second
        }

        return data?.first
    }

    /**
     * Get a municipality code from a search string
     */
    suspend fun searchMunicipalityNr(query: String): String? {
        val name = query.uppercase()
        var nr = municipalityNameCache[name]
        if (nr != null && nr.isNotBlank()) {
            return nr
        }

        nr = dataSource.loadMunicipalityNr(name)
        if (nr != null) {
            municipalityNameCache[query] = nr
        } else {
            // add empty string to cache so we don't check API again later
            municipalityNameCache[query] = ""
        }

        return nr
    }

    /**
     * Empties the cache. Call this in case of low memory warning
     */
    fun clearCache() {
        municipalityNrCache.clear()
        municipalityNameCache.clear()
    }
}