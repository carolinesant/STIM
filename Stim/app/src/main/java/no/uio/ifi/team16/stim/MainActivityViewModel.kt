package no.uio.ifi.team16.stim

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.uio.ifi.team16.stim.data.*
import no.uio.ifi.team16.stim.data.repository.*
import no.uio.ifi.team16.stim.util.LatLong
import no.uio.ifi.team16.stim.util.Options

/**
 * The binding between the data repositories and the fragments (view)
 */
class MainActivityViewModel : ViewModel() {

    /**
     * A reference to the site that the user is currently looking at
     */
    var site: Site? = null

    private lateinit var preferences: SharedPreferences

    //REPOSITORIES
    private val infectiousPressureRepository = InfectiousPressureRepository()
    private val infectiousPressureTimeSeriesRepository = InfectiousPressureTimeSeriesRepository()
    private val sitesRepository = SitesRepository()
    private val norKyst800AtSiteRepository = NorKyst800AtSiteRepository()
    private val addressRepository = AddressRepository()
    private val weatherRepository = WeatherRepository()
    private val barentsWatchRepository = BarentsWatchRepository()

    //MUTABLE LIVE DATA
    private val infectiousPressureData = MutableLiveData<InfectiousPressure?>()
    private val infectiousPressureTimeSeriesData = mutableMapOf<Site, MutableLiveData<InfectiousPressureTimeSeries?>>()
    private val municipalityData = MutableLiveData<Municipality?>()
    private val favouriteSitesData = MutableLiveData<MutableList<Site>?>()
    private val norKyst800AtSiteData = mutableMapOf<Site, MutableLiveData<NorKyst800AtSite?>>()
    private val currentSitesData = MutableLiveData<List<Site>?>()
    private val weatherData = MutableLiveData<WeatherForecast?>()
    private val barentsWatchData = mutableMapOf<Site, MutableLiveData<BarentsWatchAtSite?>>()

    /**
     * Get infectious pressure data for the heatmap
     */
    fun getInfectiousPressureData(): LiveData<InfectiousPressure?> {
        return infectiousPressureData
    }

    /**
     * Get NorKyst800-data specific to a site
     * @param site the site to observe info for
     */
    fun getNorKyst800AtSiteData(site: Site): LiveData<NorKyst800AtSite?> {
        return norKyst800AtSiteData.getOrPut(site) {
            MutableLiveData()
        }
    }

    /**
     * Get the current municipality loaded for viewing in the map
     */
    fun getMunicipalityData(): LiveData<Municipality?> {
        return municipalityData
    }

    /**
     * Get the weather at a site
     * @see loadWeatherAtSite
     */
    fun getWeatherData(): LiveData<WeatherForecast?> {
        return weatherData
    }

    /**
     * Get infectious pressure over time for displaying in a graph
     */
    fun getInfectiousPressureTimeSeriesData(site: Site): LiveData<InfectiousPressureTimeSeries?> {
        return infectiousPressureTimeSeriesData.getOrPut(site) {
            MutableLiveData()
        }
    }

    /**
     * Get the user's favourite sites
     */
    fun getFavouriteSitesData(): LiveData<MutableList<Site>?> {
        return favouriteSitesData
    }

    /**
     * Saves a SharedPreferences instance for usage in all fragments
     */
    fun setPreferences(prefs: SharedPreferences) {
        preferences = prefs
    }

    /**
     * Gets the result of a map search
     */
    fun getCurrentSitesData(): LiveData<List<Site>?> {
        return currentSitesData
    }

    /**
     * Get BarentsWatch data about infection at a site
     * @param site the site to retrieve infection at
     */
    fun getBarentsWatchData(site: Site): LiveData<BarentsWatchAtSite?> {
        return barentsWatchData.getOrPut(site) {
            MutableLiveData()
        }
    }

    /**
     * Loads infectious pressure data into memory. Only needs to be called once in the app lifecycle
     */
    fun loadInfectiousPressure() {
        viewModelScope.launch(Dispatchers.IO) {
            val loaded = infectiousPressureRepository.getDefault()
            infectiousPressureData.postValue(loaded)
        }
    }

    /**
     * Load the infectious pressure at a given site
     * @param site the site to load infectious pressure at
     */
    fun loadInfectiousPressureTimeSeriesAtSite(site: Site) {
        viewModelScope.launch(Dispatchers.IO) {
            infectiousPressureTimeSeriesData.getOrPut(site) {
                MutableLiveData()
            }.postValue(
                infectiousPressureTimeSeriesRepository.getDataAtSite(
                    site,
                    Options.infectiousPressureTimeSeriesSpan
                )
            )
        }
    }

    /**
     * Load water information spesific to a site
     * @param site the site to load info at
     */
    fun loadNorKyst800AtSite(site: Site) {
        viewModelScope.launch(Dispatchers.IO) {
            norKyst800AtSiteData.getOrPut(site) {
                MutableLiveData()
            }.postValue(
                norKyst800AtSiteRepository.getDataAtSite(site)
            )
        }
    }

    /**
     * Load BarentsWatch data about infection at a site
     * @param site the site to load infection data at
     */
    fun loadBarentsWatch(site: Site) {
        viewModelScope.launch(Dispatchers.IO) {
            barentsWatchData.getOrPut(site) {
                MutableLiveData()
            }.postValue(
                barentsWatchRepository.getDataAtSite(site)
            )
        }
    }

    /**
     * Load sites in a given municipality
     * @param municipalityCode the municipality code, which is a string of 4 numbers ie. 0313
     */
    private fun loadSitesAtMunicipality(municipalityCode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val loaded = sitesRepository.getMunicipality(municipalityCode)
            municipalityData.postValue(loaded)
        }
    }

    /**
     * Read the user's favourite sites from persistent storage (disk)
     */
    fun loadFavouriteSites() {
        viewModelScope.launch(Dispatchers.IO) {
            val loaded = sitesRepository.getFavouriteSites(preferences.getStringSet(Options.FAVOURITES, null))
            favouriteSitesData.postValue(loaded)
        }
    }

    /**
     * Fetch the weather forecast at a site
     * @param site the site to load weather at
     */
    fun loadWeatherAtSite(site: Site) {
        viewModelScope.launch(Dispatchers.IO) {
            val forecast = weatherRepository.getWeatherForecast(site)
            weatherData.postValue(forecast)
        }
    }

    /**
     * Fetch sites at a location. This fetches sites in the municipality of the location
     */
    fun loadSitesAtLocation(location: LatLong) {
        viewModelScope.launch(Dispatchers.IO) {
            val municipalityNr = addressRepository.getMunicipalityNr(location)
            if (municipalityNr != null) {
                loadSitesAtMunicipality(municipalityNr)
            }
        }
    }

    /**
     * Processes a search string from the map.
     * input is numeric -> searches by municipality number
     * if not, searches municipality name first, then by site name
     * @param query whatever the user typed into the search box in the map
     */
    fun doMapSearch(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (query.matches(Regex("^[0-9]+\$"))) {
                // search is purely numeric, so this is probably municipality number
                loadSitesAtMunicipality(query)
            } else {
                // string search, check first if this is the name of a municipality
                val municipalityNr = addressRepository.searchMunicipalityNr(query)
                if (municipalityNr != null) {
                    loadSitesAtMunicipality(municipalityNr)
                } else {
                    // not a municipality, search for sites by name
                    val sites = sitesRepository.getSitesByName(query)
                    currentSitesData.postValue(sites)
                }
            }
        }
    }

    /**
     * Save a site as a favourite
     * @param site the site to save
     */
    fun registerFavouriteSite(site: Site) {
        favouriteSitesData.value.let { favouriteSites ->
            favouriteSites?.add(site)
            favouriteSitesData.postValue(favouriteSites)
        }
        preferences.edit().apply {
            val editStringSet = preferences.getStringSet(Options.FAVOURITES, emptySet())?.toMutableSet()
            if (editStringSet != null) {
                editStringSet.add(site.nr.toString())
                putStringSet(Options.FAVOURITES, editStringSet.toSet())
            }
            apply()
        }
    }

    /**
     * Remove a site from the favourites
     * @param site the site to remove
     */
    fun removeFavouriteSite(site: Site) {
        favouriteSitesData.value.let { favouriteSites ->
            favouriteSites?.remove(site)
            favouriteSitesData.postValue(favouriteSites)
        }
        preferences.edit().apply {
            val editStringSet = preferences.getStringSet(Options.FAVOURITES, emptySet())?.toMutableSet()
            if (editStringSet != null) {
                editStringSet.remove(site.nr.toString())
                putStringSet(Options.FAVOURITES, editStringSet.toSet())
            }
            apply()
        }
    }

    /**
     * Empties the cache so the app uses less memory
     */
    fun clearCache() {
        infectiousPressureRepository.clearCache()
        infectiousPressureTimeSeriesRepository.clearCache()
        sitesRepository.clearCache()
        norKyst800AtSiteRepository.clearCache()
        addressRepository.clearCache()
        weatherRepository.clearCache()
        barentsWatchRepository.clearCache()
    }
}