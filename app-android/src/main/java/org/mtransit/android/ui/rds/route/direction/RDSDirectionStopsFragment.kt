@file:JvmName("RTSTripStopsFragment") // ANALYTICS // do not change to avoid breaking tracking
package org.mtransit.android.ui.rds.route.direction

import android.content.Context
import android.content.res.ColorStateList
import android.location.Location
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.common.repository.DefaultPreferenceRepository
import org.mtransit.android.common.repository.LocalPreferenceRepository
import org.mtransit.android.commons.ToastUtils
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.data.ServiceUpdate
import org.mtransit.android.commons.findClosestPOISIdxUuid
import org.mtransit.android.commons.updateDistance
import org.mtransit.android.data.POIArrayAdapter
import org.mtransit.android.data.POIManager
import org.mtransit.android.data.RouteDirectionManager
import org.mtransit.android.databinding.FragmentRdsDirectionStopsBinding
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.provider.FavoriteManager
import org.mtransit.android.provider.permission.LocationPermissionProvider
import org.mtransit.android.provider.sensor.MTSensorManager
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.task.StatusLoader
import org.mtransit.android.ui.fragment.MTFragmentX
import org.mtransit.android.ui.rds.route.RDSRouteFragment
import org.mtransit.android.ui.rds.route.RDSRouteViewModel
import org.mtransit.android.ui.serviceupdates.ServiceUpdatesDialog
import org.mtransit.android.ui.setUpFabEdgeToEdge
import org.mtransit.android.ui.setUpListEdgeToEdge
import org.mtransit.android.ui.setUpMapEdgeToEdge
import org.mtransit.android.ui.setNavBarProtectionEdgeToEdge
import org.mtransit.android.ui.view.MapViewController
import org.mtransit.android.ui.view.common.EventObserver
import org.mtransit.android.ui.view.common.context
import org.mtransit.android.ui.view.common.isAttached
import org.mtransit.android.ui.view.common.isVisible
import org.mtransit.android.util.FragmentUtils
import org.mtransit.commons.FeatureFlags
import javax.inject.Inject

@AndroidEntryPoint
class RDSDirectionStopsFragment : MTFragmentX(R.layout.fragment_rds_direction_stops) {

    companion object {
        private val LOG_TAG = RDSDirectionStopsFragment::class.java.simpleName

        // internal const val SHOW_SERVICE_UPDATE_FAB = false
        internal const val SHOW_SERVICE_UPDATE_FAB = true // ON to filter alerts in stop feed to avoid duplicates warnings signs

        @JvmStatic
        fun newInstance(
            agencyAuthority: String,
            routeId: Long,
            directionId: Long,
            optSelectedStopId: Int? = null,
        ): RDSDirectionStopsFragment {
            return RDSDirectionStopsFragment().apply {
                arguments = bundleOf(
                    RDSDirectionStopsViewModel.EXTRA_AGENCY_AUTHORITY to agencyAuthority,
                    RDSDirectionStopsViewModel.EXTRA_ROUTE_ID to routeId,
                    RDSDirectionStopsViewModel.EXTRA_DIRECTION_ID to directionId,
                    RDSDirectionStopsViewModel.EXTRA_SELECTED_STOP_ID to (optSelectedStopId ?: RDSDirectionStopsViewModel.EXTRA_SELECTED_STOP_ID_DEFAULT),
                )
            }
        }

        private const val TOP_PADDING_SP = 0
        private const val BOTTOM_PADDING_SP = 56
    }

    private var theLogTag: String = LOG_TAG

    override fun getLogTag(): String = this.theLogTag

    private val viewModel by viewModels<RDSDirectionStopsViewModel>()
    private val attachedViewModel get() = if (isAttached()) viewModel else null

    private val parentViewModel by viewModels<RDSRouteViewModel>({ requireParentFragment() })
    private val attachedParentViewModel get() = if (isAttached()) parentViewModel else null

    private var binding: FragmentRdsDirectionStopsBinding? = null

    @Inject
    lateinit var sensorManager: MTSensorManager

    @Inject
    lateinit var dataSourcesRepository: DataSourcesRepository

    @Inject
    lateinit var defaultPrefRepository: DefaultPreferenceRepository

    @Inject
    lateinit var localPreferenceRepository: LocalPreferenceRepository

    @Inject
    lateinit var poiRepository: POIRepository

    @Inject
    lateinit var favoriteManager: FavoriteManager

    @Inject
    lateinit var statusLoader: StatusLoader

    @Inject
    lateinit var serviceUpdateLoader: ServiceUpdateLoader

    @Inject
    lateinit var locationPermissionProvider: LocationPermissionProvider

    private val mapMarkerProvider = object : MapViewController.MapMarkerProvider {

        override fun getPOMarkers(): Collection<MapViewController.POIMarker>? = null

        override fun getPOIs(): Collection<POIManager>? {
            if (!listAdapter.isInitialized) {
                return null
            }
            val pois = mutableSetOf<POIManager>()
            for (i in 0 until listAdapter.poisCount) {
                listAdapter.getItem(i)?.let { pois.add(it) }
            }
            return pois
        }

        override fun getClosestPOI() = listAdapter.closestPOI

        override fun getPOI(uuid: String?) = listAdapter.getItem(uuid)
    }

    @Suppress("DeprecatedCall")
    private val mapViewController: MapViewController by lazy {
        MapViewController(
            logTag,
            mapMarkerProvider,
            null, // DO NOTHING (map click, camera change)
            true,
            true,
            true,
            false,
            false,
            false,
            TOP_PADDING_SP,
            BOTTOM_PADDING_SP,
            false,
            true,
            false,
            true,
            false,
            this.dataSourcesRepository
        ).apply {
            setAutoClickInfoWindow(true)
            setLocationPermissionGranted(locationPermissionProvider.allRequiredPermissionsGranted(requireContext()))
        }
    }

    private val listAdapter: POIArrayAdapter by lazy {
        POIArrayAdapter(
            this,
            this.sensorManager,
            this.dataSourcesRepository,
            this.defaultPrefRepository,
            this.localPreferenceRepository,
            this.poiRepository,
            this.favoriteManager,
            this.statusLoader,
            this.serviceUpdateLoader
        ).apply {
            logTag = this@RDSDirectionStopsFragment.logTag
            setShowExtra(false) // show route short name & direction
            setLocation(attachedParentViewModel?.deviceLocation?.value)
            if (SHOW_SERVICE_UPDATE_FAB) {
                setIgnoredTargetUUIDs(attachedViewModel?.routeDirectionM?.value?.routeDirection?.allUUIDs)
            } else if (RDSRouteFragment.SHOW_SERVICE_UPDATE_IN_TOOLBAR) {
                setIgnoredTargetUUIDs(attachedParentViewModel?.routeM?.value?.route?.allUUIDs)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.mapViewController.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.mapViewController.onViewCreated(view, savedInstanceState)
        binding = FragmentRdsDirectionStopsBinding.bind(view).apply {
            listLayout.list.apply {
                isVisible = listAdapter.isInitialized
                listAdapter.setListView(this)
                setUpListEdgeToEdge()
            }
            fabListMap?.apply {
                setOnClickListener {
                    if (context.resources.getBoolean(R.bool.two_pane)) { // large screen
                        return@setOnClickListener
                    }
                    viewModel.saveShowingListInsteadOfMap(viewModel.showingListInsteadOfMap.value == false) // switching
                }
                setUpFabEdgeToEdge(
                    originalMarginEndDimenRes = R.dimen.fab_mini_margin_end,
                    originalMarginBottomDimenRes = R.dimen.fab_mini_margin_bottom,
                )
            }
            fabServiceUpdate.apply {
                setOnClickListener {
                    val authority = attachedParentViewModel?.authority?.value ?: return@setOnClickListener
                    val routeId = attachedParentViewModel?.routeM?.value?.route?.id ?: return@setOnClickListener
                    val directionId = attachedViewModel?.directionId?.value ?: return@setOnClickListener
                    if (FeatureFlags.F_NAVIGATION) {
                        // TODO navigate to dialog
                    } else {
                        FragmentUtils.replaceDialogFragment(
                            activity ?: return@setOnClickListener,
                            FragmentUtils.DIALOG_TAG,
                            ServiceUpdatesDialog.newInstance(authority, routeId, directionId),
                            null
                        )
                    }
                }
                setUpFabEdgeToEdge(
                    originalMarginEndDimenRes = R.dimen.fab_mini_margin_end,
                    originalMarginBottomDimenRes = R.dimen.fab_mini_margin_bottom_above_fab,
                )
                updateServiceUpdateImg()
            }
            map.setUpMapEdgeToEdge(mapViewController, TOP_PADDING_SP, BOTTOM_PADDING_SP)
        }
        viewModel.routeDirectionM.observe(viewLifecycleOwner) { routeDirectionM ->
            updateServiceUpdateImg(routeDirectionM)
            if (SHOW_SERVICE_UPDATE_FAB) {
                if (listAdapter.setIgnoredTargetUUIDs(routeDirectionM.routeDirection.allUUIDs)) {
                    listAdapter.notifyDataSetChanged(true)
                }
            }
        }
        viewModel.serviceUpdateLoadedEvent.observe(viewLifecycleOwner, EventObserver { triggered ->
            updateServiceUpdateImg()
        })
        parentViewModel.routeM.observe(viewLifecycleOwner) {
            if (SHOW_SERVICE_UPDATE_FAB) {
                // elsewhere
            } else if (RDSRouteFragment.SHOW_SERVICE_UPDATE_IN_TOOLBAR) {
                if (listAdapter.setIgnoredTargetUUIDs(it.route.allUUIDs)) {
                    listAdapter.notifyDataSetChanged(true)
                }
            }
        }
        parentViewModel.colorInt.observe(viewLifecycleOwner) { colorInt ->
            binding?.apply {
                colorInt?.let { colorInt ->
                    fabListMap?.apply {
                        rippleColor = colorInt
                        backgroundTintList = ColorStateList.valueOf(colorInt)
                    }
                    fabServiceUpdate.apply {
                        rippleColor = colorInt
                        backgroundTintList = ColorStateList.valueOf(colorInt)
                    }
                }
            }
        }
        viewModel.directionId.observe(viewLifecycleOwner) { directionId ->
            theLogTag = directionId?.let { "${LOG_TAG}-$it" } ?: LOG_TAG
            listAdapter.logTag = this@RDSDirectionStopsFragment.logTag
            mapViewController.logTag = this@RDSDirectionStopsFragment.logTag
        }
        parentViewModel.deviceLocation.observe(viewLifecycleOwner) { deviceLocation ->
            mapViewController.onDeviceLocationChanged(deviceLocation)
            listAdapter.setLocation(deviceLocation)
        }
        viewModel.showingListInsteadOfMap.observe(viewLifecycleOwner) { showingListInsteadOfMap ->
            showingListInsteadOfMap?.let { listInsteadOfMap ->
                updateFabListMapUI(listInsteadOfMap)
                if (context?.resources?.getBoolean(R.bool.two_pane) == true // LARGE SCREEN
                    || !listInsteadOfMap // MAP
                ) {
                    mapViewController.onResume()
                } else { // LIST
                    mapViewController.onPause()
                }
                switchView(listInsteadOfMap)
            }
        }
        viewModel.selectedStopId.observe(viewLifecycleOwner) {
            // DO NOTHING
        }
        viewModel.closestPOIShown.observe(viewLifecycleOwner) {
            // DO NOTHING
        }
        viewModel.poiList.observe(viewLifecycleOwner) { poiList ->
            var currentSelectedItemIndexUuid: Pair<Int?, String?>? = null
            val selectedStopId = viewModel.selectedStopId.value
            val closestPOIShow = viewModel.closestPOIShown.value
            if (selectedStopId != null || closestPOIShow != true) {
                if (selectedStopId != null) {
                    currentSelectedItemIndexUuid = findStopIndexUuid(selectedStopId, poiList)
                }
                if (currentSelectedItemIndexUuid == null) {
                    if (closestPOIShow == false) {
                        currentSelectedItemIndexUuid = findClosestPOIIndexUuid(poiList)
                    }
                }
                viewModel.setSelectedOrClosestStopShown()
            }
            listAdapter.setPois(poiList)
            listAdapter.updateDistanceNowAsync(parentViewModel.deviceLocation.value)
            mapViewController.notifyMarkerChanged(mapMarkerProvider)
            if (context?.resources?.getBoolean(R.bool.two_pane) == true // LARGE SCREEN
                || viewModel.showingListInsteadOfMap.value == true // LIST
            ) {
                val selectedPosition = currentSelectedItemIndexUuid?.first ?: -1
                if (selectedPosition > 0) {
                    binding?.listLayout?.list?.setSelection(selectedPosition - 1) // show 1 more stop on top of the list
                }
            }
            switchView()
        }
    }

    private fun updateServiceUpdateImg(
        routeDirectionM: RouteDirectionManager? = attachedViewModel?.routeDirectionM?.value,
        fabServiceUpdate: FloatingActionButton? = binding?.fabServiceUpdate,
    ) {
        fabServiceUpdate?.apply {
            routeDirectionM ?: run { isVisible = false; return }
            val (isWarning, isInfo) = routeDirectionM.getServiceUpdates(
                serviceUpdateLoader,
                routeDirectionM.routeDirection.route.allUUIDs
            ).let {
                ServiceUpdate.isSeverityWarning(it) to ServiceUpdate.isSeverityInfo(it)
            }
            if (isWarning) {
                setImageResource(R.drawable.ic_warning_black_24dp)
                isVisible = SHOW_SERVICE_UPDATE_FAB
            } else if (isInfo) {
                setImageResource(R.drawable.ic_info_outline_black_24dp)
                isVisible = SHOW_SERVICE_UPDATE_FAB
            } else {
                isVisible = false
            }
        }
    }

    private fun updateFabListMapUI(showingListInsteadOfMap: Boolean? = viewModel.showingListInsteadOfMap.value) {
        showingListInsteadOfMap ?: return
        isResumed || return
        binding?.fabListMap?.apply {
            if (showingListInsteadOfMap) { // LIST
                setImageResource(R.drawable.switch_action_map_dark_16dp)
                contentDescription = getString(R.string.menu_action_map)
                activity?.setNavBarProtectionEdgeToEdge()
            } else { // MAP
                setImageResource(R.drawable.switch_action_view_headline_dark_16dp)
                contentDescription = getString(R.string.menu_action_list)
                activity?.setNavBarProtectionEdgeToEdge(false)
            }
        }
    }

    private fun findStopIndexUuid(stopId: Int, pois: List<POIManager>?): Pair<Int?, String?>? {
        return pois
            ?.withIndex()
            ?.firstOrNull { (it.value.poi as? RouteDirectionStop)?.stop?.id == stopId }
            ?.let {
                it.index to it.value.poi.uuid
            }
    }

    private fun findClosestPOIIndexUuid(
        pois: List<POIManager>?,
        deviceLocation: Location? = parentViewModel.deviceLocation.value,
    ): Pair<Int?, String?>? {
        if (deviceLocation != null && pois?.isNotEmpty() == true) {
            return pois
                .updateDistance(deviceLocation.latitude, deviceLocation.longitude)
                .findClosestPOISIdxUuid()
                .firstOrNull()
        }
        return null
    }

    private fun switchView(showingListInsteadOfMap: Boolean? = viewModel.showingListInsteadOfMap.value) {
        binding?.apply {
            when {
                !listAdapter.isInitialized || showingListInsteadOfMap == null -> { // LOADING
                    emptyLayout.isVisible = false
                    listLayout.isVisible = false
                    mapViewController.hideMap()
                    loadingLayout.isVisible = true
                }

                listAdapter.poisCount == 0 -> { // EMPTY
                    loadingLayout.isVisible = false
                    listLayout.isVisible = false
                    mapViewController.hideMap()
                    emptyLayout.isVisible = true
                }

                else -> {
                    loadingLayout.isVisible = false
                    emptyLayout.isVisible = false
                    if (context.resources.getBoolean(R.bool.two_pane)) { // LARGE SCREEN
                        listLayout.isVisible = true
                        mapViewController.showMap(view)
                    } else if (showingListInsteadOfMap) { // LIST
                        mapViewController.hideMap()
                        listLayout.isVisible = true
                    } else { // MAP
                        listLayout.isVisible = false
                        mapViewController.showMap(view)
                    }
                }
            }
        }
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        mapViewController.apply {
            setDataSourcesRepository(dataSourcesRepository)
            onAttach(requireActivity())
            setLocationPermissionGranted(locationPermissionProvider.allRequiredPermissionsGranted(context))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        mapViewController.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDetach() {
        super.onDetach()
        this.mapViewController.onDetach()
    }

    override fun onResume() {
        super.onResume()
        if (context?.resources?.getBoolean(R.bool.two_pane) == true // LARGE SCREEN
            || viewModel.showingListInsteadOfMap.value == false  // MAP
        ) {
            mapViewController.onResume()
        }
        listAdapter.onResume(this, parentViewModel.deviceLocation.value)
        updateFabListMapUI()
        switchView()
    }

    override fun onPause() {
        super.onPause()
        mapViewController.onPause()
        listAdapter.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapViewController.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listAdapter.onDestroyView()
        mapViewController.onDestroyView()
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        listAdapter.onDestroy()
        mapViewController.onDestroy()
    }

    override fun <T : View?> findViewById(id: Int) = this.view?.findViewById<T>(id)
}