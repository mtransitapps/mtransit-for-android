package org.mtransit.android.ui.inappnotification

import android.app.Activity
import android.view.View
import org.mtransit.android.ui.view.common.IFragment

interface InAppNotificationFragment : IFragment {

    fun getContextView(): View? // required to set at the fragment level (default = getView())
    fun getAnchorView(): View? = null // optional
    val viewModel: InAppNotificationViewModel
    val attachedViewModel: InAppNotificationViewModel?

    fun showInAppNotification(
        notificationId: String,
        activity: Activity?,
        view: View?,
        contextView: View?,
        anchorView: View?,
        additionalBottomMarginInPx: Int,
        labelText: CharSequence,
        actionText: CharSequence?,
        onActionClick: View.OnLongClickListener?,
    ): Boolean

    fun hideInAppNotification(notificationId: String): Boolean

    fun hideAllInAppNotifications(): Boolean
}