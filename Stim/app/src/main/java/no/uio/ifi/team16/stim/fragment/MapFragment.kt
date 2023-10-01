package no.uio.ifi.team16.stim.fragment

import android.annotation.SuppressLint
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.maps.android.heatmaps.HeatmapTileProvider
import no.uio.ifi.team16.stim.MainActivityViewModel
import no.uio.ifi.team16.stim.R
import no.uio.ifi.team16.stim.data.Municipality
import no.uio.ifi.team16.stim.data.Site
import no.uio.ifi.team16.stim.databinding.FragmentMapBinding
import no.uio.ifi.team16.stim.util.LatLong
import no.uio.ifi.team16.stim.util.RecycleViewAdapter
import kotlin.math.round
import kotlin.math.roundToInt

/**
 * Map fragment
 */
class MapFragment : StimFragment(), OnMapReadyCallback, GoogleMap.OnCameraMoveListener {

    companion object {
        private const val TAG = "MapFragment"
        private const val MIN_ZOOM_FOR_MAP_SEARCH = 6F
    }

    private lateinit var map: GoogleMap
    private lateinit var binding: FragmentMapBinding
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private var searchView: SearchView? = null
    private var locationClient: FusedLocationProviderClient? = null
    private var tileOverlay: TileOverlay? = null
    private val viewModel: MainActivityViewModel by activityViewModels()
    private var mapReady = false
    private var zoomLevel = 12F
    private val markerMap: MutableMap<Marker, Site> = mutableMapOf()
    private var usingHeatmap = false
    private var doSiteSearchOnMovement = true

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        if (!checkLocationPermission()) {
            requestLocationPermission { hasPermission ->
                if (hasPermission) {
                    onLocationPermissionGranted()
                }
            }
        } else {
            onLocationPermissionGranted()
        }

        binding = FragmentMapBinding.inflate(layoutInflater)

        if (!mapReady) {
            // map not created yet, initialize it
            val mapFragment = childFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment
            requireActivity().supportFragmentManager.beginTransaction().add(R.id.mapView, mapFragment).commit()
            mapFragment.getMapAsync(this)
        }

        // Observe municipality
        viewModel.getMunicipalityData().observe(viewLifecycleOwner, this::onMunicipalityUpdate)

        // Observe individual site (searched for by name)
        viewModel.getCurrentSitesData().observe(viewLifecycleOwner, this::onSiteSearchComplete)

        //Bottom Sheet behavior
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        bottomSheetBehavior.setPeekHeight(400, true)
        bottomSheetBehavior.isDraggable = true
        bottomSheetBehavior.isHideable = false

        // Max height of bottom sheet is 85% of screen height
        val screenHeight = Resources.getSystem().displayMetrics.heightPixels
        bottomSheetBehavior.maxHeight = round(screenHeight * 0.85).toInt()

        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        val adapter =
                RecycleViewAdapter(
                        listOf(),
                        listOf(),
                        this::adapterOnClick,
                        this::favoriteOnClick,
                        requireActivity()
                )
        binding.recyclerView.adapter = adapter

        setHasOptionsMenu(true)

        binding.heatMapButton.setOnClickListener {
            usingHeatmap = if (usingHeatmap) {
                tileOverlay?.remove()
                false
            } else {
                drawHeatmap(map)
                true
            }
        }


        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_toolbar, menu)
        val search = menu.findItem(R.id.search)
        val searchView = search.actionView as SearchView
        searchView.queryHint = getString(R.string.search_here)
        searchView.setIconifiedByDefault(false)
        searchView.setBackgroundResource(R.drawable.searchbar_background)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            /**
             * Called when the user searches for something
             */
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null && query.isNotBlank()) {
                    doSiteSearchOnMovement = false
                    closeKeyboard()
                    map.clear()
                    markerMap.clear()
                    viewModel.doMapSearch(query)
                    return true
                }
                return false
            }

            /**
             * Called on text input in search field, does nothing
             */
            override fun onQueryTextChange(newText: String): Boolean {
                return false
            }
        })

        this.searchView = searchView
        super.onCreateOptionsMenu(menu, inflater)
    }

    /**
     * Called when the map is visible
     */
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        mapReady = true

        map.setOnCameraMoveListener(this::onCameraMove)
        map.setOnCameraIdleListener(this::onCameraIdle)
        map.setOnCameraMoveStartedListener(this::onCameraMoveStarted)
        map.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style))
        map.uiSettings.isMyLocationButtonEnabled = false

        // Move camera to initial position (southern Norway)
        map.moveCamera(getInitialCameraPosition())

        if (checkLocationPermission()) {
            map.isMyLocationEnabled = true
            setMapToUserLocation()
        }

        closeKeyboard()
    }

    /**
     * Called when a site has been searched for and found
     */
    private fun onSiteSearchComplete(sites: List<Site>?) {
        if (sites != null) {
            onSiteUpdate(sites)
            val adapter =
                    RecycleViewAdapter(
                            sites,
                            listOf(),
                            this::adapterOnClick,
                            this::favoriteOnClick,
                            requireActivity()
                    )
            binding.recyclerView.adapter = adapter
            bottomSheetBehavior.isDraggable = true
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED

            binding.openHeaderBottomsheet.kommuneText.text = getString(R.string.blank)

            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(sites[0].latLong.toGoogle(), zoomLevel)
            map.animateCamera(cameraUpdate)
        }
    }

    /**
     * Called when the sites in a municipality has been loaded
     */
    private fun onMunicipalityUpdate(municipality: Municipality?) {
        if (mapReady && municipality != null && municipality.sites.isNotEmpty()) {
            onSiteUpdate(municipality.sites)

            if (!doSiteSearchOnMovement) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
                if (municipality.sites.size > 1) {
                    // Move camera to fit all sites
                    try {
                        val bounds = LatLngBounds.builder()
                        for (site in municipality.sites) {
                            bounds.include(site.latLong.toGoogle())
                        }
                        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 200))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error occured while animating map: ", e)
                    }
                } else {
                    // One site, move to it
                    val site = municipality.sites[0]
                    val cameraUpdate = CameraUpdateFactory.newLatLngZoom(site.latLong.toGoogle(), zoomLevel)
                    map.animateCamera(cameraUpdate)
                }
            }

            var favSites = emptyList<Site>()
            viewModel.getFavouriteSitesData().observe(viewLifecycleOwner) {
                if (it != null) {
                    favSites = it.toList()
                }
            }
            val adapter = RecycleViewAdapter(
                    municipality.sites,
                    favSites,
                    this::adapterOnClick,
                    this::favoriteOnClick,
                    requireActivity()
            )
            binding.recyclerView.adapter = adapter

            //get municipality name in bottom header
            val firstSite = municipality.sites[0]
            binding.openHeaderBottomsheet.kommuneText.text = firstSite.placement?.municipalityName
        }
    }

    /**
     * Called when the ViewModel has loaded some sites
     */
    private fun onSiteUpdate(sites: List<Site>?) {
        if (sites != null && mapReady) {
            for (site in sites) {
                val markerOptions = MarkerOptions()
                markerOptions.title(site.name)
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.fish_marker))
                markerOptions.position(site.latLong.toGoogle())
                val marker = map.addMarker(markerOptions)
                if (marker != null) {
                    markerMap[marker] = site
                }
            }
        }

        map.setOnInfoWindowClickListener(this::onMarkerClick)
        binding.openHeaderBottomsheet.infoText.text = sites?.size.toString()
        searchView?.setQuery("", false)
        searchView?.clearFocus()
    }

    /**
     * Called when the user starts moving the map
     */
    private fun onCameraMoveStarted(reason: Int) {
        if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            closeKeyboard()
            doSiteSearchOnMovement = true
        }
    }

    /**
     * Called when the user has stopped moving the map
     */
    private fun onCameraIdle() {
        if (doSiteSearchOnMovement && zoomLevel >= MIN_ZOOM_FOR_MAP_SEARCH) {
            val center = LatLong.fromGoogle(map.cameraPosition.target)
            viewModel.loadSitesAtLocation(center)
        }
        if (usingHeatmap) {
            tileOverlay?.remove()
            drawHeatmap(map)
        }
    }

    /**
     * When an item in the recyclerview is clicked, navigate to that site
     */
    private fun adapterOnClick(site: Site) {
        closeKeyboard()
        viewModel.site = site
        view?.findNavController()?.navigate(R.id.action_mapFragment_to_siteInfoFragment)
    }

    /**
     * Called when a marker is clicked
     */
    private fun onMarkerClick(marker: Marker): Boolean {
        val site = markerMap[marker]

        if (site != null) {
            closeKeyboard()
            viewModel.site = site
            view?.findNavController()?.navigate(R.id.action_mapFragment_to_siteInfoFragment)
            return true
        }

        return false
    }

    /**
     * Adds or removes a site from favourites
     */
    private fun favoriteOnClick(site: Site, checked: Boolean) {
        if (checked) {
            viewModel.registerFavouriteSite(site)
        } else {
            viewModel.removeFavouriteSite(site)
        }
    }

    /**
     * Get a view of southern Norway
     */
    private fun getInitialCameraPosition(): CameraUpdate {
        val coordinates = LatLng(61.42888648306541, 8.68770383298397)
        return CameraUpdateFactory.newLatLngZoom(coordinates, 5.5639253F)
    }

    /**
     * Called when the map is moved
     */
    override fun onCameraMove() {
        zoomLevel = map.cameraPosition.zoom
    }

    /**
     * Called when access to user location is granted
     */
    private fun onLocationPermissionGranted() {
        locationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    /**
     * Set the map to the users current location
     */
    @SuppressLint("MissingPermission")
    fun setMapToUserLocation() {
        locationClient?.let { client ->
            client.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel))

                    val position = LatLong(location.latitude, location.longitude)
                    viewModel.loadSitesAtLocation(position)
                }
            }
        }
    }

    /**
     * Draw a heatmap of infectious pressure
     */
    private fun drawHeatmap(googleMap: GoogleMap) {
        viewModel.getInfectiousPressureData().observe(viewLifecycleOwner) { infectiousPressure ->
            val z = googleMap.cameraPosition.zoom
            val screenBound = googleMap.projection.visibleRegion.latLngBounds

            val n = if (z > 7) {
                1
            } else {
                (1 + 20 * (z - 9.884714) / (4.992563 - 9.884714)).coerceAtLeast(1.0).toInt()
            }

            val scale = 50.0
            infectiousPressure?.let {
                val data = it.getHeatMapData(screenBound, n)
                if (data.isNotEmpty()) {
                    tileOverlay?.remove()
                    val heatMapProvider = HeatmapTileProvider.Builder()
                            .weightedData(data) // load our weighted data
                            .radius((scale).roundToInt()) // finn måte å gjøre om til 800x800 m
                            .build()
                    tileOverlay = googleMap.addTileOverlay(
                            TileOverlayOptions().tileProvider(heatMapProvider)
                    )
                }
            }
        }
    }
}