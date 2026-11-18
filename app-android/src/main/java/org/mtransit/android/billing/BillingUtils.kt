package org.mtransit.android.billing

import androidx.fragment.app.FragmentActivity
import org.mtransit.android.ui.fragment.PurchaseDialogFragment
import org.mtransit.android.ui.purchase.PurchaseActivity
import org.mtransit.android.util.FragmentUtils
import org.mtransit.android.util.UIFeatureFlags

object BillingUtils {

    @JvmStatic
    fun showPurchaseDialog(activity: FragmentActivity?) {
        if (UIFeatureFlags.F_NEW_IN_APP_SUBS) {
            activity?.startActivity(PurchaseActivity.newInstance(activity))
            return
        }
        FragmentUtils.replaceDialogFragment(activity, FragmentUtils.DIALOG_TAG, PurchaseDialogFragment.newInstance(), null)
    }
}
