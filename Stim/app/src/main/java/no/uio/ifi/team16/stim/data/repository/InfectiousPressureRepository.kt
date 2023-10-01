package no.uio.ifi.team16.stim.data.repository

import no.uio.ifi.team16.stim.data.InfectiousPressure
import no.uio.ifi.team16.stim.data.dataLoader.InfectiousPressureDataLoader

/**
 * Repository for infectious pressure data.
 * The Single Source of Truth(SST) for the viewmodel.
 * This is the class that should be queried for data when a class in the model layer wants a set of data.
 *
 * The repository can load data and return it or return cached data
 */
class InfectiousPressureRepository {
    private val dataSource = InfectiousPressureDataLoader()

    /**
     * cache for global infectiousPressure
     */
    private var cache: InfectiousPressure? = null

    /**
     * wether the cached data needs to be loaded
     */
    private var dirty: Boolean = true

    /**
     * get the entire dataset at current time
     *
     * if the cache is not up to date(dirty), load the data anew,
     * otherwise just return the data in the cache.
     *
     * @return cached or newly loaded data.
     */
    suspend fun getDefault(): InfectiousPressure? {
        if (dirty) {
            cache = dataSource.loadDefault()
            dirty = false
        }
        return cache
    }

    /**
     * Empties the cache. Call this in case of low memory warning
     */
    fun clearCache() {
        cache = null
    }
}