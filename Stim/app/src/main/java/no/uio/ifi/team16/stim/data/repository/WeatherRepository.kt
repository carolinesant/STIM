package no.uio.ifi.team16.stim.data.repository

import no.uio.ifi.team16.stim.data.Site
import no.uio.ifi.team16.stim.data.WeatherForecast
import no.uio.ifi.team16.stim.data.dataLoader.WeatherDataLoader

/**
 * Entry point for the MET LocationForecast API
 */
class WeatherRepository {

    private val dataSource = WeatherDataLoader()
    private val cache: MutableMap<Site, WeatherForecast?> = mutableMapOf()

    /**
     * Fetch the weather forecast at a given site
     * @param site the site to load the weather forecast into
     */
    suspend fun getWeatherForecast(site: Site): WeatherForecast? {
        var forecast = cache[site] ?: site.weatherForecast

        if (forecast == null) {
            forecast = dataSource.load(site.latLong)
            cache[site] = forecast
            site.weatherForecast = forecast
        }

        return forecast
    }

    /**
     * Empties the cache. Call this in case of low memory warning
     */
    fun clearCache() {
        cache.clear()
    }
}