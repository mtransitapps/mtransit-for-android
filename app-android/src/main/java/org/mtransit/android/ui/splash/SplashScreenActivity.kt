@file:JvmName("SplashScreenActivity") // ANALYTICS
package org.mtransit.android.ui.splash

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.analytics.IAnalyticsManager
import org.mtransit.android.commons.LocaleUtils
import org.mtransit.android.ui.MTActivity
import org.mtransit.android.ui.main.NextMainActivity
import org.mtransit.android.ui.MainActivity
import org.mtransit.android.ui.view.common.IActivity
import org.mtransit.android.util.UIFeatureFlags
import org.mtransit.commons.FeatureFlags
import javax.inject.Inject

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
open class SplashScreenActivity : MTActivity(), IActivity, IAnalyticsManager.Trackable {

    companion object {
        private val LOG_TAG = SplashScreenActivity::class.java.simpleName
        private const val TRACKING_SCREEN_NAME = "Splash"
    }

    override fun getLogTag(): String = LOG_TAG

    override fun getScreenName() = TRACKING_SCREEN_NAME

    @Inject
    lateinit var analyticsManager: IAnalyticsManager

    private val viewModel by viewModels<SplashScreenViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        analyticsManager.trackScreenView(this)
        viewModel.onAppOpen()
        if (UIFeatureFlags.F_LOCALE_WEB_VIEW_FIX_IN_ACTIVITY) LocaleUtils.fixWebViewLocale(this.applicationContext)
        splashScreen.setKeepOnScreenCondition { true } // Keep the splash screen visible for this Activity
        showMainActivity()
    }

    private fun showMainActivity() {
        // @Suppress("ConstantConditionIf")
        // if (true) {
        // // ADD tools:replace="android:supportsRtl" to AndroidManifest.xml <application />
        // // USE real Google Ads app ID & unit IDs in keys.xml, not Google DEBUG one provided by Google
        // com.google.android.gms.ads.MobileAds.initialize(this) // required?
        // com.google.android.gms.ads.MobileAds.openAdInspector(
        // this,
        // com.google.android.gms.ads.OnAdInspectorClosedListener { error: com.google.android.gms.ads.AdInspectorError? ->
        // if (error == null) {
        // org.mtransit.android.commons.MTLog.d(this, "Ad inspector closed.")
        // } else {
        // org.mtransit.android.commons.MTLog.w(this, "Ad inspector closed: ${error.code} > $error!")
        // }
        // })
        // finish()
        // return
        // }
        // if (true) {
        // startActivity(org.mtransit.android.ui.modules.ModulesActivity.newInstance(this))
        // finish()
        // return
        // }
        if (FeatureFlags.F_NAVIGATION) {
            startActivity(NextMainActivity.newInstance(this))
            finish()
            return
        }
        startActivity(MainActivity.newInstance(this))
        finish()
    }

    override val currentFragment: Fragment? = null
}