package org.mtransit.android.provider.experiments

import org.mtransit.android.provider.remoteconfig.RemoteConfigProvider
import javax.inject.Inject

class ExperimentsProvider @Inject constructor(
    private val remoteConfigProvider: RemoteConfigProvider,
) {

    companion object {
        // const val EXP_AD_BANNER_LOAD_ON_SCREEN_RESUME = "mt_ad_banner_load_on_screen_resume" // -> true is outperforming Baseline by 95% after 2 weeks
        // const val EXP_AD_BANNER_LOAD_ON_SCREEN_RESUME_DEFAULT = false

        // const val EXP_AD_BANNER_LOAD_ON_SCREEN_RESUME_MIN_DURATION_SEC = "mt_ad_banner_load_on_screen_resume_min_duration_sec"
        // const val EXP_AD_BANNER_LOAD_ON_SCREEN_RESUME_MIN_DURATION_SEC_DEFAULT = 1L // -> 1 second is outperforming Baseline (-1=disabled) by 114% (& 2,3,5,7,10)

        const val EXP_ALLOW_TWITTER_NEWS_FOR_FREE = "mt_twitter_news_free"
        @Suppress("SimplifyBooleanWithConstants")
        val EXP_ALLOW_TWITTER_NEWS_FOR_FREE_DEFAULT = false
                || (org.mtransit.android.commons.Constants.DEBUG && org.mtransit.android.BuildConfig.DEBUG) // DEBUG
    }

    @Suppress("unused")
    fun get(key: String, defaultValue: Boolean) =
        remoteConfigProvider.get(key, defaultValue)

    @Suppress("unused")
    fun get(key: String, defaultValue: Long) =
        remoteConfigProvider.get(key, defaultValue)
}