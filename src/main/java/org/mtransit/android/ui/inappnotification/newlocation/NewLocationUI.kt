package org.mtransit.android.ui.inappnotification.newlocation

import android.content.Context
import android.view.View
import org.mtransit.android.R
import org.mtransit.android.ui.inappnotification.InAppNotificationUI

object NewLocationUI : InAppNotificationUI<NewLocationAwareFragment> {

    override fun getNotificationId(fragment: NewLocationAwareFragment) = InAppNotificationUI.IN_APP_NOTIFICATION_NEW_LOCATION

    override fun onViewCreated(fragment: NewLocationAwareFragment) {
        fragment.viewModel.newLocationAvailable.observe(fragment.getViewLifecycleOwner()) { newLocationAvailable ->
            showOrHideInAppNotification(fragment, newLocationAvailable == true)
        }
    }

    override fun getLabelText(fragment: NewLocationAwareFragment, context: Context) =
        context.getText(R.string.new_location_in_app_notification_label)

    override fun getActionText(fragment: NewLocationAwareFragment, context: Context) = context.getText(R.string.new_location_in_app_notification_action)

    override fun onActionClick(fragment: NewLocationAwareFragment) = View.OnLongClickListener {
        fragment.attachedViewModel?.initiateRefresh() == true
    }
}