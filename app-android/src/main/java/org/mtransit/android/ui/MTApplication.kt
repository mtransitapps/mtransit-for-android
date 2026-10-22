package org.mtransit.android.ui

import android.content.Context
import android.os.Build
import android.webkit.WebView
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.mtransit.android.BuildConfig
import org.mtransit.android.analytics.AnalyticsUserProperties
import org.mtransit.android.analytics.IAnalyticsManager
import org.mtransit.android.commons.LocaleUtils
import org.mtransit.android.commons.fixWebViewLocale
import org.mtransit.android.commons.ui.MTCommonApp
import org.mtransit.android.dev.CrashReporter
import org.mtransit.android.dev.IStrictMode
import org.mtransit.android.dev.LeakDetector
import org.mtransit.android.provider.remoteconfig.RemoteConfigProvider
import org.mtransit.android.ui.view.MapsInitializerUtil
import org.mtransit.android.util.NightModeUtils
import org.mtransit.android.util.UIFeatureFlags
import org.mtransit.commons.CommonsApp
import javax.inject.Inject

@HiltAndroidApp
class MTApplication : MTCommonApp() {

    companion object {
        private val LOG_TAG: String = MTApplication::class.java.simpleName
    }

    override fun getLogTag() = LOG_TAG

    init {
        CommonsApp.setup(isAndroidPlatform = true)
    }

    @Inject
    lateinit var leakDetector: LeakDetector

    @Inject
    lateinit var crashReporter: CrashReporter

    @Inject
    lateinit var strictMode: IStrictMode

    @Inject
    lateinit var analyticsManager: IAnalyticsManager

    @Inject
    lateinit var remoteConfigProvider: RemoteConfigProvider

    private val mainScope = MainScope()

    override fun attachBaseContext(base: Context) {
        // Demo mode: cannot access manager instance before application context is attached => set in activities
        val fixedBase = LocaleUtils.attachBaseContextApplication(base)
        super.attachBaseContext(fixedBase)
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            this.leakDetector.setup(this)
        }
        NightModeUtils.setDefaultNightMode(this, null)
        fixWebViewDataDirectorySuffix() // before any webview usage
        if (!UIFeatureFlags.F_LOCALE_WEB_VIEW_FIX_IN_ACTIVITY) fixWebViewLocale(this) // before ads SDK
        this.remoteConfigProvider.init()
        this.crashReporter.setup(!BuildConfig.DEBUG)
        this.strictMode.setup() // uses crash reporter
        this.analyticsManager.setUserProperty(AnalyticsUserProperties.DEVICE_MANUFACTURER, Build.MANUFACTURER)
        this.mainScope.launch {
            MapsInitializerUtil.initMap(this@MTApplication)
        }
    }

    private fun fixWebViewDataDirectorySuffix() {
        // RuntimeException: Using WebView from more than one process at once with the same data directory is not supported. https://crbug.com/558377 :
        // Current process org.mtransit.android (pid 1111), lock owner org.mtransit.android (pid 2222) is a zombie
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val appProcessName: String = getProcessName()
            val packageName = getPackageName()

            // If it is not the main process, assign a suffix
            if (this.packageName != appProcessName) {
                // Safe suffix name using the process identifier
                val suffix = appProcessName.replace("$packageName:", "")
                WebView.setDataDirectorySuffix(suffix)
            }
        }
    }
}
