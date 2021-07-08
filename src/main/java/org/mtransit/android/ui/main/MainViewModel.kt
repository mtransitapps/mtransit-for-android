package org.mtransit.android.ui.main

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mtransit.android.R
import org.mtransit.android.common.repository.DefaultPreferenceRepository
import org.mtransit.android.common.repository.LocalPreferenceRepository
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.PreferenceUtils
import org.mtransit.android.commons.pref.liveData
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.datasource.DataSourcesRepository
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val dataSourcesRepository: DataSourcesRepository,
    private val lclPrefRepository: LocalPreferenceRepository,
    private val defaultPrefRepository: DefaultPreferenceRepository,
) : ViewModel(), MTLog.Loggable {


    companion object {
        private val LOG_TAG = "Stack-" + MainViewModel::class.java.simpleName

        private const val ITEM_ID_AGENCY_TYPE_START_WITH = "agencytype-"
        private const val ITEM_ID_STATIC_START_WITH = "static-"
        private const val ITEM_INDEX_HOME = 0
        private const val ITEM_INDEX_FAVORITE = 1
        private const val ITEM_INDEX_NEARBY = 2
        private const val ITEM_INDEX_MAP = 3
        private const val ITEM_INDEX_TRIP_PLANNER = 4
        private const val ITEM_INDEX_NEWS = 5

        private const val ITEM_ID_SELECTED_SCREEN_DEFAULT = ITEM_ID_STATIC_START_WITH + ITEM_INDEX_HOME
    }

    override fun getLogTag(): String = LOG_TAG

    val allAgenciesCount = this.dataSourcesRepository.readingAllAgenciesCount()

    fun onAppVisible() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dataSourcesRepository.updateLock()
            }
        }
    }

    val userLearnedDrawer: LiveData<Boolean> = defaultPrefRepository.pref.liveData(
        PreferenceUtils.PREF_USER_LEARNED_DRAWER, PreferenceUtils.PREF_USER_LEARNED_DRAWER_DEFAULT
    ).distinctUntilChanged()

    fun setUserLearnedDrawer(learned: Boolean) {
        defaultPrefRepository.pref.edit {
            putBoolean(PreferenceUtils.PREF_USER_LEARNED_DRAWER, learned)
        }
    }

    val selectedItemIdPref: LiveData<String> = lclPrefRepository.pref.liveData(
        PreferenceUtils.PREFS_LCL_ROOT_SCREEN_ITEM_ID, ITEM_ID_SELECTED_SCREEN_DEFAULT
    ).distinctUntilChanged()

    val selectedItemIdRes: LiveData<Int> = selectedItemIdPref.map { idPref ->
        idPrefToIdRes(idPref)
    }

    fun onSelectedItemIdChanged(@IdRes idRes: Int, args: Bundle? = null) {
        onSelectedItemIdChanged(idResToIdPref(idRes), args)
    }

    fun onSelectedItemIdChanged(idPref: String, args: Bundle? = null) {
        if (isRootScreen(idPref)) {
            lclPrefRepository.pref.edit {
                putString(PreferenceUtils.PREFS_LCL_ROOT_SCREEN_ITEM_ID, idPref)
            }
        }
    }

    private fun isRootScreen(idPref: String?) = isRootScreen(idPrefToIdRes(idPref))

    private fun isRootScreen(@IdRes idRes: Int?): Boolean {
        return when (idRes) {
            null -> false
            R.id.nav_trip_planner -> false
            R.id.nav_support -> false
            R.id.nav_rate_review -> false
            R.id.nav_send_feedback -> false
            R.id.nav_settings -> false
            else -> true
        }
    }

    private fun idPrefToIdRes(idPref: String?) = when (idPref) {
        ITEM_ID_STATIC_START_WITH + ITEM_INDEX_HOME -> R.id.nav_home
        ITEM_ID_STATIC_START_WITH + ITEM_INDEX_FAVORITE -> R.id.nav_favorites
        ITEM_ID_STATIC_START_WITH + ITEM_INDEX_NEARBY -> R.id.nav_nearby
        ITEM_ID_STATIC_START_WITH + ITEM_INDEX_MAP -> R.id.nav_map
        ITEM_ID_STATIC_START_WITH + ITEM_INDEX_TRIP_PLANNER -> R.id.nav_trip_planner
        ITEM_ID_STATIC_START_WITH + ITEM_INDEX_NEWS -> R.id.nav_news
        ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_LIGHT_RAIL.id -> R.id.nav_light_rail
        ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_SUBWAY.id -> R.id.nav_subway
        ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_RAIL.id -> R.id.nav_rail
        ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_BUS.id -> R.id.nav_bus
        ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_FERRY.id -> R.id.nav_ferry
        ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_BIKE.id -> R.id.nav_bike
        ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_MODULE.id -> R.id.nav_module
        else -> {
            MTLog.w(this, "Unknown item ID preference '$idPref'!")
            R.id.nav_home
        }
    }

    private fun idResToIdPref(@IdRes idRes: Int?) = when (idRes) {
        R.id.nav_home -> ITEM_ID_STATIC_START_WITH + ITEM_INDEX_HOME
        R.id.nav_favorites -> ITEM_ID_STATIC_START_WITH + ITEM_INDEX_FAVORITE
        R.id.nav_nearby -> ITEM_ID_STATIC_START_WITH + ITEM_INDEX_NEARBY
        R.id.nav_map -> ITEM_ID_STATIC_START_WITH + ITEM_INDEX_MAP
        R.id.nav_trip_planner -> ITEM_ID_STATIC_START_WITH + ITEM_INDEX_TRIP_PLANNER
        R.id.nav_news -> ITEM_ID_STATIC_START_WITH + ITEM_INDEX_NEWS
        R.id.nav_light_rail -> ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_LIGHT_RAIL.id
        R.id.nav_subway -> ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_SUBWAY.id
        R.id.nav_rail -> ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_RAIL.id
        R.id.nav_bus -> ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_BUS.id
        R.id.nav_ferry -> ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_FERRY.id
        R.id.nav_bike -> ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_BIKE.id
        R.id.nav_module -> ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_MODULE.id
        else -> {
            MTLog.w(this@MainViewModel, "Unknown item ID resource '$idRes'!")
            ITEM_ID_STATIC_START_WITH + ITEM_INDEX_HOME
        }
    }
}