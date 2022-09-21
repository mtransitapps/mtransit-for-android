package org.mtransit.android.util

import android.os.Build
import org.mtransit.android.commons.MTLog
import java.net.URL
import java.util.Locale

object BatteryOptimizationIssueUtils {

    private val LOG_TAG = BatteryOptimizationIssueUtils::class.java.simpleName

    const val DO_NOT_KILL_MY_APP_LABEL = "dontkillmyapp.com"

    private const val DO_NOT_KILL_MY_APP_URL = "https://dontkillmyapp.com?app=MonTransit"
    private const val DO_NOT_KILL_MY_APP_URL_AND_MANUFACTURER = "https://dontkillmyapp.com/%s?app=MonTransit"
    private val DO_NOT_KILL_MY_APP_IMAGE_URL: String? = null
    private const val DO_NOT_KILL_MY_APP_IMAGE_URL_AND_MANUFACTURER = "https://dontkillmyapp.com/badge/%s3.svg"

    val manufacturer: String = Build.MANUFACTURER

    private val manufacturerDNTLC = manufacturer.lowercase(Locale.ROOT).replace(" ", "-")

    @JvmStatic
    fun getDoNotKillMyAppUrlExtended(): String =
        try {
            URL(
                DO_NOT_KILL_MY_APP_URL_AND_MANUFACTURER
                    .format(manufacturerDNTLC)
            ).toString()
        } catch (e: Exception) {
            MTLog.w(LOG_TAG, e, "Error while creating custom URL with manufacturer")
            DO_NOT_KILL_MY_APP_URL
        }

    @JvmStatic
    fun getDoNotKillMyAppImageUrlExtended(): String? =
        try {
            URL(
                DO_NOT_KILL_MY_APP_IMAGE_URL_AND_MANUFACTURER
                    .format(manufacturerDNTLC)
            ).toString()
        } catch (e: Exception) {
            MTLog.w(LOG_TAG, e, "Error while creating custom URL with manufacturer")
            DO_NOT_KILL_MY_APP_IMAGE_URL
        }
}