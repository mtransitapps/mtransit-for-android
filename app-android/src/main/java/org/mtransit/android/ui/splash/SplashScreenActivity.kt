package org.mtransit.android.ui.splash

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.MainThread
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.ad.IAdManager
import org.mtransit.android.ad.IAdScreenActivity
import org.mtransit.android.analytics.AnalyticsScreen
import org.mtransit.android.analytics.IAnalyticsManager
import org.mtransit.android.commons.HtmlUtils
import org.mtransit.android.commons.LocaleUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.ui.MTActivity
import org.mtransit.android.ui.MainActivity
import org.mtransit.android.ui.main.NextMainActivity
import org.mtransit.android.ui.view.common.IActivity
import org.mtransit.android.ui.view.common.observeEvent
import org.mtransit.android.util.SystemSettingManager
import org.mtransit.android.util.UIFeatureFlags
import org.mtransit.commons.FeatureFlags
import javax.inject.Inject

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
open class SplashScreenActivity : MTActivity(),
    IActivity, IAdScreenActivity,
    AnalyticsScreen {

    companion object {
        private val LOG_TAG: String = SplashScreenActivity::class.java.simpleName
        private const val TRACKING_SCREEN_NAME = "Splash"
    }

    override fun getLogTag() = LOG_TAG

    override val screenName = TRACKING_SCREEN_NAME

    @Inject
    lateinit var analyticsManager: IAnalyticsManager

    @Inject
    lateinit var adManager: IAdManager

    private val viewModel by viewModels<SplashScreenViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { // Keep the splash screen visible for this Activity
            viewModel.shouldKeepSplashScreenOn.value != false // unknown OR true
        }
        viewModel.initShowingAdsFromCache()
        adManager.init(activity = this, withConsentOnly = true, onInitCompleteListener = {
            viewModel.onAdInitCompleted()
        })
        analyticsManager.trackScreenView(this)
        viewModel.onAppOpen()
        if (UIFeatureFlags.F_LOCALE_WEB_VIEW_FIX_IN_ACTIVITY) LocaleUtils.fixWebViewLocale(this.applicationContext)
        viewModel.shouldKeepSplashScreenOn.observe(this) { shouldKeepSplashScreenOn ->
            // DO NOTHING
        }
        viewModel.showNextScreen.observe(this) { showNextScreen ->
            if (showNextScreen) {
                showMainActivity()
            }
        }
        viewModel.showAppOpenAd.observeEvent(this) { showAppOpenAd ->
            if (showAppOpenAd) {
                adManager.showAppOpenAdIfAvailable(this, onShowAdComplete = {
                    viewModel.onShowAppOpenAdComplete()
                })
            }
        }
        viewModel.deployingData.observeEvent(this) { gettingReady ->
            if (gettingReady) {
                showToast(getString(R.string.deploying_data_in_progress_short))
            } else {
                _toast?.cancel()
                _toast = null // force new toast
                showToast(getString(R.string.deploying_data_done_short))
            }
        }
        viewModel.deployingDataFor.observeEvent(this) { agency ->
            showToast(HtmlUtils.fromHtmlCompact(getString(R.string.deploying_data_in_progress_for_short, agency.getShortNameAndType(context))))
        }
    }

    private var _toast: Toast? = null

    private fun showToast(text: CharSequence, durationShort: Boolean = false) {
        if (SystemSettingManager.isUsingFirebaseTestLab(this)) return
        val toast = _toast ?: Toast.makeText(this, text, if (durationShort) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).also { _toast = it }
        toast.setText(text)
        toast.show()
    }

    override fun onPrivacyOptionsRequiredChanged() {
        // DO NOTHING (in the main screen)
    }

    private fun showMainActivity() {
        if (isFinishing || isDestroyed) return
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

    @get:MainThread
    override val currentFragment: Fragment? = null
}
