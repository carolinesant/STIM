package no.uio.ifi.team16.stim.data.repository

import kotlinx.coroutines.runBlocking
import no.uio.ifi.team16.stim.util.Options
import org.junit.Assert.assertNotNull
import org.junit.Test

class WeatherRepositoryTest {

    private val repository = WeatherRepository()

    @Test
    fun getData() {
        runBlocking {
            val weather = repository.getWeatherForecast(Options.fakeSite)
            assertNotNull(weather)
        }
    }
}