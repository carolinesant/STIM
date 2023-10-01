package no.uio.ifi.team16.stim.fragment

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import no.uio.ifi.team16.stim.R

/**
 * A simple dialog explaining the different icons for infection data
 */
class InfectiousIconStatusDialogFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        return requireActivity().layoutInflater.inflate(
            R.layout.fragment_dialog_status_infection_info,
            container
        )
    }
}
