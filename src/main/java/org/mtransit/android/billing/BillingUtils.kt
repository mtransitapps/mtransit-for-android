package org.mtransit.android.billing

import android.app.Activity
import org.mtransit.android.ui.fragment.PurchaseDialogFragment
import org.mtransit.android.util.FragmentUtils

object BillingUtils {

    @JvmStatic
    fun showPurchaseDialog(activity: Activity?) {
        FragmentUtils.replaceDialogFragment(activity, FragmentUtils.DIALOG_TAG, PurchaseDialogFragment.newInstance(), null)
    }
}