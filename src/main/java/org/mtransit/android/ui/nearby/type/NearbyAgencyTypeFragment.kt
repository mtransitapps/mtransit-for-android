package org.mtransit.android.ui.nearby.type

import android.os.Bundle
import android.view.View
import android.widget.AbsListView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.commons.ThemeUtils
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.POIArrayAdapter
import org.mtransit.android.databinding.FragmentNearbyAgencyTypeBinding
import org.mtransit.android.databinding.LayoutEmptyBinding
import org.mtransit.android.databinding.LayoutPoiListBinding
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.provider.FavoriteManager
import org.mtransit.android.provider.sensor.MTSensorManager
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.task.StatusLoader
import org.mtransit.android.ui.fragment.MTFragmentX
import org.mtransit.android.ui.nearby.NearbyViewModel
import org.mtransit.android.ui.view.common.EventObserver
import org.mtransit.android.ui.view.common.IActivity
import javax.inject.Inject

@AndroidEntryPoint
class NearbyAgencyTypeFragment : MTFragmentX(R.layout.fragment_nearby_agency_type), IActivity {

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
    private val addedViewModel: NearbyAgencyTypeViewModel?
        get() = if (isAdded) viewModel else null

    private val parentViewModel by viewModels<NearbyViewModel>({ requireParentFragment() })
    private val addedParentViewModel: NearbyViewModel?
        get() = if (isAdded) parentViewModel else null

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

    private var binding: FragmentNearbyAgencyTypeBinding? = null
    private var emptyBinding: LayoutEmptyBinding? = null
    private var listBinding: LayoutPoiListBinding? = null

    private val infiniteLoadingListener = object : POIArrayAdapter.InfiniteLoadingListener {
        override fun isLoadingMore(): Boolean {
            return addedViewModel?.isLoadingMore() == true
        }

        override fun showingDone() = true
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
            setInfiniteLoading(true)
            setInfiniteLoadingListener(infiniteLoadingListener)
            setPois(addedViewModel?.nearbyPOIs?.value)
            setLocation(addedParentViewModel?.deviceLocation?.value)
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
        parentViewModel.deviceLocation.observe(viewLifecycleOwner, { deviceLocation ->
            adapter.setLocation(deviceLocation)
        })
        parentViewModel.nearbyLocationForceReset.observe(viewLifecycleOwner, EventObserver { reset ->
            if (reset) {
                adapter.clear()
            }
        })
        viewModel.nearbyPOIs.observe(viewLifecycleOwner, { poiList ->
            val scrollToTop = adapter.poisCount <= 0
            adapter.appendPois(poiList)
            if (scrollToTop) {
                listBinding?.root?.setSelection(0)
            }
            if (isResumed) {
                adapter.updateDistanceNowAsync(parentViewModel.deviceLocation.value)
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

    override fun onResume() {
        super.onResume()
        this.adapter.onResume(this, parentViewModel.deviceLocation.value)
        switchView()
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