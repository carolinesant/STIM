package no.uio.ifi.team16.stim.data

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.ImageView
import androidx.core.graphics.drawable.toDrawable
import com.bumptech.glide.Glide

/**
 * Fetches satelitte images of sites from the Google Maps Static API
 */
class StaticMapImageLoader(private val context: Context) {

    companion object {
        private const val IMAGE_WIDTH = 800
        private const val IMAGE_HEIGHT = 300
        private const val BASE_URL = "https://maps.google.com/maps/api/staticmap"
        private const val MAPS_API_KEY_METADATA = "com.google.android.geo.API_KEY"
    }

    /**
     * Load a site into a given imageview
     * @param site the site to load a picture of
     * @param imageView the imageview in which to display the image
     */
    fun loadSiteImage(site: Site, imageView: ImageView) {
        Glide.with(context)
            .load(getImageUrl(site))
            .placeholder(android.R.drawable.ic_menu_gallery.toDrawable())
            .error(android.R.drawable.ic_menu_gallery.toDrawable())
            .dontAnimate()
            .into(imageView)
    }

    /**
     * Google Maps API key
     */
    private val mapsApiKey by lazy {
        val info = context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA
        )
        info.metaData.getString(MAPS_API_KEY_METADATA)
    }

    /**
     * Get the URL for the API query
     */
    private fun getImageUrl(site: Site): String {
        return Uri.parse(BASE_URL).buildUpon().apply {
            appendQueryParameter("center", "${site.latLong.lat},${site.latLong.lng}")
            appendQueryParameter("zoom", "16")
            appendQueryParameter("size", "${IMAGE_WIDTH}x${IMAGE_HEIGHT}")
            appendQueryParameter("maptype", "satellite")
            appendQueryParameter("key", mapsApiKey)
        }.toString()
    }
}