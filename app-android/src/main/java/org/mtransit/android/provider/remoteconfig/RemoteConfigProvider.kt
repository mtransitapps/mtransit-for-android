package org.mtransit.android.provider.remoteconfig

import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import org.mtransit.android.BuildConfig
import org.mtransit.android.commons.MTLog
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigProvider @Inject constructor(
) : MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = RemoteConfigProvider::class.java.simpleName

        // const val AD_BANNER_LOAD_ON_SCREEN_RESUME = "mt_ad_banner_load_on_screen_resume" // -> true is outperforming Baseline by 95% after 2 weeks
        // const val AD_BANNER_LOAD_ON_SCREEN_RESUME_DEFAULT = false

        // const val AD_BANNER_LOAD_ON_SCREEN_RESUME_MIN_DURATION_SEC = "mt_ad_banner_load_on_screen_resume_min_duration_sec"
        // const val AD_BANNER_LOAD_ON_SCREEN_RESUME_MIN_DURATION_SEC_DEFAULT = 1L // -> 1 second is outperforming Baseline (-1=disabled) by 114% (& 2,3,5,7,10)

        const val AD_BANNER_LARGE = "mt_ad_banner_large"
        const val AD_BANNER_LARGE_DEFAULT = false

        @Deprecated("use ALLOW_TWITTER_NEWS_CACHED_DEFAULT instead")
        @Suppress("SimplifyBooleanWithConstants", "MayBeConstant", "RedundantSuppression")
        val ALLOW_TWITTER_NEWS_FOR_FREE_DEFAULT = false
        // || (org.mtransit.android.commons.Constants.DEBUG && org.mtransit.android.BuildConfig.DEBUG) // DEBUG
        @Deprecated("use ALLOW_TWITTER_NEWS_CACHED instead")
        const val ALLOW_TWITTER_NEWS_FOR_FREE = "mt_twitter_news_free"

        const val ALLOW_TWITTER_NEWS_CACHED = "mt_twitter_news_cached"
        const val ALLOW_TWITTER_NEWS_CACHED_DEFAULT = false

        const val VEHICLE_LOCATION_DATA_REFRESH_MIN_MS = "mt_vehicle_location_refresh_min_ms"
        const val VEHICLE_LOCATION_DATA_REFRESH_MIN_MS_DEFAULT = 30_000L // 30 seconds
    }

    override fun getLogTag() = LOG_TAG

    private val remoteConfig by lazy { Firebase.remoteConfig }

    private val activated = AtomicBoolean(false)

    fun init() {
        remoteConfig.setConfigSettingsAsync(remoteConfigSettings {
            if (BuildConfig.DEBUG) {
                fetchTimeoutInSeconds *= 2L
                minimumFetchIntervalInSeconds /= 2L
            }
        })

        remoteConfig
            .fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    this.activated.set(true)
                    MTLog.d(this, "Config params updated: ${task.result}")
                } else {
                    this.activated.set(false)
                    MTLog.w(this, task.exception, "Fetch failed!")
                }
            }
    }

    fun get(key: String, defaultValue: String) =
        getActivatedValueNonStatic(key)?.asString() ?: defaultValue

    fun get(key: String, defaultValue: Boolean) =
        getActivatedValueNonStatic(key)?.asBoolean() ?: defaultValue

    fun get(key: String, defaultValue: Double) =
        getActivatedValueNonStatic(key)?.asDouble() ?: defaultValue

    fun get(key: String, defaultValue: Long) =
        getActivatedValueNonStatic(key)?.asLong() ?: defaultValue

    private fun getActivatedValueNonStatic(key: String) =
        remoteConfig.takeIf { activated.get() }
            ?.getValue(key)
            ?.takeIf { it.source != FirebaseRemoteConfig.VALUE_SOURCE_STATIC }

    fun getAll(): Map<String, String>? =
        remoteConfig.takeIf { activated.get() }?.all?.mapValues { it.value.asString() }
}
