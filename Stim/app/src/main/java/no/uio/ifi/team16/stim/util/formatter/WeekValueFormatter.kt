package no.uio.ifi.team16.stim.util.formatter

import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.ValueFormatter
import java.util.*

class WeekValueFormatter : ValueFormatter() {

    /**
     * Valueformatter that returns a label representing the number of the week in decending order starting from the current week.
     */
    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        val currentWeek = Calendar.getInstance(Locale.getDefault()).get(Calendar.WEEK_OF_YEAR)
        return String.format("%.0f", (currentWeek - value - 1).mod(52.0))
    }

    // override this for e.g. LineChart or ScatterChart
    override fun getPointLabel(entry: Entry?): String {
        val currentWeek = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR)
        if (entry?.x != null) {
            return (currentWeek + entry.x).toString()
        }
        return entry?.x.toString()
    }
}