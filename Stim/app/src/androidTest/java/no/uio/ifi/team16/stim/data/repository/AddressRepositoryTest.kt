package no.uio.ifi.team16.stim.data.repository

import kotlinx.coroutines.runBlocking
import no.uio.ifi.team16.stim.util.LatLong
import org.junit.Assert.assertEquals
import org.junit.Test

class AddressRepositoryTest {

    private val repository = AddressRepository()

    @Test
    fun getMunicipalityNr() {
        runBlocking {
            val testPosition = LatLong(60.0, 10.0)
            val municipalityNr = repository.getMunicipalityNr(testPosition)
            assertEquals("3047", municipalityNr)
        }
    }
}