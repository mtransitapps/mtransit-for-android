package org.mtransit.android.ui.favorites

import android.content.Context
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import org.mtransit.android.R
import org.mtransit.android.data.POIArrayAdapter
import org.mtransit.android.databinding.FragmentFavoritesBinding
import org.mtransit.android.databinding.LayoutEmptyBinding
import org.mtransit.android.databinding.LayoutPoiListBinding
import org.mtransit.android.provider.FavoriteManager
import org.mtransit.android.provider.FavoriteManager.FavoriteUpdateListener
import org.mtransit.android.ui.MTActivityWithLocation
import org.mtransit.android.ui.MTActivityWithLocation.UserLocationListener
import org.mtransit.android.ui.fragment.ABFragment

class FavoritesFragment : ABFragment(), UserLocationListener, FavoriteUpdateListener {

    companion object {
        private val LOG_TAG = FavoritesFragment::class.java.simpleName

        private const val TRACKING_SCREEN_NAME = "Favorites"

        @JvmStatic
        fun newInstance(): FavoritesFragment {
            return FavoritesFragment()
        }
    }

    override fun getLogTag(): String = LOG_TAG

    override fun getScreenName(): String = TRACKING_SCREEN_NAME

    private val viewModel by viewModels<FavoritesViewModel>()

    private var binding: FragmentFavoritesBinding? = null
    private var emptyBinding: LayoutEmptyBinding? = null
    private var listBinding: LayoutPoiListBinding? = null

    private val adapter: POIArrayAdapter by lazy {
        POIArrayAdapter(this).apply {
            setTag(logTag)
            setShowFavorite(false) // all items in this screen are favorites
            setFavoriteUpdateListener(this@FavoritesFragment)
            setShowTypeHeader(POIArrayAdapter.TYPE_HEADER_ALL_NEARBY)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.adapter.setActivity(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return onBindingCreated(FragmentFavoritesBinding.inflate(inflater, container, false)).root
    }

    private fun onBindingCreated(newBinding: FragmentFavoritesBinding): FragmentFavoritesBinding {
        newBinding.emptyStub.setOnInflateListener { _, inflated ->
            emptyBinding = LayoutEmptyBinding.bind(inflated)
        }
        newBinding.listStub.setOnInflateListener { _, inflated ->
            listBinding = LayoutPoiListBinding.bind(inflated)
        }
        if (listBinding == null) { // IF NOT present/inflated DO
            newBinding.listStub.inflate() // inflate
            listBinding?.root?.visibility = View.GONE // hide by default
        }
        listBinding?.let { listView ->
            adapter.setListView(listView.root)
        }
        this.binding = newBinding
        return newBinding
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.favoritePOIs.observe(viewLifecycleOwner, { favoritePOIS ->
            adapter.setPois(favoritePOIS)
            adapter.updateDistanceNowAsync(viewModel.deviceLocation.value)
            binding?.loading?.root?.visibility = View.GONE // hide
            if (favoritePOIS.isEmpty()) { // SHOW EMPTY
                listBinding?.root?.visibility = View.GONE // hide (if inflated)
                if (emptyBinding == null) { // IF NOT present/inflated DO
                    binding?.emptyStub?.inflate() // inflate
                }
                emptyBinding?.root?.visibility = View.VISIBLE // show
            } else { // SHOW LIST
                emptyBinding?.root?.visibility = View.GONE // hide (if inflated)
                if (listBinding == null) { // IF NOT present/inflated DO
                    binding?.listStub?.inflate()  // inflate
                }
                listBinding?.root?.visibility = View.VISIBLE // show
            }
        })
        viewModel.deviceLocation.observe(viewLifecycleOwner, { deviceLocation: Location? ->
            adapter.setLocation(deviceLocation)
        })
    }

    override fun onFavoriteUpdated() {
        this.viewModel.onFavoriteUpdated()
    }

    override fun onResume() {
        super.onResume()
        adapter.onResume(this, viewModel.deviceLocation.value)
        activity?.let {
            onUserLocationChanged((it as MTActivityWithLocation).lastLocation)
        }
    }

    override fun onPause() {
        super.onPause()
        adapter.onPause()
    }

    override fun onUserLocationChanged(newLocation: Location?) {
        viewModel.onDeviceLocationChanged(newLocation)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_favorites, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_add_favorite_folder) {
            FavoriteManager.showAddFolderDialog(requireActivity(), this, null, null)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun getABTitle(context: Context?): CharSequence? {
        return context?.let { context.getString(R.string.favorites) } ?: super.getABTitle(context)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listBinding = null
        emptyBinding = null
        binding = null
    }
}