@file:JvmName("RTSTripStopsFragment") // ANALYTICS
package org.mtransit.android.ui.rts.route.trip

import android.content.Context
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.AbsListView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.commons.CollectionUtils
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.commons.data.RouteTripStop
import org.mtransit.android.data.POIArrayAdapter
import org.mtransit.android.data.POIManager
import org.mtransit.android.databinding.FragmentRtsTripStopsBinding
import org.mtransit.android.databinding.LayoutEmptyBinding
import org.mtransit.android.databinding.LayoutPoiListBinding
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.provider.FavoriteManager
import org.mtransit.android.provider.permission.LocationPermissionProvider
import org.mtransit.android.provider.sensor.MTSensorManager
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.task.StatusLoader
import org.mtransit.android.ui.fragment.MTFragmentX
import org.mtransit.android.ui.rts.route.RTSRouteViewModel
import org.mtransit.android.ui.view.MapViewController
import org.mtransit.android.ui.view.common.IActivity
import java.util.ArrayList
import javax.inject.Inject

@AndroidEntryPoint
class RTSTripStopsFragment : MTFragmentX(R.layout.fragment_rts_trip_stops), IActivity {

    companion object {
        private val LOG_TAG = RTSTripStopsFragment::class.java.simpleName

        @JvmStatic
        fun newInstance(
            agencyAuthority: String,
            tripId: Long,
            optSelectedStopId: Int? = null,
        ): RTSTripStopsFragment {
            return RTSTripStopsFragment().apply {
                arguments = bundleOf(
                    RTSTripStopsViewModel.EXTRA_AGENCY_AUTHORITY to agencyAuthority,
                    RTSTripStopsViewModel.EXTRA_TRIP_ID to tripId,
                    RTSTripStopsViewModel.EXTRA_SELECTED_TRIP_STOP_ID to optSelectedStopId,
                )
            }
        }
    }

    private var theLogTag: String = LOG_TAG

    override fun getLogTag(): String = this.theLogTag

    private val viewModel by viewModels<RTSTripStopsViewModel>()
    private val parentViewModel by viewModels<RTSRouteViewModel>({ requireParentFragment() })

    private var binding: FragmentRtsTripStopsBinding? = null
    private var emptyBinding: LayoutEmptyBinding? = null
    private var listBinding: LayoutPoiListBinding? = null

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
    lateinit var locationPermissionProvider: LocationPermissionProvider

    private val mapMarkerProvider = object : MapViewController.MapMarkerProvider {

        override fun getPOMarkers(): Collection<MapViewController.POIMarker>? = null

        override fun getPOIs(): Collection<POIManager>? {
            if (!adapter.isInitialized) {
                return null
            }
            val pois = mutableSetOf<POIManager>()
            for (i in 0 until adapter.poisCount) {
                adapter.getItem(i)?.let { pois.add(it) }
            }
            return pois
        }

        override fun getClosestPOI() = adapter.closestPOI

        override fun getPOI(uuid: String?) = adapter.getItem(uuid)
    }

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
            0,
            false,
            true,
            false,
            true,
            false,
            this.dataSourcesRepository
        ).apply {
            setLocationPermissionGranted(locationPermissionProvider.permissionsGranted(requireContext()))
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
            setShowExtra(false)
            setLocation(parentViewModel.deviceLocation.value)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.mapViewController.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.mapViewController.onViewCreated(view, savedInstanceState)
        binding = FragmentRtsTripStopsBinding.bind(view).apply {
            emptyStub.setOnInflateListener { _, inflated ->
                emptyBinding = LayoutEmptyBinding.bind(inflated)
            }
            listStub.setOnInflateListener { _, inflated ->
                listBinding = LayoutPoiListBinding.bind(inflated)
            }
            (listBinding?.root ?: listStub.inflate() as AbsListView).let { listView ->
                listView.isVisible = false // hide by default
                adapter.setListView(listView)
            }
        }
        viewModel.tripId.observe(viewLifecycleOwner, { tripId ->
            theLogTag = tripId?.let { "${LOG_TAG}-$it" } ?: LOG_TAG
            adapter.logTag = logTag
            mapViewController.logTag = logTag
        })
        parentViewModel.deviceLocation.observe(viewLifecycleOwner, { deviceLocation ->
            mapViewController.onDeviceLocationChanged(deviceLocation)
            adapter.setLocation(deviceLocation)
        })
        parentViewModel.showingListInsteadOfMap.observe(viewLifecycleOwner, { showingListInsteadOfMap ->
            showingListInsteadOfMap?.let { listInsteadOfMap ->
                if (listInsteadOfMap) { // LIST
                    mapViewController.onPause()
                } else { // MAP
                    mapViewController.onResume()
                }
            }
            switchView(showingListInsteadOfMap)
        })
        viewModel.selectedTripStopId.observe(viewLifecycleOwner, {
            // DO NOTHING
        })
        viewModel.closestPOIShown.observe(viewLifecycleOwner, {
            // DO NOTHING
        })
        viewModel.poiList.observe(viewLifecycleOwner, { poiList ->
            var currentSelectedItemIndexUuid: Pair<Int?, String?>? = null
            val selectedStopId = viewModel.selectedTripStopId.value
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

            adapter.setPois(poiList)
            adapter.updateDistanceNowAsync(parentViewModel.deviceLocation.value)
            mapViewController.notifyMarkerChanged(mapMarkerProvider)
            if (parentViewModel.showingListInsteadOfMap.value == true) { // LIST
                val selectedPosition = currentSelectedItemIndexUuid?.first ?: -1
                if (selectedPosition > 0) {
                    binding?.apply {
                        (listBinding?.root ?: listStub.inflate() as AbsListView).setSelection(selectedPosition - 1) // show 1 more stop on top of the list
                    }
                }
            }
            switchView()
        })
    }

    private fun findStopIndexUuid(stopId: Int, pois: List<POIManager>?): Pair<Int?, String?>? {
        return pois
            ?.withIndex()
            ?.firstOrNull { (it.value.poi as? RouteTripStop)?.stop?.id == stopId }
            ?.let {
                it.index to it.value.poi.uuid
            }
    }

    private fun findClosestPOIIndexUuid(
        pois: List<POIManager>?,
        deviceLocation: Location? = parentViewModel.deviceLocation.value
    ): Pair<Int?, String?>? {
        if (deviceLocation != null && pois?.isNotEmpty() == true) {
            LocationUtils.updateDistance(pois, deviceLocation.latitude, deviceLocation.longitude)
            val sortedPOIs = ArrayList(pois)
            CollectionUtils.sort(sortedPOIs, LocationUtils.POI_DISTANCE_COMPARATOR)
            val closestPoiUuid = sortedPOIs.getOrNull(0)?.poi?.uuid ?: return null
            for (idx in pois.indices) {
                val poim = pois[idx]
                if (poim.poi.uuid == closestPoiUuid) {
                    return idx to poim.poi.uuid
                }
            }
        }
        return null
    }

    fun switchView(showingListInsteadOfMap: Boolean? = parentViewModel.showingListInsteadOfMap.value) {
        binding?.apply {
            when {
                !adapter.isInitialized || showingListInsteadOfMap == null -> {
                    emptyBinding?.root?.isVisible = false
                    listBinding?.root?.isVisible = false
                    mapViewController.hideMap()
                    loading.root.isVisible = true
                }
                adapter.poisCount == 0 -> {
                    loading.root.isVisible = false
                    listBinding?.root?.isVisible = false
                    mapViewController.hideMap()
                    (emptyBinding?.root ?: emptyStub.inflate()).isVisible = true
                }
                else -> {
                    loading.root.isVisible = false
                    emptyBinding?.root?.isVisible = false
                    if (showingListInsteadOfMap) { // LIST
                        mapViewController.hideMap()
                        (listBinding?.root ?: listStub.inflate()).isVisible = true
                    } else { // MAP
                        listBinding?.root?.isVisible = false
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
            setLocationPermissionGranted(locationPermissionProvider.permissionsGranted(context))
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
        if (parentViewModel.showingListInsteadOfMap.value == false) { // MAP
            mapViewController.onResume()
        }
        adapter.onResume(this, parentViewModel.deviceLocation.value)
        switchView()
    }

    override fun onPause() {
        super.onPause()
        mapViewController.onPause()
        adapter.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapViewController.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapViewController.onDestroyView()
        emptyBinding = null
        listBinding = null
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        mapViewController.onDestroy()
    }

    override fun getLifecycleOwner() = this

    override fun finish() {
        activity?.finish()
    }

    override fun <T : View?> findViewById(id: Int): T? = view?.findViewById<T>(id)
}