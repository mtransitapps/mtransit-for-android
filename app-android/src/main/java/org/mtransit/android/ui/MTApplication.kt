package org.mtransit.android.ui

import android.content.Context
import android.os.Build
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.mtransit.android.BuildConfig
import org.mtransit.android.analytics.AnalyticsUserProperties
import org.mtransit.android.analytics.IAnalyticsManager
import org.mtransit.android.commons.LocaleUtils
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
        CommonsApp.setup(true)
        if (BuildConfig.DEBUG) {
            this.leakDetector.setup(this)
        }
        NightModeUtils.setDefaultNightMode(this, null)
        if (!UIFeatureFlags.F_LOCALE_WEB_VIEW_FIX_IN_ACTIVITY) LocaleUtils.fixWebViewLocale(this)
        this.remoteConfigProvider.init()
        this.crashReporter.setup(!BuildConfig.DEBUG)
        this.strictMode.setup() // uses crash reporter
        this.analyticsManager.setUserProperty(AnalyticsUserProperties.DEVICE_MANUFACTURER, Build.MANUFACTURER)
        mainScope.launch(Dispatchers.Default) {
            MapsInitializerUtil.initMap(this@MTApplication)
        }
    }
}
