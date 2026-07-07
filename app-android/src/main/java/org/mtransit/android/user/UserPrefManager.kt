package org.mtransit.android.user

import android.annotation.SuppressLint
import androidx.annotation.Discouraged
import androidx.annotation.WorkerThread
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mtransit.android.common.repository.DefaultPreferenceRepository
import org.mtransit.android.commons.pref.liveData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPrefManager @Inject constructor(
    private val defaultPrefRepository: DefaultPreferenceRepository,
) {

    // region use internal web browser

    val useInternalWebBrowser: LiveData<Boolean>
        get() = defaultPrefRepository.pref.liveData(
            DefaultPreferenceRepository.PREFS_USE_INTERNAL_WEB_BROWSER, DefaultPreferenceRepository.PREFS_USE_INTERNAL_WEB_BROWSER_DEFAULT
        )

    // endregion

    // region distance units

    val distanceUnits: LiveData<String> = defaultPrefRepository.pref.liveData(
        DefaultPreferenceRepository.PREFS_DISTANCE_UNITS, DefaultPreferenceRepository.PREFS_DISTANCE_UNITS_DEFAULT
    )

    // endregion

    // region show a18y

    val showAccessibility: LiveData<Boolean> = defaultPrefRepository.pref.liveData(
        DefaultPreferenceRepository.PREFS_SHOW_ACCESSIBILITY, DefaultPreferenceRepository.PREFS_SHOW_ACCESSIBILITY_DEFAULT
    )

    @Suppress("unused")
    suspend fun getShowAccessibility() = withContext(Dispatchers.IO) {
        defaultPrefRepository.pref.getBoolean(
            DefaultPreferenceRepository.PREFS_SHOW_ACCESSIBILITY, DefaultPreferenceRepository.PREFS_SHOW_ACCESSIBILITY_DEFAULT
        )
    }

    @WorkerThread
    @Discouraged("use suspend function or live data")
    fun getShowAccessibilityNow() = defaultPrefRepository.pref.getBoolean(
        DefaultPreferenceRepository.PREFS_SHOW_ACCESSIBILITY, DefaultPreferenceRepository.PREFS_SHOW_ACCESSIBILITY_DEFAULT
    )

    // endregion

    // region agency POIs - list/map

    suspend fun getAgencyPOIsShowingListInsteadOfMapLastSet() = withContext(Dispatchers.IO) {
        defaultPrefRepository.pref.getBoolean(
            DefaultPreferenceRepository.PREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP_LAST_SET,
            DefaultPreferenceRepository.PREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP_DEFAULT
        )
    }

    suspend fun getAgencyPOIsShowingListInsteadOfMap(authority: String): LiveData<Boolean> = defaultPrefRepository.pref.liveData(
        DefaultPreferenceRepository.getPREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP(authority),
        getAgencyPOIsShowingListInsteadOfMapLastSet()
    )

    @SuppressLint("DiscouragedApi")
    @WorkerThread
    @Discouraged("use suspend function")
    fun setAgencyPOIsShowingMapNow(authority: String) = setAgencyPOIsShowingListInsteadOfMapNow(authority, listInsteadOfMap = false)

    @WorkerThread
    @Discouraged("use suspend function")
    fun setAgencyPOIsShowingListInsteadOfMapNow(authority: String, listInsteadOfMap: Boolean) = defaultPrefRepository.pref.edit {
        putBoolean(
            DefaultPreferenceRepository.getPREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP(authority),
            listInsteadOfMap,
        )
    }

    suspend fun setAgencyPOIsShowingListInsteadOfMapAndLastSet(authority: String?, listInsteadOfMap: Boolean) = withContext(Dispatchers.IO) {
        defaultPrefRepository.pref.edit {
            putBoolean(DefaultPreferenceRepository.PREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP_LAST_SET, listInsteadOfMap)
            authority?.let { putBoolean(DefaultPreferenceRepository.getPREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP(it), listInsteadOfMap) }
        }
    }

    // endregion

    // region RDS routes - list/grid

    fun getRDSRoutesShowingListInsteadOfGrid(authority: String, routesCount: Int): LiveData<Boolean> = defaultPrefRepository.pref.liveData(
        DefaultPreferenceRepository.getPREFS_RDS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID(authority),
        defaultPrefRepository.getPREFS_RDS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID_DEFAULT(routesCount)
    )

    suspend fun setRDSRoutesShowingListInsteadOfGrid(authority: String, showingListInsteadOfGrid: Boolean) = withContext(Dispatchers.IO) {
        defaultPrefRepository.pref.edit {
            putBoolean(DefaultPreferenceRepository.getPREFS_RDS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID(authority), showingListInsteadOfGrid)
        }
    }

    // endregion

    // region theme

    val theme: LiveData<String> = defaultPrefRepository.pref.liveData(
        DefaultPreferenceRepository.PREFS_THEME, DefaultPreferenceRepository.PREFS_THEME_DEFAULT
    )

    // endregion

    // region lang

    val lang: LiveData<String> = defaultPrefRepository.pref.liveData(
        DefaultPreferenceRepository.PREFS_LANG, DefaultPreferenceRepository.PREFS_LANG_DEFAULT
    )

    suspend fun setLang(lang: String) = withContext(Dispatchers.IO) {
        defaultPrefRepository.pref.edit {
            putString(DefaultPreferenceRepository.PREFS_LANG, lang)
        }
    }

    // endregion
}
