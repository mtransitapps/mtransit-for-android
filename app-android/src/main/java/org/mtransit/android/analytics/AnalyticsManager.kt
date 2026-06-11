package org.mtransit.android.analytics

import android.content.Context
import android.os.Bundle
import androidx.annotation.MainThread
import androidx.annotation.Size
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.MTLog.Loggable
import javax.inject.Inject

// ENABLE:
// - DEBUG logging:
// adb shell setprop log.tag.FA VERBOSE
// adb shell setprop log.tag.FA-SVC VERBOSE
// - FASTER EVENTS
// adb shell setprop debug.firebase.analytics.app org.mtransit.android
//
// LOGCAT:
// adb logcat -v time -s FA FA-SVC
//
// DISABLE:
// - DEBUG logging:
// adb shell setprop log.tag.FA \"\"
// adb shell setprop log.tag.FA-SVC \"\"
// - FASTER EVENTS
// adb shell setprop debug.firebase.analytics.app .none.
//
// https://firebase.google.com/docs/analytics/android/events#view-events-in-debug-log
// https://firebase.google.com/docs/analytics/debugview
class AnalyticsManager @Inject internal constructor(
    @ApplicationContext appContext: Context,
) : IAnalyticsManager, Loggable {

    companion object {
        private val LOG_TAG: String = AnalyticsManager::class.java.getSimpleName()

        private const val ANALYTICS_ENABLED = true

        // private static final boolean ANALYTICS_ENABLED = false // DEBUG
        private const val DEBUG = false
    }

    override fun getLogTag() = LOG_TAG

    // private static final boolean DEBUG = true; // DEBUG
    private val firebaseAnalytics: FirebaseAnalytics?

    init {
        if (!ANALYTICS_ENABLED) {
            firebaseAnalytics = null
        } else {
            firebaseAnalytics = FirebaseAnalytics.getInstance(appContext)
            @Suppress("ControlFlowWithEmptyBody")
            if (DEBUG) {
                // DEBUG adb shell setprop debug.firebase.analytics.app org.mtransit.android
                // DEBUG adb shell setprop log.tag.FA VERBOSE
                // DEBUG adb shell setprop log.tag.FA-SVC VERBOSE
                // DEBUG adb logcat -v time -s FA FA-SVC
            }
        }
    }

    override fun setUserProperty(
        @Size(min = 1L, max = 24L) name: String,
        value: Int
    ) {
        setUserProperty(name, value.toString())
    }

    override fun setUserProperty(
        @Size(min = 1L, max = 24L) name: String,
        value: String
    ) {
        if (!ANALYTICS_ENABLED) return
        try {
            firebaseAnalytics?.setUserProperty(name, value)
        } catch (e: Exception) {
            MTLog.w(this, e, "Error while tracing user property (%s:%s)", name, value)
        }
    }

    override fun logEvent(@Size(min = 1L, max = 40L) name: String) {
        if (!ANALYTICS_ENABLED) return
        logEvent(name, null)
    }

    override fun logEvent(
        @Size(min = 1L, max = 40L) name: String,
        params: AnalyticsEventsParamsProvider?
    ) {
        if (!ANALYTICS_ENABLED) return
        // var bundle: Bundle? = null
        val bundle = params?.let {
            Bundle().apply {
                for (param in it.to()) {
                    param.value.let { paramValue ->
                        when (paramValue) {
                            is String -> putString(param.key, paramValue)
                            is Boolean -> putBoolean(param.key, paramValue)
                            is Int -> putInt(param.key, paramValue)

                            else -> MTLog.w(this@AnalyticsManager, "Unexpected event parameter type for '" + param.key + "'>'" + param.value + "'!")
                        }
                    }
                }
            }
        }
        this.firebaseAnalytics?.logEvent(name, bundle)
    }

    @MainThread
    override fun trackScreenView(page: AnalyticsScreen) {
        if (!ANALYTICS_ENABLED) return
        try {
            firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, page.screenName)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, page.screenClass)
            })
        } catch (e: Exception) {
            MTLog.w(this, e, "Error while tracing screen view! (%s)", page)
        }
    }

    @MainThread
    override fun trackButtonClick(buttonName: String, page: AnalyticsScreen) {
        if (!ANALYTICS_ENABLED) return
        try {
            firebaseAnalytics?.logEvent(AnalyticsEvents.BUTTON_CLICK, Bundle().apply {
                putString(AnalyticsEvents.Params.BUTTON_NAME, buttonName)
                putString(FirebaseAnalytics.Param.SCREEN_NAME, page.screenName)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, page.screenClass)
            })
        } catch (e: Exception) {
            MTLog.w(this, e, "Error while tracing screen view! (%s)", page)
        }
    }
}
