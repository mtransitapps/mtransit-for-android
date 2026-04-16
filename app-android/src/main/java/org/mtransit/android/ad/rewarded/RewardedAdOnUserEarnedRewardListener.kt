package org.mtransit.android.ad.rewarded

import com.google.android.libraries.ads.mobile.sdk.rewarded.OnUserEarnedRewardListener
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem
import org.mtransit.android.ad.AdConstants.logAdsD
import org.mtransit.android.ad.AdManager
import org.mtransit.android.ad.GlobalAdManager
import org.mtransit.android.common.IContext
import org.mtransit.android.commons.MTLog
import java.lang.ref.WeakReference

class RewardedAdOnUserEarnedRewardListener(
    private val globalAdManager: GlobalAdManager,
    private val contextWR: WeakReference<IContext>,
) : OnUserEarnedRewardListener, MTLog.Loggable {

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

    override fun onUserEarnedReward(reward: RewardItem) {
        logAdsD(this, "onUserEarnedReward() > User earned reward from ad $reward.")
        this.globalAdManager.rewardUser(this.globalAdManager.getRewardedAdAmountInMs(), this.contextWR.get())
    }
}
