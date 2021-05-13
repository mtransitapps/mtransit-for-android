package org.mtransit.android.ui.nearby.type

import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.AbsListView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import org.mtransit.android.R
import org.mtransit.android.commons.ThemeUtils
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.POIArrayAdapter
import org.mtransit.android.databinding.FragmentNearbyAgencyTypeBinding
import org.mtransit.android.databinding.LayoutEmptyBinding
import org.mtransit.android.databinding.LayoutPoiListBinding
import org.mtransit.android.ui.MTActivityWithLocation
import org.mtransit.android.ui.MTActivityWithLocation.UserLocationListener
import org.mtransit.android.ui.fragment.MTFragmentX
import org.mtransit.android.ui.nearby.NearbyViewModel
import org.mtransit.android.ui.view.common.IActivity

class NearbyAgencyTypeFragment : MTFragmentX(R.layout.fragment_nearby_agency_type), UserLocationListener, IActivity {

    companion object {
        private val LOG_TAG = NearbyAgencyTypeFragment::class.java.simpleName

        @JvmStatic
        fun newInstance(
            type: DataSourceType,
        ): NearbyAgencyTypeFragment {
            return newInstance(type.id)
        }

        @JvmStatic
        fun newInstance(
            typeId: Int,
        ): NearbyAgencyTypeFragment {
            return NearbyAgencyTypeFragment().apply {
                arguments = bundleOf(
                    NearbyAgencyTypeViewModel.EXTRA_TYPE_ID to typeId,
                )
            }
        }
    }

    private var theLogTag: String = LOG_TAG

    override fun getLogTag(): String = this.theLogTag

    private val viewModel by viewModels<NearbyAgencyTypeViewModel>()
    private val parentViewModel by viewModels<NearbyViewModel>({ requireParentFragment() })

    private var binding: FragmentNearbyAgencyTypeBinding? = null
    private var emptyBinding: LayoutEmptyBinding? = null
    private var listBinding: LayoutPoiListBinding? = null

    private val infiniteLoadingListener = object : POIArrayAdapter.InfiniteLoadingListener {
        override fun isLoadingMore(): Boolean {
            return viewModel.isLoadingMore()
        }

        override fun showingDone() = true
    }

    private val adapter: POIArrayAdapter by lazy {
        POIArrayAdapter(this).apply {
            logTag = logTag
            setInfiniteLoading(true)
            setInfiniteLoadingListener(infiniteLoadingListener)
            setPois(viewModel.nearbyPOIs.value)
            setLocation(viewModel.deviceLocation.value)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentNearbyAgencyTypeBinding.bind(view).apply {
            emptyStub.setOnInflateListener { _, inflated ->
                emptyBinding = LayoutEmptyBinding.bind(inflated)
            }
            listStub.setOnInflateListener { _, inflated ->
                listBinding = LayoutPoiListBinding.bind(inflated).apply {
                    swiperefresh.setListViewWR(this.root)
                }
            }
            (listBinding?.root ?: listStub.inflate() as AbsListView).let { listView ->
                listView.isVisible = false // hide by default
                adapter.setListView(listView)
            }
            swiperefresh.apply {
                setColorSchemeColors(
                    ThemeUtils.resolveColorAttribute(view.context, R.attr.colorAccent)
                )
                setRefreshEnabled(parentViewModel.isFixedOn.value != true)
                setOnRefreshListener {
                    if (!parentViewModel.initiateRefresh()) {
                        isRefreshing = false
                    }
                }
            }
        }
        parentViewModel.isFixedOn.observe(viewLifecycleOwner, { isFixedOn ->
            binding?.swiperefresh?.setRefreshEnabled(isFixedOn != true)
        })
        parentViewModel.nearbyLocation.observe(viewLifecycleOwner, { nearbyLocation ->
            viewModel.setNearbyLocation(nearbyLocation)
        })
        viewModel.typeAgencies.observe(viewLifecycleOwner, { // REQUIRED FOR PARAMS
            // DO NOTHING
        })
        viewModel.typeId.observe(viewLifecycleOwner, { typeId -> // REQUIRED FOR PARAMS
            theLogTag = typeId?.let { "${LOG_TAG}-$it" } ?: LOG_TAG
            adapter.logTag = logTag
        })
        viewModel.deviceLocation.observe(viewLifecycleOwner, { deviceLocation ->
            adapter.setLocation(deviceLocation)
        })
        viewModel.nearbyPOIs.observe(viewLifecycleOwner, { poiList ->
            val scrollToTop = adapter.poisCount <= 0
            adapter.appendPois(poiList)
            if (scrollToTop) {
                listBinding?.root?.setSelection(0)
            }
            if (isResumed) {
                adapter.updateDistanceNowAsync(viewModel.deviceLocation.value)
            } else {
                adapter.onPause()
            }
            switchView()
        })
    }

    private fun switchView() {
        binding?.apply {
            when {
                !adapter.isInitialized -> {
                    emptyBinding?.root?.isVisible = false
                    listBinding?.root?.isVisible = false
                    swiperefresh.setLoadingViewWR(loading.root)
                    swiperefresh.isRefreshing = true
                    loading.root.isVisible = true
                }
                adapter.poisCount == 0 -> {
                    loading.root.isVisible = false
                    swiperefresh.isRefreshing = false
                    listBinding?.root?.isVisible = false
                    (emptyBinding?.root ?: emptyStub.inflate()).isVisible = true
                    swiperefresh.setEmptyViewWR(emptyBinding?.root)
                }
                else -> {
                    loading.root.isVisible = false
                    swiperefresh.isRefreshing = false
                    emptyBinding?.root?.isVisible = false
                    (listBinding?.root ?: listStub.inflate()).isVisible = true
                }
            }
        }
    }

    override fun onUserLocationChanged(location: Location?) {
        viewModel.onDeviceLocationChanged(location)
    }

    override fun onResume() {
        super.onResume()
        this.adapter.onResume(this, viewModel.deviceLocation.value)
        switchView()
        (activity as? MTActivityWithLocation)?.let { onUserLocationChanged(it.lastLocation) }
    }

    override fun onPause() {
        super.onPause()
        this.adapter.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.swiperefresh?.setOnRefreshListener(null)
        listBinding = null
        emptyBinding = null
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        this.adapter.onDestroy()
    }

    override fun getLifecycleOwner() = this

    override fun finish() {
        activity?.finish()
    }

    override fun <T : View?> findViewById(id: Int): T? = view?.findViewById<T>(id)
}