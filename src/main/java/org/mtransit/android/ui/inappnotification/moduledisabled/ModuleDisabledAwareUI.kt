package org.mtransit.android.ui.inappnotification.moduledisabled

import org.mtransit.android.BuildConfig
import org.mtransit.android.R
import org.mtransit.android.commons.PackageManagerUtils
import org.mtransit.android.commons.StoreUtils
import org.mtransit.android.util.BatteryOptimizationIssueUtils

object ModuleDisabledAwareUI {

    private const val IN_APP_NOTIFICATION_MODULE_DISABLED = 1

    fun onViewCreated(fragment: ModuleDisabledAwareFragment) {
        fragment.viewModel.hasDisabledModule.observe(fragment.getViewLifecycleOwner()) { hasDisabledModule ->
            if (hasDisabledModule == true) {
                showModuleDisabledToast(fragment)
            } else {
                hideModuleDisabledToast(fragment)
            }
        }
    }

    private fun showModuleDisabledToast(fragment: ModuleDisabledAwareFragment) {
        val context = fragment.context ?: return
        val activity = fragment.activity
        val view = fragment.getView()
        val viewModel = fragment.viewModel
        val attachedViewModel = fragment.attachedViewModel
        val firstDisabledAgency = viewModel.moduleDisabled.value?.firstOrNull { !PackageManagerUtils.isAppEnabled(context, it.pkg) }
        val labelText = firstDisabledAgency?.let { agency ->
            context.getString(
                R.string.module_disabled_in_app_notification_label_and_agency,
                agency.getShortNameAndType(context)
            )
        } ?: context.getText(R.string.module_disabled_in_app_notification_label)
        fragment.showInAppNotification(
            firstDisabledAgency?.pkg?.hashCode() ?: IN_APP_NOTIFICATION_MODULE_DISABLED, // TODO dynamic IDs?
            activity,
            view,
            attachedViewModel?.getAdBannerHeightInPx(fragment) ?: 0,
            labelText,
            context.getText(R.string.module_disabled_in_app_notification_action),
        ) {
            attachedViewModel?.moduleDisabled?.value?.let { moduleDisabled ->
                if (moduleDisabled.isNotEmpty()) {
                    val firstDisabledPkg = moduleDisabled.first().pkg
                    activity?.let {
                        return@showInAppNotification if (BuildConfig.DEBUG && BatteryOptimizationIssueUtils.isSamsungDevice()) {
                            BatteryOptimizationIssueUtils.openDeviceCare(
                                it,
                                BatteryOptimizationIssueUtils.SAMSUNG_DEVICE_CARE_EXTRA_ACTIVITY_TYPE_APP_SLEEPING_DEEP
                            )
                        } else {
                            StoreUtils.viewAppPage(
                                it,
                                firstDisabledPkg,
                                it.getString(org.mtransit.android.commons.R.string.google_play)
                            )
                        }
                    }
                }
            }
            false // not handled
        }
    }

    private fun hideModuleDisabledToast(fragment: ModuleDisabledAwareFragment) {
        fragment.hideAllInAppNotifications() // TODO dynamic IDs? hideInAppNotification(IN_APP_NOTIFICATION_MODULE_DISABLED)
    }

    fun onDestroyView(fragment: ModuleDisabledAwareFragment) {
        hideModuleDisabledToast(fragment)
    }
}