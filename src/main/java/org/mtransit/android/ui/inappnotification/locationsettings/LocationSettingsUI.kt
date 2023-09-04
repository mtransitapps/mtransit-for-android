package org.mtransit.android.ui.inappnotification.locationsettings

import android.content.Context
import android.view.View
import org.mtransit.android.R
import org.mtransit.android.ui.inappnotification.InAppNotificationUI

object LocationSettingsUI : InAppNotificationUI<LocationSettingsAwareFragment> {

    override fun getNotificationId(fragment: LocationSettingsAwareFragment) = InAppNotificationUI.IN_APP_NOTIFICATION_LOCATION_SETTINGS

    override fun onViewCreated(fragment: LocationSettingsAwareFragment) {
        fragment.viewModel.locationSettingsNeededResolution.observe(fragment.getViewLifecycleOwner()) {
            // DO NOTHING
        }
        fragment.viewModel.locationSettingsNeeded.observe(fragment.getViewLifecycleOwner()) { needed ->
            showOrHideInAppNotification(fragment, needed == true)
        }
    }

    override fun getLabelText(fragment: LocationSettingsAwareFragment, context: Context) =
        context.getText(R.string.location_settings_in_app_notification_label)

    override fun getActionText(fragment: LocationSettingsAwareFragment, context: Context) =
        context.getText(R.string.location_settings_in_app_notification_action)

    override fun onActionClick(fragment: LocationSettingsAwareFragment) = View.OnLongClickListener {
        val attachedViewModel = fragment.attachedViewModel
        val activity = fragment.activity
        attachedViewModel?.locationSettingsNeededResolution?.value?.let { pendingIntent ->
            @Suppress("DEPRECATION")
            activity?.startIntentSenderForResult(pendingIntent.intentSender, 101, null, 0, 0, 0)
            return@OnLongClickListener true // handled
        }
        false // not handled
    }
}