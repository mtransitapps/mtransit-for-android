@file:JvmName("HomeFragment") // ANALYTICS
package org.mtransit.android.ui.home

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.PopupWindow
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.commons.ThemeUtils
import org.mtransit.android.commons.ToastUtils
import org.mtransit.android.data.POIArrayAdapter
import org.mtransit.android.databinding.FragmentHomeBinding
import org.mtransit.android.databinding.LayoutEmptyBinding
import org.mtransit.android.databinding.LayoutPoiListBinding
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.dev.DemoModeManager
import org.mtransit.android.provider.FavoriteManager
import org.mtransit.android.provider.sensor.MTSensorManager
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.task.StatusLoader
import org.mtransit.android.ui.MTActivityWithLocation
import org.mtransit.android.ui.MTActivityWithLocation.UserLocationListener
import org.mtransit.android.ui.MainActivity
import org.mtransit.android.ui.fragment.ABFragment
import org.mtransit.android.ui.fragment.POIFragment
import org.mtransit.android.ui.map.MapFragment
import org.mtransit.android.ui.nearby.NearbyFragment
import org.mtransit.android.ui.type.AgencyTypeFragment
import org.mtransit.android.ui.view.common.MTTransitions
import org.mtransit.android.ui.view.common.attached
import org.mtransit.commons.FeatureFlags
import java.lang.RuntimeException
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : ABFragment(R.layout.fragment_home), UserLocationListener {

    companion object {
        private val LOG_TAG = HomeFragment::class.java.simpleName

        private const val TRACKING_SCREEN_NAME = "Home"

        @JvmStatic
        fun newInstance(): HomeFragment {
            return HomeFragment()
        }
    }

    override fun getLogTag(): String = LOG_TAG

    override fun getScreenName(): String = TRACKING_SCREEN_NAME

    private val viewModel by viewModels<HomeViewModel>()

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

    @Inject
    lateinit var demoModeManager: DemoModeManager

    private var binding: FragmentHomeBinding? = null
    private var emptyBinding: LayoutEmptyBinding? = null
    private var listBinding: LayoutPoiListBinding? = null

    private var locationToast: PopupWindow? = null
    private var toastShown: Boolean = false

    private val infiniteLoadingListener = object : POIArrayAdapter.InfiniteLoadingListener {
        override fun isLoadingMore(): Boolean {
            return attached { viewModel }?.loadingPOIs?.value == true
        }

        override fun showingDone() = false
    }

    private val typeHeaderButtonsClickListener = POIArrayAdapter.TypeHeaderButtonsClickListener { buttonId, type ->
        when (buttonId) {
            POIArrayAdapter.TypeHeaderButtonsClickListener.BUTTON_MORE -> {
                if (FeatureFlags.F_NAVIGATION) {
                    var extras: FragmentNavigator.Extras? = null
                    if (FeatureFlags.F_TRANSITION) {
                        extras = null // TODO button ? extras = FragmentNavigatorExtras(view to view.transitionName)
                    }
                    findNavController().navigate(
                        R.id.nav_to_nearby_screen,
                        NearbyFragment.newNearbyInstanceArgs(type),
                        null,
                        extras
                    )
                } else {
                    (activity as? MainActivity)?.addFragmentToStack(
                        NearbyFragment.newNearbyInstance(type),
                        this@HomeFragment
                    )
                }
                true
            }
            else -> false
        }
    }

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
            setFavoriteUpdateListener { adapter.onFavoriteUpdated() }
            setShowBrowseHeaderSection(true)
            setShowTypeHeader(POIArrayAdapter.TYPE_HEADER_MORE)
            setShowTypeHeaderNearby(true)
            setInfiniteLoading(true)
            setInfiniteLoadingListener(infiniteLoadingListener)
            setOnTypeHeaderButtonsClickListener(typeHeaderButtonsClickListener)
            setPois(attached { viewModel }?.nearbyPOIs?.value)
            setLocation(attached { viewModel }?.deviceLocation?.value)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        MTTransitions.setContainerTransformTransition(this)
        // if (FeatureFlags.F_TRANSITION) {
        // exitTransition = MTTransitions.newHoldTransition() // not working with AdapterView // FIXME #RecyclerView
        // }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MTTransitions.postponeEnterTransition(this)
        binding = FragmentHomeBinding.bind(view).apply {
            emptyStub.setOnInflateListener { _, inflated ->
                emptyBinding = LayoutEmptyBinding.bind(inflated)
            }
            listStub.setOnInflateListener { _, inflated ->
                listBinding = LayoutPoiListBinding.bind(inflated).apply {
                    swiperefresh.setListViewWR(this.root)
                }
            }
            (listBinding?.root ?: listStub.inflate() as AbsListView).let { listView ->
                listView.isVisible = adapter.isInitialized
                adapter.setListView(listView)
                MTTransitions.startPostponedEnterTransitionOnPreDraw(view.parent as? ViewGroup, this@HomeFragment)
            }
            swiperefresh.apply {
                setColorSchemeColors(
                    ThemeUtils.resolveColorAttribute(view.context, R.attr.colorAccent)
                )
                setOnRefreshListener {
                    if (!viewModel.initiateRefresh()) {
                        isRefreshing = false
                    }
                }
            }
        }
        viewModel.deviceLocation.observe(viewLifecycleOwner, {
            adapter.setLocation(it)
        })
        viewModel.nearbyLocationAddress.observe(viewLifecycleOwner, {
            abController?.setABSubtitle(this, getABSubtitle(context), true)
        })
        viewModel.nearbyPOIsTrigger.observe(viewLifecycleOwner, {
            adapter.clear()
        })
        viewModel.nearbyPOIs.observe(viewLifecycleOwner, {
            it?.let {
                val scrollToTop = adapter.poisCount <= 0
                adapter.appendPois(it)
                if (scrollToTop) {
                    listBinding?.root?.setSelection(0)
                }
                if (isResumed) {
                    adapter.updateDistanceNowAsync(viewModel.deviceLocation.value)
                } else {
                    adapter.onPause()
                }
                switchView()
            }
        })
        viewModel.loadingPOIs.observe(viewLifecycleOwner, {
            if (it == false) {
                binding?.swiperefresh?.isRefreshing = false
            }
        })
        viewModel.newLocationAvailable.observe(viewLifecycleOwner, { newLocationAvailable ->
            if (newLocationAvailable == true) {
                showLocationToast()
            } else {
                hideLocationToast()
            }
        })
    }

    private fun switchView() {
        binding?.apply {
            loading.root.isVisible = false
            emptyBinding?.root?.isVisible = false
            (listBinding?.root ?: listStub.inflate()).isVisible = true
        }
    }

    override fun onResume() {
        super.onResume()
        this.adapter.onResume(this, viewModel.deviceLocation.value)
        switchView()
        (activity as? MTActivityWithLocation)?.let { onUserLocationChanged(it.lastLocation) }
        if (demoModeManager.isEnabledPOIScreen()) {
            val poiAuthority = demoModeManager.filterAgencyAuthority ?: throw RuntimeException("Demo mode: missing authority!")
            val poiUUID = demoModeManager.filterUUID ?: throw RuntimeException("Demo mode: missing UUID!")
            view?.post {
                if (FeatureFlags.F_NAVIGATION) {
                    findNavController().navigate(
                        R.id.nav_to_poi_screen,
                        POIFragment.newInstanceArgs(poiAuthority, poiUUID),
                    )
                } else {
                    (activity as MainActivity).addFragmentToStack(
                        POIFragment.newInstance(poiAuthority, poiUUID),
                    )
                }
            }
        } else if (demoModeManager.isEnabledBrowseScreen()) {
            val type = demoModeManager.filterTypeId ?: throw RuntimeException("Demo mode: missing type!")
            view?.post {
                if (FeatureFlags.F_NAVIGATION) {
                    findNavController().navigate(
                        R.id.nav_to_type_screen,
                        AgencyTypeFragment.newInstanceArgs(type),
                    )
                } else {
                    (activity as MainActivity).addFragmentToStack(
                        AgencyTypeFragment.newInstance(type),
                    )
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        this.adapter.onPause()
        hideLocationToast()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun makeLocationToast(): PopupWindow? {
        return ToastUtils.getNewTouchableToast(context, R.drawable.toast_frame_old, R.string.new_location_toast)?.apply {
            setTouchInterceptor { _, me ->
                when (me.action) {
                    MotionEvent.ACTION_DOWN -> {
                        val handled = attached { viewModel }?.initiateRefresh() == true
                        hideLocationToast()
                        handled
                    }
                    else -> false // not handled
                }
            }
            setOnDismissListener {
                toastShown = false
            }
        }
    }

    private fun showLocationToast() {
        if (this.toastShown) {
            return // SKIP
        }
        val theActivity: Activity? = activity
        if (theActivity == null || theActivity.isFinishing) {
            return // SKIP
        }
        (this.locationToast ?: makeLocationToast().also { this.locationToast = it })
            ?.let { locationToast ->
                this.toastShown = ToastUtils.showTouchableToastPx(
                    context,
                    locationToast,
                    view,
                    attached { viewModel }?.getAdBannerHeightInPx(this) ?: 0
                )
            }
    }

    private fun hideLocationToast() {
        this.locationToast?.dismiss()
        this.toastShown = false
    }

    override fun onUserLocationChanged(newLocation: Location?) {
        attached { viewModel }?.onDeviceLocationChanged(newLocation)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_home, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.nav_map -> {
                if (FeatureFlags.F_NAVIGATION) {
                    false // handled by navigation library
                } else {
                    (activity as? MainActivity)?.addFragmentToStack(
                        MapFragment.newInstance(),
                        this
                    )
                    true
                }
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun getABTitle(context: Context?) = context?.getString(R.string.app_name) ?: super.getABTitle(context)

    override fun getABSubtitle(context: Context?) =
        this.attached { viewModel }?.nearbyLocationAddress?.value ?: context?.getString(R.string.ellipsis) ?: super.getABSubtitle(context)

    override fun onDestroyView() {
        super.onDestroyView()
        hideLocationToast()
        this.locationToast = null
        this.toastShown = false
        binding?.swiperefresh?.setOnRefreshListener(null)
        emptyBinding = null
        listBinding = null
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        this.adapter.onDestroy()
    }
}