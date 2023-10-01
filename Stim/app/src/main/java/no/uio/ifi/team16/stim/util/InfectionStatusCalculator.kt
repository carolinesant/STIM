package no.uio.ifi.team16.stim.util

import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import no.uio.ifi.team16.stim.R

/**
 * Class that calculates the current infection status, either as text or as an drawable (image)
 */

class InfectionStatusCalculator(val resources: Resources) {

    /**
     *Method that calculates current infection status as a string.
     */
    fun calculateInfectionStatusText(infectiondata: Array<Float>): String {

        if (infectiondata.size > 2 && infectiondata.average() > Options.infectionExists) {
            //sjekker om det er signifikant økning/miskning på de siste 3 datapunktene
            val firstThree = arrayOf(
                infectiondata[0],
                infectiondata[1],
                infectiondata[2]
            )
            return if (firstThree.average() - infectiondata.average() > Options.increase) {
                "Signifikant økning i lakselussmitte"
            } else if (infectiondata.average() - firstThree.average() > Options.decrease) {
                "Signifikant minskning i lakselussmitte"
            } else {
                return if (infectiondata.average() > Options.high) {
                    "Høyt lakselussmittenivå"
                } else {
                    "Lavt lakselussmittenivå"
                }
            }
        } else {
            return "Veldig lav/Ingen lakselussmitte"
        }
    }

    /**
     * Method that calculates current infection status as an drawable
     */
    fun calculateInfectionStatusIcon(infectiondata: Array<Float>): Drawable? {
        if (infectiondata.size > 2 && infectiondata.average() > Options.infectionExists) {
            //sjekker om det er signifikant økning/miskning på de siste 3 datapunktene
            val firstThree = arrayOf(
                infectiondata[0],
                infectiondata[1],
                infectiondata[2]
            )
            return if (firstThree.average() - infectiondata.average() > Options.increase) {
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.arrow_up,
                    null
                )
            } else if (infectiondata.average() - firstThree.average() > Options.decrease) {
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.arrow_down,
                    null
                )
            } else {
                return if (infectiondata.average() > Options.high) {
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.danger,
                        null
                    )
                } else {
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.no_change,
                        null
                    )
                }
            }
        } else {
            return ResourcesCompat.getDrawable(
                resources,
                R.drawable.checkmark,
                null
            )
        }
    }


}