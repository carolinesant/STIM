package no.uio.ifi.team16.stim.data.repository

import no.uio.ifi.team16.stim.data.Municipality
import no.uio.ifi.team16.stim.data.Site
import no.uio.ifi.team16.stim.data.dataLoader.SitesDataLoader

/**
 * Repository for sites.
 */
class SitesRepository {
    private val dataSource = SitesDataLoader()

    /**
     * Maps municipality-code to site list
     */
    private val municipalityCache: MutableMap<String, Municipality?> = mutableMapOf()

    /**
     * Maps site name to list of sites
     */
    private val nameCache: MutableMap<String, MutableSet<Site>?> = mutableMapOf()

    /**
     * Maps site number to site object
     */
    private val siteNrCache: MutableMap<Int, Site?> = mutableMapOf()

    /**
     * Load the municipality at the given municipalitycode
     */
    suspend fun getMunicipality(municipalityCode: String): Municipality? {

        // start by checking cache
        var municipality = municipalityCache[municipalityCode]
        if (municipality != null) {
            if (municipality.sites.isEmpty()) {
                return null
            }
            return municipality
        }

        // nothing in cache, load from the API
        municipality = dataSource.loadDataByMunicipalityCode(municipalityCode)

        if (municipality != null) {
            // sites found, cache them for later
            municipalityCache[municipalityCode] = municipality
            for (site in municipality.sites) {
                siteNrCache[site.nr] = site
            }
        } else {
            // this municipality has no sites, save that so we don't check again later
            municipalityCache[municipalityCode] = Municipality(municipalityCode, emptyList())
        }
        return municipality
    }

    /**
     * Get a site object for each of the user's favourites
     * @param favourites a set of site IDs that the user has marked as favourite
     */
    suspend fun getFavouriteSites(favourites: Set<String>?): MutableList<Site> {
        val list = mutableListOf<Site>()
        favourites?.forEach { siteNr ->
            val loadedSite = getSiteByNr(siteNr.toInt())
            if (loadedSite != null) list.add(loadedSite)
        }
        return list
    }

    /**
     * Search for all sites matching a name
     * @param name the name of a site
     * @return a list of all matching sites, or null if none found
     */
    suspend fun getSitesByName(name: String): List<Site>? {
        // check cache first
        val sites = nameCache[name]
        if (sites != null) {
            return if (sites.isEmpty()) null else sites.toList()
        }
        val sitesList = dataSource.loadSitesByName(name)

        if (sitesList != null) {
            nameCache[name] = sitesList.toMutableSet()

            // save sites in nr-cache too
            for (site in sitesList) {
                siteNrCache[site.nr] = site
            }
        } else {
            // no sites found for this name, save this so we don't check API again
            nameCache[name] = mutableSetOf()
        }

        return sitesList
    }

    /**
     * Get a site with a given number
     */
    private suspend fun getSiteByNr(nr: Int): Site? {
        var site = siteNrCache[nr]
        if (site != null) {
            return site
        }
        site = dataSource.loadDataByNr(nr.toString())
        siteNrCache[nr] = site

        return site
    }

    /**
     * Empties the cache. Call this in case of low memory warning
     */
    fun clearCache() {
        municipalityCache.clear()
        nameCache.clear()
        siteNrCache.clear()
    }
}