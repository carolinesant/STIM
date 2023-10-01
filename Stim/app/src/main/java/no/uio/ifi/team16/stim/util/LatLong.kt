package no.uio.ifi.team16.stim.util

import com.google.android.gms.maps.model.LatLng
import kotlin.math.abs

/**
 * A latitude/longitude-pair
 */
data class LatLong(val lat: Double, val lng: Double) {

    companion object {
        const val equalsDelta = 0.005

        fun fromGoogle(latLng: LatLng): LatLong {
            return LatLong(latLng.latitude, latLng.longitude)
        }
    }

    fun toGoogle(): LatLng {
        return LatLng(lat, lng)
    }

    override fun toString(): String {
        return "Latitude: $lat, longitude: $lng"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LatLong

        return abs(lat - other.lat) < equalsDelta && abs(lng - other.lng) < equalsDelta
    }

    override fun hashCode(): Int {
        var result = lat.hashCode()
        result = 31 * result + lng.hashCode()
        return result
    }
}

fun LatLng.asLatLong() = LatLong(this.latitude, this.longitude)
