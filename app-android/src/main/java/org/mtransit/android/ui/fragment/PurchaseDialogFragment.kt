package org.mtransit.android.ui.fragment

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.collection.ArrayMap
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetails.PricingPhase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.mtransit.android.R
import org.mtransit.android.ad.IAdManager
import org.mtransit.android.billing.IBillingManager
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.PackageManagerUtils
import org.mtransit.android.commons.StoreUtils
import org.mtransit.android.commons.ThreadSafeDateFormatter
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.ToastUtils
import org.mtransit.android.databinding.FragmentDialogPurchaseBinding
import org.mtransit.android.ui.MTActivity
import org.mtransit.android.ui.view.common.IActivity
import org.mtransit.android.ui.view.common.isVisible
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import org.mtransit.android.commons.R as commonsR

@AndroidEntryPoint
class PurchaseDialogFragment : MTDialogFragmentX(),
    IAdManager.RewardedAdListener,
    MTLog.Loggable {

    companion object {

        private val LOG_TAG: String = PurchaseDialogFragment::class.java.simpleName

        @Suppress("SpellCheckingInspection")
        private const val PAID_TASKS_PKG = "com.google.android.apps.paidtasks"

        @JvmStatic
        fun newInstance(): PurchaseDialogFragment {
            return PurchaseDialogFragment()
        }

        private val SORTED_PERIOD_CAT = listOf(
            IBillingManager.WEEKLY,
            IBillingManager.MONTHLY,
            IBillingManager.YEARLY
        )

        private val PERIOD_CAT_TO_RES_ID = mapOf(
            IBillingManager.WEEKLY to R.string.support_every_week,
            IBillingManager.MONTHLY to R.string.support_every_month,
            IBillingManager.YEARLY to R.string.support_every_year,
        )
    }

    override fun getLogTag() = LOG_TAG

    @Inject
    lateinit var billingManager: IBillingManager

    @Inject
    lateinit var adManager: IAdManager

    private var binding: FragmentDialogPurchaseBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_dialog_purchase, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentDialogPurchaseBinding.bind(view).apply {
            buyBtn.setOnClickListener { onBuyBtnClick(it.context) }
            downloadOrOpenPaidTasksBtn.setOnClickListener { onDownloadOrOpenPaidTasksBtnClick(it.context) }
            rewardedAdsBtn.setOnClickListener { onRewardedAdButtonClick(it.context) }
        }
        billingManager.productIdsWithDetails.observe(viewLifecycleOwner) {
            onNewProductId(it)
        }
        adManager.rewardedUntilInMsLive.observe(viewLifecycleOwner) {
            refreshRewardedLayout()
        }
        adManager.rewardedNowLive.observe(viewLifecycleOwner) {
            refreshRewardedLayout()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(true)
            setCanceledOnTouchOutside(true)
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        ToastUtils.makeTextAndShow(parentActivity, R.string.support_subs_user_canceled_message)
    }

    val parentActivity: MTActivity? get() = super.getActivity() as? MTActivity

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun onRewardedAdButtonClick(context: Context) {
        try {
            val activity = parentActivity ?: run {
                MTLog.w(this, "onRewardedAdButtonClick() > skip (no view or no activity)")
                ToastUtils.makeTextAndShow(context, R.string.support_watch_rewarded_ad_default_failure_message)
                return
            }
            if (!adManager.isRewardedAdAvailableToShow()) {
                MTLog.w(this, "onRewardedAdButtonClick() > skip (no ad available)")
                ToastUtils.makeTextAndShow(context, R.string.support_watch_rewarded_ad_not_ready)
                return
            }
            adManager.showRewardedAd(activity)
            binding?.rewardedAdsBtn?.isEnabled = false
        } catch (e: Exception) {
            MTLog.w(this, e, "Error while handling download or open paid tasks button!")
            ToastUtils.makeTextAndShow(context, R.string.support_watch_rewarded_ad_default_failure_message)
        }
    }

    private fun onDownloadOrOpenPaidTasksBtnClick(context: Context) {
        val activity = parentActivity
        try {
            if (activity == null) {
                MTLog.w(this, "onDownloadOrOpenPaidTasksBtnClick() > skip (no view or no activity)")
                ToastUtils.makeTextAndShow(context, R.string.support_subs_default_failure_message)
                return
            }
            if (PackageManagerUtils.isAppInstalled(context, PAID_TASKS_PKG)) {
                ToastUtils.makeTextAndShow(
                    context,
                    context.getString(
                        commonsR.string.opening_and_label,
                        context.getString(R.string.support_paid_tasks_incentive_app_label)
                    )
                )
                PackageManagerUtils.openApp(context, PAID_TASKS_PKG)
            } else {
                StoreUtils.viewAppPage(
                    activity,
                    PAID_TASKS_PKG,
                    context.getString(commonsR.string.google_play)
                )
            }
            dialog?.dismiss()
        } catch (e: Exception) {
            MTLog.w(this, e, "Error while handling download or open paid tasks button!")
            ToastUtils.makeTextAndShow(context, R.string.support_subs_default_failure_message)
        }
    }

    private fun onBuyBtnClick(context: Context) {
        val activity = parentActivity as? IActivity
        val binding = binding
        try {
            if (binding == null || activity == null) {
                MTLog.w(this, "onBuyBtnClick() > skip (no view or no activity)")
                ToastUtils.makeTextAndShow(context, R.string.support_subs_default_failure_message)
                return
            }
            val periodPosition = binding.period.selectedItemPosition
            val periodS = this.periods[periodPosition]
            if (periodS.isEmpty()) {
                MTLog.w(this, "onBuyBtnClick() > skip (unexpected period position: %s)", periodPosition)
                ToastUtils.makeTextAndShow(context, R.string.support_subs_default_failure_message)
                return
            }
            val periodCat = this.periodSToPeriodCat[periodS]
            if (periodCat.isNullOrEmpty()) {
                MTLog.w(this, "onBuyBtnClick() > skip (unexpected period string: %s)", periodS)
                ToastUtils.makeTextAndShow(context, R.string.support_subs_default_failure_message)
                return
            }
            val pricePosition = binding.price.selectedItemPosition
            val priceS = this.prices[pricePosition]
            if (priceS.isEmpty()) {
                MTLog.w(this, "onBuyBtnClick() > skip (unexpected price position: %s)", pricePosition)
                ToastUtils.makeTextAndShow(context, R.string.support_subs_default_failure_message)
                return
            }
            val priceCat = this.priceSToPriceCat[priceS]
            if (priceCat.isNullOrEmpty()) {
                MTLog.w(this, "onBuyBtnClick() > skip (unexpected price string: %s)", priceS)
                ToastUtils.makeTextAndShow(context, R.string.support_subs_default_failure_message)
                return
            }
            val productId = IBillingManager.PRODUCT_ID_STARTS_WITH_F + periodCat + IBillingManager.PRODUCT_ID_SUBSCRIPTION + priceCat
            if (!IBillingManager.FLEXIBLE_SUBSCRIPTIONS.contains(productId)) {
                MTLog.w(this, "onBuyBtnClick() > skip (unexpected product ID: %s)", productId)
                ToastUtils.makeTextAndShow(context, R.string.support_subs_default_failure_message)
                return
            }
            val billingFlowLaunched = billingManager.launchBillingFlow(activity, productId)
            if (!billingFlowLaunched) {
                MTLog.w(this, "onBuyBtnClick() > skip (can not launch billing flow for: %s)", productId)
                ToastUtils.makeTextAndShow(context, R.string.support_subs_default_failure_message)
                return
            }
            dialog?.dismiss()
        } catch (e: Exception) {
            MTLog.w(this, e, "Error while handling buy button!")
            ToastUtils.makeTextAndShow(context, R.string.support_subs_default_failure_message)
        }
    }

    private val dateFormatter = ThreadSafeDateFormatter.getDateInstance(ThreadSafeDateFormatter.MEDIUM)

    @MainThread
    override fun onResume() {
        super.onResume()
        billingManager.refreshAvailableSubscriptions()
        adManager.setRewardedAdListener(this)
        parentActivity?.let { adManager.linkRewardedAd(it) }
        refreshRewardedAdStatusAsync()
        showLoading()
        binding?.apply {
            downloadOrOpenPaidTasksBtn.setText(
                if (PackageManagerUtils.isAppInstalled(root.context, PAID_TASKS_PKG)) {
                    R.string.support_paid_tasks_incentive_open_btn
                } else {
                    R.string.support_paid_tasks_incentive_download_btn
                }
            )
            refreshRewardedLayout()
        }
    }

    private fun refreshRewardedAdStatusAsync() {
        viewLifecycleOwner.lifecycleScope.launch {
            val activity = parentActivity as? IActivity ?: return@launch
            adManager.refreshRewardedAdStatus(activity)
        }
    }

    override fun onPause() {
        super.onPause()
        adManager.setRewardedAdListener(null)
    }

    @MainThread
    private fun refreshRewardedLayout() {
        val binding = binding ?: return
        val rewardedNow = adManager.rewardedNowLive.value
        if (rewardedNow == null) {
            MTLog.w(this, "refreshRewardedLayout() > skip (rewardedNow is null)")
            return
        }
        val rewardedUntilInMs = adManager.rewardedUntilInMsLive.value
        if (rewardedUntilInMs == null) {
            MTLog.w(this, "refreshRewardedLayout() > skip (rewardedUntilInMs is null)")
            return
        }
        val availableToShow = adManager.isRewardedAdAvailableToShow()
        val rewardedAmount = adManager.getRewardedAdAmount()

        binding.paidTasksDivider2.isVisible = availableToShow || rewardedNow

        if (rewardedNow) {
            binding.rewardedAdText.text = getString(
                R.string.support_watch_rewarded_ad_status_until_and_date,
                this.dateFormatter.formatThreadSafe(rewardedUntilInMs)
            )
            binding.rewardedAdText.isVisible = true
        } else {
            binding.rewardedAdText.isVisible = false
            binding.rewardedAdText.text = null
        }

        binding.rewardedAdsBtn.text = resources.getQuantityString(
            if (rewardedNow) {
                R.plurals.support_watch_rewarded_ad_btn_more_and_days
            } else {
                R.plurals.support_watch_rewarded_ad_btn_and_days
            },
            rewardedAmount,
            rewardedAmount
        )
        if (availableToShow) {
            binding.rewardedAdsBtn.isEnabled = true
            binding.rewardedAdsBtn.isVisible = true
        } else {
            binding.rewardedAdsBtn.isEnabled = false
        }
    }

    @WorkerThread
    override fun skipRewardedAd(): Boolean {
        if (!adManager.isRewardedNow()) return false
        val rewardedUntilInMs = adManager.getRewardedUntilInMs()
        val skipRewardedAdUntilInMs =
            TimeUtils.currentTimeMillis() - TimeUnit.HOURS.toMillis(1L) + adManager.getRewardedAdAmount() * adManager.getRewardedAdAmountInMs()
        return rewardedUntilInMs > skipRewardedAdUntilInMs
    }

    @AnyThread
    override fun onRewardedAdStatusChanged() {
        binding?.root?.post { refreshRewardedLayout() }
    }

    override fun onDetach() {
        super.onDetach()
        parentActivity?.let { adManager.unlinkRewardedAd(it) }
    }

    private fun showLoading() = binding?.apply {
        title.isVisible = false
        subTitle.isVisible = false
        beforeText.isVisible = false
        priceSelection.isVisible = false
        afterText.isVisible = false
        buyBtn.isVisible = false
        paidTasksDivider.isVisible = false
        paidTasksIncentive.isVisible = false
        downloadOrOpenPaidTasksBtn.isVisible = false
        loadingLayout.isVisible = true
    }

    private fun showNotLoading() = binding?.apply {
        loadingLayout.isVisible = false
        title.isVisible = true
        subTitle.isVisible = true
        beforeText.isVisible = true
        priceSelection.isVisible = true
        afterText.isVisible = true
        buyBtn.isVisible = true
        paidTasksDivider.isVisible = true
        paidTasksIncentive.isVisible = true
        downloadOrOpenPaidTasksBtn.isVisible = true
    }

    private val prices = ArrayList<String>()
    private val priceSToPriceCat = ArrayMap<String, String>()
    private val periods = ArrayList<String>()
    private val periodSToPeriodCat = ArrayMap<String, String>()

    private fun onNewProductId(productIdsWithDetails: Map<String, ProductDetails>?) {
        productIdsWithDetails ?: return
        val binding = binding ?: return
        val activity = parentActivity ?: return
        this.prices.clear()
        this.periods.clear()
        this.priceSToPriceCat.clear()
        this.periodSToPeriodCat.clear()
        var defaultPriceS: String? = null
        var defaultPeriodS: String? = null
        productIdsWithDetails.forEach { (productId, productDetails) ->
            if (!productId.startsWith(IBillingManager.PRODUCT_ID_STARTS_WITH_F)) {
                MTLog.w(this, "Skip product ID %s (unexpected)", productId)
                return@forEach
            }
            val periodCat = productId.substring(
                IBillingManager.PRODUCT_ID_STARTS_WITH_F.length,
                productId.indexOf(IBillingManager.PRODUCT_ID_SUBSCRIPTION, IBillingManager.PRODUCT_ID_STARTS_WITH_F.length)
            )
            val periodResourceId = PERIOD_CAT_TO_RES_ID[periodCat]
            if (periodResourceId == null) {
                MTLog.w(this, "Skip product ID %s (unknown periodCat: %s)", productId, periodCat)
                return@forEach
            }
            val priceCat = productId.substring(
                productId.indexOf(IBillingManager.PRODUCT_ID_SUBSCRIPTION) + IBillingManager.PRODUCT_ID_SUBSCRIPTION.length
            )
            val subOfferDetailsList = productDetails.subscriptionOfferDetails
            if (subOfferDetailsList.isNullOrEmpty()) {
                MTLog.w(this, "Skip product ID %s (no offer details)", productId)
                return@forEach
            }
            if (subOfferDetailsList.size <= IBillingManager.OFFER_DETAILS_IDX) {
                MTLog.w(this, "Skip product ID %s (no offer details item)", productId)
                return@forEach
            }
            val subOfferDetails = subOfferDetailsList[IBillingManager.OFFER_DETAILS_IDX]
            val pricingPhaseList: List<PricingPhase> = subOfferDetails.pricingPhases.pricingPhaseList
            if (pricingPhaseList.isEmpty()) {
                MTLog.w(this, "Skip product ID %s (no pricing list)", productId)
                return@forEach
            }
            val lastPricingPhase = pricingPhaseList[pricingPhaseList.size - 1]
            val priceS = lastPricingPhase.formattedPrice
            this.priceSToPriceCat[priceS] = priceCat
            if (!this.prices.contains(priceS)) {
                this.prices.add(priceS)
            }
            val periodS = activity.getString(periodResourceId)
            if (!this.periods.contains(periodS)) {
                this.periods.add(periodS)
            }
            this.periodSToPeriodCat[periodS] = periodCat
            if (IBillingManager.DEFAULT_PRICE_CAT == priceCat) {
                defaultPriceS = priceS
            }
            if (IBillingManager.DEFAULT_PERIOD_CAT == periodCat) {
                defaultPeriodS = periodS
            }
        }
        this.periods.sortWith { lPeriodS, rPeriodS ->
            try {
                val leftIndex = SORTED_PERIOD_CAT.indexOf(this.periodSToPeriodCat[lPeriodS])
                val rightIndex = SORTED_PERIOD_CAT.indexOf(this.periodSToPeriodCat[rPeriodS])
                leftIndex - rightIndex
            } catch (e: Exception) {
                MTLog.w(LOG_TAG, e, "Error while sorting periods!")
                0
            }
        }
        this.prices.sortWith { lPriceS, rPriceS ->
            try {
                val leftIndex = this.priceSToPriceCat[lPriceS]?.toIntOrNull() ?: -1
                val rightIndex = this.priceSToPriceCat[rPriceS]?.toIntOrNull() ?: -1
                leftIndex - rightIndex
            } catch (e: Exception) {
                MTLog.w(LOG_TAG, e, "Error while sorting prices!")
                0
            }
        }
        binding.price.adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, this.prices)
        defaultPriceS?.let {
            binding.price.setSelection(this.prices.indexOf(it))
        }
        binding.period.adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, this.periods)
        defaultPeriodS?.let {
            binding.period.setSelection(this.periods.indexOf(it))
        }
        showNotLoading()
    }
}
