package org.mtransit.android.provider.experiments

import org.mtransit.android.provider.remoteconfig.RemoteConfigProvider
import javax.inject.Inject

class ExperimentsProvider @Inject constructor(
    private val remoteConfigProvider: RemoteConfigProvider,
) {

    companion object {
        const val EXP_AD_BANNER_LOAD_ON_SCREEN_RESUME = "mt_ad_banner_load_on_screen_resume"
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return remoteConfigProvider.getBoolean(key, defaultValue)
    }
}