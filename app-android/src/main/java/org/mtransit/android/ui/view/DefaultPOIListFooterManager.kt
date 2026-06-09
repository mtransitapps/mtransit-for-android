package org.mtransit.android.ui.view

import android.view.View
import org.mtransit.android.R
import org.mtransit.android.ad.IAdManager
import org.mtransit.android.ad.IAdScreenActivity
import org.mtransit.android.billing.BillingUtils
import org.mtransit.android.billing.IBillingManager
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.ToastUtils
import org.mtransit.android.commons.getQuantityText
import org.mtransit.android.data.POIListFooterManager
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.dev.DemoModeManager
import org.mtransit.android.ui.fragment.ABFragment
import kotlin.random.Random

class DefaultPOIListFooterManager(
    private val adManager: IAdManager,
    private val demoModeManager: DemoModeManager,
    private val billingManager: IBillingManager,
    private val dataSourcesRepository: DataSourcesRepository,
    private val getFragment: () -> ABFragment?,
    private val getShowLoading: () -> Boolean,
    private val canShowRewardedAd: () -> Boolean = { adManager.isRewardedAdAvailableToShow() },
) : POIListFooterManager, MTLog.Loggable {

    companion object {

        private val LOG_TAG: String = DefaultPOIListFooterManager::class.java.simpleName

        private const val SHOW_SUPPORT_INSTEAD_OF_REWARDED_AD_PCT = 50 // 50% support | 50% rewarded
    }

    override fun getLogTag() = LOG_TAG

    @Volatile
    private var _showSupportInsteadOfRewardedAd: Boolean? = null

    private var showSupportInsteadOfRewardedAd: Boolean
        get() {
            if (_showSupportInsteadOfRewardedAd == null) {
                _showSupportInsteadOfRewardedAd = Random.nextInt(1, 100) > SHOW_SUPPORT_INSTEAD_OF_REWARDED_AD_PCT
            }
            return _showSupportInsteadOfRewardedAd ?: false
        }
        set(value) {
            _showSupportInsteadOfRewardedAd = value
        }

    override val isShowLoading get() = getShowLoading()
        // attachedViewModel?.loadingPOIs?.value == true

    override val isShowText
        get() =
            dataSourcesRepository.hasAgenciesEnabled()
                    && billingManager.hasSubscription.value != true
                    && !demoModeManager.isFullDemo()

    override val text: CharSequence?
        get() =
            if (!isShowText) {
                null
            } else if (canShowRewardedAd() && !showSupportInsteadOfRewardedAd) {
                MTLog.d(LOG_TAG, "adManager.rewardedAdAmountInDays: ${adManager.rewardedAdAmountInDays}")
                getFragment()?.resources?.getQuantityText(
                    if (adManager.isRewardedNow()) R.plurals.watch_rewarded_ad_btn_more_and_days_formatted
                    else R.plurals.watch_rewarded_ad_btn_and_days_formatted,
                    adManager.rewardedAdAmountInDays,
                    adManager.rewardedAdAmountInDays
                )
            } else {
                showSupportInsteadOfRewardedAd = true
                getFragment()?.context?.getString(R.string.support)
            }

    override val textStartDrawableRes: Int?
        get() = if (!isShowText) {
            null
        } else if (canShowRewardedAd() && !showSupportInsteadOfRewardedAd) {
            R.drawable.ic_on_demand_video_black_24
        } else {
            showSupportInsteadOfRewardedAd = true
            R.drawable.ic_volunteer_activism_black_24
        }

    override val onTextClickListener = View.OnClickListener {
        if (!isShowText) {
            return@OnClickListener
        } else if (!showSupportInsteadOfRewardedAd) { // rewarded ad
            if (!adManager.isRewardedAdAvailableToShow()) {
                MTLog.w(LOG_TAG, "footer.onTextClick() > skip (no ad available)")
                ToastUtils.makeTextAndShow(getFragment()?.context, R.string.support_watch_rewarded_ad_not_ready)
                return@OnClickListener
            }
            (getFragment()?.activity as? IAdScreenActivity)?.let { adManager.showRewardedAd(it) }
                ?: run {
                    MTLog.w(LOG_TAG, "onRewardedAdButtonClick() > skip (no view or no activity)")
                    ToastUtils.makeTextAndShow(getFragment()?.context, R.string.support_watch_rewarded_ad_default_failure_message)
                    return@OnClickListener
                }
        } else { // support
            getFragment()?.activity?.let { BillingUtils.showPurchaseDialog(it) }
        }
    }
}
