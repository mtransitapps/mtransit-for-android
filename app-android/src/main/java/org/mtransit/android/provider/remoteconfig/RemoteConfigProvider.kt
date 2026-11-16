package org.mtransit.android.provider.remoteconfig

import com.google.firebase.Firebase
import com.google.firebase.installations.FirebaseInstallationsException
import com.google.firebase.installations.InstallationTokenResult
import com.google.firebase.installations.installations
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import org.mtransit.android.BuildConfig
import org.mtransit.android.commons.MTLog
import org.mtransit.commons.suspendRunCatching
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigProvider @Inject constructor() : MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = RemoteConfigProvider::class.java.simpleName
    }

    override fun getLogTag() = LOG_TAG

    private val remoteConfig by lazy { Firebase.remoteConfig }

    private val installations by lazy { Firebase.installations }

    @Volatile
    private var snapshot: RemoteConfigData? = null
    private val snapshotDeferred = CompletableDeferred<RemoteConfigData>()
    private val initMutex = Mutex()

    suspend fun init() = initMutex.withLock {
        if (snapshotDeferred.isCompleted) return@withLock

        // setup remote config settings
        remoteConfig.setConfigSettingsAsync(
            remoteConfigSettings {
                if (BuildConfig.DEBUG) {
                    fetchTimeoutInSeconds *= 2L
                    minimumFetchIntervalInSeconds /= 2L
                }
            }
        )

        // activate cached Remote Config values
        suspendRunCatching { remoteConfig.activate().await() }
            .onFailure { e ->
                MTLog.w(this, e, "Remote config activation failed!")
            }

        // use activated Remote Config values
        with(remoteConfig) {
            @Suppress("DEPRECATION")
            RemoteConfigData(
                getValueNonStatic(RemoteConfigData.AD_BANNER_LOAD_ON_SCREEN_RESUME_MIN_DURATION_SEC)?.asLongOrNull(),
                getValueNonStatic(RemoteConfigData.AD_BANNER_LARGE)?.asBooleanOrNull(),
                getValueNonStatic(RemoteConfigData.AD_APP_OPEN_ENABLED)?.asBooleanOrNull(),
                getValueNonStatic(RemoteConfigData.AD_REWARDED_AMOUNT_IN_DAYS)?.asLongOrNull(),
                getValueNonStatic(RemoteConfigData.ALLOW_TWITTER_NEWS_FOR_FREE)?.asBooleanOrNull(),
                getValueNonStatic(RemoteConfigData.ALLOW_TWITTER_NEWS_CACHED)?.asBooleanOrNull(),
                getValueNonStatic(RemoteConfigData.VEHICLE_LOCATION_DATA_REFRESH_MIN_MS)?.asLongOrNull(),
                getValueNonStatic(RemoteConfigData.NEW_USER_ADS_FREE)?.asBooleanOrNull(),
                getValueNonStatic(RemoteConfigData.MAPS_DIRECTION_EMBED_API_ENABLED)?.asBooleanOrNull(),
            ).let {
                snapshot = it
                snapshotDeferred.complete(it)
            }
        }

        // fetch latest remote config value for next time
        remoteConfig.fetch()
            .addOnFailureListener { e ->
                MTLog.w(this, e, "Remote config fetch failed!")
            }
    }

    private fun FirebaseRemoteConfig.getValueNonStatic(key: String) =
        getValue(key).takeIf { it.source != FirebaseRemoteConfig.VALUE_SOURCE_STATIC }

    private fun FirebaseRemoteConfigValue.asLongOrNull() = try {
        asLong()
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun FirebaseRemoteConfigValue.asBooleanOrNull() = try {
        asBoolean()
    } catch (_: IllegalArgumentException) {
        null
    }

    fun getNow() = snapshot

    private val defaultConfig by lazy { RemoteConfigData() }

    fun getNowOrDefault() = snapshot ?: defaultConfig

    suspend fun get() = snapshotDeferred.await()

    private fun getAllReceived() = remoteConfig.all
        .filterValues { it.source != FirebaseRemoteConfig.VALUE_SOURCE_STATIC }
        .mapValues { (_, value) -> value.asString() }

    fun getAll(): Map<String, Any?>? = getNow()?.toMap(getAllReceived())

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
