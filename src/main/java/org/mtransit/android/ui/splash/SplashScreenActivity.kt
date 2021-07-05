@file:JvmName("SplashScreenActivity") // ANALYTICS
package org.mtransit.android.ui.splash

import android.os.Bundle
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.analytics.IAnalyticsManager
import org.mtransit.android.common.repository.DefaultPreferenceRepository
import org.mtransit.android.ui.MTActivity
import org.mtransit.android.ui.MainActivity
import org.mtransit.android.ui.view.common.IActivity
import org.mtransit.commons.FeatureFlags
import javax.inject.Inject

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
        setTheme(R.style.MTTheme)
        super.onCreate(savedInstanceState)
        viewModel.onAppOpen()
        analyticsManager.trackScreenView(this, this)
        showMainActivity()
    }

    private fun showMainActivity() {
        // if (true) {
        // // ADD tools:replace="android:supportsRtl" to AndroidManifest.xml <application />
        // // USE real Google Ads app ID & unit IDs in keys.xml
        // com.google.android.ads.mediationtestsuite.MediationTestSuite.launch(this);
        // finish();
        // return;
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