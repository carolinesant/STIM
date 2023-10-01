package no.uio.ifi.team16.netcdf

class NativeLib {

    /**
     * A native method that is implemented by the 'netcdf' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'netcdf' library on application startup.
        init {
            System.loadLibrary("netcdf")
        }
    }
}