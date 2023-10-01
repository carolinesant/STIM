package no.uio.ifi.team16.stim.fragment

import android.os.Bundle
import android.view.*
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.get
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import no.uio.ifi.team16.stim.MainActivityViewModel
import no.uio.ifi.team16.stim.R
import no.uio.ifi.team16.stim.data.Site
import no.uio.ifi.team16.stim.data.WeatherForecast
import no.uio.ifi.team16.stim.databinding.FragmentSiteInfoBinding
import no.uio.ifi.team16.stim.util.InfectionStatusCalculator

/**
 * Fragment showing info about a site, including:
 * - General info
 * - Water info
 * - Infection
 * - Weather and storm forecast
 */
class SiteInfoFragment : StimFragment() {

    private lateinit var binding: FragmentSiteInfoBinding
    private lateinit var site: Site
    private val viewModel: MainActivityViewModel by activityViewModels()
    private var checkIfFavorite = false

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        binding = FragmentSiteInfoBinding.inflate(inflater, container, false)
        site = viewModel.site ?: return binding.root

        // Check if site is favorited
        if (viewModel.getFavouriteSitesData().value?.contains(site) == true) checkIfFavorite = true

        // load data pertaining to this site
        viewModel.loadNorKyst800AtSite(site)
        viewModel.loadInfectiousPressureTimeSeriesAtSite(site)
        viewModel.loadBarentsWatch(site)
        viewModel.loadWeatherAtSite(site)

        // fill inn data not requiring loads
        binding.LoadingScreen.loadingLayout.visibility = View.VISIBLE //show loading screen
        binding.siteName.text = site.name
        binding.temperatureToday.text = getString(R.string.temperature, site.weatherForecast?.first?.temperature)
        binding.temperatureTomorrow.text = getString(R.string.temperature, site.weatherForecast?.second?.temperature)
        binding.positionView.text = getString(
                R.string.Location_placeholder,
                site.latLong.lat.toFloat(),
                site.latLong.lng.toFloat()
        )

        // weatherdata
        viewModel.getWeatherData().observe(viewLifecycleOwner, this::onWeatherLoaded)

        // barentswatchdata
        setBarrentsWatchInfo()

        // set info to cards
        setWaterInfo()
        setInfectionInfo()

        // infocards
        setExpandableGeneralInfo()

        setWeatherInfoCard()

        // Stop the user if data not loaded
        binding.waterInfoCard.setOnClickListener {
            val newFragment = NoDataStatusDialogFragment()
            newFragment.show(parentFragmentManager, "nodata")
        }

        binding.infectionInfoCard.setOnClickListener {
            val newFragment = NoDataStatusDialogFragment()
            newFragment.show(parentFragmentManager, "nodata")
        }

        setHasOptionsMenu(true)

        // remove loading screen if ANY of norkyst800, barentsWatch or infectiousPressure are loaded for this site
        viewModel.getNorKyst800AtSiteData(site).observe(viewLifecycleOwner) {
            binding.LoadingScreen.loadingLayout.visibility = View.GONE
            setWaterInfoCard()
        }
        viewModel.getInfectiousPressureTimeSeriesData(site).observe(viewLifecycleOwner) {
            binding.LoadingScreen.loadingLayout.visibility = View.GONE
            setInfectionInfoCard()
        }
        viewModel.getBarentsWatchData(site).observe(viewLifecycleOwner) {
            binding.LoadingScreen.loadingLayout.visibility = View.GONE
            setInfectionInfoCard()
        }

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.site_info_toolbar, menu)
        val item = menu[0]
        if (checkIfFavorite) item.setIcon(R.drawable.heart)
        else item.setIcon(R.drawable.heart_outline)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.toString()) {
            "Fav" -> {
                if (checkIfFavorite) item.setIcon(R.drawable.heart_outline)
                else item.setIcon(R.drawable.heart)
                checkIfFavorite = !checkIfFavorite
                favoriteOnClick(site, checkIfFavorite)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun favoriteOnClick(site : Site, checked : Boolean) {
        if (checked) viewModel.registerFavouriteSite(site)
        else viewModel.removeFavouriteSite(site)
    }

    private fun onWeatherLoaded(forecast: WeatherForecast?) {
        forecast?.apply {
            binding.weatherToday.setImageDrawable(first.icon.asDrawable(requireContext()))
            binding.weatherTomorrow.setImageDrawable(second.icon.asDrawable(requireContext()))
            binding.temperatureToday.text = getString(R.string.temperature, first.temperature)
            binding.temperatureTomorrow.text = getString(R.string.temperature, second.temperature)

            forecast.storm?.let { storm ->
                if (storm.day.isToday()) {
                    binding.stormSignal.text = getString(R.string.storm_today)
                } else {
                    binding.stormSignal.text =
                            getString(R.string.storm_at, storm.day.getTranslation(requireContext()))
                }
                binding.stormWeather.setImageDrawable(
                        ResourcesCompat.getDrawable(
                                resources,
                                R.drawable.storm, null
                        )
                )
            }
        }
    }

    private fun setWaterInfo() {
        binding.tempratureValue
        binding.tempratureValue.text = "..."
        binding.salinityValue.text = "..."
        binding.currentsValue.text = "..."
        viewModel.getNorKyst800AtSiteData(site).observe(viewLifecycleOwner) {
            it?.apply { //succesfully loaded data
                //but there might be no data around!
                val temp = getTemperature()
                val salt = getSalinity()
                val strom = getVelocity()
                if (temp != null) {
                    binding.tempratureValue.text = getString(R.string.Temperature_placeholder, temp)
                } else { // data is loaded, but no information in this area
                    binding.tempratureValue.text = getString(R.string.no_info)
                }
                if (salt != null) {
                    binding.salinityValue.text = getString(R.string.Salt_placeholder, salt)
                } else { // data is loaded, but no information in this area
                    binding.salinityValue.text = getString(R.string.no_info)
                }
                if (strom != null) {
                    binding.currentsValue.text = getString(R.string.Currents_placeholder, strom)
                } else { // data is loaded, but no information in this area
                    binding.currentsValue.text = getString(R.string.no_info)
                }
            } ?: run {
                binding.tempratureValue.text = getString(R.string.no_info)
                binding.salinityValue.text = getString(R.string.no_info)
                binding.currentsValue.text = getString(R.string.no_info)
            }
        }
    }

    private fun setInfectionInfo() {
        val statuscalculator = InfectionStatusCalculator(resources)

        viewModel.getInfectiousPressureTimeSeriesData(site).observe(viewLifecycleOwner) {
            it?.observeConcentrations(viewLifecycleOwner) { _, infectiondata ->
                binding.dangerSalmonLouse.setImageDrawable(
                        statuscalculator.calculateInfectionStatusIcon(
                                infectiondata.toTypedArray()
                        )
                )
                binding.dangerSalmonLouse.contentDescription =
                        statuscalculator.calculateInfectionStatusText(infectiondata.toTypedArray())
                binding.dangerSalmonLouse.setImageDrawable(
                        statuscalculator.calculateInfectionStatusIcon(
                                infectiondata.toTypedArray()
                        )
                )
            } ?: run { //failed to load InfPRTS:
                binding.dangerSalmonLouse.setImageDrawable(
                        ResourcesCompat.getDrawable(
                                resources,
                                R.drawable.no_data,
                                null
                        )
                )
                binding.dangerSalmonLouse.contentDescription = resources.getText(R.string.No_infection_found)
            }
        }
    }

    private fun setBarrentsWatchInfo() {
        viewModel.getBarentsWatchData(site).observe(viewLifecycleOwner) {
            if (it?.listPD?.isNotEmpty() == true) {
                binding.pdIcon.setImageDrawable(
                        ResourcesCompat.getDrawable(
                                resources,
                                R.drawable.danger,
                                null
                        )
                )
            }
            if (it?.listILA?.isNotEmpty() == true) {
                binding.ilaIcon.setImageDrawable(
                        ResourcesCompat.getDrawable(
                                resources,
                                R.drawable.ila_bad,
                                null
                        )
                )
            }
        }
    }

    private fun setExpandableGeneralInfo() {
        binding.generalInfoBox.setOnClickListener {
            //fix expandable infobox
            if (binding.relativelayout.visibility == View.VISIBLE) {
                TransitionManager.beginDelayedTransition(
                        binding.generalInfoBox,
                        AutoTransition()
                )
                binding.relativelayout.visibility = View.GONE
                binding.arrow.setImageResource(R.drawable.down_darkblue)
            } else {
                TransitionManager.beginDelayedTransition(
                        binding.generalInfoBox,
                        AutoTransition()
                )
                binding.relativelayout.visibility = View.VISIBLE
                binding.arrow.setImageResource(R.drawable.up_darkblue)
            }

            //Put all general info of site
            binding.siteNrView.text = site.nr.toString()
            binding.placementView.text = site.placementType ?: "-----"
            binding.capacityView.text = site.capacity.toString()
            binding.waterTypeView.text = site.waterType ?: "-----"
            binding.municipalityView.text = site.placement?.municipalityName ?: "-----"
        }
    }

    private fun setWeatherInfoCard() {
        binding.weatherInfoCard.setOnClickListener {
            val extras = FragmentNavigatorExtras(binding.weatherIcon to "image_weather")
            view?.findNavController()?.navigate(
                    R.id.action_siteInfoFragment_to_weatherFragment,
                    null,
                    null,
                    extras
            )
        }
    }

    private fun setWaterInfoCard() {
        binding.waterInfoCard.setOnClickListener {
            val extras = FragmentNavigatorExtras(binding.wavesIcon to "icon_water")
            view?.findNavController()
                    ?.navigate(
                            R.id.action_siteInfoFragment_to_waterFragment,
                            null,
                            null,
                            extras
                    )
        }
    }

    private fun setInfectionInfoCard() {
        binding.infectionInfoCard.setOnClickListener {
            val extras = FragmentNavigatorExtras(binding.infectionIcon to "image_big")
            view?.findNavController()?.navigate(
                    R.id.action_siteInfoFragment_to_infectionFragment,
                    null,
                    null,
                    extras
            )
        }
    }
}