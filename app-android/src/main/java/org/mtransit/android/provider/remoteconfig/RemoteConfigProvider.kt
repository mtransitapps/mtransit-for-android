package org.mtransit.android.provider.remoteconfig

import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.google.firebase.remoteconfig.remoteConfig
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
                    MTLog.d(this, "Fetch failed!")
                }
            }
    }

    fun get(key: String, defaultValue: Boolean) =
        remoteConfig.takeIf { activated.get() }?.getBoolean(key) ?: defaultValue

    fun get(key: String, defaultValue: Long) =
        remoteConfig.takeIf { activated.get() }?.getLong(key) ?: defaultValue
}