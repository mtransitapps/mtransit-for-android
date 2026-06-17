package org.mtransit.android.ui.view.listfooter

import android.content.Context
import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import org.mtransit.android.R
import org.mtransit.android.ad.IAdManager
import org.mtransit.android.ad.IAdScreenActivity
import org.mtransit.android.analytics.IAnalyticsManager
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
    private val analyticsManager: IAnalyticsManager,
    private val demoModeManager: DemoModeManager,
    private val billingManager: IBillingManager,
    private val dataSourcesRepository: DataSourcesRepository,
    private val getFragment: () -> ABFragment?,
    private val getShowLoading: () -> Boolean,
    private val getHideText: () -> Boolean = { false },
    private val canShowRewardedAd: () -> Boolean = { adManager.isRewardedAdAvailableToShow() },
) : POIListFooterManager, MTLog.Loggable {

    companion object {

        private val LOG_TAG: String = DefaultPOIListFooterManager::class.java.simpleName

        fun getMinListItemToNotHide(context: Context): Int = context.resources.getInteger(R.integer.footer_text_list_min_item)

        private const val SHOW_SUPPORT_INSTEAD_OF_REWARDED_AD_PCT = 30 // 30% support | 70% rewarded

        @JvmStatic
        fun observe(
            lifecycleOwner: LifecycleOwner,
            mainListLivedata: LiveData<*>,
            billingManager: IBillingManager,
            dataSourcesRepository: DataSourcesRepository,
            onChanged: () -> Unit,
        ) {
            mainListLivedata.observe(lifecycleOwner) {
                onChanged()
            }
            billingManager.hasSubscription.observe(lifecycleOwner) {
                onChanged()
            }
            dataSourcesRepository.readingHasAgenciesEnabled().observe(lifecycleOwner) {
                onChanged()
            }
        }
    }

    override fun getLogTag() = LOG_TAG

    private val pickShowSupportInsteadOfRewardedAd: Boolean by lazy {
        Random.nextInt(100) < SHOW_SUPPORT_INSTEAD_OF_REWARDED_AD_PCT
    }

    @Volatile
    private var _showSupportInsteadOfRewardedAd: Boolean? = null

    private var showSupportInsteadOfRewardedAd: Boolean
        get() = _showSupportInsteadOfRewardedAd ?: pickShowSupportInsteadOfRewardedAd.also { _showSupportInsteadOfRewardedAd = it }
        set(value) {
            _showSupportInsteadOfRewardedAd = value
        }

    override val isShowLoading get() = getShowLoading()

    override val isShowText
        get() =
            dataSourcesRepository.hasAgenciesEnabled()
                    && billingManager.hasSubscription.value != true
                    && !demoModeManager.isFullDemo()
                    && !getHideText()

    override val text: CharSequence?
        get() =
            if (!isShowText) {
                null
            } else if (canShowRewardedAd() && !showSupportInsteadOfRewardedAd) {
                MTLog.d(LOG_TAG, "adManager.rewardedAdAmountInDays: ${adManager.rewardedAdAmountInDays}")
                getFragment()?.context?.resources?.getQuantityText(
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
            this.analyticsManager.trackButtonClick("list_footer_rewarded_ad", getFragment())
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
            this.analyticsManager.trackButtonClick("list_footer_support", getFragment())
            getFragment()?.activity?.let { BillingUtils.showPurchaseDialog(it) }
        }
    }
}