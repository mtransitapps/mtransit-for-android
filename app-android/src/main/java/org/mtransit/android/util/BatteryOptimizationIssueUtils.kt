package org.mtransit.android.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.IntDef
import org.mtransit.android.R
import org.mtransit.android.commons.DeviceUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.PackageManagerUtils
import org.mtransit.android.commons.StoreUtils
import java.net.URL
import java.util.Locale
import org.mtransit.android.commons.R as commonsR

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
    fun getDoNotKillMyAppUrlExtended() = try {
        URL(
            DO_NOT_KILL_MY_APP_URL_AND_MANUFACTURER
                .format(manufacturerDNTLC)
        ).toString()
    } catch (e: Exception) {
        MTLog.w(LOG_TAG, e, "Error while creating custom URL with manufacturer")
        DO_NOT_KILL_MY_APP_URL
    }

    @JvmStatic
    fun getDoNotKillMyAppImageUrlExtended() = try {
        URL(
            DO_NOT_KILL_MY_APP_IMAGE_URL_AND_MANUFACTURER
                .format(manufacturerDNTLC)
        ).toString()
    } catch (e: Exception) {
        MTLog.w(LOG_TAG, e, "Error while creating custom URL with manufacturer")
        DO_NOT_KILL_MY_APP_IMAGE_URL
    }

    @JvmStatic
    fun openDeviceBatteryOptimizationSettings(activity: Activity) {
        if (isSamsungDevice()) {
            if (isSamsungDeviceCareInstalled(activity)) {
                openDeviceCare(activity, SAMSUNG_DEVICE_CARE_EXTRA_ACTIVITY_TYPE_APP_SLEEPING_NEVER)
            } else {
                installSamsungDeviceCare(activity)
            }
        } else {
            DeviceUtils.showIgnoreBatteryOptimizationSettings(activity)
        }
    }

    // region Samsung

    private const val MANUFACTURER_SAMSUNG = "samsung"
    private const val SAMSUNG_DEVICE_CARE_PKG = "com.samsung.android.lool"
    private const val SAMSUNG_DEVICE_CARE_ACTION = "com.samsung.android.sm.ACTION_OPEN_CHECKABLE_LISTACTIVITY"
    private const val SAMSUNG_DEVICE_CARE_EXTRA_ACTIVITY_TYPE = "activity_type"

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        SAMSUNG_DEVICE_CARE_EXTRA_ACTIVITY_TYPE_APP_SLEEPING,
        SAMSUNG_DEVICE_CARE_EXTRA_ACTIVITY_TYPE_APP_SLEEPING_DEEP,
        SAMSUNG_DEVICE_CARE_EXTRA_ACTIVITY_TYPE_APP_SLEEPING_NEVER
    )
    annotation class SamsungDeviceCareActivityType

    const val SAMSUNG_DEVICE_CARE_EXTRA_ACTIVITY_TYPE_APP_SLEEPING = 0
    const val SAMSUNG_DEVICE_CARE_EXTRA_ACTIVITY_TYPE_APP_SLEEPING_DEEP = 1
    const val SAMSUNG_DEVICE_CARE_EXTRA_ACTIVITY_TYPE_APP_SLEEPING_NEVER = 2

    @JvmStatic
    fun isSamsungDevice() = this.manufacturerDNTLC == MANUFACTURER_SAMSUNG

    @JvmStatic
    fun isSamsungDeviceCareInstalled(context: Context) = PackageManagerUtils.isAppInstalled(context, SAMSUNG_DEVICE_CARE_PKG)

    @JvmStatic
    fun installSamsungDeviceCare(context: Context) {
        StoreUtils.viewAppPage(context, SAMSUNG_DEVICE_CARE_PKG, context.getString(commonsR.string.google_play))
    }

    @JvmStatic
    fun openDeviceCare(
        activity: Activity,
        @SamsungDeviceCareActivityType activityType: Int? = null,
    ): Boolean {
        return LinkUtils.open(
            activity,
            Intent(SAMSUNG_DEVICE_CARE_ACTION).apply {
                setPackage(SAMSUNG_DEVICE_CARE_PKG)
                activityType?.let { putExtra(SAMSUNG_DEVICE_CARE_EXTRA_ACTIVITY_TYPE, it) }
                this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or // make sure it does NOT open in the stack of your activity
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or // task re-parenting if needed
                        Intent.FLAG_ACTIVITY_CLEAR_TOP // make sure it opens on app page even if already open in search result
            },
            activity.getString(R.string.samsung_device_care),
            false,
        )
    }

    // endregion
}