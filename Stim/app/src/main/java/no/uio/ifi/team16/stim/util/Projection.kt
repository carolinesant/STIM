package no.uio.ifi.team16.stim.util

import org.locationtech.proj4j.CoordinateTransform
import org.locationtech.proj4j.ProjCoordinate

/**
 * Extensions for CoordinateTransform to project from latlngs to indexes(when rounded!) in a grid
 */
fun CoordinateTransform.project(latLng: LatLong): Pair<Float, Float> =
    transform(ProjCoordinate(latLng.lng, latLng.lat), ProjCoordinate(0.0, 0.0)).let { p ->
        Pair(p.x.toFloat(), p.y.toFloat())
    }

fun CoordinateTransform.projectXY(xy: Pair<Float, Float>): LatLong =
    transform(
        ProjCoordinate(xy.first.toDouble(), xy.second.toDouble()),
        ProjCoordinate(0.0, 0.0)
    ).let { p ->
        LatLong(p.y, p.x)
    }