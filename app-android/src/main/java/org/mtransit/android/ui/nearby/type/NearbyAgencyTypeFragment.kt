package org.mtransit.android.ui.nearby.type

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.common.repository.DefaultPreferenceRepository
import org.mtransit.android.common.repository.LocalPreferenceRepository
import org.mtransit.android.commons.ThemeUtils
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.POIArrayAdapter
import org.mtransit.android.databinding.FragmentNearbyAgencyTypeBinding
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.provider.FavoriteManager
import org.mtransit.android.provider.sensor.MTSensorManager
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.task.StatusLoader
import org.mtransit.android.ui.empty.EmptyLayoutUtils.updateEmptyLayout
import org.mtransit.android.ui.fragment.MTFragmentX
import org.mtransit.android.ui.main.NextMainViewModel
import org.mtransit.android.ui.nearby.NearbyViewModel
import org.mtransit.android.ui.setUpListEdgeToEdge
import org.mtransit.android.ui.view.common.isAttached
import org.mtransit.android.ui.view.common.isVisible
import org.mtransit.commons.FeatureFlags
import javax.inject.Inject

@AndroidEntryPoint
class NearbyAgencyTypeFragment : MTFragmentX(R.layout.fragment_nearby_agency_type) {

    companion object {
        private val LOG_TAG: String = NearbyAgencyTypeFragment::class.java.simpleName

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
    private val attachedViewModel
        get() = if (isAttached()) viewModel else null

    private val nextMainViewModel by activityViewModels<NextMainViewModel>()

    private val parentViewModel by viewModels<NearbyViewModel>({ requireParentFragment() })
    private val attachedParentViewModel
        get() = if (isAttached()) parentViewModel else null

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

    private var binding: FragmentNearbyAgencyTypeBinding? = null

    private val infiniteLoadingListener = object : POIArrayAdapter.InfiniteLoadingListener {
        override fun isLoadingMore(): Boolean {
            return attachedViewModel?.isLoadingMore() == true
        }

        override fun showingDone() = true
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
            logTag = this@NearbyAgencyTypeFragment.logTag
            setInfiniteLoading(true)
            setInfiniteLoadingListener(infiniteLoadingListener)
            setPois(attachedViewModel?.nearbyPOIs?.value)
            setLocation(attachedParentViewModel?.deviceLocation?.value)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentNearbyAgencyTypeBinding.bind(view).apply {
            listLayout.list.apply {
                swipeRefresh.setListViewWR(this)
                isVisible = listAdapter.isInitialized
                listAdapter.setListView(this)
                setUpListEdgeToEdge()
            }
            swipeRefresh.apply {
                setColorSchemeColors(
                    ThemeUtils.resolveColorAttribute(view.context, android.R.attr.colorAccent)
                )
                setRefreshEnabled(parentViewModel.isFixedOn.value != true)
                setOnRefreshListener {
                    if (!parentViewModel.initiateRefresh()) {
                        isRefreshing = false
                    }
                }
            }
        }
        parentViewModel.isFixedOn.observe(viewLifecycleOwner) { isFixedOn ->
            binding?.swipeRefresh?.setRefreshEnabled(isFixedOn != true)
        }
        parentViewModel.nearbyLocation.observe(viewLifecycleOwner) { nearbyLocation ->
            viewModel.setNearbyLocation(nearbyLocation)
        }
        viewModel.typeAgencies.observe(viewLifecycleOwner) { // REQUIRED FOR PARAMS
            // DO NOTHING
        }
        viewModel.typeId.observe(viewLifecycleOwner) { typeId -> // REQUIRED FOR PARAMS
            theLogTag = typeId?.let { "${LOG_TAG}-$it" } ?: LOG_TAG
            listAdapter.logTag = this@NearbyAgencyTypeFragment.logTag
        }
        parentViewModel.deviceLocation.observe(viewLifecycleOwner) { deviceLocation ->
            listAdapter.setLocation(deviceLocation)
        }
        parentViewModel.nearbyLocationForceReset.observe(viewLifecycleOwner) { resetEvent ->
            if (resetEvent.peekContent()) { // event used in view model, do not mark has handled
                listAdapter.clear()
            }
        }
        viewModel.hasNearbyPOIAgencyDisabled.observe(viewLifecycleOwner) { hasNearbyPOIAgencyDisabled ->
            updateEmptyLayout(hasNearbyPOIAgencyDisabled = hasNearbyPOIAgencyDisabled)
        }
        viewModel.oneTypeAgency.observe(viewLifecycleOwner) { oneTypeAgency ->
            updateEmptyLayout(pkg = oneTypeAgency?.pkg)
        }
        viewModel.nearbyPOIs.observe(viewLifecycleOwner) { poiList ->
            updateEmptyLayout(empty = poiList.isNullOrEmpty())
            val scrollToTop = listAdapter.poisCount <= 0
            listAdapter.appendPois(poiList)
            if (scrollToTop) {
                binding?.listLayout?.list?.setSelection(0)
            }
            if (isResumed) {
                listAdapter.updateDistanceNowAsync(parentViewModel.deviceLocation.value)
            } else {
                listAdapter.onPause()
            }
            switchView()
        }
        if (FeatureFlags.F_NAVIGATION) {
            nextMainViewModel.scrollToTopEvent.observe(viewLifecycleOwner) { scrollEvent ->
                if (isResumed) { // only consumed for current tab
                    val scroll = scrollEvent.getContentIfNotHandled() == true
                    if (scroll) {
                        binding?.listLayout?.list?.setSelection(0)
                    }
                }
            }
        }
    }

    private fun updateEmptyLayout(
        hasNearbyPOIAgencyDisabled: Boolean = attachedViewModel?.hasNearbyPOIAgencyDisabled?.value == true,
        empty: Boolean = attachedViewModel?.nearbyPOIs?.value.isNullOrEmpty(),
        pkg: String? = attachedViewModel?.oneTypeAgency?.value?.pkg,
    ) = binding?.apply {
        emptyLayout.updateEmptyLayout(empty = hasNearbyPOIAgencyDisabled && empty, pkg = pkg, activity)
    }

    private fun switchView() {
        binding?.apply {
            when {
                !listAdapter.isInitialized -> {
                    emptyLayout.isVisible = false
                    listLayout.isVisible = false
                    swipeRefresh.setLoadingViewWR(loadingLayout.root)
                    swipeRefresh.isRefreshing = true
                    loadingLayout.isVisible = true
                }

                listAdapter.poisCount == 0 -> {
                    loadingLayout.isVisible = false
                    swipeRefresh.isRefreshing = false
                    listLayout.isVisible = false
                    emptyLayout.isVisible = true
                    swipeRefresh.setEmptyViewWR(emptyLayout.root)
                }

                else -> {
                    loadingLayout.isVisible = false
                    swipeRefresh.isRefreshing = false
                    emptyLayout.isVisible = false
                    listLayout.isVisible = true
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        this.listAdapter.onResume(this, parentViewModel.deviceLocation.value)
        switchView()
    }

    override fun onPause() {
        super.onPause()
        this.listAdapter.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listAdapter.onDestroyView()
        binding?.swipeRefresh?.apply {
            onDestroyView()
            setOnRefreshListener(null)
        }
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        this.listAdapter.onDestroy()
    }

    override fun <T : View?> findViewById(id: Int) = this.view?.findViewById<T>(id)
}