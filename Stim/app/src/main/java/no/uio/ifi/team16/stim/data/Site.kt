package no.uio.ifi.team16.stim.data

import no.uio.ifi.team16.stim.util.LatLong

/**
 * A municipality
 * @property id the municipality number
 * @property sites a list of sites in this municipality
 */
data class Municipality(val id: String, val sites: List<Site>)

/**
 * Class containing the placement of a site
 * @property municipalityCode ID of municipality, i.e 0301 for Oslo
 * @property municipalityName name of municipality
 * @property countyCode ID of surrounding county
 */
data class AreaPlacement(
    val municipalityCode: Int,
    val municipalityName: String,
    val countyCode: Int
)

/**
 * Production capacity of a site
 * @property value the value
 * @property unit the unit of the value (usually tons)
 */
data class Capacity(
    val value: Double,
    val unit: String
) {
    override fun toString(): String {
        if (value > 0) {
            return "$value $unit"
        }
        return "-----"
    }
}

/**
 * A site (oppdrettsanlegg)
 * @property nr unique ID
 * @property name name of site
 * @property latLong position on the map
 * @property placement object with info about municipality nr/name
 * @property capacity capacity, and object with value and unit
 * @property placementType onshore / offshore
 * @property waterType salt/fresh water
 */
data class Site(
    val nr: Int,
    val name: String,
    val latLong: LatLong,
    val placement: AreaPlacement?,
    val capacity: Capacity,
    val placementType: String?,
    val waterType: String?
) {
    /**
     * Current weather forecast at site, null if not loaded yet
     */
    var weatherForecast: WeatherForecast? = null

    override fun hashCode(): Int {
        return nr.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is Site) {
            return this.nr == other.nr
        }
        return false
    }
}