package org.mtransit.android.ui.pref.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import dagger.hilt.android.lifecycle.HiltViewModel
import org.mtransit.android.ad.IAdManager
import org.mtransit.android.billing.IBillingManager
import org.mtransit.android.common.repository.DefaultPreferenceRepository
import org.mtransit.android.common.repository.LocalPreferenceRepository
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.pref.liveData
import javax.inject.Inject

@HiltViewModel
class MainPreferencesViewModel @Inject constructor(
    private val billingManager: IBillingManager,
    private val adManager: IAdManager,
    lclPrefRepository: LocalPreferenceRepository,
    defaultPrefRepository: DefaultPreferenceRepository,
) : ViewModel(), MTLog.Loggable {

    companion object {
        private val LOG_TAG = MainPreferencesViewModel::class.java.simpleName

        internal const val DEVICE_SETTINGS_GROUP_PREF = "pDeviceSettings"
        internal const val DEVICE_SETTINGS_LANGUAGE_PREF = "pDeviceSettingsLanguage"
        internal const val DEVICE_SETTINGS_DATE_AND_TIME_PREF = "pDeviceSettingsDateAndTime"
        internal const val DEVICE_SETTINGS_LOCATION_PREF = "pDeviceSettingsLocation"
        internal const val DEVICE_SETTINGS_POWER_MANAGEMENT_PREF = "pDeviceSettingsPowerManagement"

        internal const val FEEDBACK_EMAIL_PREF = "pFeedbackEmail"
        internal const val FEEDBACK_STORE_PREF = "pFeedbackStore"

        internal const val SUPPORT_SUBSCRIPTIONS_PREF = "pSupportSubs"

        internal const val ABOUT_PRIVACY_POLICY_PREF = "pAboutPrivacyPolicy"
        internal const val ABOUT_TERMS_OF_USE_PREF = "pAboutTermsOfUse"
        internal const val ABOUT_APP_VERSION_PREF = "pAboutAppVersion"

        internal const val THIRD_PARTY_GOOGLE_PRIVACY_POLICY_PREF = "p3rdPartyGooglePrivacyPolicy"
        internal const val THIRD_PARTY_YOUTUBE_TERMS_OF_SERVICE_PREF = "p3rdPartyYouTubeTermsOfService"

        internal const val SOCIAL_FACEBOOK_PREF = "pSocialFacebook"
        internal const val SOCIAL_TWITTER_PREF = "pSocialTwitter"

        internal const val DEV_MODE_GROUP_PREF = "pDevMode"
        internal const val DEV_MODE_MODULE_PREF = "pDevModeModule"
        internal const val DEV_MODE_REWARDED_RESET_PREF = "pDevModeRewardedReset"
        internal const val DEV_MODE_AD_INSPECTOR_PREF = "pDevModeAdInspector"
        internal const val DEV_MODE_AD_MEDIATION_TEST_PREF = "pDevModeAdMediationTest"

        internal const val TWITTER_PAGE_URL = "https://twitter.com/montransit"
        internal const val FACEBOOK_PAGE_URL = "https://facebook.com/MonTransit"

        internal const val PRIVACY_POLICY_PAGE_URL = "https://github.com/mtransitapps/mtransit-for-android/wiki/PrivacyPolicy"
        internal const val PRIVACY_POLICY_FR_PAGE_URL = "https://github.com/mtransitapps/mtransit-for-android/wiki/PrivacyPolicyFr"
        internal const val TERMS_OF_USE_PAGE_URL = "https://github.com/mtransitapps/mtransit-for-android/wiki/TermsOfUse"
        internal const val TERMS_OF_USE_FR_PAGE_URL = "https://github.com/mtransitapps/mtransit-for-android/wiki/TermsOfUseFr"

        internal const val GOOGLE_PRIVACY_POLICY_PAGE_URL = "https://policies.google.com/privacy"
        internal const val YOUTUBE_TERMS_OF_SERVICE_PAGE_URL = "https://www.youtube.com/t/terms"
    }

    override fun getLogTag(): String = LOG_TAG

    val currentSubscription: String? = billingManager.getCurrentSubscription()
    val hasSubscription: Boolean? = billingManager.isHasSubscription()

    val theme: LiveData<String> = defaultPrefRepository.pref.liveData(
        DefaultPreferenceRepository.PREFS_THEME, DefaultPreferenceRepository.PREFS_THEME_DEFAULT
    ).distinctUntilChanged()

    val units: LiveData<String> = defaultPrefRepository.pref.liveData(
        DefaultPreferenceRepository.PREFS_UNITS, DefaultPreferenceRepository.PREFS_UNITS_DEFAULT
    ).distinctUntilChanged()

    val useInternalWebBrowser: LiveData<Boolean> = defaultPrefRepository.pref.liveData(
        DefaultPreferenceRepository.PREFS_USE_INTERNAL_WEB_BROWSER, DefaultPreferenceRepository.PREFS_USE_INTERNAL_WEB_BROWSER_DEFAULT
    ).distinctUntilChanged()

    val devModeEnabled: LiveData<Boolean> = lclPrefRepository.pref.liveData(
        LocalPreferenceRepository.PREFS_LCL_DEV_MODE_ENABLED, LocalPreferenceRepository.PREFS_LCL_DEV_MODE_ENABLED_DEFAULT
    ).distinctUntilChanged()

    fun resetRewardedAd() {
        adManager.resetRewarded()
    }

    fun openAdInspector() {
        adManager.openAdInspector()
    }

    fun refreshData() {
        billingManager.refreshPurchases()
    }
}