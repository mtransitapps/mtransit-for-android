@file:JvmName("FavoritesFragment") // ANALYTICS
package org.mtransit.android.ui.favorites

import android.content.Context
import android.location.Location
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.AbsListView
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.data.POIArrayAdapter
import org.mtransit.android.databinding.FragmentFavoritesBinding
import org.mtransit.android.databinding.LayoutEmptyBinding
import org.mtransit.android.databinding.LayoutPoiListBinding
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.provider.FavoriteManager
import org.mtransit.android.provider.FavoriteManager.FavoriteUpdateListener
import org.mtransit.android.provider.sensor.MTSensorManager
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.task.StatusLoader
import org.mtransit.android.ui.MTActivityWithLocation
import org.mtransit.android.ui.MTActivityWithLocation.UserLocationListener
import org.mtransit.android.ui.fragment.ABFragment
import org.mtransit.android.ui.view.common.attached
import javax.inject.Inject

@AndroidEntryPoint
class FavoritesFragment : ABFragment(R.layout.fragment_favorites), UserLocationListener, FavoriteUpdateListener {

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

    @Inject
    lateinit var sensorManager: MTSensorManager

    @Inject
    lateinit var dataSourcesRepository: DataSourcesRepository

    @Inject
    lateinit var poiRepository: POIRepository

    @Inject
    lateinit var favoriteManager: FavoriteManager

    @Inject
    lateinit var statusLoader: StatusLoader

    @Inject
    lateinit var serviceUpdateLoader: ServiceUpdateLoader

    private var binding: FragmentFavoritesBinding? = null
    private var emptyBinding: LayoutEmptyBinding? = null
    private var listBinding: LayoutPoiListBinding? = null

    private val adapter: POIArrayAdapter by lazy {
        POIArrayAdapter(
            this,
            this.sensorManager,
            this.dataSourcesRepository,
            this.poiRepository,
            this.favoriteManager,
            this.statusLoader,
            this.serviceUpdateLoader
        ).apply {
            logTag = logTag
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentFavoritesBinding.bind(view).apply {
            emptyStub.setOnInflateListener { _, inflated ->
                emptyBinding = LayoutEmptyBinding.bind(inflated)
            }
            listStub.setOnInflateListener { _, inflated ->
                listBinding = LayoutPoiListBinding.bind(inflated)
            }
            (listBinding?.root ?: listStub.inflate() as AbsListView).let { listView ->
                listView.isVisible = adapter.isInitialized
                adapter.setListView(listView)
            }
        }
        viewModel.favoritePOIs.observe(viewLifecycleOwner, { favoritePOIS ->
            adapter.setPois(favoritePOIS)
            adapter.updateDistanceNowAsync(viewModel.deviceLocation.value)
            when {
                favoritePOIS == null -> { // LOADING
                    listBinding?.root?.isVisible = false
                    emptyBinding?.root?.isVisible = false
                    binding?.loading?.root?.isVisible = true
                }
                favoritePOIS.isEmpty() -> { // EMPTY
                    binding?.loading?.root?.isVisible = false
                    listBinding?.root?.isVisible = false
                    (emptyBinding?.root ?: binding?.emptyStub?.inflate())?.isVisible = true
                }
                else -> { // LIST
                    binding?.loading?.root?.isVisible = false
                    emptyBinding?.root?.isVisible = false
                    (listBinding?.root ?: binding?.listStub?.inflate())?.isVisible = true
                }
            }
        })
        viewModel.deviceLocation.observe(viewLifecycleOwner, { deviceLocation ->
            adapter.setLocation(deviceLocation)
        })
    }

    override fun onFavoriteUpdated() {
        attached { viewModel }?.onFavoriteUpdated()
    }

    override fun onResume() {
        super.onResume()
        adapter.onResume(this, viewModel.deviceLocation.value)
        (activity as? MTActivityWithLocation)?.let { onUserLocationChanged(it.lastLocation) }
    }

    override fun onPause() {
        super.onPause()
        adapter.onPause()
    }

    override fun onUserLocationChanged(newLocation: Location?) {
        attached { viewModel }?.onDeviceLocationChanged(newLocation)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_favorites, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_add_favorite_folder -> {
                this.favoriteManager.showAddFolderDialog(requireActivity(), this, null, null)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun getABTitle(context: Context?) = context?.getString(R.string.favorites) ?: super.getABTitle(context)

    override fun onDestroyView() {
        super.onDestroyView()
        listBinding = null
        emptyBinding = null
        binding = null
    }
}