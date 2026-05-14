package org.mtransit.android.ad

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import org.mtransit.android.R
import org.mtransit.android.ad.AdConstants.logAdsD
import org.mtransit.android.commons.MTLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdsConsentManager @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
) : MTLog.Loggable {

    companion object {
        val LOG_TAG: String = "${AdManager.LOG_TAG}>${AdsConsentManager::class.java.simpleName}"
    }

    override fun getLogTag() = LOG_TAG

    private val consentInformation: ConsentInformation by lazy {
        UserMessagingPlatform.getConsentInformation(appContext)
    }

    /**
     * > "Important: canRequestAds() always returns false until you have called requestConsentInfoUpdate()"
     */
    val canRequestAds: Boolean
        get() = consentInformation.canRequestAds()

    val isPrivacyOptionsRequired: Boolean
        get() = consentInformation.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun gatherConsent(
        activity: Activity,
        noUI: Boolean = false,
        onConsentGatheringComplete: (error: FormError?) -> Unit = {},
    ) {
        val consentRequestParams = ConsentRequestParameters.Builder().apply {
            if (AdConstants.DEBUG) {
                setConsentDebugSettings(
                    ConsentDebugSettings.Builder(activity).apply {
                        AdConstants.DEBUG_CONSENT_GEOGRAPHY?.let {
                            setDebugGeography(it)
                            setForceTesting(true)
                            appContext.resources.getStringArray(R.array.google_ads_test_devices_ids).forEach { testDeviceHashedId ->
                                addTestDeviceHashedId(testDeviceHashedId)
                            }
                        }
                    }.build()
                )
            }
        }.build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            consentRequestParams,
            { // SUCCESS
                logAdsD(this, "Consent information successfully updated.")
                if (noUI) {
                    onConsentGatheringComplete(null)
                    return@requestConsentInfoUpdate
                }
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                    activity,
                    { formError -> // DISMISSED
                        onConsentGatheringComplete(formError)
                    }
                )
            },
            { requestConsentError -> // FAILURE
                MTLog.w(this@AdsConsentManager, "Error updating consent information: [${requestConsentError.errorCode}]: ${requestConsentError.message}.")
                onConsentGatheringComplete(requestConsentError)
            },
        )
    }

    fun showPrivacyOptionsForm(
        activity: Activity,
        onConsentFormDismissedListener: ConsentForm.OnConsentFormDismissedListener,
    ) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity, onConsentFormDismissedListener)
    }

    fun resetConsent() {
        consentInformation.reset()
    }
}