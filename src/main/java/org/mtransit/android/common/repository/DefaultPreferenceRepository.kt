package org.mtransit.android.common.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import org.mtransit.android.commons.PreferenceUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultPreferenceRepository @Inject constructor(
    @ApplicationContext appContext: Context
) : PreferenceRepository(appContext) {

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

    val pref: SharedPreferences
        get() = PreferenceUtils.getPrefDefault(requireContext())
}