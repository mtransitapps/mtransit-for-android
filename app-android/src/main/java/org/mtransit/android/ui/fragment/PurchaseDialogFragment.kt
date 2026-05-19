package org.mtransit.android.ui.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.collection.ArrayMap
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetails.PricingPhase
import com.android.billingclient.api.ProductDetails.SubscriptionOfferDetails
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import org.mtransit.android.R
import org.mtransit.android.ad.IAdManager
import org.mtransit.android.billing.IBillingManager
import org.mtransit.android.commons.ArrayUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.PackageManagerUtils
import org.mtransit.android.commons.StoreUtils
import org.mtransit.android.commons.ThreadSafeDateFormatter
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.ToastUtils
import org.mtransit.android.ui.view.common.IActivity
import java.util.concurrent.TimeUnit

class PurchaseDialogFragment : MTDialogFragmentX(), IActivity, IAdManager.RewardedAdListener {

    override fun getLogTag() = LOG_TAG

    private val noOpStringObserver = Observer<String> { }
    private val noOpBooleanObserver = Observer<Boolean> { }
    private val noOpLongObserver = Observer<Long> { }

    private var newProductDetailsObserver: Observer<Map<String, ProductDetails>>? = null

    // TODO migrate to 100% Hilt after migrating to AndroidX
    // TODO @InstallIn(FragmentComponent::class) ?
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface PurchaseDialogEntryPoint {
        fun billingManager(): IBillingManager
        fun adManager(): IAdManager
    }

    private fun getEntryPoint(context: Context): PurchaseDialogEntryPoint {
        return EntryPointAccessors.fromApplication(context.applicationContext, PurchaseDialogEntryPoint::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_dialog_purchase, container, false).also {
            setupView(it)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            setCancelable(true)
            setCanceledOnTouchOutside(true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.newProductDetailsObserver = Observer { onNewProductId(it) }
        @SuppressLint("DeprecatedCall") // FIXME
        val entryPoint = getEntryPoint(requireContext())
        val billingManager = entryPoint.billingManager()
        billingManager.currentSubscription.observeForever(this.noOpStringObserver)
        this.newProductDetailsObserver?.let {
            billingManager.productIdsWithDetails.observeForever(it)
        }
        val adManager = entryPoint.adManager()
        adManager.rewardedUntilInMsLive.observeForever(this.noOpLongObserver)
        adManager.rewardedNowLive.observeForever(this.noOpBooleanObserver)
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        ToastUtils.makeTextAndShow(activity, R.string.support_subs_user_canceled_message)
    }

    private fun setupView(view: View?) {
        if (view == null) return
        view.findViewById<View>(R.id.buyBtn).setOnClickListener {
            onBuyBtnClick(it.context)
        }
        view.findViewById<View>(R.id.downloadOrOpenPaidTasksBtn).setOnClickListener {
            onDownloadOrOpenPaidTasksBtnClick(it.context)
        }
        view.findViewById<View>(R.id.rewardedAdsBtn).setOnClickListener {
            onRewardedAdButtonClick(it.context)
        }
    }

    override val currentFragment: Fragment
        get() = this

    override fun onDestroy() {
        super.onDestroy()
        @SuppressLint("DeprecatedCall") // FIXME
        val entryPoint = getEntryPoint(requireContext())
        val billingManager = entryPoint.billingManager()
        this.newProductDetailsObserver?.let {
            billingManager.productIdsWithDetails.removeObserver(it)
        }
        billingManager.currentSubscription.removeObserver(this.noOpStringObserver)
        val adManager = entryPoint.adManager()
        adManager.rewardedUntilInMsLive.removeObserver(this.noOpLongObserver)
        adManager.rewardedNowLive.removeObserver(this.noOpBooleanObserver)
    }

    override fun finish() {
        requireActivity().finish()
    }

    private fun onRewardedAdButtonClick(context: Context) {
        val activity = activity
        try {
            if (activity == null) {
                MTLog.w(this, "onRewardedAdButtonClick() > skip (no view or no activity)")
                ToastUtils.makeTextAndShow(context, R.string.support_watch_rewarded_ad_default_failure_message)
                return
            }
            @SuppressLint("DeprecatedCall") // FIXME
            val entryPoint = getEntryPoint(context)
            val adManager = entryPoint.adManager()
            if (!adManager.isRewardedAdAvailableToShow()) {
                MTLog.w(this, "onRewardedAdButtonClick() > skip (no ad available)")
                ToastUtils.makeTextAndShow(context, R.string.support_watch_rewarded_ad_not_ready)
                return
            }
            adManager.showRewardedAd(this)
            view?.findViewById<View>(R.id.rewardedAdsBtn)?.isEnabled = false
        } catch (e: Exception) {
            MTLog.w(this, e, "Error while handling download or open paid tasks button!")
            ToastUtils.makeTextAndShow(context, R.string.support_watch_rewarded_ad_default_failure_message)
        }
    }

    private fun onDownloadOrOpenPaidTasksBtnClick(context: Context) {
        val activity = activity
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
                        org.mtransit.android.commons.R.string.opening_and_label,
                        context.getString(R.string.support_paid_tasks_incentive_app_label)
                    )
                )
                PackageManagerUtils.openApp(context, PAID_TASKS_PKG)
            } else {
                StoreUtils.viewAppPage(
                    activity,
                    PAID_TASKS_PKG,
                    context.getString(org.mtransit.android.commons.R.string.google_play)
                )
            }
            dialog?.dismiss()
        } catch (e: Exception) {
            MTLog.w(this, e, "Error while handling download or open paid tasks button!")
            ToastUtils.makeTextAndShow(context, R.string.support_subs_default_failure_message)
        }
    }

    private fun onBuyBtnClick(context: Context) {
        val activity = activity
        try {
            val view = view
            if (view == null || activity == null) {
                MTLog.w(this, "onBuyBtnClick() > skip (no view or no activity)")
                ToastUtils.makeTextAndShow(context, R.string.support_subs_default_failure_message)
                return
            }
            val periodSpinner: Spinner = view.findViewById(R.id.period)
            val periodPosition = periodSpinner.selectedItemPosition
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
            val priceSpinner: Spinner = view.findViewById(R.id.price)
            val pricePosition = priceSpinner.selectedItemPosition
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
            @SuppressLint("DeprecatedCall") // FIXME
            val entryPoint = getEntryPoint(requireContext())
            val billingManager = entryPoint.billingManager()
            val billingFlowLaunched = billingManager.launchBillingFlow(this, productId)
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
        @SuppressLint("DeprecatedCall") // FIXME
        val entryPoint = getEntryPoint(requireContext())
        val billingManager = entryPoint.billingManager()
        billingManager.refreshAvailableSubscriptions()
        val adManager = entryPoint.adManager()
        adManager.setRewardedAdListener(this)
        adManager.linkRewardedAd(this)
        onResumeKt(adManager)
        showLoading()
        view?.let { view ->
            (view.findViewById<Button>(R.id.downloadOrOpenPaidTasksBtn)).setText(
                if (PackageManagerUtils.isAppInstalled(view.context, PAID_TASKS_PKG)) {
                    R.string.support_paid_tasks_incentive_open_btn
                } else {
                    R.string.support_paid_tasks_incentive_download_btn
                }
            )
            refreshRewardedLayout(view)
        }
    }

    override fun onPause() {
        super.onPause()
        @SuppressLint("DeprecatedCall") // FIXME
        val entryPoint = getEntryPoint(requireContext())
        val adManager = entryPoint.adManager()
        adManager.setRewardedAdListener(null)
    }

    @MainThread
    private fun refreshRewardedLayout(view: View) {
        @SuppressLint("DeprecatedCall") // FIXME
        val entryPoint = getEntryPoint(view.context)
        val adManager = entryPoint.adManager()
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

        val rewardedDivider = view.findViewById<View>(R.id.paidTasksDivider2)
        val rewardedAdStatusTv = view.findViewById<TextView>(R.id.rewardedAdText)
        val rewardedAdsBtn = view.findViewById<Button>(R.id.rewardedAdsBtn)

        rewardedDivider.visibility = if (availableToShow || rewardedNow) View.VISIBLE else View.GONE

        if (rewardedNow) {
            rewardedAdStatusTv.text = getString(
                R.string.support_watch_rewarded_ad_status_until_and_date,
                this.dateFormatter.formatThreadSafe(rewardedUntilInMs)
            )
            rewardedAdStatusTv.visibility = View.VISIBLE
        } else {
            rewardedAdStatusTv.visibility = View.GONE
            rewardedAdStatusTv.text = null
        }

        rewardedAdsBtn.text = resources.getQuantityString(
            if (rewardedNow) {
                R.plurals.support_watch_rewarded_ad_btn_more_and_days
            } else {
                R.plurals.support_watch_rewarded_ad_btn_and_days
            },
            rewardedAmount,
            rewardedAmount
        )
        if (availableToShow) {
            rewardedAdsBtn.isEnabled = true
            rewardedAdsBtn.visibility = View.VISIBLE
        } else {
            rewardedAdsBtn.isEnabled = false
        }
    }

    @WorkerThread
    override fun skipRewardedAd(): Boolean {
        @SuppressLint("DeprecatedCall") // FIXME
        val entryPoint = getEntryPoint(requireContext())
        val adManager = entryPoint.adManager()
        if (!adManager.isRewardedNow()) return false
        val rewardedUntilInMs = adManager.getRewardedUntilInMs()
        val skipRewardedAdUntilInMs =
            TimeUtils.currentTimeMillis() - TimeUnit.HOURS.toMillis(1L) + adManager.getRewardedAdAmount() * adManager.getRewardedAdAmountInMs()
        return rewardedUntilInMs > skipRewardedAdUntilInMs
    }

    @AnyThread
    override fun onRewardedAdStatusChanged() {
        val view = view ?: return
        view.post { refreshRewardedLayout(view) }
    }

    override fun onDetach() {
        super.onDetach()
        @SuppressLint("DeprecatedCall") // FIXME
        val entryPoint = getEntryPoint(requireContext())
        @SuppressLint("DeprecatedCall") // FIXME
        val adManager = entryPoint.adManager()
        adManager.unlinkRewardedAd(this)
    }

    private fun showLoading() {
        val view = view ?: return
        view.findViewById<View>(R.id.title).visibility = View.GONE
        view.findViewById<View>(R.id.subTitle).visibility = View.GONE
        view.findViewById<View>(R.id.beforeText).visibility = View.GONE
        view.findViewById<View>(R.id.priceSelection).visibility = View.GONE
        view.findViewById<View>(R.id.afterText).visibility = View.GONE
        view.findViewById<View>(R.id.buyBtn).visibility = View.GONE
        view.findViewById<View>(R.id.paidTasksDivider).visibility = View.GONE
        view.findViewById<View>(R.id.paidTasksIncentive).visibility = View.GONE
        view.findViewById<View>(R.id.downloadOrOpenPaidTasksBtn).visibility = View.GONE
        view.findViewById<View>(R.id.loading_layout).visibility = View.VISIBLE
    }

    private fun showNotLoading() {
        val view = view ?: return
        view.findViewById<View>(R.id.loading_layout).visibility = View.GONE
        view.findViewById<View>(R.id.title).visibility = View.VISIBLE
        view.findViewById<View>(R.id.subTitle).visibility = View.VISIBLE
        view.findViewById<View>(R.id.beforeText).visibility = View.VISIBLE
        view.findViewById<View>(R.id.priceSelection).visibility = View.VISIBLE
        view.findViewById<View>(R.id.afterText).visibility = View.VISIBLE
        view.findViewById<View>(R.id.buyBtn).visibility = View.VISIBLE
        view.findViewById<View>(R.id.paidTasksDivider).visibility = View.VISIBLE
        view.findViewById<View>(R.id.paidTasksIncentive).visibility = View.VISIBLE
        view.findViewById<View>(R.id.downloadOrOpenPaidTasksBtn).visibility = View.VISIBLE
    }

    private val prices = ArrayList<String>()
    private val priceSToPriceCat = ArrayMap<String, String>()
    private val periods = ArrayList<String>()
    private val periodSToPeriodCat = ArrayMap<String, String>()

    private fun onNewProductId(productIdsWithDetails: Map<String, ProductDetails>?) {
        if (productIdsWithDetails == null) return
        val view = view
        val activity = activity
        if (view == null || activity == null) return
        this.prices.clear()
        this.periods.clear()
        this.priceSToPriceCat.clear()
        this.periodSToPeriodCat.clear()
        var defaultPriceS: String? = null
        var defaultPeriodS: String? = null
        for (productId in productIdsWithDetails.keys) {
            if (!productId.startsWith(IBillingManager.PRODUCT_ID_STARTS_WITH_F)) {
                MTLog.w(this, "Skip product ID %s (unexpected)", productId)
                continue
            }
            val productDetails = productIdsWithDetails[productId]
            if (productDetails == null) {
                MTLog.w(this, "Skip product ID %s (unknown product detail)", productId)
                continue
            }
            val periodCat = productId.substring(
                IBillingManager.PRODUCT_ID_STARTS_WITH_F.length,
                productId.indexOf(IBillingManager.PRODUCT_ID_SUBSCRIPTION, IBillingManager.PRODUCT_ID_STARTS_WITH_F.length)
            )
            val resId = PERIOD_RES_ID[periodCat]
            if (resId == null) {
                MTLog.w(this, "Skip product ID %s (unknown periodCat: %s)", productId, periodCat)
                continue
            }
            val priceCat = productId.substring(
                productId.indexOf(IBillingManager.PRODUCT_ID_SUBSCRIPTION) + IBillingManager.PRODUCT_ID_SUBSCRIPTION.length
            )
            val subOfferDetailsList = productDetails.subscriptionOfferDetails
            if (subOfferDetailsList.isNullOrEmpty()) {
                MTLog.w(this, "Skip product ID %s (no offer details)", productId)
                return
            }
            val subOfferDetails = subOfferDetailsList[IBillingManager.OFFER_DETAILS_IDX]
            val pricingPhaseList: List<PricingPhase> = subOfferDetails.pricingPhases.pricingPhaseList
            if (pricingPhaseList.isEmpty()) {
                MTLog.w(this, "Skip product ID %s (no pricing list)", productId)
                return
            }
            val lastPricingPhase = pricingPhaseList[pricingPhaseList.size - 1]
            val priceS = lastPricingPhase.formattedPrice
            this.priceSToPriceCat[priceS] = priceCat
            if (!this.prices.contains(priceS)) {
                this.prices.add(priceS)
            }
            val periodS = activity.getString(resId)
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
                val lPriceCat = this.periodSToPeriodCat[lPeriodS]
                val lIndexOf = SORTED_PERIOD_CAT.indexOf(lPriceCat)
                val rPriceCat = this.periodSToPeriodCat[rPeriodS]
                val rIndexOf = SORTED_PERIOD_CAT.indexOf(rPriceCat)
                lIndexOf - rIndexOf
            } catch (e: Exception) {
                MTLog.w(LOG_TAG, e, "Error while sorting periods!")
                0
            }
        }
        this.prices.sortWith { lPriceS, rPriceS ->
            try {
                val lPriceCat = this.priceSToPriceCat[lPriceS]
                val lIndexOf = if (lPriceCat == null || !TextUtils.isDigitsOnly(lPriceCat)) -1 else lPriceCat.toInt()
                val rPriceCat = this.priceSToPriceCat[rPriceS]
                val rIndexOf = if (rPriceCat == null || !TextUtils.isDigitsOnly(rPriceCat)) -1 else rPriceCat.toInt()
                lIndexOf - rIndexOf
            } catch (e: Exception) {
                MTLog.w(LOG_TAG, e, "Error while sorting prices!")
                0
            }
        }
        val priceSpinner = view.findViewById<Spinner>(R.id.price)
        priceSpinner.adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, this.prices)
        defaultPriceS?.let {
            priceSpinner.setSelection(this.prices.indexOf(it))
        }
        val periodSpinner = view.findViewById<Spinner>(R.id.period)
        periodSpinner.adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, this.periods)
        defaultPeriodS?.let {
            periodSpinner.setSelection(this.periods.indexOf(it))
        }
        showNotLoading()
    }

    companion object {

        private val LOG_TAG = PurchaseDialogFragment::class.java.simpleName

        private const val PAID_TASKS_PKG = "com.google.android.apps.paidtasks"

        @JvmStatic
        fun newInstance(): PurchaseDialogFragment {
            return PurchaseDialogFragment()
        }

        private val SORTED_PERIOD_CAT = ArrayUtils.asArrayList(
            IBillingManager.WEEKLY,
            IBillingManager.MONTHLY,
            IBillingManager.YEARLY
        )

        private val PERIOD_RES_ID: ArrayMap<String, Int> = ArrayMap<String, Int>().apply {
            put(IBillingManager.WEEKLY, R.string.support_every_week)
            put(IBillingManager.MONTHLY, R.string.support_every_month)
            put(IBillingManager.YEARLY, R.string.support_every_year)
        }
    }
}
