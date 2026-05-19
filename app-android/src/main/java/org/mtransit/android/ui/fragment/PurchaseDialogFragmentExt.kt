package org.mtransit.android.ui.fragment

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mtransit.android.ad.IAdManager

fun PurchaseDialogFragment.onResumeKt(iAdManager: IAdManager) {
    @Suppress("DEPRECATION") // FIXME migrate fragment to AndroidX
    val parentActivity = this.activity as? FragmentActivity ?: return
    parentActivity.lifecycleScope.launch(Dispatchers.IO) {
        iAdManager.refreshRewardedAdStatus(this@onResumeKt)
    }
}
