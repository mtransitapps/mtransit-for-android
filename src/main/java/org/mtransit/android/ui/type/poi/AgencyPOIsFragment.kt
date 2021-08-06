@file:JvmName("AgencyPOIsFragment") // ANALYTICS
package org.mtransit.android.ui.type.poi

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.util.StateSet
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.res.ResourcesCompat
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

    private var listMapToggleMenuItem: MenuItem? = null
    private var listMapSwitchMenuItem: SwitchCompat? = null

    private val listMapToggleSelector: StateListDrawable by lazy {
        StateListDrawable().apply {
            (ResourcesCompat.getDrawable(resources, R.drawable.switch_thumb_list, requireContext().theme) as? LayerDrawable)?.apply {
                attachedViewModel?.colorInt?.value?.let { (findDrawableByLayerId(R.id.switch_list_oval_shape) as? GradientDrawable)?.setColor(it) }
                addState(intArrayOf(android.R.attr.state_checked), this)
            }
            (ResourcesCompat.getDrawable(resources, R.drawable.switch_thumb_map, requireContext().theme) as? LayerDrawable)?.apply {
                attachedViewModel?.colorInt?.value?.let { (findDrawableByLayerId(R.id.switch_map_oval_shape) as? GradientDrawable)?.setColor(it) }
                addState(StateSet.WILD_CARD, this)
            }
        }
    }

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
            setPois(attachedViewModel?.poiList?.value)
            setLocation(attachedParentViewModel?.deviceLocation?.value)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        this.mapViewController.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.mapViewController.onViewCreated(view, savedInstanceState)
        binding = FragmentAgencyPoisBinding.bind(view).apply {
            this.listLayout.list.let { listView ->
                listView.isVisible = adapter.isInitialized
                adapter.setListView(listView)
            }
        }
        viewModel.colorInt.observe(viewLifecycleOwner, { colorInt ->
            colorInt?.let {
                activity?.invalidateOptionsMenu() // initialize action bar list/map switch icon
            }
        })
        viewModel.agency.observe(viewLifecycleOwner, { agency ->
            theLogTag = agency?.shortName?.let { "${LOG_TAG}-$it" } ?: LOG_TAG
            adapter.logTag = logTag
            mapViewController.logTag = logTag
        })
        parentViewModel.deviceLocation.observe(viewLifecycleOwner, { deviceLocation ->
            mapViewController.onDeviceLocationChanged(deviceLocation)
            adapter.setLocation(deviceLocation)
        })
        viewModel.showingListInsteadOfMap.observe(viewLifecycleOwner, { showingListInsteadOfMap ->
            showingListInsteadOfMap?.let { listInsteadOfMap ->
                if (listInsteadOfMap) { // LIST
                    mapViewController.onPause()
                } else { // MAP
                    mapViewController.onResume()
                }
            }
            switchView(showingListInsteadOfMap)
            updateListMapToggleMenuItem()
        })
        viewModel.poiList.observe(viewLifecycleOwner, { poiList ->
            adapter.setPois(poiList)
            adapter.updateDistanceNowAsync(parentViewModel.deviceLocation.value)
            mapViewController.notifyMarkerChanged(mapMarkerProvider)
            switchView()
        })
    }

    private fun switchView(showingListInsteadOfMap: Boolean? = viewModel.showingListInsteadOfMap.value) {
        binding?.apply {
            when {
                !adapter.isInitialized || showingListInsteadOfMap == null -> {
                    emptyLayout.isVisible = false
                    listLayout.isVisible = false
                    mapViewController.hideMap()
                    loadingLayout.isVisible = true
                }
                adapter.poisCount == 0 -> {
                    loadingLayout.isVisible = false
                    listLayout.isVisible = false
                    mapViewController.hideMap()
                    emptyLayout.isVisible = true
                }
                else -> {
                    loadingLayout.isVisible = false
                    emptyLayout.isVisible = false
                    if (showingListInsteadOfMap) { // LIST
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if (isResumed) {
            if (menu.findItem(R.id.menu_toggle_list_map) == null) {
                inflater.inflate(R.menu.menu_agency_pois, menu)
            }
            listMapToggleMenuItem = menu.findItem(R.id.menu_toggle_list_map)
            listMapSwitchMenuItem = listMapToggleMenuItem?.actionView?.findViewById(R.id.action_bar_switch_list_map)
            listMapSwitchMenuItem?.thumbDrawable = listMapToggleSelector
        } else {
            listMapSwitchMenuItem?.setOnCheckedChangeListener(null)
            listMapSwitchMenuItem?.visibility = View.GONE
            listMapSwitchMenuItem = null
            listMapToggleMenuItem?.isVisible = false
            listMapToggleMenuItem = null
        }
        updateListMapToggleMenuItem()
    }

    private fun updateListMapToggleMenuItem() {
        if (!isResumed) {
            return
        }
        listMapSwitchMenuItem?.isChecked = viewModel.showingListInsteadOfMap.value != false
        listMapSwitchMenuItem?.setOnCheckedChangeListener { buttonView: CompoundButton, isChecked: Boolean ->
            onCheckedChanged(buttonView, isChecked)
        }
        listMapSwitchMenuItem?.isVisible = viewModel.showingListInsteadOfMap.value != null
        listMapToggleMenuItem?.isVisible = viewModel.showingListInsteadOfMap.value != null
    }

    private fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        if (!isResumed) {
            return
        }
        if (buttonView.id == R.id.action_bar_switch_list_map) {
            viewModel.saveShowingListInsteadOfMap(isChecked)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (!isResumed) {
            if (item.itemId == R.id.action_bar_switch_list_map) {
                viewModel.saveShowingListInsteadOfMap(viewModel.showingListInsteadOfMap.value == false) // switching
                return true // handled
            }
        }
        return super.onOptionsItemSelected(item)
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
        if (viewModel.showingListInsteadOfMap.value == false) { // MAP
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