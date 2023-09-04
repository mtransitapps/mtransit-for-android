package org.mtransit.android.ui.inappnotification

import android.content.Context
import android.view.View

interface InAppNotificationUI<F : InAppNotificationFragment> {

    companion object {
        const val IN_APP_NOTIFICATION_NEW_LOCATION = 1
        const val IN_APP_NOTIFICATION_LOCATION_SETTINGS = 2
        const val IN_APP_NOTIFICATION_MODULE_DISABLED = 3
    }

    fun getNotificationId(fragment: F): Int

    fun onViewCreated(fragment: F)

    fun showOrHideInAppNotification(fragment: F, show: Boolean) {
        if (show) {
            showInAppNotification(fragment)
        } else {
            hideInAppNotification(fragment)
        }
    }

    fun getLabelText(fragment: F, context: Context): CharSequence
    fun getActionText(fragment: F, context: Context): CharSequence? = null // optional
    fun onActionClick(fragment: F): View.OnLongClickListener? = null // optional

    fun showInAppNotification(fragment: F) {
        val context = fragment.context ?: return
        val activity = fragment.activity
        val view = fragment.getView()
        val attachedViewModel = fragment.attachedViewModel
        fragment.showInAppNotification(
            getNotificationId(fragment),
            activity,
            view,
            attachedViewModel?.getAdBannerHeightInPx(fragment) ?: 0,
            getLabelText(fragment, context),
            getActionText(fragment, context),
            onActionClick(fragment)
        )
    }

    fun hideInAppNotification(fragment: F) {
        fragment.hideInAppNotification(getNotificationId(fragment))
    }
}