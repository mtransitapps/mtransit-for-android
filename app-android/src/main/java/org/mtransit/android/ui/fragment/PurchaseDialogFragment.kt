package org.mtransit.android.ui.fragment

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.RadioGroup
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.ProductDetails
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import org.mtransit.android.R
import org.mtransit.android.ad.IAdManager
import org.mtransit.android.billing.IBillingManager
import org.mtransit.android.billing.billingDatePeriod
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.PackageManagerUtils
import org.mtransit.android.commons.StoreUtils
import org.mtransit.android.commons.TimeUtilsK
import org.mtransit.android.commons.ToastUtils
import org.mtransit.android.commons.getQuantityText
import org.mtransit.android.databinding.FragmentDialogPurchaseBinding
import org.mtransit.android.ui.MTActivity
import org.mtransit.android.ui.view.common.context
import org.mtransit.android.ui.view.common.isVisible
import org.mtransit.android.ui.view.common.textAndVisibility
import org.mtransit.commons.weeks
import javax.inject.Inject
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
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
        fun newInstance() =
            PurchaseDialogFragment()

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

        private val PRODUCT_ID_REGEX = Regex(
            buildString {
                append("^")
                append(Regex.escape(IBillingManager.PRODUCT_ID_STARTS_WITH_F))
                append("(${Regex.escape(IBillingManager.WEEKLY)}|${Regex.escape(IBillingManager.MONTHLY)}|${Regex.escape(IBillingManager.YEARLY)})")
                append(Regex.escape(IBillingManager.PRODUCT_ID_SUBSCRIPTION))
                append("(\\d+)")
                append("$")
            }
        )
        private const val PRODUCT_ID_REGEX_GROUP_PERIOD = 1
        private const val PRODUCT_ID_REGEX_GROUP_PRICE = 2
    }

    override fun getLogTag() = LOG_TAG

    @Inject
    lateinit var billingManager: IBillingManager

    @Inject
    lateinit var adManager: IAdManager

    private var binding: FragmentDialogPurchaseBinding? = null

    private val onCheckedChangeListener = RadioGroup.OnCheckedChangeListener { _, checkedId ->
        onPriceOrPeriodSelectionChanged()
    }

    private fun onPriceOrPeriodSelectionChanged() {
        binding?.apply {
            val periodCat = periodRadioGroup.checkedRadioButtonId.takeIf { it != View.NO_ID }
                ?.let { radioButtonResId ->
                    when (radioButtonResId) {
                        periodWeekly.id -> IBillingManager.WEEKLY
                        periodMonthly.id -> IBillingManager.MONTHLY
                        periodYearly.id -> IBillingManager.YEARLY
                        else -> null
                    }
                }
            val priceCat = price.selectedItemPosition.takeIf { it != AdapterView.INVALID_POSITION }
                ?.let { pricePosition ->
                    pricesFormatted.getOrNull(pricePosition)?.takeIf { it.isNotEmpty() }
                }?.let { priceFormatted ->
                    priceFormattedToPriceCat[priceFormatted]?.takeIf { it.isNotEmpty() }
                }
            val productId = if (periodCat == null || priceCat == null) null else periodAndPriceCatToProductId[periodCat to priceCat]
            val trial = productId?.let { productIdToFreePeriod[it] }
            buyBtn.text = trial?.let { trial ->
                when {
                    trial.months > 0 -> context.resources.getQuantityString(R.plurals.support_subs_start_trial_and_months, trial.months, trial.months)
                    trial.weeks > 0 -> context.resources.getQuantityString(R.plurals.support_subs_start_trial_and_weeks, trial.weeks, trial.weeks)
                    trial.days > 0 -> context.resources.getQuantityString(R.plurals.support_subs_start_trial_and_days, trial.days, trial.days)
                    else -> context.getString(R.string.support_subs_start_trial) // unexpected trial duration
                }
            } ?: context.getString(R.string.support_subs_buy_with_play) // no trial? (trial already used)
            buyBtn.isEnabled = !productId.isNullOrBlank()
        }
    }

    private val onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            onPriceOrPeriodSelectionChanged()
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            onPriceOrPeriodSelectionChanged()
        }
    }

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
            paidTasksIncentive.text = context.getText(R.string.support_link_paid_tasks_incentive_formatted)
            closeButton.setOnClickListener { dialog?.cancel() ?: dismiss() }
            buyBtn.setOnClickListener { onBuyBtnClick(it.context) }
            subTitle.setOnClickListener { onSubTitleClick(it.context) }
            paidTasksIncentive.setOnClickListener { onDownloadOrOpenPaidTasksBtnClick(it.context) }
            rewardedAdsText.setOnClickListener { onRewardedAdButtonClick(it.context) }
            price.onItemSelectedListener = onItemSelectedListener
            periodRadioGroup.setOnCheckedChangeListener(onCheckedChangeListener)
        }
        billingManager.productIdsWithDetails.observe(viewLifecycleOwner) {
            onProductIdsLoaded(it)
        }
        adManager.rewardedUntilLive.observe(viewLifecycleOwner) {
            refreshRewardedLayout()
        }
        adManager.rewardedNowLive.observe(viewLifecycleOwner) {
            refreshRewardedLayout()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) =
        super.onCreateDialog(savedInstanceState).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(true)
            setCanceledOnTouchOutside(true)
        }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        ToastUtils.makeTextAndShow(context, R.string.support_subs_user_canceled_message)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        // DO NOTHING
    }

    val parentActivity: MTActivity? get() = activity as? MTActivity

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun onRewardedAdButtonClick(context: Context) {
        try {
            val parentActivity = parentActivity ?: run {
                MTLog.w(this, "onRewardedAdButtonClick() > skip (no view or no activity)")
                ToastUtils.makeTextAndShow(context, R.string.support_watch_rewarded_ad_default_failure_message)
                return
            }
            if (!adManager.isRewardedAdAvailableToShow()) {
                MTLog.w(this, "onRewardedAdButtonClick() > skip (no ad available)")
                ToastUtils.makeTextAndShow(context, R.string.support_watch_rewarded_ad_not_ready)
                return
            }
            adManager.showRewardedAd(parentActivity)
            binding?.rewardedAdsText?.isEnabled = false
        } catch (e: Exception) {
            MTLog.w(this, e, "Error while handling rewarded ad click/show!")
            ToastUtils.makeTextAndShow(context, R.string.support_watch_rewarded_ad_default_failure_message)
        }
    }

    private fun onDownloadOrOpenPaidTasksBtnClick(context: Context) {
        try {
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
                    context,
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

    private fun onSubTitleClick(context: Context) {
        binding?.apply {
            if (subTitle.text == context.getString(R.string.support_about)) {
                subTitle.text = context.getString(R.string.support_about_short_w_more)
            } else {
                subTitle.text = context.getString(R.string.support_about)
            }
        }
    }

    private fun onBuyBtnClick(context: Context) {
        val parentActivity = parentActivity
        val binding = binding
        try {
            if (binding == null || parentActivity == null) {
                MTLog.w(this, "onBuyBtnClick() > skip (no view or no activity)")
                ToastUtils.makeTextAndShow(context, R.string.support_subs_default_failure_message)
                return
            }
            val periodCat = binding.periodRadioGroup.checkedRadioButtonId.takeIf { it != View.NO_ID }
                ?.let { radioButtonResId ->
                    when (radioButtonResId) {
                        binding.periodWeekly.id -> IBillingManager.WEEKLY
                        binding.periodMonthly.id -> IBillingManager.MONTHLY
                        binding.periodYearly.id -> IBillingManager.YEARLY
                        else -> null
                    }
                } ?: run {
                MTLog.w(this, "onBuyBtnClick() > skip (unexpected period radio button: ${binding.periodRadioGroup.checkedRadioButtonId})")
                ToastUtils.makeTextAndShow(context, R.string.support_subs_default_failure_message)
                return
            }
            val pricePosition = binding.price.selectedItemPosition
            val priceFormatted = this.pricesFormatted.getOrNull(pricePosition)?.takeIf { it.isNotEmpty() } ?: run {
                MTLog.w(this, "onBuyBtnClick() > skip (unexpected price position: $pricePosition)")
                ToastUtils.makeTextAndShow(context, R.string.support_subs_default_failure_message)
                return
            }
            val priceCat = this.priceFormattedToPriceCat[priceFormatted]?.takeIf { it.isNotEmpty() } ?: run {
                MTLog.w(this, "onBuyBtnClick() > skip (unexpected formatted price: $priceFormatted)")
                ToastUtils.makeTextAndShow(context, R.string.support_subs_default_failure_message)
                return
            }
            val productId = this.periodAndPriceCatToProductId[periodCat to priceCat] ?: run {
                MTLog.w(this, "onBuyBtnClick() > skip (no product ID for period/price: $periodCat/$priceCat)")
                ToastUtils.makeTextAndShow(context, R.string.support_subs_default_failure_message)
                return
            }
            val billingFlowLaunched = billingManager.launchBillingFlow(parentActivity, productId)
            if (!billingFlowLaunched) {
                MTLog.w(this, "onBuyBtnClick() > skip (can not launch billing flow for: $productId)", productId)
                ToastUtils.makeTextAndShow(context, R.string.support_subs_default_failure_message)
                return
            }
            dialog?.dismiss()
        } catch (e: Exception) {
            MTLog.w(this, e, "Error while handling buy button!")
            ToastUtils.makeTextAndShow(context, R.string.support_subs_default_failure_message)
        }
    }

    @MainThread
    override fun onResume() {
        super.onResume()
        billingManager.refreshAvailableSubscriptions()
        adManager.setRewardedAdListener(this)
        parentActivity?.let { adManager.linkRewardedAd(it) }
        refreshRewardedAdStatusAsync()
        showLoading()
    }

    private fun refreshRewardedAdStatusAsync() {
        viewLifecycleOwner.lifecycleScope.launch {
            val parentActivity = parentActivity ?: return@launch
            adManager.refreshRewardedAdStatus(parentActivity)
        }
    }

    override fun onPause() {
        super.onPause()
        adManager.setRewardedAdListener(null)
    }

    @MainThread
    private fun refreshRewardedLayout() = binding?.apply {
        val rewardedNow = adManager.rewardedNowLive.value ?: run {
            MTLog.w(this, "refreshRewardedLayout() > skip (rewardedNow is null)")
            return@apply
        }
        val availableToShow = adManager.isRewardedAdAvailableToShow()
        val rewardedAmountInDays = adManager.rewardedAdAmountInDays
        rewardedAdsText.textAndVisibility = if (!availableToShow) null else resources.getQuantityText(
            if (rewardedNow) R.plurals.support_watch_rewarded_ad_btn_more_and_days_formatted
            else R.plurals.support_watch_rewarded_ad_btn_and_days_formatted,
            rewardedAmountInDays,
            rewardedAmountInDays
        )
        paidTasksDivider2.isVisible = rewardedAdsText.isVisible
        rewardedAdsText.isEnabled = availableToShow
    }

    @WorkerThread
    override fun skipLoadingRewardedAd(): Boolean {
        if (!adManager.isRewardedNow()) return false
        return adManager.getRewardedUntil() > TimeUtilsK.currentInstant() - 1.hours + adManager.rewardedAdAmountInDays.days
    }

    @AnyThread
    override fun onRewardedAdStatusChanged() {
        binding?.root?.post { refreshRewardedLayout() }
    }

    override fun onDetach() {
        parentActivity?.let { adManager.unlinkRewardedAd(it) }
        super.onDetach()
    }

    private fun showLoading() = binding?.apply {
        title.isVisible = false
        subTitle.isVisible = false
        beforeText.isVisible = false
        priceAndPeriodSelection.isVisible = false
        afterText.isVisible = false
        buyBtn.isVisible = false
        paidTasksDivider.isVisible = false
        paidTasksIncentive.isVisible = false
        loadingLayout.isVisible = true
    }

    private fun showNotLoading() = binding?.apply {
        loadingLayout.isVisible = false
        title.isVisible = true
        subTitle.isVisible = true
        beforeText.isVisible = true
        priceAndPeriodSelection.isVisible = true
        afterText.isVisible = true
        buyBtn.isVisible = true
        paidTasksDivider.isVisible = true
        paidTasksIncentive.isVisible = true
    }

    private val pricesFormatted = mutableListOf<String>()
    private val priceFormattedToPriceCat = mutableMapOf<String, String>()
    private val periodLabels = mutableListOf<String>()
    private val periodLabelsToPeriodCat = mutableMapOf<String, String>()
    private val periodAndPriceCatToProductId = mutableMapOf<Pair<String, String>, String>()
    private val productIdToFreePeriod = mutableMapOf<String, DatePeriod?>()

    private fun onProductIdsLoaded(productIdsWithDetails: Map<String, ProductDetails>?) {
        productIdsWithDetails ?: return
        val binding = binding ?: return
        val context = binding.context
        this.pricesFormatted.clear()
        this.periodLabels.clear()
        this.priceFormattedToPriceCat.clear()
        this.periodLabelsToPeriodCat.clear()
        this.periodAndPriceCatToProductId.clear()
        this.productIdToFreePeriod.clear()
        var defaultPriceLabel: String? = null
        productIdsWithDetails.forEach { (productId, productDetails) ->
            val productIdMatch = PRODUCT_ID_REGEX.matchEntire(productId) ?: run {
                MTLog.w(this, "Skip product ID '$productId' (unsupported)")
                return@forEach
            }
            val periodCat = productIdMatch.groups[PRODUCT_ID_REGEX_GROUP_PERIOD]?.value ?: run {
                MTLog.w(this, "Skip product ID '$productId' (missing period)")
                return@forEach
            }
            val periodResId = PERIOD_CAT_TO_RES_ID[periodCat] ?: run {
                MTLog.w(this, "Skip product ID '$productId' (unknown periodCat: $periodCat)")
                return@forEach
            }
            val priceCat = productIdMatch.groups[PRODUCT_ID_REGEX_GROUP_PRICE]?.value ?: run {
                MTLog.w(this, "Skip product ID '$productId' (missing price)")
                return@forEach
            }
            this.periodAndPriceCatToProductId[periodCat to priceCat] = productId
            val subOfferDetailsList = productDetails.subscriptionOfferDetails
            if (subOfferDetailsList.isNullOrEmpty()) {
                MTLog.w(this, "Skip product ID '$productId' (no offer details)")
                return@forEach
            }
            if (subOfferDetailsList.size <= IBillingManager.OFFER_DETAILS_IDX) {
                MTLog.w(this, "Skip product ID '$productId' (no offer details item)")
                return@forEach
            }
            val subOfferDetails = subOfferDetailsList[IBillingManager.OFFER_DETAILS_IDX]
            val pricingPhaseList = subOfferDetails.pricingPhases.pricingPhaseList
            if (pricingPhaseList.isEmpty()) {
                MTLog.w(this, "Skip product ID '$productId' (no pricing list)")
                return@forEach
            }
            val freePricingPhase = pricingPhaseList.firstOrNull { it.priceAmountMicros == 0L }
            productIdToFreePeriod[productId] = freePricingPhase?.billingDatePeriod
            binding.buyBtn.text = context.getString(R.string.support_subs_buy_with_play)
            binding.buyBtn.isEnabled = false // wait for selection
            val lastPricingPhase = pricingPhaseList.last()
            val priceFormatted = lastPricingPhase.formattedPrice
            this.priceFormattedToPriceCat[priceFormatted] = priceCat
            if (!this.pricesFormatted.contains(priceFormatted)) {
                this.pricesFormatted.add(priceFormatted)
            }
            val periodLabel = context.getString(periodResId)
            if (!this.periodLabels.contains(periodLabel)) {
                this.periodLabels.add(periodLabel)
            }
            this.periodLabelsToPeriodCat[periodLabel] = periodCat
            if (IBillingManager.DEFAULT_PRICE_CAT == priceCat) {
                defaultPriceLabel = priceFormatted
            }
        }
        this.periodLabels.sortWith { lPeriodS, rPeriodS ->
            try {
                val leftIndex = SORTED_PERIOD_CAT.indexOf(this.periodLabelsToPeriodCat[lPeriodS])
                val rightIndex = SORTED_PERIOD_CAT.indexOf(this.periodLabelsToPeriodCat[rPeriodS])
                leftIndex - rightIndex
            } catch (e: Exception) {
                MTLog.w(LOG_TAG, e, "Error while sorting periods!")
                0
            }
        }
        this.pricesFormatted.sortWith { lPriceS, rPriceS ->
            try {
                val leftIndex = this.priceFormattedToPriceCat[lPriceS]?.toIntOrNull() ?: -1
                val rightIndex = this.priceFormattedToPriceCat[rPriceS]?.toIntOrNull() ?: -1
                leftIndex - rightIndex
            } catch (e: Exception) {
                MTLog.w(LOG_TAG, e, "Error while sorting prices!")
                0
            }
        }
        binding.price.apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, pricesFormatted).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            defaultPriceLabel?.let { setSelection(pricesFormatted.indexOf(it)) }
        }
        binding.periodWeekly.isVisible = periodLabelsToPeriodCat.values.contains(IBillingManager.WEEKLY)
        binding.periodMonthly.isVisible = periodLabelsToPeriodCat.values.contains(IBillingManager.MONTHLY)
        binding.periodYearly.isVisible = periodLabelsToPeriodCat.values.contains(IBillingManager.YEARLY)
        when (IBillingManager.DEFAULT_PERIOD_CAT) {
            IBillingManager.WEEKLY -> binding.periodWeekly.isChecked = true
            IBillingManager.MONTHLY -> binding.periodMonthly.isChecked = true
            IBillingManager.YEARLY -> binding.periodYearly.isChecked = true
        }
        showNotLoading()
        onPriceOrPeriodSelectionChanged()
    }
}
