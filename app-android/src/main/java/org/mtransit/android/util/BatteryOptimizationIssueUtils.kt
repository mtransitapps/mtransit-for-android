package org.mtransit.android.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.IntDef
import androidx.annotation.MainThread
import androidx.core.content.edit
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mtransit.android.BuildConfig
import org.mtransit.android.R
import org.mtransit.android.common.repository.LocalPreferenceRepository
import org.mtransit.android.commons.DeviceUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.PackageManagerUtils
import org.mtransit.android.commons.StoreUtils
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.ui.InvisibleActivity
import org.mtransit.android.datasource.DataSourcesRepository
import java.net.URL
import java.util.Locale
import java.util.concurrent.TimeUnit
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

    // region invisible activity

    private const val ANY = "any"

    private val INVISIBLE_ACTIVITY_ANY_MIN_OPEN_MS = TimeUnit.HOURS.toMillis(1L)

    private val INVISIBLE_ACTIVITY_MIN_OPEN_MS = TimeUnit.DAYS.toMillis(7L)

    private fun isInvisibleActivityEnabled() = BuildConfig.DEBUG
            || (isSamsungDevice() && Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU)

    @JvmStatic
    fun onAppResumeInvisibleActivity(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        dataSourceRepository: DataSourcesRepository,
        lclPrefRepository: LocalPreferenceRepository
    ) {
        if (!isInvisibleActivityEnabled()) {
            return // SKIP not-debug && not-Samsung < Android 13 (ignore Android 14+ for now)
        }
        try {
            lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val nowMs = TimeUtils.currentTimeMillis()

                val anyLastAgencyInvisibleActivityOpenedMs = lclPrefRepository.getValue( // I/O
                    LocalPreferenceRepository.getPREFS_LCL_AGENCY_LAST_OPENED_DEFAULT(ANY),
                    nowMs // not the 1st time
                )
                if (nowMs - anyLastAgencyInvisibleActivityOpenedMs < INVISIBLE_ACTIVITY_ANY_MIN_OPEN_MS) {
                    MTLog.d(LOG_TAG, "onAppResumeInvisibleActivity() > SKIP (any)")
                    return@launch // SKIP (too soon)
                }

                dataSourceRepository
                    .getAllAgenciesEnabled()
                    .filter { it.pkg != context.packageName }
                    .forEach { agency ->
                        val lastAgencyInvisibleActivityOpenedMs = lclPrefRepository.getValue( // I/O
                            LocalPreferenceRepository.getPREFS_LCL_AGENCY_LAST_OPENED_DEFAULT(agency.authority),
                            nowMs // not the 1st time
                        )
                        if (nowMs - lastAgencyInvisibleActivityOpenedMs < INVISIBLE_ACTIVITY_MIN_OPEN_MS) {
                            return@forEach // SKIP (too soon)
                        }
                        val opened = withContext(Dispatchers.Main) {
                            return@withContext openInvisibleActivity(context, agency.pkg)
                        }
                        lclPrefRepository.pref.edit {
                            putLong(
                                LocalPreferenceRepository.getPREFS_LCL_AGENCY_LAST_OPENED_DEFAULT(ANY),
                                nowMs
                            )
                            putLong(
                                LocalPreferenceRepository.getPREFS_LCL_AGENCY_LAST_OPENED_DEFAULT(agency.authority),
                                nowMs
                            )
                        }
                        if (opened) {
                            return@launch // STOP
                        }
                    }
            }
        } catch (e: Exception) {
            MTLog.w(LOG_TAG, e, "Error while finding invisible activity!")
        }
    }

    @MainThread
    private fun openInvisibleActivity(context: Context, pkg: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setPackage(pkg)
                setClassName(pkg, InvisibleActivity.CLASS_NAME)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (_: ActivityNotFoundException) {
            MTLog.d(this, "'${InvisibleActivity.CLASS_NAME}' not found in $pkg!")
            false
        } catch (_: Exception) {
            MTLog.d(this, "Unexpected error while opening '${InvisibleActivity.CLASS_NAME}' not found in $pkg!")
            false
        }
    }

    // endregion

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