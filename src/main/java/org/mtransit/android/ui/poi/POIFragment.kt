//@file:JvmName("POIFragment") // ANALYTICS
package org.mtransit.android.ui.poi
//
//import android.content.Context
//import android.content.res.Configuration
//import android.graphics.Color
//import android.hardware.Sensor
//import android.hardware.SensorEvent
//import android.hardware.SensorEventListener
//import android.location.Location
//import android.os.Bundle
//import android.view.Menu
//import android.view.MenuInflater
//import android.view.MenuItem
//import android.view.View
//import androidx.core.os.bundleOf
//import androidx.core.view.isVisible
//import androidx.fragment.app.viewModels
//import androidx.viewbinding.ViewBinding
//import com.google.android.gms.maps.model.LatLng
//import com.google.android.gms.maps.model.LatLngBounds
//import dagger.hilt.android.AndroidEntryPoint
//import org.mtransit.android.R
//import org.mtransit.android.ad.IAdManager.RewardedAdListener
//import org.mtransit.android.commons.Constants
//import org.mtransit.android.commons.LocationUtils
//import org.mtransit.android.commons.MTLog
//import org.mtransit.android.commons.StoreUtils
//import org.mtransit.android.commons.ThreadSafeDateFormatter
//import org.mtransit.android.commons.data.News
//import org.mtransit.android.commons.data.POI
//import org.mtransit.android.commons.data.POIStatus
//import org.mtransit.android.commons.data.RouteTripStop
//import org.mtransit.android.commons.data.ServiceUpdate
//import org.mtransit.android.commons.provider.NewsProviderContract
//import org.mtransit.android.data.AgencyProperties
//import org.mtransit.android.data.POIArrayAdapter
//import org.mtransit.android.data.POIManager
//import org.mtransit.android.data.ScheduleProviderProperties
//import org.mtransit.android.databinding.FragmentPoiBinding
//import org.mtransit.android.databinding.LayoutPoiAppUpdateBinding
//import org.mtransit.android.databinding.LayoutPoiRewardedAdBinding
//import org.mtransit.android.databinding.LayoutPoiServiceUpdateBinding
//import org.mtransit.android.datasource.DataSourcesRepository
//import org.mtransit.android.provider.FavoriteManager
//import org.mtransit.android.provider.FavoriteManager.FavoriteUpdateListener
//import org.mtransit.android.provider.permission.LocationPermissionProvider
//import org.mtransit.android.provider.sensor.MTSensorManager
//import org.mtransit.android.provider.sensor.MTSensorManager.CompassListener
//import org.mtransit.android.task.ServiceUpdateLoader
//import org.mtransit.android.task.StatusLoader
//import org.mtransit.android.ui.MTActivityWithLocation
//import org.mtransit.android.ui.MTActivityWithLocation.UserLocationListener
//import org.mtransit.android.ui.MainActivity
//import org.mtransit.android.ui.fragment.ABFragment
//import org.mtransit.android.ui.map.MapFragment.Companion.newInstance
//import org.mtransit.android.ui.nearby.NearbyFragment
//import org.mtransit.android.ui.news.NewsListFragment
//import org.mtransit.android.ui.schedule.ScheduleFragment
//import org.mtransit.android.ui.view.MapViewController
//import org.mtransit.android.ui.view.POIDataProvider
//import org.mtransit.android.ui.view.POINewsViewController
//import org.mtransit.android.ui.view.POIServiceUpdateViewController
//import org.mtransit.android.ui.view.POIStatusDetailViewController
//import org.mtransit.android.ui.view.POIViewController
//import org.mtransit.android.ui.view.common.EventObserver
//import org.mtransit.android.util.FragmentUtils
//import org.mtransit.android.util.LinkUtils
//import org.mtransit.android.util.MapUtils
//import org.mtransit.android.util.UITimeUtils
//import org.mtransit.android.util.UITimeUtils.TimeChangedReceiver
//import org.mtransit.android.util.UITimeUtils.TimeChangedReceiver.TimeChangedListener
//import javax.inject.Inject
//
//@AndroidEntryPoint
//class POIFragment : ABFragment(R.layout.fragment_poi), UserLocationListener, POIDataProvider, TimeChangedListener, FavoriteUpdateListener {
//
//    companion object {
//        private val LOG_TAG = POIFragment::class.java.simpleName
//
//        private const val PKG_COMMON = "org.mtransit.android."
//
//        private const val TRACKING_SCREEN_NAME = "POI"
//
//        private val rewardedAdDateFormatter = ThreadSafeDateFormatter.getDateInstance(ThreadSafeDateFormatter.MEDIUM)
//
//        @JvmStatic
//        fun newInstance(poi: POI): POIFragment {
//            return newInstance(poi.authority, poi.uuid)
//        }
//
//        @JvmStatic
//        fun newInstance(agencyAuthority: String, uuid: String): POIFragment {
//            return POIFragment().apply {
//                arguments = bundleOf(
//                    POIViewModel.EXTRA_AUTHORITY to agencyAuthority,
//                    POIViewModel.EXTRA_POI_UUID to uuid,
//                )
//            }
//        }
//    }
//
//    private var theLogTag: String = LOG_TAG
//
//    override fun getLogTag(): String = this.theLogTag
//
//    override fun getScreenName(): String = viewModel.uuid.value?.let { "$TRACKING_SCREEN_NAME/$it" } ?: TRACKING_SCREEN_NAME
//
//    private val viewModel by viewModels<POIViewModel>()
//
//    @Inject
//    lateinit var sensorManager: MTSensorManager
//
//    @Inject
//    lateinit var dataSourcesRepository: DataSourcesRepository
//
//    @Inject
//    lateinit var favoriteManager: FavoriteManager
//
//    @Inject
//    lateinit var statusLoader: StatusLoader
//
//    @Inject
//    lateinit var serviceUpdateLoader: ServiceUpdateLoader
//
//    @Inject
//    lateinit var locationPermissionProvider: LocationPermissionProvider
//
//    private var binding: FragmentPoiBinding? = null
//    private var thisPoiBinding: ViewBinding? = null
//    private var rtsScheduleBtn: View? = null
//    private var poiStatusDetailBinding: ViewBinding? = null
//    private var poiServiceUpdateBinding: ViewBinding? = null
//    private var poiAppUpdateBinding: LayoutPoiAppUpdateBinding? = null
//    private var poiNewsBinding: ViewBinding? = null
//
//    // private var newsMoreBtn: View? = null
//    private var poiRewardedAdBinding: LayoutPoiRewardedAdBinding? = null
//    // private var nearbyMoreBtn: View? = null
//
//    private val mapMarkerProvider = object : MapViewController.MapMarkerProvider {
//
//        override fun getPOMarkers(): Collection<MapViewController.POIMarker>? = null
//
//        override fun getPOIs() = viewModel.poimV?.let { listOf(it) }
//
//        override fun getClosestPOI() = viewModel.poimV
//
//        override fun getPOI(uuid: String?) = viewModel.poimV?.let { poim ->
//            if (poim.poi.uuid == uuid) poim else null
//        }
//    }
//
//    private val mapListener = object : MapViewController.MapListener {
//
//        override fun onMapClick(position: LatLng) {
//            if (!FragmentUtils.isFragmentReady(this@POIFragment)) {
//                return
//            }
//            // val poim: POIManager = viewModel.poimV ?: return
//            val poi: POI = viewModel.poi.value ?: return
//            val activity = activity ?: return
//            (activity as MainActivity).addFragmentToStack( //
//                newInstance(
//                    LocationUtils.getNewLocation(poi.lat, poi.lng),
//                    poi.uuid,
//                    poi.dataSourceTypeId
//                ),
//                this@POIFragment
//            )
//        }
//
//        override fun onCameraChange(latLngBounds: LatLngBounds) {
//            // DO NOTHING
//        }
//
//        override fun onMapReady() {
//            // DO NOTHING
//        }
//    }
//
//    private val mapViewController: MapViewController by lazy {
//        MapViewController(
//            logTag,
//            mapMarkerProvider,
//            mapListener,
//            false,
//            true,
//            false,
//            false,
//            false,
//            false,
//            32,
//            true,
//            false,
//            true,
//            true,
//            false,
//            this.dataSourcesRepository
//        ).apply {
//            setLocationPermissionGranted(locationPermissionProvider.permissionsGranted(requireContext()))
//        }
//    }
//
//    private val adapter: POIArrayAdapter by lazy {
//        POIArrayAdapter(
//            this,
//            this.sensorManager,
//            this.dataSourcesRepository,
//            this.favoriteManager,
//            this.statusLoader,
//            this.serviceUpdateLoader
//        ).apply {
//            logTag = logTag
//            // setShowExtra(false)
//            setLocation(viewModel.deviceLocation.value)
//        }
//    }
//
//    private val onSensorTaskCompleted = object : MTSensorManager.SensorTaskCompleted {
//
//        override fun onSensorTaskCompleted(result: Boolean, orientation: Int, now: Long) {
//            viewModel.onSensorTaskCompleted(result, orientation, now)
//            if (result && isResumed) {
//                updatePOIDistanceAndCompass()
//            }
//        }
//    }
//
//    private val compassListener = object : CompassListener {
//        override fun updateCompass(orientation: Float, force: Boolean) {
//            viewModel.updateCompass(orientation, force, onSensorTaskCompleted)
//        }
//    }
//
//    private val sensorEventListener = object : SensorEventListener {
//        override fun onSensorChanged(event: SensorEvent?) {
//            event?.let { viewModel.onSensorChanged(this@POIFragment, it, compassListener) }
//        }
//
//        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
//            // DO NOTHING
//        }
//    }
//
//    private val rewardedAdListener = object : RewardedAdListener {
//        override fun onRewardedAdStatusChanged() {
//            viewModel.onRewardedAdStatusChanged()
//        }
//
//        override fun skipRewardedAd() = viewModel.skipRewardedAd()
//    }
//
//    override fun onAttach(context: Context) {
//        super.onAttach(context)
//        mapViewController.apply {
//            setDataSourcesRepository(dataSourcesRepository)
//            onAttach(requireActivity())
//            setLocationPermissionGranted(locationPermissionProvider.permissionsGranted(context))
//        }
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setHasOptionsMenu(true)
//        mapViewController.onCreate(savedInstanceState)
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        mapViewController.onViewCreated(view, savedInstanceState)
//        binding = FragmentPoiBinding.bind(view).apply {
//            thisPoiStub.setOnInflateListener { _, inflated ->
//                // // T ODO which stub? thisPoiBinding = LayoutEmptyBinding.bind(inflated)
//                updatePOIView(poiView = inflated)
//                updatePOIStatusView(poiView = inflated)
//                updatePOIColorView(poiView = inflated)
//                updatePOIDistanceAndCompass(poiView = inflated)
//                updatePOIServiceUpdatesView(poiView = inflated)
//            }
//            poiStatusDetailStub.setOnInflateListener { _, inflated ->
//                rtsScheduleBtn = inflated.findViewById<View>(R.id.fullScheduleBtn)?.apply {
//                    setOnClickListener {
//                        viewModel.poimV?.let { poim ->
//                            (poim.poi as? RouteTripStop)?.let {
//                                (activity as? MainActivity)?.addFragmentToStack(
//                                    ScheduleFragment.newInstance(poim, dataSourcesRepository),
//                                    this@POIFragment
//                                )
//                            }
//                        }
//                    }
//                    setupRTSFullScheduleBtn(rtsScheduleBtn = this)
//                }
//                updateStatusDetailView(statusDetailView = inflated)
//                updateStatusDetailColorView(statusDetailView = inflated)
//            }
//            poiServiceUpdateStub.setOnInflateListener { _, inflated ->
//                poiServiceUpdateBinding = LayoutPoiServiceUpdateBinding.bind(inflated)
//                updateServiceUpdateView(serviceUpdateView = inflated)
//            }
//            poiAppUpdateStub.setOnInflateListener { _, inflated ->
//                poiAppUpdateBinding = LayoutPoiAppUpdateBinding.bind(inflated).apply {
//                    appUpdateBtn.setOnClickListener {
//                        viewModel.agency.value?.let { agency ->
//                            activity?.let { activity ->
//                                viewModel.onClickAppUpdatePOI(agency)
//                                StoreUtils.viewAppPage(activity, agency.pkg, activity.getString(R.string.google_play))
//                            }
//                        }
//                    }
//                    refreshAppUpdateLayout(poiAppUpdateBinding = this)
//                }
//            }
//            poiNewsStub.setOnInflateListener { _, inflated ->
//                // poiNewsBinding = LayoutPoiNewsBinding.bind(inflated)
//                val newsMoreBtn = inflated.findViewById<View>(R.id.moreBtn)
//                newsMoreBtn.apply {
//                    setOnClickListener {
//                        viewModel.poimV?.let { poim ->
//                            (activity as? MainActivity)?.addFragmentToStack(
//                                NewsListFragment.newInstance(
//                                    poim.getColor { viewModel.agency.value },
//                                    poim.getNewOneLineDescription(viewModel.agency.value),
//                                    // POIManager.getNewOneLineDescription(poim.poi, dataSourcesRepository),
//                                    listOf(poim.poi.authority),
//                                    null,
//                                    NewsProviderContract.Filter.getNewTargetFilter(poim.poi).targets
//                                ),
//                                this@POIFragment
//                            )
//                        }
//                    }
//                    isVisible = true
//                }
//                updateNewsView(newsView = inflated)
//            }
//            poiRewardedAdStub.setOnInflateListener { _, inflated ->
//                poiRewardedAdBinding = LayoutPoiRewardedAdBinding.bind(inflated).apply {
//                    rewardedAdsBtn.apply {
//                        setOnClickListener { rewardedAdsBtn ->
//                            if (viewModel.onRewardedAdClick(this@POIFragment) == false) {
//                                rewardedAdsBtn.isEnabled = false // avoid double-click
//                            }
//                        }
//                    }
//                    refreshRewardedLayout(poiRewardedAdBinding = this)
//                }
//            }
//            adapter.setManualScrollView(scrollview)
//            adapter.setManualLayout(poiNearbyPoisList)
//            //            if (adapter.isInitialized) {
//            //                showNearbyList()
//            //            } else {
//            //                hideNearbyList(true)
//            //            }
//            //            binding?.apply {
//            poiNearbyPoisTitle.moreBtn.apply {
//                setOnClickListener {
//                    viewModel.poimV?.let { poim ->
//                        val agency = viewModel.agency.value
//                        (activity as? MainActivity)?.addFragmentToStack(
//                            NearbyFr agment.newFixedOnInstance(poim, agency),
//                            this@POIFragment
//                        )
//                    }
//                }
//                isVisible = !Constants.FORCE_NEARBY_POI_LIST_OFF
//            }
//            poiNearbyPoisTitle.root.isVisible = !Constants.FORCE_NEARBY_POI_LIST_OFF && adapter.isInitialized
//            poiNearbyPoisList.isVisible = !Constants.FORCE_NEARBY_POI_LIST_OFF && adapter.isInitialized
//            //            }
//        }
//        viewModel.uuid.observe(viewLifecycleOwner, {
//            MTLog.v(this, "onChanged($it) - uuid")
//            theLogTag = it?.let { "${LOG_TAG}-${it.substringAfter(PKG_COMMON)}" } ?: LOG_TAG
//        })
//        viewModel.agency.observe(viewLifecycleOwner, {
//            MTLog.v(this, "onChanged($it) - agency")
//            it?.let {
//                val context = view.context
//                //                viewModel.poimV?.let { poim ->
//                //                    POIStatusDetailViewController.updateView(context, getPOIStatusView(), poim, this)
//                //                }
//                updateStatusDetailView()
//                abController?.setABTitle(this, getABTitle(context), false)
//                abController?.setABReady(this, isABReady, true)
//                //refreshAppUpdateLayout(it)
//            }
//            refreshAppUpdateLayout(agency = it)
//        })
//        viewModel.poiTypeAndStatus.observe(viewLifecycleOwner, {
//            MTLog.v(this, "onChanged($it) - poiTypeAndStatus")
//            // val context = view.context
//            // val poim = viewModel.poimV
//            updatePOIView(poiTypeAndStatus = it)
//            updatePOIColorView(poiTypeAndStatus = it)
//            updatePOIDistanceAndCompass(poiTypeAndStatus = it)
//            updatePOIStatusView(poiTypeAndStatus = it)
//            updatePOIServiceUpdatesView(poiTypeAndStatus = it)
//            updateStatusDetailView(poiTypeAndStatus = it)
//            updateStatusDetailColorView(poiTypeAndStatus = it)
//            // updateServiceUpdateView(poi)
//            // POIViewController.updateView(context, getPOIView(it), poim, this)
//        })
//        viewModel.poi.observe(viewLifecycleOwner, {
//            MTLog.v(this, "onChanged($it) - poi")
//            updatePOIView(poiN = it)
//            updateStatusDetailView(poi = it)
//            updateFavMenuItem(poi = it)
//        })
//        viewModel.poiColor.observe(viewLifecycleOwner, {
//            MTLog.v(this, "onChanged($it) - poiColor")
//            //            viewModel.poiTypeAndStatus.value?.let { (poiType, poiStatusType) ->
//            //                POIViewController.updatePOIColorView(getPOIView(), poiType, poiStatusType, it, this)
//            //            }
//            updatePOIColorView(poiColor = it)
//            updateStatusDetailColorView(poiColor = it)
//        })
//        viewModel.poiDistanceTrigger.observe(viewLifecycleOwner, {
//            MTLog.v(this, "onChanged($it) - poiDistanceTrigger")
//            // DO NOTHING
//        })
//        viewModel.poiDistanceAndString.observe(viewLifecycleOwner, {
//            MTLog.v(this, "onChanged($it) - poiDistanceAndString")
//            updatePOIDistanceAndCompass(poiDistanceAndString = it)
//        })
//        viewModel.poiStatus.observe(viewLifecycleOwner, {
//            MTLog.v(this, "onChanged($it) - poiStatus")
//            updatePOIStatusView(poiStatusN = it)
//            updateStatusDetailView(poiStatus = it)
//        })
//        viewModel.poiServiceUpdates.observe(viewLifecycleOwner, {
//            updatePOIServiceUpdatesView(poiServiceUpdates = it)
//            updateServiceUpdateView(poiServiceUpdates = it)
//        })
//        viewModel.poimTrigger.observe(viewLifecycleOwner, {
//            MTLog.v(this, "onChanged($it) - poimTrigger")
//            // DO NOTHING
//        })
//        // TODO
//        @Suppress("DEPRECATION") // FIXME
//        viewModel.poim.observe(viewLifecycleOwner, {
//            MTLog.v(this, "onChanged($it) - poim")
//            it?.let {
//                // TODO test all , it's a mess  ... try to fix it POIManager is hard to work with? can we use it in MVVM ?
//                // TODO kind of starting to work but not working very well ... POIM + MVVM = !
//                // TODO Maybe just refactor Java code to extract small ViewModel to remove loader and stabilize before opening this can of worm
//                // val context = view.context
//                // adapter.clear()
//                // TO DO ? resetFavorite()
//                mapViewController.notifyMarkerChanged(mapMarkerProvider)
//                mapViewController.showMap(view)
//                // updatePOIView(poimN = poim)
//                //                if (isShowingStatus) {
//                // poim.setStatusLoaderListener(this@POIFragment) // TOD O ?
//                viewModel.refreshStatus(context, this@POIFragment)
//                //                }
//                //                if (isShowingServiceUpdates) {
//                // poim.setServiceUpdateLoaderListener(this@POIFragment) // TO DO ?
//                viewModel.refreshServiceUpdate(context, this@POIFragment)
//                //                }
//                // POIViewController.updateView(context, getPOIView(poim), poim, this)
//                // updateStatusDetailView(poimN = poim)
//                // POIStatusDetailViewController.updateView(context, getPOIStatusView(), poim, this)
//                // POIServiceUpdateViewController.updateView(getPOIServiceUpdateView(), poim, this)
//                // updateServiceUpdateView(poimN = poim)
//                // POINewsViewController.updateView(getPOINewsView(), viewModel.latestNewsArticleList.value)
//                // TODO ? updateNewsView()
//                //  updateFavMenuItem(poim = poim)
//                activity?.invalidateOptionsMenu() // add/remove star from action bar
//                // setupRTSFullScheduleBtn()
//                // setupMoreNewsButton()
//                // setupMoreNearbyButton()
//                // setupNearbyList()
//            }
//        })
//        viewModel.scheduleProviders.observe(viewLifecycleOwner, {
//            //            val poim = viewModel.poimV
//            //            getPOIStatusViewBinding(poim) as? POI
//            setupRTSFullScheduleBtn(scheduleProviders = it)
//        })
//        viewModel.poiFavorite.observe(viewLifecycleOwner, {
//            MTLog.v(this, "onChanged($it) - poiFavorite")
//            it?.let {
//                // val poim = viewModel.poimV
//                updateFavMenuItem(poiFavorite = it)
//                activity?.invalidateOptionsMenu() // add/remove star from action bar
//                // POIViewController.updateView(view.context, getPOIView(poim), poim, this)
//                // updatePOIView(context = view.context)
//            }
//        })
//        viewModel.dataSourceRemovedEvent.observe(viewLifecycleOwner, EventObserver { removed ->
//            MTLog.v(this, "onChanged($removed) - dataSourceRemovedEvent")
//            if (removed) {
//                (activity as MainActivity?)?.popFragmentFromStack(this) // close this fragment
//            }
//        })
//        viewModel.deviceLocation.observe(viewLifecycleOwner, {
//            MTLog.v(this, "onChanged($it) - deviceLocation")
//            context?.let { context ->
//                mapViewController.setLocationPermissionGranted(locationPermissionProvider.permissionsGranted(context))
//            }
//            mapViewController.onDeviceLocationChanged(it)
//            // TODO if (!this.compassUpdatesEnabled) {
//            //				sensorManager.registerCompassListener(this);
//            //				this.compassUpdatesEnabled = true;
//            //			}
//            viewModel.updatePOIDistanceWithString(deviceLocation = it)
//            //            viewModel.poimV?.let { poim ->
//            //                POIViewController.updatePOIDistanceAndCompass(getPOIView(poim), poim, this);
//            //            }
//            updatePOIDistanceAndCompass()
//            // POIManager poim = getPoimOrNull();
//            //			if (poim != null) {
//            //				LocationUtils.updateDistanceWithString(requireContext(), poim, newLocation);
//            //				POIViewController.updatePOIDistanceAndCompass(getPOIView(getView()), poim, this);
//            //			}
//            adapter.setLocation(it)
//
//        })
//        if (!Constants.FORCE_NEARBY_POI_LIST_OFF) {
//            viewModel.nearbyPOIs.observe(viewLifecycleOwner, {
//                MTLog.v(this, "onChanged(${it?.size}) - nearbyPOIs")
//                it?.let { nearbyPOIs ->
//                    adapter.apply {
//                        setPois(nearbyPOIs)
//                        updateDistanceNowAsync(viewModel.deviceLocation.value)
//                        initManual()
//                        //                    if (poisCount > 0) {
//                        //                        showNearbyList()
//                        //                    } else {
//                        //                        hideNearbyList(false)
//                        //                    }
//                    }
//                }
//                // ?: run { hideNearbyList(false) }
//                //            if (it.isNullOrEmpty()) {
//                //                hideNearbyList(false)
//                //            } else {
//                //                showNearbyList()
//                //            }
//                binding?.apply {
//                    poiNearbyPoisTitle.root.isVisible = !it.isNullOrEmpty()
//                    poiNearbyPoisList.isVisible = !it.isNullOrEmpty()
//                }
//            })
//        }
//        viewModel.rewardedAdStatus.observe(viewLifecycleOwner, {
//            MTLog.v(this, "onChanged($it) - rewardedAdStatus")
//            refreshRewardedLayout(status = it)
//        })
//        //        viewModel.rewardedAdAmount.observe(viewLifecycleOwner, {
//        //            refreshRewardedLayout(rewardedAmount = it)
//        //        })
//        //        viewModel.rewardedAdNowUntilInMs.observe(viewLifecycleOwner, {
//        //            refreshRewardedLayout(rewardedNowUntilInMs = it)
//        //        })
//        viewModel.latestNewsArticleList.observe(viewLifecycleOwner, {
//            MTLog.v(this, "onChanged($it) - rewardedAdStatus")
//            updateNewsView(news = it)
//        })
//    }
//
//    private fun updatePOIView(
//        poiTypeAndStatus: Pair<Int, Int>? = viewModel.poiTypeAndStatus.value,
//        poiN: POI? = viewModel.poi.value,
//        poiView: View? = getPOIView(poiTypeAndStatus),
//    ) {
//        poiN?.let { poi ->
//            POIViewController.updatePOIView(poiView, poi, this)
//        }
//    }
//
//    private fun updatePOIColorView(
//        poiTypeAndStatus: Pair<Int, Int>? = viewModel.poiTypeAndStatus.value,
//        poiColor: Int? = viewModel.poiColor.value,
//        poiView: View? = getPOIView(poiTypeAndStatus),
//    ) {
//        poiTypeAndStatus?.let { (poiType, poiStatusType) ->
//            POIViewController.updatePOIColorView(poiView, poiType, poiStatusType, poiColor, this)
//        }
//    }
//
//    private fun updatePOIStatusView(
//        poiTypeAndStatus: Pair<Int, Int>? = viewModel.poiTypeAndStatus.value,
//        poiStatusN: POIStatus? = viewModel.poiStatus.value,
//        poiView: View? = getPOIView(poiTypeAndStatus),
//    ) {
//        poiStatusN?.let { poiStatus ->
//            POIViewController.updatePOIStatus(poiView, poiStatus, this)
//        }
//    }
//
//    private fun updatePOIServiceUpdatesView(
//        poiTypeAndStatus: Pair<Int, Int>? = viewModel.poiTypeAndStatus.value,
//        poiServiceUpdates: List<ServiceUpdate>? = viewModel.poiServiceUpdates.value,
//        poiView: View? = getPOIView(poiTypeAndStatus),
//    ) {
//        //        poiStatusN?.let { poiStatus ->
//        // POIViewController.updatePOIServiceUpdate(poiView, poiServiceUpdates, this)
//        POIViewController.updateServiceUpdatesView(poiView, poiServiceUpdates, this)
//        //        }
//    }
//
//    //    // TO DO
//    //    private fun updatePOIMView(
//    //        poiTypeAndStatus: Pair<Int, Int>? = viewModel.poiTypeAndStatus.value,
//    //        poimN: POIManager? = viewModel.poimV,
//    //        poiView: View? = getPOIView(poiTypeAndStatus),
//    //    ) {
//    //        poimN?.let { poim ->
//    //            POIViewController.updateView(poiView, poim, this)
//    //        }
//    //    }
//    //
//    private fun updatePOIDistanceAndCompass(
//        poiTypeAndStatus: Pair<Int, Int>? = viewModel.poiTypeAndStatus.value,
//        poiDistanceAndString: Pair<Float, CharSequence?>? = viewModel.poiDistanceAndString.value,
//        poiView: View? = getPOIView(poiTypeAndStatus),
//    ) {
//        poiTypeAndStatus?.let { (poiType, poiStatusType) ->
//            // TO DO:
//            // - this method is too big w/ too big dependency #POIM
//            // - split in multiple methods than can be called w/ poi OR distance OR status type OR ....
//            POIViewController.updatePOIDistanceAndCompass(poiView, poiType, poiStatusType, poiDistanceAndString?.first, poiDistanceAndString?.second, this)
//        }
//    }
//
//    private fun updateStatusDetailView(
//        poiTypeAndStatus: Pair<Int, Int>? = viewModel.poiTypeAndStatus.value,
//        poiStatus: POIStatus? = viewModel.poiStatus.value,
//        poi: POI? = viewModel.poi.value,
//        statusDetailView: View? = getPOIStatusView(poiTypeAndStatus),
//    ) {
//        MTLog.v(this, "updateStatusDetailView($statusDetailView, $poiStatus)")
//        poiTypeAndStatus?.let { (_, poiStatusType) ->
//            // TO DO:
//            // - updateView = init view holder + updateView
//            // ==> call init view holder w/ view + poim/status (+color?)
//            // => call update view w/ view holder directly stored in this class
//            // XOR:
//            // => keep calling one method to rule them all
//            POIStatusDetailViewController.updateView(statusDetailView, poiStatusType, poiStatus, poi, this)
//        }
//    }
//
//    private fun updateStatusDetailColorView(
//        poiTypeAndStatus: Pair<Int, Int>? = viewModel.poiTypeAndStatus.value,
//        poiColor: Int? = viewModel.poiColor.value,
//        statusDetailView: View? = getPOIStatusView(poiTypeAndStatus),
//    ) {
//        MTLog.v(this, "updateStatusDetailColorView($statusDetailView, $poiColor)")
//        poiTypeAndStatus?.let { (_, statusType) ->
//            POIStatusDetailViewController.updateColorView(statusDetailView, statusType, poiColor, this)
//        }
//    }
//
//    private fun updateServiceUpdateView(
//        // poimN: POIManager? = viewModel.poimV,
//        poiServiceUpdates: List<ServiceUpdate>? = viewModel.poiServiceUpdates.value,
//        serviceUpdateView: View? = getPOIServiceUpdateView(),
//    ) {
//        //        poimN?.let { poim ->
//        POIServiceUpdateViewController.updateView(serviceUpdateView, poiServiceUpdates, this)
//        //        }
//    }
//
//    private fun updateNewsView(
//        news: List<News>? = viewModel.latestNewsArticleList.value,
//        newsView: View? = getPOINewsView(),
//    ) {
//        //        poimN?.let { poim ->
//        POINewsViewController.updateView(newsView, news)
//        //        }
//    }
//
//    private fun getPOIView(poiTypeAndStatus: Pair<Int, Int>? = viewModel.poiTypeAndStatus.value) = getPOIViewBinding(poiTypeAndStatus)?.root
//
//    private fun getPOIViewBinding(poiTypeAndStatus: Pair<Int, Int>? = viewModel.poiTypeAndStatus.value): ViewBinding? {
//        return thisPoiBinding ?: binding?.let { binding ->
//            poiTypeAndStatus?.let { poiTypeAndStatus ->
//                POIViewController.getLayoutViewBinding(poiTypeAndStatus.first, poiTypeAndStatus.second, binding.thisPoiStub)
//            }
//        }?.also { thisPoiBinding = it }
//    }
//
//    private fun getPOIStatusView(poiTypeAndStatus: Pair<Int, Int>? = viewModel.poiTypeAndStatus.value) = getPOIStatusViewBinding(poiTypeAndStatus)?.root
//
//    private fun getPOIStatusViewBinding(poiTypeAndStatus: Pair<Int, Int>? = viewModel.poiTypeAndStatus.value): ViewBinding? {
//        return poiStatusDetailBinding ?: binding?.let { binding ->
//            poiTypeAndStatus?.let { poiTypeAndStatus ->
//                POIStatusDetailViewController.getLayoutViewBinding(poiTypeAndStatus.first, binding.poiStatusDetailStub)
//            }
//        }?.also { poiStatusDetailBinding = it }
//    }
//
//    private fun getPOIServiceUpdateView() = getPOIServiceUpdateViewBinding()?.root
//
//    private fun getPOIServiceUpdateViewBinding(): ViewBinding? {
//        return poiServiceUpdateBinding ?: binding?.let { binding ->
//            POIServiceUpdateViewController.getLayoutViewBinding(binding.poiServiceUpdateStub)
//        }?.also { poiServiceUpdateBinding = it }
//    }
//
//    private fun getPOINewsView() = getPOINewsViewBinding()?.root
//
//    private fun getPOINewsViewBinding(): ViewBinding? {
//        return poiNewsBinding ?: binding?.let { binding ->
//            POINewsViewController.getLayoutViewBinding(binding.poiNewsStub)
//        }?.also { poiNewsBinding = it }
//    }
//
//    private fun setupRTSFullScheduleBtn(
//        rtsScheduleBtn: View? = this.rtsScheduleBtn,
//        scheduleProviders: List<ScheduleProviderProperties>? = viewModel.scheduleProviders.value,
//    ) {
//        rtsScheduleBtn?.isVisible = !scheduleProviders.isNullOrEmpty()
//    }
//
//    private fun refreshAppUpdateLayout(
//        poiAppUpdateBinding: LayoutPoiAppUpdateBinding? = this.poiAppUpdateBinding,
//        agency: AgencyProperties? = viewModel.agency.value,
//    ) {
//        poiAppUpdateBinding?.apply {
//            agency?.apply {
//                if (updateAvailable) {
//                    if (!root.isVisible) {
//                        viewModel.onShowedAppUpdatePOI(this)
//                        root.isVisible = true
//                    }
//                } else {
//                    if (root.isVisible) {
//                        viewModel.onHiddenAppUpdatePOI(this)
//                        root.isVisible = false
//                    }
//                }
//            } ?: run {
//                root.isVisible = false
//            }
//        } ?: run { binding?.poiAppUpdateStub?.inflate() }
//    }
//
//    private fun refreshRewardedLayout(
//        poiRewardedAdBinding: LayoutPoiRewardedAdBinding? = this.poiRewardedAdBinding,
//        status: RewardedAdStatus? = viewModel.rewardedAdStatus.value,
//    ) {
//        poiRewardedAdBinding?.apply {
//            rewardedAdsTitleLayout.rewardAdTitle.apply {
//                text = if (status?.rewardedNow == true) {
//                    getString(
//                        R.string.watch_rewarded_ad_title_text_and_date,
//                        rewardedAdDateFormatter.formatThreadSafe(status.rewardedUntilInMs)
//                    )
//                } else {
//                    getString(
//                        R.string.watch_rewarded_ad_title_text
//                    )
//                }
//                isVisible = status != null
//            }
//            rewardedAdsBtn.apply {
//                text = status?.rewardedAmount?.let { rewardedAmount ->
//                    getString(
//                        if (status.rewardedNow) R.string.watch_rewarded_ad_btn_more_and_days else R.string.watch_rewarded_ad_btn_and_days,
//                        rewardedAmount
//                    )
//                }
//                isEnabled = status?.let { it.availableToShow && it.rewardedAmount > 0 } ?: false // keep but disable
//                if (status?.availableToShow == true) {  // only if NOT paying user
//                    isVisible = true // GONE by default but never hidden once visible
//                }
//            }
//            root.isVisible = status?.availableToShow == true
//        } ?: run { binding?.poiRewardedAdStub?.inflate() }
//    }
//
//    override fun onStatusLoaded(status: POIStatus) {
//        MTLog.v(this, "onStatusLoaded($status)")
//        // TO DO this status needs to be added to view model POI Manager
//        //
//        //        context?.let { context ->
//        viewModel.onStatusLoaded(status) // TODO ?
//        //         POIViewController.updatePOIStatus(getPOIView(), status, this)
//        updatePOIStatusView(poiStatusN = status)
//        // POIStatusDetailViewController.updatePOIStatus(getPOIStatusView(), status, this, viewModel.poimV)
//        updateStatusDetailView(poiStatus = status)
//        //        }
//    }
//
//    override fun onServiceUpdatesLoaded(targetUUID: String, serviceUpdates: List<ServiceUpdate>?) {
//        MTLog.v(this, "onServiceUpdatesLoaded($targetUUID, $serviceUpdates)")
//        // T ODO this serviceUpdates needs to be added to view model POI Manager
//        //
//        // context?.let { context ->
//        viewModel.onServiceUpdatesLoaded(serviceUpdates) // TODO ?
//        // POIViewController.updateServiceUpdatesView(getPOIView(), serviceUpdates, this)
//        updatePOIServiceUpdatesView(poiServiceUpdates = serviceUpdates)
//        // POIServiceUpdateViewController.updateServiceUpdate(getPOIServiceUpdateView(), serviceUpdates, this)
//        updateServiceUpdateView(poiServiceUpdates = serviceUpdates)
//        // }
//    }
//
//    override fun onURLClick(url: String) =
//        LinkUtils.open(requireActivity(), url, getString(R.string.web_browser), true)
//
//    private var nowToTheMinute = -1L
//
//    override fun getNowToTheMinute(): Long {
//        MTLog.v(this, "getNowToTheMinute()")
//        if (this.nowToTheMinute < 0L) {
//            resetNowToTheMinute()
//            enableTimeChangedReceiver()
//        }
//        return this.nowToTheMinute
//    }
//
//    override fun onTimeChanged() {
//        resetNowToTheMinute()
//    }
//
//    private var timeChangedReceiverEnabled = false
//
//    private fun enableTimeChangedReceiver() {
//        MTLog.v(this, "enableTimeChangedReceiver()")
//        if (!timeChangedReceiverEnabled) {
//            timeChangedReceiverEnabled = true
//            context?.registerReceiver(timeChangedReceiver, UITimeUtils.TIME_CHANGED_INTENT_FILTER)
//        }
//    }
//
//    private fun disableTimeChangedReceiver() {
//        MTLog.v(this, "disableTimeChangedReceiver()")
//        if (timeChangedReceiverEnabled) {
//            context?.unregisterReceiver(timeChangedReceiver)
//            timeChangedReceiverEnabled = false
//            nowToTheMinute = -1L
//        }
//    }
//
//    private val timeChangedReceiver = TimeChangedReceiver(this)
//
//    private fun resetNowToTheMinute() {
//        // TO DO actually needs to call POIM .getStatus/ServiceUpdate to trigger data refresh!!!
//        MTLog.v(this, "resetNowToTheMinute()")
//        MTLog.i(this, "Refreshing UI data...")
//        nowToTheMinute = UITimeUtils.currentTimeToTheMinuteMillis()
//        // val view = view
//        // val poim: POIManager = getPoimOrNull()
//        //        viewModel.poimV?.let { poim ->
//        // if (poim != null) {
//        // val poiView = getPOIView()
//        // POIViewController.updatePOIStatus(poiView, poim, this)
//        viewModel.refreshStatus(context, this@POIFragment)
//        //            poim.setStatusLoaderListener(this@POIFragment)
//        //            poim.getStatus(context, providesStatusLoader())
//        viewModel.refreshServiceUpdate(context, this@POIFragment)
//        //            poim.setServiceUpdateLoaderListener(this@POIFragment)
//        //            poim.getServiceUpdates(context, providesServiceUpdateLoader())
//        // POIViewController.updatePOIServiceUpdate(poiView, poim, this)
//        // POIStatusDetailViewController.updateView(context, getPOIStatusView(), poim, this)
//        // updateStatusDetailView(poimN = poim)
//        // POIServiceUpdateViewController.updateView(getPOIServiceUpdateView(), poim, this)
//        // updateServiceUpdateView(poimN = poim)
//        //        }
//        updateServiceUpdateView()
//        updateStatusDetailView()
//        //        viewModel.latestNewsArticleList.value?.let {
//        // val news: ArrayList<News> = getNewsOrNull()
//        // if (news != null) {
//        // POINewsViewController.updateView(getPOINewsView(), news)
//        updateNewsView() // "X minutes ago on www.news.com"
//        //        }
//    }
//
//    override fun onSaveInstanceState(outState: Bundle) {
//        mapViewController.onSaveInstanceState(outState)
//        super.onSaveInstanceState(outState)
//    }
//
//    override fun onDetach() {
//        super.onDetach()
//        this.mapViewController.onDetach()
//    }
//
//    override fun onConfigurationChanged(newConfig: Configuration) {
//        super.onConfigurationChanged(newConfig)
//        mapViewController.onConfigurationChanged(newConfig)
//    }
//
//    override fun onResume() {
//        super.onResume()
//        adapter.onResume(this, viewModel.deviceLocation.value)
//        mapViewController.onResume()
//        mapViewController.showMap(view)
//        (activity as? MTActivityWithLocation)?.let { onUserLocationChanged(it.lastLocation) }
//        viewModel.onResumeRewardedAd(this, rewardedAdListener)
//        sensorManager.registerCompassListener(sensorEventListener)
//    }
//
//    override fun onUserLocationChanged(newLocation: Location?) {
//        viewModel.onDeviceLocationChanged(newLocation)
//        // TODO ?
//    }
//
//    private var addRemoveFavoriteMenuItem: MenuItem? = null
//
//    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
//        super.onCreateOptionsMenu(menu, inflater)
//        inflater.inflate(R.menu.menu_poi, menu)
//        addRemoveFavoriteMenuItem = menu.findItem(R.id.menu_add_remove_favorite)
//        updateFavMenuItem()
//    }
//
//    private fun updateFavMenuItem(
//        poi: POI? = viewModel.poi.value,
//        poiFavorite: Boolean? = viewModel.poiFavorite.value,
//    ) {
//        MTLog.v(this, "updateFavMenuItem($poi, $poiFavorite)")
//        this.addRemoveFavoriteMenuItem?.let { addRemoveFavoriteMenuItem ->
//            // val isFav = poim?.let { poim -> viewModel.isFavorite(poim) } ?: false
//            addRemoveFavoriteMenuItem.apply {
//                val isFav = poiFavorite ?: false
//                MTLog.d(this@POIFragment, "updateFavMenuItem() > isFav: $isFav")
//                val isFavoritable = poi?.let { POIManager.isFavoritable(it) }
//                MTLog.d(this@POIFragment, "updateFavMenuItem() > isFavoritable: $isFavoritable")
//                setIcon(if (isFav) R.drawable.ic_star_black_24dp else R.drawable.ic_star_border_black_24dp)
//                setTitle(
//                    if (isFav) {
//                        if (favoriteManager.isUsingFavoriteFolders) R.string.menu_action_edit_favorite else R.string.menu_action_remove_favorite
//                    } else {
//                        R.string.menu_action_add_favorite
//                    }
//                )
//                isVisible = isFavoritable == true && poiFavorite != null
//            }
//        }
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        return when (item.itemId) {
//            R.id.menu_add_remove_favorite -> {
//                viewModel.poi.value?.let { poi ->
//                    if (POIManager.isFavoritable(poi)) {
//                        this.favoriteManager.addRemoveFavorite(requireActivity(), poi.uuid, this)
//                    } else true // handled
//                } ?: false
//            }
//            R.id.menu_show_directions -> {
//                viewModel.poi.value?.let { poi ->
//                    viewModel.onShowDirectionClick()
//                    MapUtils.showDirection(requireActivity(), poi.lat, poi.lng, null, null, poi.name)
//                    return true // handled
//                } ?: false
//            }
//            else -> super.onOptionsItemSelected(item)
//        }
//    }
//
//    override fun onFavoriteUpdated() {
//        this.viewModel.onFavoriteUpdated()
//        context?.let {
//            // POIViewController.updateView(it, getPOIView(), viewModel.poimV, this)
//            updatePOIView()
//        }
//    }
//
//    override fun onPause() {
//        super.onPause()
//        // T ODO if (this.compassUpdatesEnabled) {
//        sensorManager.unregisterSensorListener(sensorEventListener)
//        //			this.compassUpdatesEnabled = false;
//        //		}
//        disableTimeChangedReceiver()
//        mapViewController.onPause()
//        adapter.onPause()
//        viewModel.onPauseRewardedAd()
//    }
//
//    override fun onLowMemory() {
//        super.onLowMemory()
//        mapViewController.onLowMemory()
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        mapViewController.onDestroyView()
//        rtsScheduleBtn = null
//        thisPoiBinding = null
//        poiStatusDetailBinding = null
//        poiRewardedAdBinding = null
//        poiNewsBinding = null
//        // newsMoreBtn = null
//        poiAppUpdateBinding = null
//        poiServiceUpdateBinding = null
//        binding = null
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        mapViewController.onDestroy()
//        adapter.onDestroy()
//    }
//
//    override fun isABReady() = viewModel.agency.value != null
//
//    override fun getABTitle(context: Context?) = viewModel.agency.value?.shortName ?: context?.getString(R.string.ellipsis) ?: super.getABTitle(null)
//
//    override fun getABBgColor(context: Context?) = Color.TRANSPARENT
//
//    override fun isShowingFavorite() = false // shown in the action bar
//
//    override fun isShowingStatus() = true
//
//    override fun isShowingExtra() = true
//
//    override fun isShowingServiceUpdates() = true
//
//    override fun providesDataSourcesRepository() = this.dataSourcesRepository
//
//    override fun providesStatusLoader() = this.statusLoader
//
//    override fun providesServiceUpdateLoader() = this.serviceUpdateLoader
//
//    override fun isClosestPOI(uuid: String) = false
//
//    override fun isFavorite(uuid: String) = viewModel.isFavorite(uuid)
//
//    override fun getLocation() = viewModel.deviceLocation.value
//
//    override fun hasLocation() = location != null
//
//    // private val locationDeclination = 0f
//    //
//    override fun getLocationDeclination() = viewModel.locationDeclination.value
//    //this.locationDeclination
//
//    // private var lastCompassInDegree: Int? = null
//    //
//    override fun getLastCompassInDegree() = viewModel.lastCompassInDegree.value
//
//    override fun hasLastCompassInDegree() = lastCompassInDegree != null
//}