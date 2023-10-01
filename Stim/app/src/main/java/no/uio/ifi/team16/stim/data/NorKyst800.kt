package no.uio.ifi.team16.stim.data

import no.uio.ifi.team16.stim.util.FloatArray1D
import no.uio.ifi.team16.stim.util.NullableFloatArray4D
import no.uio.ifi.team16.stim.util.prettyPrint
import org.locationtech.proj4j.CoordinateTransform

/**
 * data from the NorKyst800 model.
 */
data class NorKyst800(
    val depth: FloatArray1D,
    val salinity: NullableFloatArray4D,
    val temperature: NullableFloatArray4D,
    val time: FloatArray1D,
    val velocity: Triple<NullableFloatArray4D, NullableFloatArray4D, NullableFloatArray4D>,
    val projection: CoordinateTransform, //transforms between latlong and projection coordinates
) {
    companion object {
        const val TAG = "NORKYST800"
    }

    ///////////////
    // UTILITIES //
    ///////////////
    override fun toString() =
        "NorKyst800: \n" +
                "\tdepth: $depth\n" +
                "\ttime: $time\n" +
                "\tsalinity: ${salinity.prettyPrint()}" +
                "\ttemperature: ${temperature.prettyPrint()}" +
                "\tvelocity.x: ${velocity.first.prettyPrint()}" +
                "\tvelocity.y: ${velocity.second.prettyPrint()}" +
                "\tvelocity.z: ${velocity.third.prettyPrint()}"

    //join two datasets at time dimension. Assumess all other dimensions of equal size
    fun append(other: NorKyst800): NorKyst800 =
        NorKyst800(
            depth,
            salinity.plus(other.salinity),
            temperature.plus(other.temperature),
            time.plus(other.time),
            Triple(
                velocity.first.plus(other.velocity.first),
                velocity.second.plus(other.velocity.second),
                velocity.third.plus(other.velocity.third)
            ),
            projection
        )


    ////////////////////
    // AUTO-GENERATED //
    ////////////////////
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NorKyst800

        if (!depth.contentEquals(other.depth)) return false
        if (!salinity.contentDeepEquals(other.salinity)) return false
        if (!temperature.contentDeepEquals(other.temperature)) return false
        if (!time.contentEquals(other.time)) return false
        if (velocity != other.velocity) return false
        if (projection != other.projection) return false

        return true
    }

    override fun hashCode(): Int {
        var result = depth.contentHashCode()
        result = 31 * result + salinity.contentDeepHashCode()
        result = 31 * result + temperature.contentDeepHashCode()
        result = 31 * result + time.contentHashCode()
        result = 31 * result + velocity.hashCode()
        result = 31 * result + projection.hashCode()
        return result
    }
}