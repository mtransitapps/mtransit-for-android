package org.mtransit.android.ad.rewarded

import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardItem
import org.mtransit.android.ad.AdManager
import org.mtransit.android.ad.GlobalAdManager
import org.mtransit.android.common.IContext
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.MTLog.Loggable
import java.lang.ref.WeakReference

class RewardedAdOnUserEarnedRewardListener(
    private val globalAdManager: GlobalAdManager,
    private val contextWR: WeakReference<IContext>,
) : OnUserEarnedRewardListener, Loggable {

    constructor(
        globalAdManager: GlobalAdManager,
        context: IContext,
    ) : this(
        globalAdManager,
        WeakReference(context),
    )

    companion object {
        private val LOG_TAG = "${AdManager.LOG_TAG}>${RewardedAdOnUserEarnedRewardListener::class.java.simpleName}"
    }

    override fun getLogTag() = LOG_TAG

    override fun onUserEarnedReward(rewardItem: RewardItem) {
        MTLog.d(this, "onUserEarnedReward() > User earned reward from ad %s.", rewardItem)
        this.globalAdManager.rewardUser(this.globalAdManager.getRewardedAdAmountInMs(), this.contextWR.get())
    }
}
