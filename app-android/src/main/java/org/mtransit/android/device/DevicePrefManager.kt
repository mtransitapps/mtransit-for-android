package org.mtransit.android.device

import android.annotation.SuppressLint
import androidx.annotation.Discouraged
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mtransit.android.common.repository.LocalPreferenceRepository
import org.mtransit.android.commons.data.DataSourceTypeId
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.data.RouteDirection
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.data.toRouteDirection
import org.mtransit.android.commons.pref.liveData
import org.mtransit.android.commons.pref.preferenceChangeLiveData
import org.mtransit.android.data.IAgencyProperties
import org.mtransit.android.data.POIManager
import org.mtransit.android.util.MapUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DevicePrefManager @Inject constructor(
    private val lclPrefRepository: LocalPreferenceRepository,
) {

    val prefChanged: LiveData<String?> = lclPrefRepository.pref.preferenceChangeLiveData()

    // region route direction - list/map

    fun routeDirectionShowingListInsteadOfMap(routeDirection: RouteDirection): LiveData<Boolean> = lclPrefRepository.pref.liveData(
        LocalPreferenceRepository.getPREFS_LCL_RDS_DIRECTION_SHOWING_LIST_INSTEAD_OF_MAP_KEY(routeDirection),
        LocalPreferenceRepository.PREFS_LCL_RDS_DIRECTION_SHOWING_LIST_INSTEAD_OF_MAP_DEFAULT
    )

    @Suppress("unused")
    suspend fun getRouteDirectionShowingListInsteadOfMap(routeDirection: RouteDirection) = withContext(Dispatchers.IO) {
        lclPrefRepository.pref.getBoolean(
            LocalPreferenceRepository.getPREFS_LCL_RDS_DIRECTION_SHOWING_LIST_INSTEAD_OF_MAP_KEY(routeDirection),
            LocalPreferenceRepository.PREFS_LCL_RDS_DIRECTION_SHOWING_LIST_INSTEAD_OF_MAP_DEFAULT
        )
    }

    @SuppressLint("DiscouragedApi")
    @Discouraged("use suspend function")
    fun updateRouteDirectionShowingListInsteadOfMapNow(rds: RouteDirectionStop, showListInsteadOfMap: Boolean) =
        updateRouteDirectionShowingListInsteadOfMapNow(rds.toRouteDirection(), showListInsteadOfMap)

    @Discouraged("use suspend function")
    fun updateRouteDirectionShowingListInsteadOfMapNow(routeDirection: RouteDirection, showListInsteadOfMap: Boolean) {
        lclPrefRepository.pref.edit {
            putBoolean(
                LocalPreferenceRepository.getPREFS_LCL_RDS_DIRECTION_SHOWING_LIST_INSTEAD_OF_MAP_KEY(routeDirection),
                showListInsteadOfMap
            )
        }
    }

    @Suppress("unused")
    suspend fun updateRouteDirectionShowingListInsteadOfMap(rds: RouteDirectionStop, showListInsteadOfMap: Boolean) =
        updateRouteDirectionShowingListInsteadOfMap(rds.toRouteDirection(), showListInsteadOfMap)

    suspend fun updateRouteDirectionShowingListInsteadOfMap(routeDirection: RouteDirection, showListInsteadOfMap: Boolean) = withContext(Dispatchers.IO) {
        lclPrefRepository.pref.edit {
            putBoolean(
                LocalPreferenceRepository.getPREFS_LCL_RDS_DIRECTION_SHOWING_LIST_INSTEAD_OF_MAP_KEY(routeDirection),
                showListInsteadOfMap
            )
        }
    }

    // endregion

    // region agency type - selected tab

    fun selectedAgencyTypeTab(@DataSourceTypeId.DataSourceType dstId: Int): LiveData<String> = lclPrefRepository.pref.liveData(
        LocalPreferenceRepository.getPREFS_LCL_AGENCY_TYPE_TAB_AGENCY(dstId),
        LocalPreferenceRepository.PREFS_LCL_AGENCY_TYPE_TAB_AGENCY_DEFAULT
    )

    suspend fun getSelectedAgencyTypeTab(@DataSourceTypeId.DataSourceType dstId: Int) = withContext(Dispatchers.IO) {
        lclPrefRepository.pref.getString(
            LocalPreferenceRepository.getPREFS_LCL_AGENCY_TYPE_TAB_AGENCY(dstId),
            LocalPreferenceRepository.PREFS_LCL_AGENCY_TYPE_TAB_AGENCY_DEFAULT
        )
    }

    @Suppress("unused")
    suspend fun updateSelectedAgencyTypeTab(poim: POIManager) = updateSelectedAgencyTypeTab(poim.poi)
    suspend fun updateSelectedAgencyTypeTab(poi: POI) = updateSelectedAgencyTypeTab(poi.dataSourceTypeId, poi.authority)
    suspend fun updateSelectedAgencyTypeTab(agency: IAgencyProperties) = updateSelectedAgencyTypeTab(agency.getSupportedType().id, agency.authority)
    suspend fun updateSelectedAgencyTypeTab(@DataSourceTypeId.DataSourceType dstId: Int, agencyAuthority: String) = withContext(Dispatchers.IO) {
        lclPrefRepository.pref.edit {
            putString(LocalPreferenceRepository.getPREFS_LCL_AGENCY_TYPE_TAB_AGENCY(dstId), agencyAuthority)
        }
    }

    @SuppressLint("DiscouragedApi")
    @Discouraged("use suspend function")
    fun updateSelectedAgencyTypeTabNow(poim: POIManager) = updateSelectedAgencyTypeTabNow(poim.poi)

    @SuppressLint("DiscouragedApi")
    @Discouraged("use suspend function")
    fun updateSelectedAgencyTypeTabNow(poi: POI) = updateSelectedAgencyTypeTabNow(poi.dataSourceTypeId, poi.authority)

    @SuppressLint("DiscouragedApi")
    @Discouraged("use suspend function")
    fun updateSelectedAgencyTypeTabNow(agency: IAgencyProperties) = updateSelectedAgencyTypeTabNow(agency.getSupportedType().id, agency.authority)

    @Discouraged("use suspend function")
    fun updateSelectedAgencyTypeTabNow(@DataSourceTypeId.DataSourceType dstId: Int, agencyAuthority: String) {
        lclPrefRepository.pref.edit {
            putString(LocalPreferenceRepository.getPREFS_LCL_AGENCY_TYPE_TAB_AGENCY(dstId), agencyAuthority)
        }
    }

    // endregion

    // region map type

    @Suppress("unused")
    suspend fun getMayType() = withContext(Dispatchers.IO) {
        lclPrefRepository.pref.getInt(
            MapUtils.PREFS_LCL_MAP_TYPE, MapUtils.PREFS_LCL_MAP_TYPE_DEFAULT
        )
    }

    @Discouraged("use suspend function")
    fun getMapTypeNow() = lclPrefRepository.pref.getInt(
        MapUtils.PREFS_LCL_MAP_TYPE, MapUtils.PREFS_LCL_MAP_TYPE_DEFAULT
    )

    @Suppress("unused")
    suspend fun setMapType(mapType: Int) = withContext(Dispatchers.IO) {
        lclPrefRepository.pref.edit {
            putInt(MapUtils.PREFS_LCL_MAP_TYPE, mapType)
        }
    }

    @SuppressLint("DiscouragedApi")
    @Discouraged("use suspend function")
    fun setMapTypeNow(mapType: Int) {
        lclPrefRepository.pref.edit {
            putInt(MapUtils.PREFS_LCL_MAP_TYPE, mapType)
        }
    }

    // endregion
}
