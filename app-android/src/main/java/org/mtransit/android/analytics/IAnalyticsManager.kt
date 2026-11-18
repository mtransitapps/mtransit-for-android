package org.mtransit.android.analytics

import androidx.annotation.MainThread
import androidx.annotation.Size

@Suppress("unused")
interface IAnalyticsManager {
    fun setUserProperty(@Size(min = 1L, max = 24L) name: String, value: Int)

    fun setUserProperty(@Size(min = 1L, max = 24L) name: String, value: String)

    fun logEvent(@Size(min = 1L, max = 40L) name: String) = logEvent(name, params = null)

    fun logEvent(@Size(min = 1L, max = 40L) name: String, params: AnalyticsEventsParamsProvider?)

    @MainThread
    fun trackScreenView(page: AnalyticsScreen)

    @MainThread
    fun trackButtonClick(buttonName: String, page: AnalyticsScreen?) =
        trackButtonClick(buttonName, null, page)

    @MainThread
    fun trackButtonClick(buttonName: String, buttonId: String? = null, page: AnalyticsScreen? = null)
}
