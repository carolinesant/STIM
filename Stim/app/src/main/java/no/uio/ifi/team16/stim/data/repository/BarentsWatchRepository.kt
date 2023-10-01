package no.uio.ifi.team16.stim.data.repository

import android.util.Log
import no.uio.ifi.team16.stim.data.BarentsWatchAtSite
import no.uio.ifi.team16.stim.data.BarentsWatchToken
import no.uio.ifi.team16.stim.data.Site
import no.uio.ifi.team16.stim.data.dataLoader.BarentsWatchDataLoader

class BarentsWatchRepository {

    private val TAG = "BarentsWatchRepository"
    private val dataSource = BarentsWatchDataLoader()
    private var tokenCache: BarentsWatchToken? = null
    private val dataCache: MutableMap<Site, BarentsWatchAtSite> = mutableMapOf()

    /**
     * Load the token from the datasource
     */
    private suspend fun getToken(): BarentsWatchToken? {
        if (tokenCache == null || tokenCache?.isValid() == false) {
            tokenCache = dataSource.getToken()
        }
        return tokenCache
    }

    /**
     * Load the data from the datasource
     */
    suspend fun getDataAtSite(site: Site): BarentsWatchAtSite? {
        val token = getToken() ?: run { //only null if cache empty and load fails
            Log.w(TAG, "Failed to load token")
            return null
        }

        return dataCache.getOrPutOrPass(site) { //if not cached, run the following and cache that.
            dataSource.loadData(site, token)
        }
    }

    fun clearCache() {
        dataCache.clear()
    }

    ///////////////
    // UTILITIES //
    ///////////////
    /**
     * same as Map.getOrPut, but if the put value resuls in null, don't put
     *
     * if the key exists, return its value
     * if not, evaluate default,
     *      if default succeeds(not null) put it in the cache, and return the value
     *      if default fails (null), dont put anything in cache(get or put would put null in cache) and return null
     * @see MutableMap.getOrPut
     */
    private inline fun <K, V> MutableMap<K, V>.getOrPutOrPass(key: K, default: () -> V?): V? =
        getOrElse(key) {
            default()?.let { value ->
                this[key] = value
                value
            }
        }

}
