package org.mtransit.android.ui.poi
//
//import android.content.Context
//import android.hardware.SensorEvent
//import android.location.Location
//import android.widget.AbsListView
//import androidx.annotation.WorkerThread
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.SavedStateHandle
//import androidx.lifecycle.distinctUntilChanged
//import androidx.lifecycle.liveData
//import androidx.lifecycle.map
//import androidx.lifecycle.switchMap
//import androidx.lifecycle.viewModelScope
//import dagger.hilt.android.lifecycle.HiltViewModel
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import org.mtransit.android.ad.IAdManager
//import org.mtransit.android.ad.IAdManager.RewardedAdListener
//import org.mtransit.android.analytics.AnalyticsEvents
//import org.mtransit.android.analytics.AnalyticsEventsParamsProvider
//import org.mtransit.android.analytics.IAnalyticsManager
//import org.mtransit.android.commons.Constants
//import org.mtransit.android.commons.LocationUtils
//import org.mtransit.android.commons.MTLog
//import org.mtransit.android.commons.data.News
//import org.mtransit.android.commons.data.POI
//import org.mtransit.android.commons.data.POIStatus
//import org.mtransit.android.commons.data.Schedule.ScheduleStatusFilter
//import org.mtransit.android.commons.data.ServiceUpdate
//import org.mtransit.android.commons.provider.news.NewsProviderContract
//import org.mtransit.android.commons.provider.poi.POIProviderContract
//import org.mtransit.android.data.AgencyProperties
//import org.mtransit.android.data.POIManager
//import org.mtransit.android.data.ScheduleProviderProperties
//import org.mtransit.android.datasource.DataSourceRequestManager
//import org.mtransit.android.datasource.DataSourcesRepository
//import org.mtransit.android.provider.FavoriteRepository
//import org.mtransit.android.provider.location.MTLocationProvider
//import org.mtransit.android.provider.sensor.MTSensorManager
//import org.mtransit.android.provider.sensor.MTSensorManager.CompassListener
//import org.mtransit.android.provider.sensor.MTSensorManager.SensorTaskCompleted
//import org.mtransit.android.task.ServiceUpdateLoader
//import org.mtransit.android.task.ServiceUpdateLoader.ServiceUpdateLoaderListener
//import org.mtransit.android.task.StatusLoader
//import org.mtransit.android.task.StatusLoader.StatusLoaderListener
//import org.mtransit.android.ui.MTViewModelWithLocation
//import org.mtransit.android.ui.news.details.NewsDetailsViewModel
//import org.mtransit.android.ui.view.common.Event
//import org.mtransit.android.ui.view.common.IActivity
//import org.mtransit.android.ui.view.common.PairMediatorLiveData
//import org.mtransit.android.util.DegreeUtils.convertToPositive360Degree
//import org.mtransit.android.util.UITimeUtils
//import java.util.concurrent.TimeUnit
//import javax.inject.Inject
//
//@HiltViewModel
//class POIViewModel @Inject constructor(
// savedStateHandle: SavedStateHandle,
// private val dataSourcesRepository: DataSourcesRepository,
// private val dataSourceRequestManager: DataSourceRequestManager,
// private val favoriteRepository: FavoriteRepository,
// private val sensorManager: MTSensorManager,
// private val locationProvider: MTLocationProvider,
// private val adManager: IAdManager,
// private val analyticsManager: IAnalyticsManager,
// private val statusLoader: StatusLoader,
// private val serviceUpdateLoader: ServiceUpdateLoader,
// // private val lclPrefRepository: LocalPreferenceRepository,
//) : MTViewModelWithLocation() {
//
//
// companion object {
// private val LOG_TAG: String = POIViewModel::class.java.simpleName
//
// internal const val EXTRA_AUTHORITY = "extra_agency_authority"
// internal const val EXTRA_POI_UUID = "extra_poi_uuid"
// }
//
// override fun getLogTag() = LOG_TAG
//
// val uuid: LiveData<String?> = savedStateHandle.getLiveData<String?>(EXTRA_POI_UUID).distinctUntilChanged()
//
// private val _authority: LiveData<String?> = savedStateHandle.getLiveData<String?>(NewsDetailsViewModel.EXTRA_AUTHORITY).distinctUntilChanged()
//
// val agency: LiveData<AgencyProperties?> = this._authority.switchMap { authority ->
// authority?.let {
// this.dataSourcesRepository.readingAgency(authority) // #onModulesUpdated
// } ?: MutableLive Data(null)
// }
//
// private val _favoriteUpdatedTrigger = MutableLiveData(0)
//
// val dataSourceRemovedEvent = MutableLiveData<Event<Boolean>>()
//
// private val _poimMutable = MutableLiveData<POIManager?>(null)
//
// private val _poim: LiveData<POIManager?> = _poimMutable.distinctUntilChanged()
//
// val poimTrigger: LiveData<Any?> = PairMediatorLiveData(agency, uuid).map { (agency, uuid) -> // use agency // #onModulesUpdated
// //liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
// // emit(getPOIManager(agency, uuid))
// loadPOIManager(agency, uuid)
// // emit(null)
// // }
// null
// }
//
// @Deprecated(message = "use smaller data")
// val poim: LiveData<POIManager?> = _poim // FIXME remove
//
// val poimV: POIManager?
// get() = this._poim.value
//
// val poi: LiveData<POI?> = _poim.map {
// it?.poi
// }.distinctUntilChanged()
//
// val poiDistanceAndString: LiveData<Pair<Float, CharSequence?>?> = _poim.map { poim ->
// // locationProvider.updateDistanceWithString(poim, deviceLocation)
// // this._poimMutable.value = poim
// poim?.let { it.distance to it.distanceString }
// }.distinctUntilChanged()
//
// val poiDistanceTrigger = PairMediatorLiveData(_poim, deviceLocation).map { (poim, deviceLocation) ->
// locationProvider.updateDistanceWithString(poim, deviceLocation)
// this._poimMutable.value = poim
// }
//
// fun updatePOIDistanceWithString(
// poim: POIManager? = this._poimMutable.value,
// deviceLocation: Location? = this.deviceLocation.value,
// ) {
// // TO DO this does not work, poim is not updated!!!!!!
// // val poim = this._poimMutable.value
// locationProvider.updateDistanceWithString(poim ?: return, deviceLocation)
// this._poimMutable.value = poim
// }
//
// val poiStatus: LiveData<POIStatus?> = _poim.map {
// it?.statusOrNull
// }.distinctUntilChanged()
//
// fun onStatusLoaded(status: POIStatus) {
// MTLog.v(this, "onStatusLoaded($status)")
// val poim = this._poimMutable.value
// poim?.setStatus(status)
// this._poimMutable.value = poim
// }
//
// fun refreshStatus(context: Context?, statusLoaderListener: StatusLoaderListener) {
// val poim = this._poimMutable.value
// poim?.apply {
// setStatusLoaderListener(statusLoaderListener)
// getStatus(context, statusLoader)
// }
// this._poimMutable.value = poim
// }
//
// fun onServiceUpdatesLoaded(serviceUpdates: List<ServiceUpdate>?) {
// val poim = this._poimMutable.value
// poim?.setServiceUpdates(serviceUpdates)
// this._poimMutable.value = poim
// }
//
// fun refreshServiceUpdate(context: Context?, serviceUpdateLoaderListener: ServiceUpdateLoaderListener) {
// val poim = this._poimMutable.value
// poim?.apply {
// setServiceUpdateLoaderListener(serviceUpdateLoaderListener)
// getServiceUpdates(context, serviceUpdateLoader)
// }
// this._poimMutable.value = poim
// }
//
// val poiServiceUpdates: LiveData<List<ServiceUpdate>?> = _poim.map {
// it?.serviceUpdatesOrNull
// }.distinctUntilChanged()
// // .distinctUntilChanged()
//
// val poiTypeAndStatus: LiveData<Pair<Int, Int>?> = _poim.map {
// it?.let { it.poi.type to it.statusType }
// }.distinctUntilChanged()
//
// private fun loadPOIManager(agency: AgencyProperties?, uuid: String?) {
// viewModelScope.launch(context = viewModelScope.coroutineContext + Dispatchers.IO) {
// _poimMutable.postValue(getPOIManager(agency, uuid))
// }
// }
//
// private fun getPOIManager(agency: AgencyProperties?, uuid: String?): POIManager? {
// if (uuid.isNullOrEmpty()) {
// MTLog.d(this, "getPOI() > SKIP (no uuid)")
// return null
// }
// if (agency == null) {
// if (_poim.value != null) {
// MTLog.d(this, "getPOI() > data source removed (no more agency)")
// dataSourceRemovedEvent.postValue(Event(true))
// }
// return null
// }
// return this.dataSourceRequestManager.findPOIM(agency.authority, POIProviderContract.Filter.getNewUUIDFilter(uuid))
// ?.apply {
// isInFocus = true
// setScheduleMaxDataRequests(ScheduleStatusFilter.DATA_REQUEST_MONTHS)
// resetLastFindTimestamps()
// // updatePOIDistanceWithString(poim = this)
// locationProvider.updateDistanceWithString(this, deviceLocation.value)
// } ?: run {
// MTLog.d(this, "getPOI() > SKIP (data source removed!)")
// dataSourceRemovedEvent.postValue(Event(true))
// null
// }
// }
//
// val poiColor: LiveData<Int?> = PairMediatorLiveData(poi, agency).map { (poi, agency) ->
// // poim?.getColor { agency }
// poi?.let { POIManager.getNewColor(it) { agency } }
// }.distinctUntilChanged()
//
// val poimOneLineDescription: LiveData<String?> = PairMediatorLiveData(agency, poi).map { (agency, poi) ->
// poi?.let { POIManager.getNewOneLineDescription(it) { agency } }
// }.distinctUntilChanged()
//
// fun onShowDirectionClick() {
// analyticsManager.logEvent(AnalyticsEvents.OPENED_GOOGLE_MAPS_TRIP_PLANNER)
// }
//
// fun onShowedAppUpdatePOI(agency: AgencyProperties? = this.agency.value) {
// val params = AnalyticsEventsParamsProvider()
// agency?.let {
// params.put(AnalyticsEvents.Params.PKG, it.pkg)
// }
// analyticsManager.logEvent(AnalyticsEvents.SHOWED_APP_UPDATE_POI, params)
// }
//
// fun onHiddenAppUpdatePOI(agency: AgencyProperties? = this.agency.value) {
// val params = AnalyticsEventsParamsProvider()
// agency?.let {
// params.put(AnalyticsEvents.Params.PKG, it.pkg)
// }
// analyticsManager.logEvent(AnalyticsEvents.HIDDEN_APP_UPDATE_POI, params)
// }
//
// fun onClickAppUpdatePOI(agency: AgencyProperties? = this.agency.value) {
// val params = AnalyticsEventsParamsProvider()
// agency?.let {
// params.put(AnalyticsEvents.Params.PKG, it.pkg)
// }
// analyticsManager.logEvent(AnalyticsEvents.CLICK_APP_UPDATE_POI, params)
// }
//
// private val _rewardedAdStatus = MutableLiveData(RewardedAdStatus(this.adManager))
// val rewardedAdStatus: LiveData<RewardedAdStatus?> = _rewardedAdStatus.distinctUntilChanged()
//
// // private val _rewardedAdNowUntilInMs = MutableLiveData(if (this.adManager.isRewardedNow) this.adManager.rewardedUntilInMs else null)
// // val rewardedAdNowUntilInMs: LiveData<Long?> = _rewardedAdNowUntilInMs.distinctUntilChanged()
// //
// // private val _rewardedAdAmount = MutableLiveData(this.adManager.rewardedAdAmount)
// // val rewardedAdAmount: LiveData<Int?> = _rewardedAdAmount.distinctUntilChanged()
// //
// // private val _rewardedAdAvailableToShow = MutableLiveData(this.adManager.isRewardedAdAvailableToShow)
// // val rewardedAdAvailableToShow: LiveData<Boolean?> = _rewardedAdAvailableToShow.distinctUntilChanged()
// //
// fun onResumeRewardedAd(activity: IActivity, rewardedAdListener: RewardedAdListener) {
// this.adManager.setRewardedAdListener(rewardedAdListener)
// this.adManager.refreshRewardedAdStatus(activity)
// }
//
// fun onPauseRewardedAd() {
// adManager.setRewardedAdListener(null)
// }
//
// fun onRewardedAdStatusChanged() {
// // _rewardedAdNowUntilInMs.postValue(if (this.adManager.isRewardedNow) this.adManager.rewardedUntilInMs else null)
// // _rewardedAdAmount.postValue(this.adManager.rewardedAdAmount)
// // _rewardedAdAvailableToShow.postValue(this.adManager.isRewardedAdAvailableToShow)
// _rewardedAdStatus.postValue(RewardedAdStatus(this.adManager))
// }
//
// fun skipRewardedAd() = this.adManager.shouldSkipRewardedAd()
//
// fun onRewardedAdClick(activity: IActivity): Boolean? {
// if (activity.activity == null) {
// MTLog.w(this, "onRewardedAdClick() > skip (no view or no activity)")
// return false // hide
// }
// if (!this.adManager.isRewardedAdAvailableToShow) {
// MTLog.w(this, "onRewardedAdClick() > skip (no ad available)")
// return false // hide
// }
// val adShowTriggered = this.adManager.showRewardedAd(activity)
// return if (!adShowTriggered) false else null
// }
//
// val poiFavorite: LiveData<Boolean?> = PairMediatorLiveData(_favoriteUpdatedTrigger, poi).switchMap { (_, poi) ->
// liveData {
// poi?.let {
// emit(getPOIFavorite(poi))
// } ?: run { emit(null) }
// }
// }
//
// fun getPOIFavorite(poi: POI): Boolean {
// return POIManager.isFavoritable(poi) && favoriteRepository.isFavorite(poi.uuid)
// }
//
//
// fun onFavoriteUpdated() {
// _favoriteUpdatedTrigger.value = (_favoriteUpdatedTrigger.value ?: 0) + 1
// }
//
// // fun onAddRemoveFavoriteClick(activity: Activity, listener: FavoriteUpdateListener) {
// // val poim: POIManager = this.poim.value ?: return
// // if (poim.isFavoritable) {
// // favoriteRepository.addRe
// // }
// // }
// //
// @WorkerThread
// fun isFavorite(uuid: String): Boolean {
// MTLog.v(this, "isFavorite($uuid)")
// val poiUUID = this.uuid.value
// if (poiUUID == uuid) {
// poiFavorite.value?.let {
// return it
// }
// }
// return favoriteRepository.isFavorite(uuid)
// }
//
// val locationDeclination: LiveData<Float?> = deviceLocation.map { newDeviceLocation ->
// newDeviceLocation?.let { sensorManager.getLocationDeclination(it) }
// }
//
// private val _accelerometerValues = FloatArray(3)
//
// private val _magneticFieldValues = FloatArray(3)
//
// fun onSensorChanged(activity: IActivity, event: SensorEvent, compassListener: CompassListener) {
// sensorManager.checkForCompass(activity, event, this._accelerometerValues, this._magneticFieldValues, compassListener)
// }
//
// private val _lastCompassInDegree = MutableLiveData<Int?>(null)
// val lastCompassInDegree: LiveData<Int?> = _lastCompassInDegree
//
// // private var _lastCompassInDegree: Int? = null
// private var _lastCompassChanged = -1L
//
// fun updateCompass(orientation: Float, force: Boolean, onSensorTaskCompleted: SensorTaskCompleted) {
// val now = UITimeUtils.currentTimeMillis()
// val roundedOrientation = convertToPositive360Degree(orientation.toInt())
// val deviceLocation = this.deviceLocation.value
// sensorManager.updateCompass(
// force,
// deviceLocation,
// roundedOrientation,
// now,
// AbsListView.OnScrollListener.SCROLL_STATE_IDLE,
// this._lastCompassChanged,
// this._lastCompassInDegree.value,
// Constants.ADAPTER_NOTIFY_THRESHOLD_IN_MS,
// onSensorTaskCompleted
// )
// }
//
// fun onSensorTaskCompleted(result: Boolean, roundedOrientation: Int, now: Long) {
// if (result) {
// this._lastCompassInDegree.postValue(roundedOrientation)
// this._lastCompassChanged = now
// // if (this.deviceLocation.value != null && this._lastCompassInDegree ?: -1 >= 0) {
// // this.compassUpdatesEnabled &&
// // val poim: POIManager = getPoimOrNull()
// // if (poim != null) {
// // POIViewController.updatePOIDistanceAndCompass(getPOIView(getView()), poim, this)
// // }
// // }
// }
// }
//
// val latestNewsArticleList: LiveData<List<News>?> = poi.switchMap { poi ->
// liveData {
// emit(getLatestNewsArticles(poi))
// }
// }
//
// private fun getLatestNewsArticles(poi: POI?): List<News>? {
// MTLog.v(this, "getLatestNewsArticles($poi)")
// if (poi == null) {
// return null
// }
// val nowInMs = UITimeUtils.currentTimeMillis()
// val last2Weeks = nowInMs - TimeUnit.DAYS.toMillis(14L)
// val newsFilter = NewsProviderContract.Filter
// .getNewTargetFilter(poi)
// .setMinCreatedAtInMs(last2Weeks)
// // val allNews = mutableListOf<News>()
// val allNews = dataSourcesRepository.getNewsProviders(poi)
// .mapNotNull { newsProvider ->
// this.dataSourceRequestManager.findNews(newsProvider, newsFilter)
// }.flatten()
// .toMutableList()
// allNews.sortWith(News.NEWS_SEVERITY_COMPARATOR)
// // allNews.sortedWith(compareBy({ it.severity }, { it.createdAtInMs }))
// val selectedNews = mutableListOf<News>()
// var noteworthiness = 1L
// while (selectedNews.isEmpty() && noteworthiness < 10L) {
// for (news in allNews) {
// if (news.createdAtInMs + news.noteworthyInMs * noteworthiness < nowInMs) {
// continue  // news too old to be worthy
// }
// selectedNews.add(0, news)
// break // found news article
// }
// noteworthiness++
// }
// return selectedNews
// }
//
// val scheduleProviders: LiveData<List<ScheduleProviderProperties>> = _authority.switchMap { authority ->
// this.dataSourcesRepository.readingScheduleProviders(authority)
// }
//
// // like Home screen (no infinite loading like in Nearby screen)
// val nearbyPOIs: LiveData<List<POIManager>?> = poi.switchMap { poi ->
// liveData {
// emit(getNearbyPOIs(poi))
// }
// }
//
// private fun getNearbyPOIs(poi: POI?): List<POIManager>? {
// return getNearbyPOIs(poi?.authority, poi?.lat, poi?.lng)
// }
//
// private fun getNearbyPOIs(
// authority: String? = poi.value?.authority,
// lat: Double? = poi.value?.lat,
// lng: Double? = poi.value?.lng,
// excludedUUID: String? = poi.value?.uuid
// ): List<POIManager>? {
// MTLog.v(this, "getNearbyPOIs()")
// if (Constants.FORCE_NEARBY_POI_LIST_OFF) {
// return null
// }
// if (authority == null || lat == null || lng == null) {
// return null
// }
// val nearbyPOIs = mutableListOf<POIManager>()
// val ad = LocationUtils.getNewDefaultAroundDiff()
// // TODO latter ? var lastTypeAroundDiff: Double? = null
// val maxSize = LocationUtils.MAX_POI_NEARBY_POIS_LIST
// val minCoverageInMeters = LocationUtils.MIN_POI_NEARBY_POIS_LIST_COVERAGE_IN_METERS.toFloat()
// while (true) {
// val aroundDiff = ad.aroundDiff
// val maxDistance = LocationUtils.getAroundCoveredDistanceInMeters(lat, lng, aroundDiff)
// val poiFilter = POIProviderContract.Filter.getNewAroundFilter(lat, lng, aroundDiff).apply {
// addExtra(POIProviderContract.POI_FILTER_EXTRA_AVOID_LOADING, true)
// // addExtra(GTFSProviderContract.POI_FILTER_EXTRA_NO_PICKUP, false)
// }
// dataSourceRequestManager.findPOIMs(authority, poiFilter)
// ?.filterNot { it.poi.uuid == excludedUUID }
// ?.let { agencyPOIs ->
// LocationUtils.updateDistance(agencyPOIs, lat, lng)
// LocationUtils.removeTooFar(agencyPOIs, maxDistance)
// LocationUtils.removeTooMuchWhenNotInCoverage(agencyPOIs, minCoverageInMeters, maxSize)
// nearbyPOIs.addAll(agencyPOIs)
// }
// if (nearbyPOIs.size > LocationUtils.MIN_NEARBY_LIST // enough POI
// || LocationUtils.searchComplete(lat, lng, aroundDiff) // world explored
// ) {
// // && LocationUtils.getAroundCoveredDistanceInMeters(lat, lng, aroundDiff) >= minCoverageInMeters
// break
// } else {
// // TODO latter ? lastTypeAroundDiff = if (nearbyPOIs.isNullOrEmpty()) aroundDiff else null
// LocationUtils.incAroundDiff(ad)
// }
// }
// return nearbyPOIs.take(LocationUtils.MAX_POI_NEARBY_POIS_LIST)
// }
//}