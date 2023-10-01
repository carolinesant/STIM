package no.uio.ifi.team16.stim.fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import no.uio.ifi.team16.stim.databinding.FragmentAppInfoBinding

/**
 * Fragment showing some basic info about the app, such as the APIs used
 */
class AppInfoFragment : StimFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentAppInfoBinding.inflate(inflater, container, false).root
    }
}