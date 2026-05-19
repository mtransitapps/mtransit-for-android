package org.mtransit.android.ui.fragment

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mtransit.android.ad.IAdManager

fun PurchaseDialogFragment.onResumeKt(iAdManager: IAdManager) {
    this.lifecycleScope.launch(Dispatchers.IO) {
        iAdManager.refreshRewardedAdStatus(this@onResumeKt)
    }
}
