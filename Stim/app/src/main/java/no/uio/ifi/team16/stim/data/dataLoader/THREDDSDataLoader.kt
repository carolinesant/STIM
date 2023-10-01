package no.uio.ifi.team16.stim.data.dataLoader

//import ucar.nc2.dataset.NetcdfDatasets
import android.util.Log
import org.locationtech.proj4j.CRSFactory
import org.locationtech.proj4j.CoordinateTransform
import org.locationtech.proj4j.CoordinateTransformFactory
import ucar.ma2.InvalidRangeException
import ucar.nc2.Variable
import ucar.nc2.dataset.NetcdfDataset
import java.io.IOException

/**
 * ABSTRACT CLASS FOR THREDDS DATALOADERS
 *
 * Handles common methods for THREDDS datasources, specifically infectiouspressure. Not
 * used by Norkyst800AtSite since another loading procedure is used
 *
 * Used to read projecting from a netcdfVariable and to load from a thredds catalog.
 */
abstract class THREDDSDataLoader {
    companion object {
        const val TAG = "THREDDSDataLoader"
    }

    private val maxX = 901
    private val maxY = 2601
    protected val separation = 800  //separation(in meters) between points(in real life)

    //ranges of the NorKyst800(and smittepress) THREDDS dataset
    private val minLongitude = 0
    private val maxLongitude = 90
    private val longitudeDiff = maxLongitude - minLongitude
    private val minLatitude = 0
    private val maxLatitude = 90
    private val latitudeDiff = maxLatitude - minLatitude

    /////////////
    // LOADERS //
    /////////////
    /**
     * general method for opening THREDDS file.
     * @param url: url of data to open
     * @param action: action to perform on the opened file, must return a representation of the data
     * @return the result of the action on the netcdf file, f.ex. an infectiousPressure-object
     */
    inline fun <D> threddsLoad(url: String, action: (NetcdfDataset) -> D?): D? {
        var ncfile: NetcdfDataset? = null
        var data: D? = null
        try {
            ncfile = NetcdfDataset.openDataset(url) ?: return null
            data = action(ncfile)
        } catch (e: IOException) {
            Log.e("ERROR", e.toString())
        } catch (e: InvalidRangeException) {
            Log.e("ERROR", e.toString())
        } catch (e: NullPointerException) {
            Log.e(
                TAG,
                "ERROR: a Variable might be read as null, are you sure you are using the correct url/dataset?$e"
            )
            Log.e("ERROR", e.toString())
        } catch (e: Exception) {
            Log.e(TAG, "didnt catch this one! $e")
        } finally {
            ncfile?.close()
        }
        return data
    }

    ////////////////////////
    // UTILITIES - NETCDF //
    ////////////////////////
    /**
     * for a netcdfdataset with a gridmappingvariable, read out the projection
     */
    protected fun readAndMakeProjectionFromGridMapping(gridMapping: Variable): CoordinateTransform {
        val crsFactory = CRSFactory()
        val stereoCRT = crsFactory.createFromParameters(
            null,
            gridMapping.findAttribute("proj4string")?.stringValue
                ?: throw NullPointerException("Failed to read attribute <proj4string> from <gridMapping> from infectiousPressure")
        )
        val latLngCRT = stereoCRT.createGeographic()
        val ctFactory = CoordinateTransformFactory()
        return ctFactory.createTransform(latLngCRT, stereoCRT)
    }
}