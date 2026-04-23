package org.mtransit.android.ad.rewarded

import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardItem
// import com.google.android.libraries.ads.mobile.sdk.rewarded.OnUserEarnedRewardListener #gmaNextGen
// import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem #gmaNextGen
import org.mtransit.android.ad.AdConstants.logAdsD
import org.mtransit.android.ad.AdManager
import org.mtransit.android.ad.GlobalAdManager
import org.mtransit.android.commons.MTLog
import org.mtransit.android.ui.view.common.IActivity
import java.lang.ref.WeakReference

class RewardedAdOnUserEarnedRewardListener(
    private val globalAdManager: GlobalAdManager,
    private val activityWR: WeakReference<IActivity>,
) : OnUserEarnedRewardListener, MTLog.Loggable {

    constructor(
        globalAdManager: GlobalAdManager,
        activity: IActivity,
    ) : this(
        globalAdManager,
        WeakReference(activity),
    )

    companion object {
        private val LOG_TAG = "${AdManager.LOG_TAG}>${RewardedAdOnUserEarnedRewardListener::class.java.simpleName}"
    }

    override fun getLogTag() = LOG_TAG

    override fun onUserEarnedReward(reward: RewardItem) {
        logAdsD(this, "onUserEarnedReward() > User earned reward from ad $reward.")
        this.globalAdManager.rewardUser(this.globalAdManager.getRewardedAdAmountInMs(), this.activityWR.get())
    }
}
