package org.mtransit.android.ad.rewarded

import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardItem
import org.mtransit.android.ad.AdManager
import org.mtransit.android.ad.GlobalAdManager
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.MTLog.Loggable
import org.mtransit.android.ui.view.common.IActivity
import java.lang.ref.WeakReference

class RewardedAdOnUserEarnedRewardListener(
    private val globalAdManager: GlobalAdManager,
    private val activityWR: WeakReference<IActivity>,
) : OnUserEarnedRewardListener, Loggable {

    constructor(
        globalAdManager: GlobalAdManager,
        activity: IActivity,
    ) : this(
        globalAdManager,
        WeakReference<IActivity>(activity),
    )

    companion object {
        private val LOG_TAG = "${AdManager.LOG_TAG}>${RewardedAdOnUserEarnedRewardListener::class.java.simpleName}"
    }

    override fun getLogTag() = LOG_TAG

    override fun onUserEarnedReward(rewardItem: RewardItem) {
        MTLog.d(this, "onUserEarnedReward() > User earned reward from ad %s.", rewardItem)
        val activity = this.activityWR.get()
        this.globalAdManager.rewardUser(this.globalAdManager.getRewardedAdAmountInMs(), activity)
    }
}
