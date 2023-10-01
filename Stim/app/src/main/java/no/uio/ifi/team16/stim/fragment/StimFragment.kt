package no.uio.ifi.team16.stim.fragment

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment

/**
 * Base class for all fragments in the app
 */
abstract class StimFragment : Fragment() {

    /**
     * Check if you can use the user's location
     */
    fun checkLocationPermission(): Boolean {
        activity?.let {
            val fineLocation = ActivityCompat.checkSelfPermission(it, Manifest.permission.ACCESS_COARSE_LOCATION)
            val coarseLocation = ActivityCompat.checkSelfPermission(it, Manifest.permission.ACCESS_COARSE_LOCATION)
            return fineLocation == PackageManager.PERMISSION_GRANTED || coarseLocation == PackageManager.PERMISSION_GRANTED
        }
        return false
    }

    /**
     * Ask for permission to show user location
     */
    fun requestLocationPermission(callback: (result: Boolean) -> Unit) {
        val contract = ActivityResultContracts.RequestMultiplePermissions()
        val request = registerForActivityResult(contract) { permissions ->
            when {
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                    callback(true)
                }
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                    callback(true)
                }
                else -> callback(false)
            }
        }

        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        request.launch(permissions)
    }

    /**
     * Closes the keyboard
     */
    protected fun closeKeyboard() {
        view?.let {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }
}