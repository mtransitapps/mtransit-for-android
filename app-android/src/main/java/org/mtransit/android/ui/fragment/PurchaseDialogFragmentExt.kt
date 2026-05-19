package org.mtransit.android.ui.fragment

import android.annotation.SuppressLint
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.mtransit.android.ad.IAdManager

@SuppressLint("DeprecatedCall") // FIXME migrate fragment to AndroidX
@Suppress("DEPRECATION") // FIXME migrate fragment to AndroidX
fun PurchaseDialogFragment.onResumeKt(iAdManager: IAdManager) {
    val parentActivity = this.activity as? FragmentActivity ?: return
    parentActivity.lifecycleScope.launch {
        if (!isAdded) return@launch
        iAdManager.refreshRewardedAdStatus(this@onResumeKt)
    }
}
