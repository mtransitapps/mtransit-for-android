package org.mtransit.android.ui.empty

import android.app.Activity
import android.os.Build
import androidx.core.view.isVisible
import org.mtransit.android.R
import org.mtransit.android.commons.DeviceUtils
import org.mtransit.android.commons.StoreUtils
import org.mtransit.android.databinding.LayoutEmptyBinding
import org.mtransit.android.ui.view.common.context
import org.mtransit.android.util.BatteryOptimizationIssueUtils
import org.mtransit.android.util.BatteryOptimizationIssueUtils.SAMSUNG_DEVICE_CARE_EXTRA_ACTIVITY_TYPE_APP_SLEEPING_NEVER
import org.mtransit.android.util.BatteryOptimizationIssueUtils.installSamsungDeviceCare
import org.mtransit.android.util.BatteryOptimizationIssueUtils.isSamsungDeviceCareInstalled
import org.mtransit.android.util.BatteryOptimizationIssueUtils.openDeviceCare
import org.mtransit.android.commons.R as commonsR

object EmptyLayoutUtils {

    fun LayoutEmptyBinding.updateEmptyLayout(empty: Boolean, pkg: String?, activity: Activity?) {
        if (empty) {
            emptyTitle.apply {
                text = context.getString(R.string.sorry_about_that)
                isVisible = true
            }
            emptyText.text = context.getString(R.string.no_routes_found_text)
            emptyButton1.apply {
                pkg?.let { pkg ->
                    text = context.getString(R.string.manage_app)
                    setIconResource(R.drawable.ic_settings_black_24dp)
                    setOnClickListener { v ->
                        DeviceUtils.showAppDetailsSettings(v.context, pkg)
                    }
                    isVisible = true
                } ?: run {
                    isVisible = false
                }
            }
            emptyButton2.apply {
               pkg?.let { pkg ->
                    text = context.getString(commonsR.string.google_play)
                    setIconResource(R.drawable.ic_baseline_shop_24)
                    setOnClickListener { v ->
                        StoreUtils.viewAppPage(v.context, pkg, context.getString(commonsR.string.google_play))
                    }
                    isVisible = true
                } ?: run {
                    isVisible = false
                }
            }
            emptyButton3.apply {
                if (BatteryOptimizationIssueUtils.isSamsungDevice()) {
                    text = context.getString(R.string.samsung_device_care)
                    setIconResource(R.drawable.ic_settings_black_24dp)
                    setOnClickListener { v ->
                        val activity = activity
                        val samsungDeviceCareInstalled = activity != null && isSamsungDeviceCareInstalled(v.context)
                        if (samsungDeviceCareInstalled) {
                            openDeviceCare(activity, SAMSUNG_DEVICE_CARE_EXTRA_ACTIVITY_TYPE_APP_SLEEPING_NEVER)
                        } else {
                            installSamsungDeviceCare(v.context)
                        }
                    }
                    isVisible = true
                } else {
                    isVisible = false
                }
            }
            emptySubText.apply {
                text = buildString {
                    append(Build.MANUFACTURER)
                    append(" ")
                    append(Build.MODEL)
                    append(" - Android ")
                    append(Build.VERSION.RELEASE)
                    append("\n\n")
                    val enabledState = pkg?.let { context.packageManager.getApplicationEnabledSetting(it) }
                    append(context.getString(R.string.enabled_setting))
                    append(
                        when (enabledState) {
                            0 -> context.getString(R.string.enabled_setting_0)
                            1 -> context.getString(R.string.enabled_setting_1)
                            2 -> context.getString(R.string.enabled_setting_2)
                            3 -> context.getString(R.string.enabled_setting_3)
                            4 -> context.getString(R.string.enabled_setting_4)
                            null -> context.getString(R.string.enabled_setting_null)
                            else -> context.getString(R.string.enabled_setting_other_and_state, enabledState)
                        }
                    )
                }
                isVisible = true
            }
        } else {
            emptyTitle.isVisible = false
            emptyText.setText(R.string.no_results)
            emptyButton1.isVisible = false
            emptyButton2.isVisible = false
            emptyButton3.isVisible = false
            emptySubText.isVisible = false
        }
    }
}
