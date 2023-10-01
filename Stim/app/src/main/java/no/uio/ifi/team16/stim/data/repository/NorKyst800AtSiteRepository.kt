package no.uio.ifi.team16.stim.data.repository

import no.uio.ifi.team16.stim.data.NorKyst800AtSite
import no.uio.ifi.team16.stim.data.Site
import no.uio.ifi.team16.stim.data.dataLoader.NorKyst800AtSiteDataLoader
import no.uio.ifi.team16.stim.util.getOrPutOrPass

/**
 * Repository for Norkyst800AtSite
 */
class NorKyst800AtSiteRepository {
    private val dataSource = NorKyst800AtSiteDataLoader()

    /**
     * maps sitenr to a Norkyst800AtSite at that site
     */
    private var cache: MutableMap<Int, NorKyst800AtSite> = mutableMapOf()

    /**
     * get NorKyst800 data around a specified site
     *
     * @param site site to load data around
     * @return cached or newly loaded data.
     */
    suspend fun getDataAtSite(site: Site): NorKyst800AtSite? {
        return cache.getOrPutOrPass(site.nr) {
            dataSource.load(site)
        }
    }

    /**
     * Empties the cache. Call this in case of low memory warning
     */
    fun clearCache() {
        cache.clear()
    }
}