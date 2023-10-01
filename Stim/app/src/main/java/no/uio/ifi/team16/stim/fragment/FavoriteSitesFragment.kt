package no.uio.ifi.team16.stim.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import no.uio.ifi.team16.stim.MainActivityViewModel
import no.uio.ifi.team16.stim.R
import no.uio.ifi.team16.stim.data.Site
import no.uio.ifi.team16.stim.databinding.FragmentFavoriteSitesBinding
import no.uio.ifi.team16.stim.util.RecycleViewAdapter

/**
 * Fragment showing a list of the user's favourite sites
 */
class FavoriteSitesFragment : StimFragment() {

    private lateinit var binding: FragmentFavoriteSitesBinding
    private val viewModel: MainActivityViewModel by activityViewModels()

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentFavoriteSitesBinding.inflate(inflater, container, false)

        val adapter = RecycleViewAdapter(listOf(), listOf(), this::adapterOnClick, this::favoriteOnClick, requireActivity())
        binding.recyclerview.adapter = adapter

        viewModel.getFavouriteSitesData().observe(viewLifecycleOwner) { sites ->
            if (sites != null) {
                adapter.sites = sites
                adapter.favs = sites
                adapter.notifyDataSetChanged()
            }
            binding.noFavoriteSites.isVisible = sites.isNullOrEmpty()
        }
        return binding.root
    }

    /**
     * Called when a site is pressed in the list
     */
    private fun adapterOnClick(site: Site) {
        viewModel.site = site
        view?.findNavController()?.navigate(R.id.action_favoriteSitesFragment_to_siteInfoFragment)
    }

    /**
     * Called when the user clicks on the favourite button
     */
    private fun favoriteOnClick(site: Site, checked: Boolean) {
        if (checked) {
            viewModel.registerFavouriteSite(site)
        } else {
            viewModel.removeFavouriteSite(site)
        }
    }
}