package no.uio.ifi.team16.stim.data.dataLoader

import android.util.Log
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitString
import no.uio.ifi.team16.stim.data.*
import no.uio.ifi.team16.stim.util.LatLong
import org.json.JSONObject
import java.time.ZonedDateTime

/**
 * Loads weather using the LocationForecast API
 */
class WeatherDataLoader {

    companion object {
        private const val TAG = "WeatherDataLoader"

        /**
         * URL to the API
         */
        private const val BASE_URL = "https://in2000-apiproxy.ifi.uio.no/weatherapi/locationforecast/2.0/compact"

        /**
         * Wind speed in m/s that defines a storm according to Beaufort's scale
         */
        private const val STORM_THRESHOLD = 20.0
    }

    /**
     * Loads the weather forecast at a given position
     *
     * @param position a position
     */
    suspend fun load(position: LatLong): WeatherForecast? {

        val lat = "lat" to position.lat
        val lon = "lon" to position.lng

        val responseStr = Fuel.get(BASE_URL, listOf(lat, lon)).awaitString()

        if (responseStr.isEmpty()) {
            Log.e(TAG, "No response from weather API at $position")
            return null
        }

        val response = JSONObject(responseStr)
        val properties = response.getJSONObject("properties")
        val timeseries = properties.getJSONArray("timeseries")

        if (timeseries.length() == 0) {
            Log.e(TAG, "No weather found at location $position")
            return null
        }

        // List of wind speeds at each day
        val windSpeeds = mutableListOf<Pair<Weekday, Double>>()

        val first = parseWeatherFromTimeseries(timeseries.getJSONObject(0), windSpeeds)

        val now = ZonedDateTime.now()
        val nextThreeDays = mutableListOf<Weather>()
        for (i in 0 until timeseries.length()) {
            if (nextThreeDays.size == 3) break

            val series = timeseries.getJSONObject(i)
            val time = ZonedDateTime.parse(series.getString("time"))

            if (time.hour != 12 || time.dayOfMonth == now.dayOfMonth) continue

            nextThreeDays.add(parseWeatherFromTimeseries(series, windSpeeds))
        }

        val forecast = WeatherForecast(first, nextThreeDays[0], nextThreeDays[1], nextThreeDays[2])
        for (windSpeed in windSpeeds) {
            val (day, speed) = windSpeed
            if (speed > STORM_THRESHOLD) {
                forecast.storm = Storm(day, speed)
                break
            }
        }

        return forecast
    }

    /**
     * Create a Weather object from one part of the forecast
     */
    private fun parseWeatherFromTimeseries(
        timeseries: JSONObject,
        windSpeeds: MutableList<Pair<Weekday, Double>>
    ): Weather {
        val data = timeseries.getJSONObject("data")
        val details = data.getJSONObject("instant").getJSONObject("details")
        val temperature = details.getDouble("air_temperature")

        val next12Hours = data.getJSONObject("next_12_hours").getJSONObject("summary")
        val icon = WeatherIcon.fromMetName(next12Hours.getString("symbol_code"))

        val weekday = Weekday.fromISOString(timeseries.getString("time"))

        val windSpeed = details.getDouble("wind_speed")
        windSpeeds.add(Pair(weekday, windSpeed))

        return Weather(temperature, icon, weekday)
    }
}