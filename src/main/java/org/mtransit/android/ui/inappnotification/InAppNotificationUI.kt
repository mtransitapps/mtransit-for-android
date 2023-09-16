package org.mtransit.android.ui.inappnotification

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.widget.PopupWindow
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import org.mtransit.android.R
import org.mtransit.android.commons.ToastUtils

interface InAppNotificationUI<F : InAppNotificationFragment> {

    companion object {

        const val IN_APP_NOTIFICATION_NEW_LOCATION = 1
        const val IN_APP_NOTIFICATION_LOCATION_SETTINGS = 2
        const val IN_APP_NOTIFICATION_MODULE_DISABLED = 3

        @SuppressLint("ClickableViewAccessibility")
        @JvmOverloads
        @JvmStatic
        fun makeInAppNotification(
            context: Context?,
            labelText: CharSequence,
            onDismiss: () -> Boolean,
            actionText: CharSequence? = null,
            onActionClick: View.OnLongClickListener? = null,
            onActionClicked: (() -> Boolean)? = null,
        ): PopupWindow? {
            val toastText = buildSpannedString {
                append(labelText)
                if (!actionText.isNullOrBlank() && onActionClick != null) {
                    append(" | ")
                    bold {
                        append(actionText)
                    }
                }
            }
            return ToastUtils.getNewTouchableToast(context, R.drawable.toast_frame_old, toastText)?.apply {
                onActionClick?.let {
                    setTouchInterceptor(View.OnTouchListener { v, event ->
                        return@OnTouchListener when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                val handled = onActionClick.onLongClick(v)
                                // hideInAppNotification(notificationId)
                                onActionClicked?.invoke()
                                handled
                            }

                            else -> false // not handled
                        }
                    })
                    setOnDismissListener {
                        onDismiss()
                    }
                }
            }
        }

        @JvmStatic
        fun showInAppNotification(
            activity: Activity?,
            inAppNotification: PopupWindow?,
            view: View?,
            additionalBottomMarginInPx: Int
        ) = ToastUtils.showTouchableToastPx(
            activity,
            inAppNotification,
            view,
            additionalBottomMarginInPx
        )
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