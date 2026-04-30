package org.mtransit.android.common.repository

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import dagger.hilt.android.qualifiers.ApplicationContext
import org.mtransit.android.R
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultPreferenceRepository @Inject constructor(
    @param:ApplicationContext private val appContext: Context
) : PreferenceRepository(appContext) {

    companion object {

        const val PREF_USER_RATING_REQUEST_OPEN_COUNTS = "pRatingRequestAppOpenCounts"
        const val PREF_USER_RATING_REQUEST_OPEN_COUNTS_DEFAULT = 0

        const val PREF_USER_APP_OPEN_COUNTS = "pAppOpenCounts"
        const val PREF_USER_APP_OPEN_COUNTS_DEFAULT = 0

        const val PREF_USER_APP_OPEN_LAST = "pAppOpenLast"
        const val PREF_USER_APP_OPEN_LAST_DEFAULT = 0L

        const val PREF_USER_DAILY = "pDailyUser"
        const val PREF_USER_DAILY_DEFAULT = false

        const val PREF_USER_REWARDED_UNTIL = "pRewardedUtil"
        const val PREF_USER_REWARDED_UNTIL_DEFAULT = 0L

        const val PREF_USER_REWARDED_LOAD_COUNTS = "pRewardedLoads"
        const val PREF_USER_REWARDED_LOAD_COUNTS_DEFAULT = 0

        const val PREF_USER_REWARDED_SHOW_COUNTS = "pRewardedShows"
        const val PREF_USER_REWARDED_SHOW_COUNTS_DEFAULT = 0

        const val PREF_USER_LEARNED_DRAWER = "pUserLearnedDrawer"
        const val PREF_USER_LEARNED_DRAWER_DEFAULT = false

        const val PREFS_THEME = "pTheme"
        const val PREFS_THEME_LIGHT = "light"
        const val PREFS_THEME_DARK = "dark"
        const val PREFS_THEME_SYSTEM_DEFAULT = "system_default"
        const val PREFS_THEME_DEFAULT = PREFS_THEME_SYSTEM_DEFAULT

        const val PREFS_UNITS = "pUnits"
        const val PREFS_UNITS_METRIC = "metric"
        const val PREFS_UNITS_IMPERIAL = "imperial"
        const val PREFS_UNITS_DEFAULT = PREFS_UNITS_METRIC // TODO smarter default?

        const val PREFS_LANG = "pLang"
        const val PREFS_LANG_EN = "en"
        const val PREFS_LANG_FR = "fr"
        const val PREFS_LANG_SYSTEM_DEFAULT = "system_default"
        const val PREFS_LANG_DEFAULT = PREFS_LANG_SYSTEM_DEFAULT

        const val PREFS_SHOW_ACCESSIBILITY = "pShowA11y"
        const val PREFS_SHOW_ACCESSIBILITY_DEFAULT = false

        const val PREFS_USE_INTERNAL_WEB_BROWSER = "pUseInternalWebBrowser"
        const val PREFS_USE_INTERNAL_WEB_BROWSER_DEFAULT = true

        private const val PREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP = "pAgencyPoisShowingListInsteadOfMap"
        const val PREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP_LAST_SET = PREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP + "LastSet"
        const val PREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP_DEFAULT = false


        @JvmStatic
        @Suppress("FunctionName")
        fun getPREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP(authority: String) =
            PREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP + authority

        private const val PREFS_RDS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID = "pRTSRouteShowingListInsteadOfGrid" // do not change to avoid breaking existing prefs

        @Suppress("FunctionName")
        fun getPREFS_RDS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID(authority: String) = PREFS_RDS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID + authority

        @Suppress("DEPRECATION")
        @JvmStatic
        fun makePref(context: Context): SharedPreferences = android.preference.PreferenceManager.getDefaultSharedPreferences(context)
    }

    @Suppress("FunctionName")
    fun getPREFS_RDS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID_DEFAULT(routesCount: Int): Boolean {
        return routesCount < requireContext().resources.getInteger(R.integer.rds_routes_default_grid_min_count)
    }

    private var _prefs: SharedPreferences? = null

    private val _executorService = Executors.newSingleThreadExecutor()

    init {
        _executorService.execute { _prefs = loadPrefs() }
    }

    @WorkerThread // @WorkerThread
    private fun loadPrefs() = makePref(requireContext())

    @get:AnyThread
    val pref: SharedPreferences
        @SuppressLint("ThreadConstraint") // should almost never call loadPrefs()
        get() = _prefs ?: loadPrefs().also { _prefs = it }
}
