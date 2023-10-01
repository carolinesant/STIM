package no.uio.ifi.team16.stim.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import androidx.transition.AutoTransition
import androidx.transition.TransitionInflater
import androidx.transition.TransitionManager
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import no.uio.ifi.team16.stim.MainActivityViewModel
import no.uio.ifi.team16.stim.R
import no.uio.ifi.team16.stim.data.Site
import no.uio.ifi.team16.stim.databinding.FragmentWaterBinding
import no.uio.ifi.team16.stim.util.Options
import no.uio.ifi.team16.stim.util.closestHour
import no.uio.ifi.team16.stim.util.formatter.TempValueFormatter
import no.uio.ifi.team16.stim.util.formatter.TimeValueFormatter
import no.uio.ifi.team16.stim.util.linestyle.SalinityLineStyle
import no.uio.ifi.team16.stim.util.linestyle.TemperatureLineStyle
import no.uio.ifi.team16.stim.util.takeEvery
import no.uio.ifi.team16.stim.util.toShortString
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.roundToLong

/**
 * Fragment showing information about the water at a site
 */
class WaterFragment : StimFragment() {

    companion object {
        const val SECONDS_PER_HOUR = 3600
    }

    private lateinit var binding: FragmentWaterBinding
    private val viewModel: MainActivityViewModel by activityViewModels()
    private var salinityChartPressed = true
    private lateinit var site: Site

    @Inject
    lateinit var saltChartStyle: SalinityLineStyle

    @Inject
    lateinit var tempChartStyle: TemperatureLineStyle

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        binding = FragmentWaterBinding.inflate(inflater, container, false)

        val animation = TransitionInflater.from(requireContext()).inflateTransition(android.R.transition.move)
        sharedElementEnterTransition = animation
        sharedElementReturnTransition = animation

        site = viewModel.site ?: return binding.root
        binding.sitename.text = site.name

        binding.salinityChart.apply {
            setNoDataText(getString(R.string.loading))
            setNoDataTextColor(R.color.primaryTextColor)
            setNoDataTextTypeface(ResourcesCompat.getFont(requireContext(), R.font.montserrat_semibold))
        }

        binding.watertempChart.apply {
            setNoDataText(getString(R.string.loading))
            setNoDataTextColor(R.color.primaryTextColor)
            setNoDataTextTypeface(ResourcesCompat.getFont(requireContext(), R.font.montserrat_semibold))
        }

        binding.InformationCard.setOnClickListener {
            // If the CardView is already expanded, set its visibility
            //  to gone and change the expand less icon to expand more.
            // If the CardView is already expanded, set its visibility
            //  to gone and change the expand less icon to expand more.
            if (binding.infoTextExtra.visibility == View.VISIBLE) {
                // The transition of the hiddenView is carried out
                //  by the TransitionManager class.
                // Here we use an object of the AutoTransition
                // Class to create a default transition.
                TransitionManager.beginDelayedTransition(
                        binding.InformationCard,
                        AutoTransition()
                )
                binding.infoTextExtra.visibility = View.GONE
                binding.arrow.setImageResource(R.drawable.down_darkblue)
            } else {
                TransitionManager.beginDelayedTransition(
                        binding.InformationCard,
                        AutoTransition()
                )
                binding.infoTextExtra.visibility = View.VISIBLE
                binding.arrow.setImageResource(R.drawable.up_darkblue)
            }
        }

        setOnTemperatureOrSalinityClickListener(binding.temperatureTextview, requireContext())
        setOnTemperatureOrSalinityClickListener(binding.saltTextview, requireContext())
        setTemperatureAndSalt()

        setSalinityChart()

        binding.chartButton.setOnClickListener {
            if (binding.chartButton.isChecked) {
                setTemperatureChart()
            } else {
                setSalinityChart()
            }
        }

        setTemperatureTable(inflater, container)
        setSalinityTable(inflater, container)

        return binding.root
    }

    /**
     * Creates the salinity chart from NordKyst800AtSiteData. takes all data from getSalinitAtSurfaceGraph().
     * Style: SalinityLineStyle
     */
    private fun setSalinityChart() {
        binding.salinityChart.visibility = View.VISIBLE
        binding.watertempChart.visibility = View.GONE
        binding.salinityChartHeader.text = getString(R.string.salinityChart)

        saltChartStyle = SalinityLineStyle()

        viewModel.getNorKyst800AtSiteData(site).observe(viewLifecycleOwner) { norkAtSite ->
            norkAtSite?.observeSalinityAtSurfaceAsGraph(viewLifecycleOwner) { salinityChart ->
                if (salinityChart.isNotEmpty()) {
                    val linedatasetSalinity = LineDataSet(salinityChart, getString(R.string.salinity))

                    binding.salinityChart.apply {
                        xAxis.apply {
                            valueFormatter = TimeValueFormatter()
                            //find hours from 1970 to now
                            val currentHour = Instant.now().epochSecond.toFloat() / SECONDS_PER_HOUR
                            addLimitLine(LimitLine(currentHour, getString(R.string.now)))
                            setDrawLimitLinesBehindData(true)
                            moveViewToX(currentHour) //start at current time, showing values after

                            //add lines for each change of weekday in dataset
                            salinityChart.forEach { e ->
                                val valueDate =
                                        Instant.ofEpochSecond(SECONDS_PER_HOUR * e.x.toLong()).atZone(
                                                ZoneId.systemDefault()
                                        )
                                if (valueDate.hour == 0) {
                                    addLimitLine(
                                            LimitLine(
                                                    valueDate.toEpochSecond().toFloat() / SECONDS_PER_HOUR,
                                                    valueDate.dayOfWeek.toShortString(context)
                                            )
                                    )
                                }
                            }
                        }
                    }

                    //style linedataset
                    saltChartStyle.styleLineDataSet(linedatasetSalinity, requireContext())
                    binding.salinityChart.data = LineData(linedatasetSalinity)
                    binding.salinityChart.notifyDataSetChanged()
                    binding.salinityChart.invalidate()

                    saltChartStyle.styleChart(binding.salinityChart)

                    binding.salinityChartHeader.text = getString(R.string.salinityChart)
                    salinityChartPressed = true
                } else {
                    binding.salinityChart.setNoDataText(getString(R.string.no_data_available))
                    binding.salinityChart.invalidate()
                }
            }
        }
    }

    /**
     * Creates the temperature chart from NordKyst800AtSiteData. takes all data from getTemperatureAtSurfaceGraph().
     * Style: TemperatureLineStyle
     */
    private fun setTemperatureChart() {
        binding.salinityChart.visibility = View.GONE
        binding.watertempChart.visibility = View.VISIBLE
        binding.salinityChartHeader.text = getString(R.string.tempChart)

        tempChartStyle = TemperatureLineStyle()

        viewModel.getNorKyst800AtSiteData(site).observe(viewLifecycleOwner) { norkAtSite ->
            norkAtSite?.observeTemperatureAtSurfaceAsGraph(viewLifecycleOwner) { temperatureChart ->
                if (temperatureChart.isNotEmpty()) {
                    val linedataset = LineDataSet(temperatureChart, getString(R.string.waterTemp))

                    binding.watertempChart.apply {
                        axisLeft.apply {
                            valueFormatter = TempValueFormatter()
                        }
                        xAxis.apply {
                            valueFormatter = TimeValueFormatter()
                            //find hours from 1970 to now
                            val currentHour = Instant.now().epochSecond.toFloat() / SECONDS_PER_HOUR
                            addLimitLine(LimitLine(currentHour, getString(R.string.now)))
                            //add lines for each change of weekday in dataset
                            temperatureChart.forEach { e ->
                                val valueDate =
                                        Instant.ofEpochSecond(SECONDS_PER_HOUR * e.x.toLong()).atZone(
                                                ZoneId.systemDefault()
                                        )
                                if (valueDate.hour == 0) {
                                    addLimitLine(
                                            LimitLine(
                                                    valueDate.toEpochSecond().toFloat() / SECONDS_PER_HOUR,
                                                    valueDate.dayOfWeek.toShortString(context)
                                            )
                                    )
                                }
                            }
                            setDrawLimitLinesBehindData(true)
                            moveViewToX(currentHour) //start at current time, showing values after
                        }
                    }
                    //style linedataset
                    tempChartStyle.styleLineDataSet(linedataset, requireContext())
                    binding.watertempChart.data = LineData(linedataset)
                    binding.watertempChart.invalidate()

                    tempChartStyle.styleChart(binding.watertempChart)

                    binding.salinityChartHeader.text = getString(R.string.tempChart)
                    salinityChartPressed = false
                } else {
                    binding.watertempChart.setNoDataText(getString(R.string.no_data_available))
                    binding.watertempChart.invalidate()
                }
            }
        }
    }

    /**
     * Creates the temperature table from NordKyst800AtSiteData. takes all data from getTemperatureAtSurfaceGraph().
     * Uses same layout as the infection table.
     */
    private fun setTemperatureTable(inflater: LayoutInflater, container: ViewGroup?) {
        viewModel.getNorKyst800AtSiteData(site).observe(viewLifecycleOwner) { norkAtSite ->
            norkAtSite?.observeTemperatureAtSurfaceAsGraph(viewLifecycleOwner) { tempgraphdata ->
                binding.tablelayout.removeAllViews()

                tempgraphdata
                        .takeEvery(Options.hoursPerEntryInTable)
                        .take(Options.entriesPerTable)
                        .forEachIndexed { i, e ->
                            val x = e.x
                            val y = e.y
                            val newRow = TableRow(requireContext())
                            val view = inflater.inflate(R.layout.infection_table_row, container, false)
                            view.findViewById<TextView>(R.id.table_display_week).text =
                                    convertTime(x)
                            if (!y.toString().contains("NaN")) {
                                view.findViewById<TextView>(R.id.table_display_float).text =
                                        String.format("%.4f°", y)
                            } else {
                        view.findViewById<TextView>(R.id.table_display_float).text =
                                getString(R.string.no_data_available)
                    }
                    view.layoutParams = TableRow.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    newRow.addView(view)
                    binding.tablelayout.addView(newRow, i)
                    newRow.layoutParams = TableLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    binding.tablelayout.requestLayout()
                }
            }
        }
    }

    /**
     * Creates the salinity table from NordKyst800AtSiteData. takes all data from getSalinityAtSurfaceGraph().
     * Uses same layout as the infection table.
     */
    private fun setSalinityTable(inflater: LayoutInflater, container: ViewGroup?) {
        viewModel.getNorKyst800AtSiteData(site).observe(viewLifecycleOwner) { norkAtSite ->
            norkAtSite?.observeSalinityAtSurfaceAsGraph(viewLifecycleOwner) { saltgraphdata ->
                binding.Salttablelayout.removeAllViews()

                saltgraphdata
                        .takeEvery(Options.hoursPerEntryInTable)
                        .take(Options.entriesPerTable)
                        .forEachIndexed { i, e ->
                            val x = e.x
                            val y = e.y
                            val newRow = TableRow(requireContext())
                            val view = inflater.inflate(R.layout.infection_table_row, container, false)
                            view.findViewById<TextView>(R.id.table_display_week).text =
                                    convertTime(x)
                            if (!y.toString().contains("NaN")) {
                                view.findViewById<TextView>(R.id.table_display_float).text =
                                        y.toString()
                            } else {
                        view.findViewById<TextView>(R.id.table_display_float).text =
                                getString(R.string.no_data_available)
                    }
                    view.layoutParams = TableRow.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    newRow.addView(view)
                    binding.Salttablelayout.addView(newRow, i)
                    newRow.layoutParams = TableLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
                binding.Salttablelayout.requestLayout()
            }
        }
    }

    /**
     * Convert a time to the format HH:00 (don't show minutes)
     */
    private val hourFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH")
    private fun convertTime(value: Float): String {
        val valueDate =
                Instant.ofEpochSecond(SECONDS_PER_HOUR.toLong() * value.roundToLong()).atZone(
                        ZoneId.systemDefault()
                )
        //add weekdayname if between 00:00 and hoursPerEntryInTable:00
        val prefix = if (valueDate.closestHour().div(Options.hoursPerEntryInTable) == 0)
            valueDate.dayOfWeek.toShortString(requireContext()) + " "
        else
            ""
        return prefix + valueDate.format(hourFormatter) + ":00"
    }

    /**
     * Set data to salinity and temperature cards
     * Set clicklisteners to cards
     */
    private fun setTemperatureAndSalt() {
        //TOAST IF NO TEMP/SALT
        setOnTemperatureOrSalinityClickListener(binding.temperatureTextview, requireContext())
        setOnTemperatureOrSalinityClickListener(binding.saltTextview, requireContext())

        var hasLoadedData = false

        //try to get from at-site data
        viewModel.getNorKyst800AtSiteData(site).observe(viewLifecycleOwner) {
                it?.apply {
                    if (!hasLoadedData) {
                        binding.temperatureTextview.text =
                                getTemperature()?.let { temp ->
                                    "%4.1f".format(temp) + "°"
                                } ?: getString(R.string.no_info)
                        binding.saltTextview.text =
                                getSalinity()?.let { salt ->
                                    "%4.1f".format(salt)
                                } ?: getString(R.string.no_info)
                        binding.Velocitytext.text =
                                getVelocity()?.let { velocity ->
                                    "%4.1f m/s".format(velocity)
                                } ?: getString(R.string.no_info)
                        val velocity = getVelocityDirectionInXYPlane()
                        if (velocity != null) {
                            setVelocityDirection(velocity)
                        }
                        hasLoadedData = true
                    }
                } ?: run {
                    Toast.makeText(
                            context,
                            getString(R.string.norkyst_local_load_error),
                            Toast.LENGTH_LONG
                    ).show()
                    binding.temperatureTextview.text = getString(R.string.no_info)
                    binding.saltTextview.text = getString(R.string.no_info)
                    binding.Velocitytext.text = getString(R.string.no_info)
                }

        }
    }


    /**
     * make toasts explaining certain textvalues of the salinity or temperature textview.
     */
    private fun setOnTemperatureOrSalinityClickListener(textView: TextView, context: Context) {
        textView.setOnClickListener {
            val txt = it as TextView
            if (txt.text == getString(R.string.no_info)) {
                Toast.makeText(
                        context,
                        getString(R.string.no_local_data),
                        Toast.LENGTH_LONG
                ).show()
            }
            if (txt.text == getString(R.string.blank)) {
                Toast.makeText(
                        context,
                        getString(R.string.no_data_loaded),
                        Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Rotate the arrow to show which way the water is flowing
     */
    private fun setVelocityDirection(direction: Float) {
        val degrees = 270 - Math.toDegrees(direction.toDouble())
        binding.VelocityDirectionArrow.rotation = degrees.toFloat()
    }
}