package org.mtransit.android.ui.main

import androidx.annotation.IdRes
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
import org.mtransit.android.dev.DemoModeManager
import org.mtransit.android.ui.view.common.Event
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val dataSourcesRepository: DataSourcesRepository,
    private val lclPrefRepository: LocalPreferenceRepository,
    private val defaultPrefRepository: DefaultPreferenceRepository,
    private val demoModeManager: DemoModeManager,
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

    val scrollToTopEvent = MutableLiveData<Event<Boolean>>()

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

    private val _selectedItemIdPref: LiveData<String> = lclPrefRepository.pref.liveData(
        PreferenceUtils.PREFS_LCL_ROOT_SCREEN_ITEM_ID, ITEM_ID_SELECTED_SCREEN_DEFAULT
    ).distinctUntilChanged()
    val selectedItemIdRes: LiveData<Int> = _selectedItemIdPref.map { idPref ->
        idPrefToIdRes(
            if (demoModeManager.enabled) {
                ITEM_ID_SELECTED_SCREEN_DEFAULT
            } else {
                idPref
            }
        ) ?: R.id.root_nav_home
    }

    fun onSelectedItemIdChanged(@IdRes idRes: Int) {
        onSelectedItemIdChanged(
            idResToIdPref(if (isRootScreen(idRes)) idRes else null) // only root screens
        )
    }

    fun onSelectedItemIdChanged(idPref: String?) {
        if (demoModeManager.enabled) {
            return // SKIP (demo mode ON)
        }
        if (isRootScreen(idPref)) {
            lclPrefRepository.pref.edit {
                putString(PreferenceUtils.PREFS_LCL_ROOT_SCREEN_ITEM_ID, idPref)
            }
        }
    }

    private fun isRootScreen(idPref: String?) = isRootScreen(idPrefToIdRes(idPref))

    private fun isRootScreen(@IdRes idRes: Int?): Boolean {
        return getRootScreenResId().contains(idRes)
    }

    private fun idPrefToIdRes(idPref: String?) = when (idPref) {
        ITEM_ID_STATIC_START_WITH + ITEM_INDEX_HOME -> R.id.root_nav_home
        ITEM_ID_STATIC_START_WITH + ITEM_INDEX_FAVORITE -> R.id.root_nav_favorites
        ITEM_ID_STATIC_START_WITH + ITEM_INDEX_NEARBY -> R.id.root_nav_nearby
        ITEM_ID_STATIC_START_WITH + ITEM_INDEX_MAP -> R.id.root_nav_map
        ITEM_ID_STATIC_START_WITH + ITEM_INDEX_TRIP_PLANNER -> R.id.nav_trip_planner
        ITEM_ID_STATIC_START_WITH + ITEM_INDEX_NEWS -> R.id.root_nav_news
        ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_LIGHT_RAIL.id -> R.id.root_nav_light_rail
        ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_SUBWAY.id -> R.id.root_nav_subway
        ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_RAIL.id -> R.id.root_nav_rail
        ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_BUS.id -> R.id.root_nav_bus
        ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_FERRY.id -> R.id.root_nav_ferry
        ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_BIKE.id -> R.id.root_nav_bike
        ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_MODULE.id -> R.id.root_nav_module
        null -> null
        else -> {
            MTLog.w(this, "Unknown item ID preference '$idPref'!")
            null
        }
    }

    private fun idResToIdPref(@IdRes idRes: Int?) = when (idRes) {
        R.id.root_nav_home -> ITEM_ID_STATIC_START_WITH + ITEM_INDEX_HOME
        R.id.root_nav_favorites -> ITEM_ID_STATIC_START_WITH + ITEM_INDEX_FAVORITE
        R.id.root_nav_nearby -> ITEM_ID_STATIC_START_WITH + ITEM_INDEX_NEARBY
        R.id.root_nav_map -> ITEM_ID_STATIC_START_WITH + ITEM_INDEX_MAP
        R.id.nav_trip_planner -> ITEM_ID_STATIC_START_WITH + ITEM_INDEX_TRIP_PLANNER
        R.id.root_nav_news -> ITEM_ID_STATIC_START_WITH + ITEM_INDEX_NEWS
        R.id.root_nav_light_rail -> ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_LIGHT_RAIL.id
        R.id.root_nav_subway -> ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_SUBWAY.id
        R.id.root_nav_rail -> ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_RAIL.id
        R.id.root_nav_bus -> ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_BUS.id
        R.id.root_nav_ferry -> ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_FERRY.id
        R.id.root_nav_bike -> ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_BIKE.id
        R.id.root_nav_module -> ITEM_ID_AGENCY_TYPE_START_WITH + DataSourceType.TYPE_MODULE.id
        null -> null
        else -> {
            MTLog.w(this@MainViewModel, "Unknown item ID resource '$idRes'!")
            null
        }
    }

    fun getRootScreenResId() = setOf(
        R.id.root_nav_home,
        R.id.root_nav_favorites,
        R.id.root_nav_nearby,
        R.id.root_nav_map,
        R.id.nav_trip_planner, // TODO ?
        R.id.root_nav_news,
        R.id.root_nav_bike,
        R.id.root_nav_bus,
        R.id.root_nav_ferry,
        R.id.root_nav_light_rail,
        R.id.root_nav_subway,
        R.id.root_nav_rail,
        R.id.root_nav_module
    )

    fun onItemReselected() {
        scrollToTopEvent.postValue(Event(true))
    }
}