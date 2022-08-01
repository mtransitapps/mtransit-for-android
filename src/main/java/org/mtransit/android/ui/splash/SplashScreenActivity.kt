@file:JvmName("SplashScreenActivity") // ANALYTICS
package org.mtransit.android.ui.splash

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.analytics.IAnalyticsManager
import org.mtransit.android.common.repository.DefaultPreferenceRepository
import org.mtransit.android.ui.MTActivity
import org.mtransit.android.ui.MainActivity
import org.mtransit.android.ui.view.common.IActivity
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

    @Inject
    lateinit var defaultPrefRepository: DefaultPreferenceRepository

    private val viewModel by viewModels<SplashScreenViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        analyticsManager.trackScreenView(this, this)
        viewModel.onAppOpen()
        splashScreen.setKeepOnScreenCondition { true } // Keep the splash screen visible for this Activity
        showMainActivity()
    }

    private fun showMainActivity() {
        // @Suppress("ConstantConditionIf")
        // if (true) {
        // // ADD tools:replace="android:supportsRtl" to AndroidManifest.xml <application />
        // // USE real Google Ads app ID & unit IDs in keys.xml, not Google DEBUG one provided by Google
        // com.google.android.gms.ads.MobileAds.initialize(this) // required?
        // com.google.android.ads.mediationtestsuite.MediationTestSuite.launch(this)
        // finish()
        // return
        // }
        // if (true) {
        // startActivity(org.mtransit.android.ui.modules.ModulesActivity.newInstance(this))
        // finish()
        // return
        // }
        if (FeatureFlags.F_NAVIGATION) {
            startActivity(org.mtransit.android.ui.main.MainActivity.newInstance(this))
            finish()
            return
        }
        startActivity(MainActivity.newInstance(this))
        finish()
    }

}