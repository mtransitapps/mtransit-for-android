package org.mtransit.android.ui.inappnotification.locationpermission

import android.content.Context
import android.view.View
import org.mtransit.android.R
import org.mtransit.android.ui.MTActivityWithLocation
import org.mtransit.android.ui.inappnotification.InAppNotificationUI
import org.mtransit.android.ui.inappnotification.InAppNotificationUI.Companion.IN_APP_NOTIFICATION_LOCATION_PERMISSION

object LocationPermissionUI : InAppNotificationUI<LocationPermissionAwareFragment> {

    override fun getNotificationId(fragment: LocationPermissionAwareFragment) =
        InAppNotificationUI.getNotificationId(IN_APP_NOTIFICATION_LOCATION_PERMISSION)

    override fun onViewCreated(fragment: LocationPermissionAwareFragment) {
        fragment.viewModel.onboarding.observe(fragment.getViewLifecycleOwner()) {
            // DO NOTHING
        }
        fragment.viewModel.locationPermissionNeeded.observe(fragment.getViewLifecycleOwner()) { needed ->
            showOrHideInAppNotification(fragment, needed == true)
        }
    }

    override fun getLabelText(fragment: LocationPermissionAwareFragment, context: Context) =
        context.getText(
            if (fragment.viewModel.onboarding.value == true) R.string.location_permission_in_app_notification_label_onboarding
            else R.string.location_permission_in_app_notification_label
        )

    override fun getActionText(fragment: LocationPermissionAwareFragment, context: Context) =
        context.getText(R.string.location_permission_in_app_notification_action)

    override fun onActionClick(fragment: LocationPermissionAwareFragment) = View.OnLongClickListener {
        (fragment.activity as? MTActivityWithLocation)?.let { activityWithLocation ->
            fragment.attachedViewModel?.let { attachedViewModel ->
                attachedViewModel.enableLocationPermission(activityWithLocation)
                true // handled
            }
        } ?: false // not handled
    }
}