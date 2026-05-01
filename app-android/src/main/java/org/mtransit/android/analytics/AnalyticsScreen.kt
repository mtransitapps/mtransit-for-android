package org.mtransit.android.analytics

interface AnalyticsScreen {
    val screenName: String
    val screenClass: String get() = this.javaClass::class.java.simpleName
}
