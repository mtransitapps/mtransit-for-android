package org.mtransit.android.ui.inappnotification.moduledisabled

import android.content.Context
import android.view.View
import org.mtransit.android.BuildConfig
import org.mtransit.android.R
import org.mtransit.android.commons.StoreUtils
import org.mtransit.android.commons.isAppEnabled
import org.mtransit.android.ui.inappnotification.InAppNotificationUI
import org.mtransit.android.ui.inappnotification.InAppNotificationUI.Companion.IN_APP_NOTIFICATION_MODULE_DISABLED
import org.mtransit.android.util.BatteryOptimizationIssueUtils
import org.mtransit.android.commons.R as commonsR

object ModuleDisabledUI : InAppNotificationUI<ModuleDisabledAwareFragment> {

    override fun getNotificationId(fragment: ModuleDisabledAwareFragment): String =
        InAppNotificationUI.getNotificationId(
            IN_APP_NOTIFICATION_MODULE_DISABLED,
            fragment.context?.let { context ->
                fragment.viewModel.moduleDisabled.value?.firstOrNull { !context.packageManager.isAppEnabled(it.pkg) }?.pkg
            } ?: fragment.viewModel.moduleDisabled.value?.firstOrNull()?.pkg,
        )

    override fun onViewCreated(fragment: ModuleDisabledAwareFragment) {
        fragment.viewModel.moduleDisabled.observe(fragment.getViewLifecycleOwner()) {
            // DO NOTHING
        }
        fragment.viewModel.hasDisabledModule.observe(fragment.getViewLifecycleOwner()) { hasDisabledModule ->
            showOrHideInAppNotification(fragment, hasDisabledModule == true)
        }
    }

    override fun getLabelText(fragment: ModuleDisabledAwareFragment, context: Context) =
        fragment.viewModel.moduleDisabled.value?.firstOrNull { !context.packageManager.isAppEnabled(it.pkg) }?.let { agency ->
            context.getString(
                R.string.module_disabled_in_app_notification_label_and_agency,
                agency.getShortNameAndType(context)
            )
        } ?: context.getText(R.string.module_disabled_in_app_notification_label)

    override fun getActionText(fragment: ModuleDisabledAwareFragment, context: Context) = context.getText(R.string.module_disabled_in_app_notification_action)

    override fun onActionClick(fragment: ModuleDisabledAwareFragment) = View.OnLongClickListener {
        val attachedViewModel = fragment.attachedViewModel
        val activity = fragment.getActivity() ?: return@OnLongClickListener false // not handled
        attachedViewModel?.moduleDisabled?.value?.let { moduleDisabled ->
            if (moduleDisabled.isNotEmpty()) {
                return@OnLongClickListener if (BuildConfig.DEBUG && BatteryOptimizationIssueUtils.isSamsungDevice()) {
                    BatteryOptimizationIssueUtils.openDeviceCare(
                        activity,
                        BatteryOptimizationIssueUtils.SAMSUNG_DEVICE_CARE_EXTRA_ACTIVITY_TYPE_APP_SLEEPING_DEEP
                    )
                } else {
                    val firstDisabledPkg = moduleDisabled.firstOrNull()?.pkg ?: return@OnLongClickListener false // not handled
                    StoreUtils.viewAppPage(
                        activity,
                        firstDisabledPkg,
                        activity.getString(commonsR.string.google_play)
                    )
                }
            }
        }
        false // not handled
    }
}