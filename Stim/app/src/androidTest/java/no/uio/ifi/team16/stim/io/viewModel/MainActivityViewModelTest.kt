package no.uio.ifi.team16.stim.io.viewModel

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import no.uio.ifi.team16.stim.MainActivityViewModel
import no.uio.ifi.team16.stim.data.Site
import no.uio.ifi.team16.stim.util.Options
import org.junit.Assert.*
import org.junit.Test
import kotlin.system.measureTimeMillis

/**
 * Integrasjonstest av ViewModel
 */
class MainActivityViewModelTest {

    private val viewModel = MainActivityViewModel()

    @Test
    fun testMapSearch() {
        // Søk etter kommunenr
        viewModel.doMapSearch("3004")
        Thread.sleep(1000)
        assertEquals("3004", viewModel.getMunicipalityData().value?.id)
        assertEquals("Fredrikstad", viewModel.getMunicipalityData().value?.sites?.get(0)?.placement?.municipalityName)

        // Søk etter kommunenavn
        viewModel.doMapSearch("Fitjar")
        Thread.sleep(1000)
        assertEquals("4615", viewModel.getMunicipalityData().value?.id)
        assertEquals("Fitjar", viewModel.getMunicipalityData().value?.sites?.get(0)?.placement?.municipalityName)

        //assertnotequals
        // Søk etter navn på anlegg
        viewModel.doMapSearch("Klubben")
        Thread.sleep(1000)
        assertEquals(true, viewModel.getCurrentSitesData().value?.get(0)?.name?.contains("KLUBBEN"))

    }

    @Test
    fun testResponstidSokAnlegg() {
        // Søk etter site navn
        val time=measureTimeMillis{
            viewModel.doMapSearch("Torangskjeret")
            Thread.sleep(1000)
            assertNotNull(viewModel.getCurrentSitesData().value?.get(0)!!)
        }
        assertTrue(time<2000)


    }

    @Test
    fun testFavourites() {
        // Sett opp sharedPreferences
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val sharedPreferences = appContext.getSharedPreferences(Options.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
        viewModel.setPreferences(sharedPreferences)
        viewModel.loadFavouriteSites()

        val site = getTestSite()

        // Lagre som favoritt
        viewModel.registerFavouriteSite(site)
        assertEquals(true, viewModel.getFavouriteSitesData().value?.contains(site))

        // Fjern som favoritt
        viewModel.removeFavouriteSite(site)
        assertEquals(false, viewModel.getFavouriteSitesData().value?.contains(site))
    }

    private fun getTestSite(): Site {
        viewModel.doMapSearch("Klubben")
        Thread.sleep(1000)
        return viewModel.getCurrentSitesData().value?.get(0)!!
    }
}