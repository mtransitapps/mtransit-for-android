@file:JvmName("SearchFragment") // ANALYTICS
package org.mtransit.android.ui.search

import android.content.Context
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.common.repository.LocalPreferenceRepository
import org.mtransit.android.commons.KeyboardUtils.Companion.hideKeyboard
import org.mtransit.android.commons.ToastUtils
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.POIArrayAdapter
import org.mtransit.android.data.POIArrayAdapter.TypeHeaderButtonsClickListener
import org.mtransit.android.databinding.FragmentSearchBinding
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.provider.FavoriteManager
import org.mtransit.android.provider.sensor.MTSensorManager
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.task.StatusLoader
import org.mtransit.android.ui.MTActivityWithLocation
import org.mtransit.android.ui.MTActivityWithLocation.UserLocationListener
import org.mtransit.android.ui.MainActivity
import org.mtransit.android.ui.fragment.ABFragment
import org.mtransit.android.ui.view.MTSearchView
import org.mtransit.android.ui.view.common.isAttached
import org.mtransit.android.ui.view.common.isVisible
import javax.inject.Inject

@AndroidEntryPoint
class SearchFragment : ABFragment(R.layout.fragment_search), UserLocationListener, TypeHeaderButtonsClickListener, OnItemSelectedListener {

    companion object {
        private val LOG_TAG = SearchFragment::class.java.simpleName

        private const val TRACKING_SCREEN_NAME = "Search"

        @JvmOverloads
        @JvmStatic
        fun newInstance(
            optQuery: String? = null,
            optTypeIdFilter: Int? = null
        ): SearchFragment {
            return SearchFragment().apply {
                arguments = bundleOf(
                    SearchViewModel.EXTRA_QUERY to (optQuery?.trim() ?: SearchViewModel.EXTRA_QUERY_DEFAULT),
                    SearchViewModel.EXTRA_TYPE_FILTER to optTypeIdFilter,
                )
            }
        }

        private const val DEV_QUERY = "MTDEV"
    }

    override fun getLogTag(): String = LOG_TAG

    override fun getScreenName(): String = TRACKING_SCREEN_NAME

    private val viewModel by viewModels<SearchViewModel>()
    private val attachedViewModel
        get() = if (isAttached()) viewModel else null

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
    lateinit var lclPrefRepository: LocalPreferenceRepository

    private var binding: FragmentSearchBinding? = null

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
            setOnTypeHeaderButtonsClickListener(this@SearchFragment)
            setPois(emptyList()) // empty search = no result
        }
    }

    private val typeFilterAdapter: SearchTypeFilterAdapter by lazy { SearchTypeFilterAdapter(requireContext()) }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.adapter.setActivity(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSearchBinding.bind(view).apply {
            listLayout.list.let { listView ->
                listView.isVisible = adapter.isInitialized
                adapter.setListView(listView)
            }
            typeFilters.apply {
                onItemSelectedListener = this@SearchFragment
                adapter = typeFilterAdapter
            }
        }
        viewModel.query.observe(viewLifecycleOwner, { query ->
            binding?.apply {
                emptyLayout.isVisible = false // hide by default
                emptyLayout.emptyText.text = if (query.isNullOrEmpty()) {
                    getString(R.string.search_hint)
                } else {
                    getString(R.string.search_no_result_for_and_query, query)
                }
                if (query.isNullOrEmpty()) {
                    adapter.setPois(emptyList()) // empty search = no result
                    loadingLayout.isVisible = false // hide
                    listLayout.isVisible = false // hide
                    emptyLayout.isVisible = true // show
                }
            }
        })
        viewModel.loading.observe(viewLifecycleOwner, { loading ->
            if (loading) {
                adapter.clear() // mark not initialized == loading
                binding?.apply {
                    listLayout.isVisible = false // hide
                    emptyLayout.isVisible = false // hide
                    loadingLayout.isVisible = true // show
                }
            }
        })
        viewModel.searchResults.observe(viewLifecycleOwner, { searchResults ->
            adapter.setPois(searchResults)
            adapter.updateDistanceNowAsync(viewModel.deviceLocation.value)
            binding?.apply {
                loadingLayout.isVisible = false
                if (searchResults.isEmpty()) { // SHOW EMPTY
                    listLayout.isVisible = false
                    emptyLayout.isVisible = true
                } else { // SHOW LIST
                    emptyLayout.isVisible = false
                    listLayout.isVisible = true
                }
            }
        })
        viewModel.deviceLocation.observe(viewLifecycleOwner, { deviceLocation ->
            adapter.setLocation(deviceLocation)
        })
        viewModel.searchableDataSourceTypes.observe(viewLifecycleOwner, { dstList ->
            typeFilterAdapter.setData(dstList)
        })
        viewModel.typeFilter.observe(viewLifecycleOwner, { dst ->
            binding?.typeFilters?.apply {
                setSelection(typeFilterAdapter.getPosition(dst))
                isVisible = dst != null
            }
            adapter.setShowTypeHeader(if (dst == null) POIArrayAdapter.TYPE_HEADER_MORE else POIArrayAdapter.TYPE_HEADER_NONE)
        })
    }

    override fun onTypeHeaderButtonClick(buttonId: Int, type: DataSourceType): Boolean {
        if (buttonId == TypeHeaderButtonsClickListener.BUTTON_MORE) {
            hideKeyboard(activity, view)
            viewModel.setTypeFilter(type)
            return true // handled
        }
        return false // not handled
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val dst = typeFilterAdapter.getItem(position)
        viewModel.setTypeFilter(dst)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // DO NOTHING
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
        attachedViewModel?.onDeviceLocationChanged(newLocation)
    }

    private var devEnabled: Boolean? = null

    fun setSearchQuery(
        query: String?,
        @Suppress("UNUSED_PARAMETER") alreadyInSearchView: Boolean
    ) {
        if (DEV_QUERY == query) {
            devEnabled = devEnabled != true // flip
            lclPrefRepository.saveAsync(LocalPreferenceRepository.PREFS_LCL_DEV_MODE_ENABLED, devEnabled)
            ToastUtils.makeTextAndShowCentered(context, "DEV MODE: $devEnabled")
            return
        }
        viewModel.onNewQuery(query)
    }

    override fun isABReady(): Boolean {
        return searchView != null
    }

    override fun isABShowSearchMenuItem(): Boolean {
        return false
    }

    override fun isABCustomViewFocusable(): Boolean {
        return true
    }

    override fun isABCustomViewRequestFocus(): Boolean {
        return searchHasFocus()
    }

    private fun searchHasFocus(): Boolean {
        return refreshSearchHasFocus()
    }

    private fun refreshSearchHasFocus(): Boolean {
        return searchView?.let {
            val focus = it.hasFocus()
            attachedViewModel?.setSearchHasFocus(focus)
            focus
        } ?: false
    }

    override fun getABCustomView(): View? {
        return getSearchView()
    }

    private var searchView: MTSearchView? = null

    private fun getSearchView(): MTSearchView? {
        if (searchView == null) {
            initSearchView()
        }
        return searchView
    }

    private fun initSearchView() {
        val activity = activity ?: return
        val mainActivity = activity as MainActivity
        val supportActionBar = mainActivity.supportActionBar
        val context = if (supportActionBar == null) mainActivity else supportActionBar.themedContext
        searchView = MTSearchView(mainActivity, context).apply {
            setQuery(attachedViewModel?.query?.value, false)
            if (attachedViewModel?.searchHasFocus?.value == false) {
                clearFocus()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        searchView = null // part of the activity
    }
}