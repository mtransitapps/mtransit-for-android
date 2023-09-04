package org.mtransit.android.ui.inappnotification

import android.app.Activity
import android.view.View
import androidx.lifecycle.LifecycleOwner
import org.mtransit.android.ui.view.common.IActivity

interface InAppNotificationFragment : IActivity {

    fun getViewLifecycleOwner(): LifecycleOwner
    fun getView(): View?
    val viewModel: InAppNotificationViewModel
    val attachedViewModel: InAppNotificationViewModel?

    fun showInAppNotification(
        notificationId: Int,
        activity: Activity?,
        view: View?,
        additionalBottomMarginInPx: Int,
        labelText: CharSequence,
        actionText: CharSequence?,
        onActionClick: View.OnLongClickListener?,
    )

    fun hideAllInAppNotifications()
}