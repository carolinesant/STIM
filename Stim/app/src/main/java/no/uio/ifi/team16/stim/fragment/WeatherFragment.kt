package no.uio.ifi.team16.stim.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.transition.TransitionInflater
import com.bumptech.glide.Glide
import no.uio.ifi.team16.stim.MainActivityViewModel
import no.uio.ifi.team16.stim.R
import no.uio.ifi.team16.stim.data.WeatherForecast
import no.uio.ifi.team16.stim.databinding.FragmentWeatherBinding

/**
 * Fragment showing weather and storm forecast
 */
class WeatherFragment : StimFragment() {

    private lateinit var binding: FragmentWeatherBinding
    private val viewModel: MainActivityViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentWeatherBinding.inflate(inflater, container, false)

        val animation = TransitionInflater.from(requireContext()).inflateTransition(android.R.transition.move)
        sharedElementEnterTransition = animation
        sharedElementReturnTransition = animation

        val site = viewModel.site ?: return binding.root
        binding.sitename.text = site.name

        viewModel.getWeatherData().observe(viewLifecycleOwner, this::onWeatherLoaded)
        viewModel.loadWeatherAtSite(site)

        return binding.root
    }

    /**
     * Called when the weather is ready to be displayed
     */
    private fun onWeatherLoaded(forecast: WeatherForecast?) {
        forecast?.apply {
            val context = requireContext()

            binding.dayText1.text = first.day.getTranslation(context)
            binding.dayText2.text = second.day.getTranslation(context)
            binding.dayText3.text = third.day.getTranslation(context)
            binding.dayText4.text = fourth.day.getTranslation(context)

            binding.weatherIcon1.setImageDrawable(first.icon.asDrawable(context))
            binding.weatherIcon2.setImageDrawable(second.icon.asDrawable(context))
            binding.weatherIcon3.setImageDrawable(third.icon.asDrawable(context))
            binding.weatherIcon4.setImageDrawable(fourth.icon.asDrawable(context))

            binding.temperatureView1.text = getString(R.string.temperature, first.temperature)
            binding.temperatureView2.text = getString(R.string.temperature, second.temperature)
            binding.temperatureView3.text = getString(R.string.temperature, third.temperature)
            binding.temperatureView4.text = getString(R.string.temperature, fourth.temperature)

            forecast.storm?.let { storm ->
                var dayText = storm.day.getTranslation(context)
                if (!storm.day.isToday()) {
                    dayText = getString(R.string.on) + " " + dayText
                }
                binding.stormForecastText.text =
                        getString(R.string.storm_predicted, storm.strength, dayText.lowercase())
                Glide.with(requireActivity()).asGif().load(R.drawable.erstorm_animasjon).into(binding.stormanimation)
            } ?: run {
                binding.stormForecastText.text = getString(R.string.no_storm)
            }
        }
    }
}