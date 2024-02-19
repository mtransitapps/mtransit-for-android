package org.mtransit.android.analytics

class AnalyticsEventsParamsProvider {

    private val params = mutableMapOf<String, Any>()

    fun put(name: String, value: String): AnalyticsEventsParamsProvider {
        params[name] = value
        return this
    }

    fun put(name: String, value: Int) = put(name, value.toLong())

    fun put(name: String, value: Long): AnalyticsEventsParamsProvider {
        params[name] = value
        return this
    }

    fun put(name: String, value: Double): AnalyticsEventsParamsProvider {
        params[name] = value
        return this
    }

    fun to(): Map<String, Any> {
        return params
    }
}
