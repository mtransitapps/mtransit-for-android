package org.mtransit.android.provider.remoteconfig

import com.google.firebase.Firebase
import com.google.firebase.installations.FirebaseInstallationsException
import com.google.firebase.installations.InstallationTokenResult
import com.google.firebase.installations.installations
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import org.mtransit.android.BuildConfig
import org.mtransit.android.commons.MTLog
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Singleton
class RemoteConfigProvider @Inject constructor() : MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = RemoteConfigProvider::class.java.simpleName
    }

    override fun getLogTag() = LOG_TAG

    private val remoteConfig by lazy { Firebase.remoteConfig }

    private val installations by lazy { Firebase.installations }

    private val activated = AtomicBoolean(false)

    private val snapshotDeferred = CompletableDeferred<RemoteConfigData>()

    suspend fun init(timeout: Duration = 3.seconds) {
        if (snapshotDeferred.isCompleted) return
        remoteConfig.setConfigSettingsAsync(
            remoteConfigSettings {
                if (BuildConfig.DEBUG) {
                    fetchTimeoutInSeconds *= 2L
                    minimumFetchIntervalInSeconds /= 2L
                }
            }
        )

        withTimeoutOrNull(timeout) {
            try {
                remoteConfig.fetchAndActivate().await()
            } catch (e: Exception) {
                MTLog.w(this, e, "Remote config fetch failed!")
            }
        }

        with(remoteConfig) {
            @Suppress("DEPRECATION")
            RemoteConfigData(
                adBannerLoadOnScreenResumeMinDurationSec =
                    getValueNonStatic(RemoteConfigData.AD_BANNER_LOAD_ON_SCREEN_RESUME_MIN_DURATION_SEC)?.asLong(),
                adBannerLarge =
                    getValueNonStatic(RemoteConfigData.AD_BANNER_LARGE)?.asBoolean(),
                adAppOpenEnabled =
                    getValueNonStatic(RemoteConfigData.AD_APP_OPEN_ENABLED)?.asBoolean(),
                adRewardedAmountInDays =
                    getValueNonStatic(RemoteConfigData.AD_REWARDED_AMOUNT_IN_DAYS)?.asLong(),
                allowTwitterNewsForFree =
                    getValueNonStatic(RemoteConfigData.ALLOW_TWITTER_NEWS_FOR_FREE)?.asBoolean(),
                allowTwitterNewsCached =
                    getValueNonStatic(RemoteConfigData.ALLOW_TWITTER_NEWS_CACHED)?.asBoolean(),
                vehicleLocationDataRefreshMinMs =
                    getValueNonStatic(RemoteConfigData.VEHICLE_LOCATION_DATA_REFRESH_MIN_MS)?.asLong(),
                newUserAdsFree =
                    getValueNonStatic(RemoteConfigData.NEW_USER_ADS_FREE)?.asBoolean(),
                mapsDirectionsEmbedApiEnabled =
                    getValueNonStatic(RemoteConfigData.MAPS_DIRECTION_EMBED_API_ENABLED)?.asBoolean(),
            ).let {
                snapshotDeferred.complete(it)
            }
        }
    }

    private fun FirebaseRemoteConfig.getValueNonStatic(key: String) =
        getValue(key).takeIf { it.source != FirebaseRemoteConfig.VALUE_SOURCE_STATIC }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getNow() =
        snapshotDeferred.takeIf { it.isCompleted }?.getCompleted()

    private val defaultConfig by lazy { RemoteConfigData() }

    fun getNowOrDefault() = getNow() ?: defaultConfig

    suspend fun get() = snapshotDeferred.await()

    private val activatedRemoteConfig: FirebaseRemoteConfig?
        get() = remoteConfig.takeIf { activated.get() }

    fun getAll(): Map<String, String>? =
        activatedRemoteConfig
            ?.all?.mapValues { it.value.asString() }

    suspend fun getInstallationToken(forceRefresh: Boolean): InstallationTokenResult? = try {
        installations.getToken(forceRefresh).await()
    } catch (e: FirebaseInstallationsException) {
        MTLog.w(this, e, "Firebase installation error while getting installation token!")
        null
    } catch (e: IOException) {
        MTLog.w(this, e, "I/O Error while getting installation token!")
        null
    }
}
