package org.mtransit.android.common.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import org.mtransit.android.R
import org.mtransit.android.commons.PreferenceUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultPreferenceRepository @Inject constructor(
    @ApplicationContext private val appContext: Context
) : PreferenceRepository(appContext) {

    companion object {

        const val PREF_USER_APP_OPEN_COUNTS = PreferenceUtils.PREF_USER_APP_OPEN_COUNTS
        const val PREF_USER_APP_OPEN_COUNTS_DEFAULT = PreferenceUtils.PREF_USER_APP_OPEN_COUNTS_DEFAULT

        const val PREF_USER_APP_OPEN_LAST = PreferenceUtils.PREF_USER_APP_OPEN_LAST
        const val PREF_USER_APP_OPEN_LAST_DEFAULT = PreferenceUtils.PREF_USER_APP_OPEN_LAST_DEFAULT

        const val PREF_USER_DAILY = PreferenceUtils.PREF_USER_DAILY
        const val PREF_USER_DAILY_DEFAULT = PreferenceUtils.PREF_USER_DAILY_DEFAULT

        const val PREF_USER_REWARDED_UNTIL = PreferenceUtils.PREF_USER_REWARDED_UNTIL
        const val PREF_USER_REWARDED_UNTIL_DEFAULT = PreferenceUtils.PREF_USER_REWARDED_UNTIL_DEFAULT

        const val PREF_USER_LEARNED_DRAWER = PreferenceUtils.PREF_USER_LEARNED_DRAWER
        const val PREF_USER_LEARNED_DRAWER_DEFAULT = PreferenceUtils.PREF_USER_LEARNED_DRAWER_DEFAULT

        const val PREFS_THEME = PreferenceUtils.PREFS_THEME
        const val PREFS_THEME_LIGHT = PreferenceUtils.PREFS_THEME_LIGHT
        const val PREFS_THEME_DARK = PreferenceUtils.PREFS_THEME_DARK
        const val PREFS_THEME_SYSTEM_DEFAULT = PreferenceUtils.PREFS_THEME_SYSTEM_DEFAULT
        const val PREFS_THEME_DEFAULT = PreferenceUtils.PREFS_THEME_DEFAULT

        const val PREFS_UNITS = PreferenceUtils.PREFS_UNITS
        const val PREFS_UNITS_METRIC = PreferenceUtils.PREFS_UNITS_METRIC
        const val PREFS_UNITS_IMPERIAL = PreferenceUtils.PREFS_UNITS_IMPERIAL
        const val PREFS_UNITS_DEFAULT = PreferenceUtils.PREFS_UNITS_DEFAULT

        const val PREFS_USE_INTERNAL_WEB_BROWSER = PreferenceUtils.PREFS_USE_INTERNAL_WEB_BROWSER
        const val PREFS_USE_INTERNAL_WEB_BROWSER_DEFAULT = PreferenceUtils.PREFS_USE_INTERNAL_WEB_BROWSER_DEFAULT

        const val PREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP_LAST_SET = PreferenceUtils.PREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP_LAST_SET
        const val PREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP_DEFAULT = PreferenceUtils.PREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP_DEFAULT

        @Suppress("FunctionName")
        fun getPREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP(authority: String) = PreferenceUtils.getPREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP(authority)

        private const val PREFS_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID = "pRTSRouteShowingListInsteadOfGrid";

        @Suppress("FunctionName")
        fun getPREFS_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID(authority: String) = PREFS_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID + authority
    }

    @Suppress("FunctionName")
    fun getPREFS_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID_DEFAULT(routesCount: Int): Boolean {
        return routesCount < appContext.resources.getInteger(R.integer.rts_routes_default_grid_min_count)
    }

    override fun hasKey(key: String): Boolean {
        return PreferenceUtils.hasPrefDefault(requireContext(), key)
    }

    override fun getValue(key: String, defaultValue: Boolean): Boolean {
        return PreferenceUtils.getPrefDefault(requireContext(), key, defaultValue)
    }

    override fun saveAsync(key: String, value: Boolean?) {
        PreferenceUtils.savePrefDefault(requireContext(), key, value, false)
    }

    override fun getValue(key: String, defaultValue: String?): String? {
        return PreferenceUtils.getPrefDefault(requireContext(), key, defaultValue)
    }

    override fun getValueNN(key: String, defaultValue: String): String {
        return PreferenceUtils.getPrefDefaultNN(requireContext(), key, defaultValue)
    }

    override fun saveAsync(key: String, value: String?) {
        PreferenceUtils.savePrefDefault(requireContext(), key, value, false)
    }

    override fun getValue(key: String, defaultValue: Int): Int {
        return PreferenceUtils.getPrefDefault(requireContext(), key, defaultValue)
    }

    override fun saveAsync(key: String, value: Int) {
        PreferenceUtils.savePrefDefault(requireContext(), key, value, false)
    }

    override fun getValue(key: String, defaultValue: Long): Long {
        return PreferenceUtils.getPrefDefault(requireContext(), key, defaultValue)
    }

    override fun saveAsync(key: String, value: Long) {
        PreferenceUtils.savePrefDefault(requireContext(), key, value, false)
    }

    val pref: SharedPreferences
        get() = PreferenceUtils.getPrefDefault(requireContext())
}