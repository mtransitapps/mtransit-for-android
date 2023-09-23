package org.mtransit.android.ui.inappnotification

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.widget.PopupWindow
import com.google.android.material.snackbar.Snackbar
import org.mtransit.android.R
import org.mtransit.android.commons.ToastUtils
import androidx.core.util.Pair as AndroidXPair

interface InAppNotificationUI<F : InAppNotificationFragment> {

    companion object {

        const val IN_APP_NOTIFICATION_NEW_LOCATION = 1
        const val IN_APP_NOTIFICATION_LOCATION_SETTINGS = 2
        const val IN_APP_NOTIFICATION_MODULE_DISABLED = 3

        fun getNotificationId(typeId: Int, specificId: String? = null) = "$typeId${specificId.orEmpty()}"

        private const val SNACKBAR_INSTEAD_OF_TOAST = false
        // private const val SNACKBAR_INSTEAD_OF_TOAST = true

        @SuppressLint("ClickableViewAccessibility")
        @JvmOverloads
        @JvmStatic
        fun makeInAppNotification(
            context: Context?,
            view: View?,
            @Suppress("UNUSED_PARAMETER") anchorView: View?,
            labelText: CharSequence,
            onDismiss: () -> Boolean,
            actionText: CharSequence? = null,
            onActionClick: View.OnLongClickListener? = null,
            onActionClicked: (() -> Boolean)? = null,
        ): AndroidXPair<PopupWindow?, Snackbar?> {
            if (SNACKBAR_INSTEAD_OF_TOAST) {
                return AndroidXPair(null, view?.let {
                    Snackbar.make(it, labelText, Snackbar.LENGTH_INDEFINITE).apply {
                        if (!actionText.isNullOrBlank() && onActionClick != null) {
                            setAction(actionText) { v ->
                                onActionClick.onLongClick(v)
                            }
                        }
                    }
                }
                )
            }
            return AndroidXPair(ToastUtils.getNewTouchableToast(context, R.drawable.toast_frame_old, labelText, actionText)?.apply {
                onActionClick?.let {
                    setTouchInterceptor(View.OnTouchListener { v, event ->
                        return@OnTouchListener when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                val handled = onActionClick.onLongClick(v)
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
            }, null)
        }

        @JvmStatic
        fun showInAppNotification(
            activity: Activity?,
            inAppNotification: AndroidXPair<PopupWindow?, Snackbar?>?,
            view: View?,
            @Suppress("UNUSED_PARAMETER") anchorView: View?,
            additionalBottomMarginInPx: Int
        ): Boolean {
            if (SNACKBAR_INSTEAD_OF_TOAST) {
                return inAppNotification?.second?.let { it.show(); true } ?: false
            }
            return ToastUtils.showTouchableToastPx(
                activity,
                inAppNotification?.first,
                view,
                additionalBottomMarginInPx
            )
        }

        @JvmStatic
        fun hideInAppNotification(
            inAppNotification: AndroidXPair<PopupWindow?, Snackbar?>?,
        ): Boolean {
            if (SNACKBAR_INSTEAD_OF_TOAST) {
                return inAppNotification?.second?.let { it.dismiss(); true } ?: false
            }
            return inAppNotification?.first?.let { it.dismiss(); true } ?: false
        }
    }

    fun getNotificationId(fragment: F): String

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

    fun showInAppNotification(fragment: F): Boolean {
        val context = fragment.context ?: return false
        val activity = fragment.activity
        val attachedViewModel = fragment.attachedViewModel
        return fragment.showInAppNotification(
            getNotificationId(fragment),
            activity,
            fragment.getView(),
            fragment.getAnchorView(),
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