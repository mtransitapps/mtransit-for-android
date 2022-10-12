@file:JvmName("AgencyPOIsFragment") // ANALYTICS
package org.mtransit.android.ui.type.poi

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.data.POIArrayAdapter
import org.mtransit.android.data.POIManager
import org.mtransit.android.databinding.FragmentAgencyPoisBinding
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.provider.FavoriteManager
import org.mtransit.android.provider.permission.LocationPermissionProvider
import org.mtransit.android.provider.sensor.MTSensorManager
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.task.StatusLoader
import org.mtransit.android.ui.fragment.MTFragmentX
import org.mtransit.android.ui.type.AgencyTypeViewModel
import org.mtransit.android.ui.view.MapViewController
import org.mtransit.android.ui.view.MapViewController.POIMarker
import org.mtransit.android.ui.view.common.IActivity
import org.mtransit.android.ui.view.common.isAttached
import org.mtransit.android.ui.view.common.isVisible
import javax.inject.Inject

@AndroidEntryPoint
class AgencyPOIsFragment : MTFragmentX(R.layout.fragment_agency_pois), IActivity {

    companion object {
        private val LOG_TAG = AgencyPOIsFragment::class.java.simpleName

        @JvmStatic
        fun newInstance(
            agencyAuthority: String,
            optColorInt: Int? = null,
        ): AgencyPOIsFragment {
            return AgencyPOIsFragment().apply {
                arguments = bundleOf(
                    AgencyPOIsViewModel.EXTRA_AGENCY_AUTHORITY to agencyAuthority,
                    AgencyPOIsViewModel.EXTRA_COLOR_INT to optColorInt,
                )
            }
        }
    }

    private var theLogTag: String = LOG_TAG

    override fun getLogTag(): String = this.theLogTag

    private val viewModel by viewModels<AgencyPOIsViewModel>()
    private val attachedViewModel
        get() = if (isAttached()) viewModel else null

    private val parentViewModel by viewModels<AgencyTypeViewModel>({ requireParentFragment() })
    private val attachedParentViewModel
        get() = if (isAttached()) parentViewModel else null

    private var binding: FragmentAgencyPoisBinding? = null

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

        override fun getPOMarkers(): Collection<POIMarker>? = null

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
            56,
            false,
            true,
            false,
            true,
            false,
            this.dataSourcesRepository
        ).apply {
            setLocationPermissionGranted(locationPermissionProvider.allRequiredPermissionsGranted(requireContext()))
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
            setPois(attachedViewModel?.poiList?.value)
            setLocation(attachedParentViewModel?.deviceLocation?.value)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.mapViewController.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.mapViewController.onViewCreated(view, savedInstanceState)
        binding = FragmentAgencyPoisBinding.bind(view).apply {
            listLayout.list.let { listView ->
                listView.isVisible = adapter.isInitialized
                adapter.setListView(listView)
                fabListMap?.setOnClickListener {
                    if (context?.resources?.getBoolean(R.bool.two_pane) == true) { // LARGE SCREEN
                        return@setOnClickListener
                    }
                    viewModel.saveShowingListInsteadOfMap(viewModel.showingListInsteadOfMap.value == false) // switching
                }
            }
        }
        viewModel.colorInt.observe(viewLifecycleOwner) { colorInt ->
            colorInt?.let {
                binding?.fabListMap?.apply {
                    rippleColor = colorInt
                    backgroundTintList = ColorStateList.valueOf(colorInt)
                }
            }
        }
        viewModel.agency.observe(viewLifecycleOwner) { agency ->
            theLogTag = agency?.shortName?.let { "${LOG_TAG}-$it" } ?: LOG_TAG
            adapter.logTag = logTag
            mapViewController.logTag = logTag
        }
        parentViewModel.deviceLocation.observe(viewLifecycleOwner) { deviceLocation ->
            mapViewController.onDeviceLocationChanged(deviceLocation)
            adapter.setLocation(deviceLocation)
        }
        viewModel.showingListInsteadOfMap.observe(viewLifecycleOwner) { showingListInsteadOfMap ->
            showingListInsteadOfMap?.let { listInsteadOfMap ->
                binding?.fabListMap?.apply {
                    @Suppress("LiftReturnOrAssignment")
                    if (listInsteadOfMap) { // LIST
                        setImageResource(R.drawable.switch_action_map_dark_16dp)
                        contentDescription = getString(R.string.menu_action_map)
                    } else { // MAP
                        setImageResource(R.drawable.switch_action_view_headline_dark_16dp)
                        contentDescription = getString(R.string.menu_action_list)
                    }
                }
                if (context?.resources?.getBoolean(R.bool.two_pane) == true // LARGE SCREEN
                    || !listInsteadOfMap // MAP
                ) {
                    mapViewController.onResume()
                } else { // LIST
                    mapViewController.onPause()
                }
            }
            switchView(showingListInsteadOfMap)
        }
        viewModel.poiList.observe(viewLifecycleOwner) { poiList ->
            adapter.setPois(poiList)
            adapter.updateDistanceNowAsync(parentViewModel.deviceLocation.value)
            mapViewController.notifyMarkerChanged(mapMarkerProvider)
            switchView()
        }
    }

    private fun switchView(showingListInsteadOfMap: Boolean? = viewModel.showingListInsteadOfMap.value) {
        binding?.apply {
            when {
                !adapter.isInitialized || showingListInsteadOfMap == null -> { // LOADING
                    emptyLayout.isVisible = false
                    listLayout.isVisible = false
                    mapViewController.hideMap()
                    loadingLayout.isVisible = true
                }
                adapter.poisCount == 0 -> { // EMPTY
                    loadingLayout.isVisible = false
                    listLayout.isVisible = false
                    mapViewController.hideMap()
                    emptyLayout.isVisible = true
                }
                else -> {
                    loadingLayout.isVisible = false
                    emptyLayout.isVisible = false
                    if (context?.resources?.getBoolean(R.bool.two_pane) == true) { // LARGE SCREEN
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
            || viewModel.showingListInsteadOfMap.value == false // MAP
        ) {
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