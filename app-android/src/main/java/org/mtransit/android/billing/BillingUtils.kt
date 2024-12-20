package org.mtransit.android.billing

import android.app.Activity
import org.mtransit.android.ui.fragment.PurchaseDialogFragment
import org.mtransit.android.ui.purchase.PurchaseActivity
import org.mtransit.android.util.FragmentUtils
import org.mtransit.android.util.UIFeatureFlags

object BillingUtils {

    @JvmStatic
    fun showPurchaseDialog(activity: Activity?) {
        if (UIFeatureFlags.F_NEW_IN_APP_SUBS) {
            activity?.startActivity(PurchaseActivity.newInstance(activity))
            return
        }
        @Suppress("DEPRECATION") // TODO migrate to AndroidX Fragment Dialog
        FragmentUtils.replaceDialogFragment(activity, FragmentUtils.DIALOG_TAG, PurchaseDialogFragment.newInstance(), null)
    }
}