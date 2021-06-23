package org.mtransit.android.billing

import android.app.Activity
import org.mtransit.android.ui.fragment.PurchaseDialogFragment
import org.mtransit.android.util.FragmentUtils

object BillingUtils {

    @JvmStatic
    fun showPurchaseDialog(activity: Activity?) {
        @Suppress("DEPRECATION") // TODO migrate to AndroidX Fragment Dialog
        FragmentUtils.replaceDialogFragment(activity, FragmentUtils.DIALOG_TAG, PurchaseDialogFragment.newInstance(), null)
    }
}