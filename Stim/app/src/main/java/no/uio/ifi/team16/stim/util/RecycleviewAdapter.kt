package no.uio.ifi.team16.stim.util

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.ToggleButton
import androidx.recyclerview.widget.RecyclerView
import no.uio.ifi.team16.stim.R
import no.uio.ifi.team16.stim.data.Site
import no.uio.ifi.team16.stim.data.StaticMapImageLoader

/**
 * Recycleview showing a list of sites
 *
 * Uses the layout files:
 * Recycleview_element.xml
 * Recycleview.xml
 */
class RecycleViewAdapter(
    var sites: List<Site>,
    var favs: List<Site>,
    private val onClick: (Site) -> Unit,
    private val favOnClick: (Site, Boolean) -> Unit,
    val context: Context
) : RecyclerView.Adapter<RecycleViewAdapter.ViewHolder>() {

    private val imageLoader = StaticMapImageLoader(context)

    /**
     * A viewHolder representing a site
     */
    inner class ViewHolder(view: View, val onClick: (Site) -> Unit) : RecyclerView.ViewHolder(view) {
        val nameView: TextView = view.findViewById(R.id.textview_name)
        val locationView: TextView = view.findViewById(R.id.textview_location)
        val pictureView: ImageView = view.findViewById(R.id.imageView_overview)
        val favoriteButton: ToggleButton = view.findViewById(R.id.favoriteButton)

        private var site: Site? = null

        init {
            view.setOnClickListener { site?.let { onClick(it) } }
        }

        fun bind(s: Site) {
            site = s
        }
    }

    /**
     * Creates a new view, which defines the UI of the list item
     */
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.recycleview_element_overview, viewGroup, false)

        return ViewHolder(view, onClick)
    }

    /**
     * Puts data about a site into a view
     */
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        val site = sites[position]

        viewHolder.bind(site)
        viewHolder.nameView.text = site.name

        site.placement?.let { placement ->
            viewHolder.locationView.text = context.getString(R.string.site_location, placement.municipalityName)
        }

        imageLoader.loadSiteImage(site, viewHolder.pictureView)

        viewHolder.favoriteButton.isChecked = favs.contains(site)

        viewHolder.favoriteButton.setOnClickListener {
            favOnClick(site, viewHolder.favoriteButton.isChecked)
        }
    }

    /**
     * Returns the number of sites in the list
     */
    override fun getItemCount() = sites.size
}