package org.mtransit.android.ui.nearby

import android.location.Location
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import org.mtransit.android.ad.IAdManager
import org.mtransit.android.analytics.AnalyticsEvents
import org.mtransit.android.analytics.IAnalyticsManager
import org.mtransit.android.common.repository.LocalPreferenceRepository
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.PreferenceUtils
import org.mtransit.android.commons.pref.liveData
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.provider.location.MTLocationProvider
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.task.StatusLoader
import org.mtransit.android.ui.MTViewModelWithLocation
import org.mtransit.android.ui.view.common.Event
import org.mtransit.android.ui.view.common.IActivity
import org.mtransit.android.ui.view.common.PairMediatorLiveData
import org.mtransit.android.ui.view.common.TripleMediatorLiveData
import org.mtransit.android.ui.view.common.getLiveDataDistinct
import javax.inject.Inject

@HiltViewModel
class NearbyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val analyticsManager: IAnalyticsManager,
    private val adManager: IAdManager,
    private val locationProvider: MTLocationProvider,
    private val dataSourcesRepository: DataSourcesRepository,
    private val lclPrefRepository: LocalPreferenceRepository,
    private val statusLoader: StatusLoader,
    private val serviceUpdateLoader: ServiceUpdateLoader,
) : MTViewModelWithLocation() {

    companion object {
        private val LOG_TAG = NearbyViewModel::class.java.simpleName

        private const val IGNORE_SAME_LOCATION_CHECK = false
        // private const val IGNORE_SAME_LOCATION_CHECK = true // DEBUG

        internal const val EXTRA_SELECTED_TYPE = "extra_selected_type"
        internal const val EXTRA_FIXED_ON_LAT = "extra_fixed_on_lat"
        internal const val EXTRA_FIXED_ON_LNG = "extra_fixed_on_lng"
        internal const val EXTRA_FIXED_ON_NAME = "extra_fixed_on_name"
        internal const val EXTRA_FIXED_ON_COLOR = "extra_fixed_on_color"
    }

    override fun getLogTag(): String = LOG_TAG

    private val _selectedTypeId = savedStateHandle.getLiveDataDistinct<Int?>(EXTRA_SELECTED_TYPE)

    private val _fixedOnLat = savedStateHandle.getLiveDataDistinct<Double?>(EXTRA_FIXED_ON_LAT)
    private val _fixedOnLng = savedStateHandle.getLiveDataDistinct<Double?>(EXTRA_FIXED_ON_LNG)

    val fixedOnLocation: LiveData<Location?> = PairMediatorLiveData(_fixedOnLat, _fixedOnLng).map { (fixedOnLat, fixedOnLng) ->
        if (fixedOnLat == null || fixedOnLng == null) {
            null
        } else {
            LocationUtils.getNewLocation(fixedOnLat, fixedOnLng)
        }
    }.distinctUntilChanged()

    private val _nearbyLocationForceReset = MutableLiveData(Event(false))
    val nearbyLocationForceReset = _nearbyLocationForceReset

    val nearbyLocation: LiveData<Location?> =
        TripleMediatorLiveData(fixedOnLocation, deviceLocation, _nearbyLocationForceReset).switchMap { (fixedOnLocation, lastDeviceLocation, forceResetEvent) ->
            liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
                val forceReset: Boolean = forceResetEvent?.getContentIfNotHandled() ?: false
                if (forceReset) {
                    emit(null) // force reset
                }
                emit(getNearbyLocation(fixedOnLocation, lastDeviceLocation, forceReset))
            }
        }.distinctUntilChanged()

    private fun getNearbyLocation(fixedOnLocation: Location?, lastDeviceLocation: Location?, forceReset: Boolean): Location? {
        if (!forceReset) {
            nearbyLocation.value?.let {
                MTLog.d(this, "getNearbyLocation() > keep same ($it)")
                return it
            }
        }
        if (fixedOnLocation != null) {
            MTLog.d(this, "getNearbyLocation() > use fixed ON ($fixedOnLocation)")
            return fixedOnLocation
        }
        MTLog.d(this, "getNearbyLocation() > use last device location ($lastDeviceLocation)")
        return lastDeviceLocation
    }

    val nearbyLocationAddress: LiveData<String?> = nearbyLocation.switchMap { nearbyLocation ->
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(getNearbyLocationAddress(nearbyLocation))
        }
    }

    private fun getNearbyLocationAddress(location: Location?): String? {
        return location?.let {
            locationProvider.getLocationAddressString(it)
        }
    }

    val fixedOnName = savedStateHandle.getLiveDataDistinct<String?>(EXTRA_FIXED_ON_NAME)

    val isFixedOn: LiveData<Boolean?> = TripleMediatorLiveData(_fixedOnLat, _fixedOnLng, fixedOnName).map { (lat, lng, name) ->
        lat != null && lng != null && !name.isNullOrBlank()
    }

    val newLocationAvailable: LiveData<Boolean?> =
        TripleMediatorLiveData(isFixedOn, nearbyLocation, deviceLocation).map { (isFixedOn, nearbyLocation, deviceLocation) ->
            isFixedOn == false
                    && nearbyLocation != null
                    && deviceLocation != null
                    && !LocationUtils.areAlmostTheSame(nearbyLocation, deviceLocation, LocationUtils.LOCATION_CHANGED_NOTIFY_USER_IN_METERS)
        }

    val fixedOnColorInt = savedStateHandle.getLiveDataDistinct<Int?>(EXTRA_FIXED_ON_COLOR)

    val availableTypes: LiveData<List<DataSourceType>?> = this.dataSourcesRepository.readingAllDataSourceTypes().map { // #onModulesUpdated
        it.filter { dst -> dst.isNearbyScreen }
    }

    private val _selectedTypeIdPref: LiveData<Int?> = lclPrefRepository.pref.liveData(
        PreferenceUtils.PREFS_LCL_NEARBY_TAB_TYPE,
    )

    val selectedTypeId: LiveData<Int?> = PairMediatorLiveData(_selectedTypeId, _selectedTypeIdPref).map { (selectedTypeId, selectedTypeIdPref) ->
        selectedTypeId ?: selectedTypeIdPref
    }

    val selectedType: LiveData<DataSourceType?> = selectedTypeId.map {
        it?.let { DataSourceType.parseId(it) }
    }

    val selectedTypePosition: LiveData<Int?> = PairMediatorLiveData(selectedType, availableTypes).map { (selectedType, availableTypes) ->
        selectedType?.let { selected ->
            availableTypes
                ?.indexOf(selected)
                ?.takeIf { it >= 0 } // selected might not be in available (installed) types
        }
    }

    fun onPageSelected(position: Int) {
        this.statusLoader.clearAllTasks()
        this.serviceUpdateLoader.clearAllTasks()
        saveSelectedTypePosition(position)
    }

    private fun saveSelectedTypePosition(position: Int) {
        saveSelectedType(
            availableTypes.value?.getOrNull(position) ?: return
        )
    }

    private fun saveSelectedType(dst: DataSourceType) {
        lclPrefRepository.pref.edit {
            putInt(PreferenceUtils.PREFS_LCL_NEARBY_TAB_TYPE, dst.id)
        }
    }

    fun onShowDirectionClick() {
        analyticsManager.logEvent(AnalyticsEvents.OPENED_GOOGLE_MAPS_TRIP_PLANNER)
    }

    fun initiateRefresh(): Boolean {
        if (isFixedOn.value == true) {
            MTLog.d(this, "initiateRefresh() > SKIP (fixed on location)")
            return false
        }
        val newDeviceLocation = this.deviceLocation.value ?: return false
        val currentNearbyLocation = this.nearbyLocation.value
        if (!IGNORE_SAME_LOCATION_CHECK
            && LocationUtils.areAlmostTheSame(currentNearbyLocation, newDeviceLocation, LocationUtils.LOCATION_CHANGED_ALLOW_REFRESH_IN_METERS)
        ) {
            MTLog.d(this, "initiateRefresh() > SKIP (same location)")
            return false
        }
        this._nearbyLocationForceReset.value = Event(true)
        MTLog.d(this, "initiateRefresh() > use NEW location")
        return true
    }

    fun getAdBannerHeightInPx(activity: IActivity?): Int {
        return this.adManager.getBannerHeightInPx(activity)
    }
}