package org.mtransit.android.ad

object AdConstants {

    const val DEBUG = false
    // const val DEBUG = true // DEBUG

    val DEBUG_CONSENT_GEOGRAPHY: Int? = null
    // val DEBUG_CONSENT_GEOGRAPHY: Int? = com.google.android.ump.ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_REGULATED_US_STATE // DEBUG
    // val DEBUG_CONSENT_GEOGRAPHY: Int? = com.google.android.ump.ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA // DEBUG

    const val AD_ENABLED = true
    // const val AD_ENABLED = false // DEBUG

    const val IGNORE_REWARD_HIDING_BANNER = false
    // private const val IGNORE_REWARD_HIDING_BANNER = true // DEBUG

    @JvmField
    val KEYWORDS = listOf(
        "transit", "bus", "subway", "bike", "sharing", "ferries", "boat", "trail", "lrt", "streetcar", "tram", "tramway",
        "light rail", "transport", "velo", "metro", "taxi", "train", "traversier"
    )

}