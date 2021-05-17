@file:JvmName("SearchFragment") // ANALYTICS
package org.mtransit.android.ui.search

import android.content.Context
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import org.mtransit.android.R
import org.mtransit.android.commons.KeyboardUtils.Companion.hideKeyboard
import org.mtransit.android.commons.PreferenceUtils
import org.mtransit.android.commons.ToastUtils
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.POIArrayAdapter
import org.mtransit.android.data.POIArrayAdapter.TypeHeaderButtonsClickListener
import org.mtransit.android.databinding.FragmentSearchBinding
import org.mtransit.android.databinding.LayoutEmptyBinding
import org.mtransit.android.databinding.LayoutPoiListBinding
import org.mtransit.android.ui.MTActivityWithLocation
import org.mtransit.android.ui.MTActivityWithLocation.UserLocationListener
import org.mtransit.android.ui.MainActivity
import org.mtransit.android.ui.fragment.ABFragment
import org.mtransit.android.ui.view.MTSearchView

class SearchFragment : ABFragment(R.layout.fragment_search), UserLocationListener, TypeHeaderButtonsClickListener, OnItemSelectedListener {

    companion object {
        private val LOG_TAG = SearchFragment::class.java.simpleName

        private const val TRACKING_SCREEN_NAME = "Search"

        @JvmStatic
        fun newInstance(optQuery: String?, optTypeIdFilter: Int?): SearchFragment {
            return SearchFragment().apply {
                arguments = Bundle().apply {
                    putString(SearchViewModel.EXTRA_QUERY, optQuery?.trim() ?: SearchViewModel.EXTRA_QUERY_DEFAULT)
                    optTypeIdFilter?.let {
                        putInt(SearchViewModel.EXTRA_TYPE_FILTER, it)
                    }
                }
            }
        }

        private const val DEV_QUERY = "MTDEV"
    }

    override fun getLogTag(): String = LOG_TAG

    override fun getScreenName(): String = TRACKING_SCREEN_NAME

    private val viewModel by viewModels<SearchViewModel>()

    private var binding: FragmentSearchBinding? = null
    private var emptyBinding: LayoutEmptyBinding? = null
    private var listBinding: LayoutPoiListBinding? = null

    private val adapter: POIArrayAdapter by lazy {
        POIArrayAdapter(this).apply {
            setLogTag(logTag)
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
            typeFilters.apply {
                onItemSelectedListener = this@SearchFragment
                adapter = typeFilterAdapter
            }
        }
        viewModel.query.observe(viewLifecycleOwner, { query ->
            (emptyBinding?.root ?: binding?.emptyStub?.inflate())?.isVisible = false // hide by default
            emptyBinding?.emptyText?.text = if (query.isNullOrEmpty()) {
                getString(R.string.search_hint)
            } else {
                getString(R.string.search_no_result_for_and_query, query)
            }
            if (query.isNullOrEmpty()) {
                adapter.setPois(emptyList()) // empty search = no result
                binding?.loading?.root?.isVisible = false // hide
                listBinding?.root?.isVisible = false // hide (if inflated)
                emptyBinding?.root?.isVisible = true // show
            }
        })
        viewModel.loading.observe(viewLifecycleOwner, { loading ->
            if (loading) {
                adapter.clear() // mark not initialized == loading
                listBinding?.root?.isVisible = false // hide (if inflated)
                emptyBinding?.root?.isVisible = false // hide (if inflated)
                binding?.loading?.root?.isVisible = true // show
            }
        })
        viewModel.searchResults.observe(viewLifecycleOwner, { searchResults ->
            adapter.setPois(searchResults)
            adapter.updateDistanceNowAsync(viewModel.deviceLocation.value)
            binding?.loading?.root?.isVisible = false
            if (searchResults.isEmpty()) { // SHOW EMPTY
                listBinding?.root?.isVisible = false
                (emptyBinding?.root ?: binding?.emptyStub?.inflate())?.isVisible = true
            } else { // SHOW LIST
                emptyBinding?.root?.isVisible = false
                (listBinding?.root ?: binding?.listStub?.inflate())?.isVisible = true
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
        viewModel.onDeviceLocationChanged(newLocation)
    }

    private var devEnabled: Boolean? = null

    fun setSearchQuery(
        query: String?,
        @Suppress("UNUSED_PARAMETER") alreadyInSearchView: Boolean
    ) {
        if (DEV_QUERY == query) {
            devEnabled = devEnabled != true // flip
            PreferenceUtils.savePrefLcl(context, PreferenceUtils.PREFS_LCL_DEV_MODE_ENABLED, devEnabled, false) // ASYNC
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
            viewModel.setSearchHasFocus(focus)
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
            setQuery(viewModel.query.value, false)
            if (viewModel.searchHasFocus.value == false) {
                clearFocus()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listBinding = null
        emptyBinding = null
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        searchView = null // part of the activity
    }
}