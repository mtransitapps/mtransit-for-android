package org.mtransit.android.billing

import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.android.billingclient.api.SkuDetailsResponseListener
import org.mtransit.android.billing.IBillingManager.OnBillingResultListener
import org.mtransit.android.common.IApplication
import org.mtransit.android.common.repository.IKeyValueRepository
import org.mtransit.android.commons.MTLog
import org.mtransit.android.ui.view.common.IActivity
import java.util.WeakHashMap


// DEBUG: adb shell setprop log.tag.BillingClient VERBOSE
class MTBillingManager(
    appContext: IApplication,
    private val cacheRepository: IKeyValueRepository
) : MTLog.Loggable,
    IBillingManager,
    BillingClientStateListener, // connection to billing
    PurchasesUpdatedListener, // purchases updated
    SkuDetailsResponseListener { // sku details (name, price...)


    companion object {
        private val LOG_TAG = MTBillingManager::class.java.simpleName

        private const val PREF_KEY_SUBSCRIPTION = "pSubscription"
        private const val PREF_KEY_SUBSCRIPTION_DEFAULT = ""
    }

    override fun getLogTag(): String {
        return LOG_TAG
    }

    private var billingClientConnected: Boolean? = false

    private var billingClient = BillingClient.newBuilder(appContext.requireContext())
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private var _currentSubSku: String? = null

    private val listenersWR = WeakHashMap<OnBillingResultListener, Void?>()

    private val purchases = MutableLiveData<List<Purchase>>()

    private val _skusWithSkuDetails = MutableLiveData<Map<String, SkuDetails>>()

    override val skusWithSkuDetails = _skusWithSkuDetails

    init {
        startConnection()
    }

    private fun startConnection() {
        billingClientConnected = null
        billingClient.startConnection(this)
    }

    override fun onBillingServiceDisconnected() {
        billingClientConnected = false // will try again at next data refresh triggered from UI
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        val responseCode = billingResult.responseCode
        val debugMessage = billingResult.debugMessage
        MTLog.d(this, "onBillingSetupFinished: $responseCode $debugMessage")
        if (responseCode == BillingResponseCode.OK) {
            billingClientConnected = true
            querySkuDetails()
            queryPurchases()
        } else {
            billingClientConnected = false
        }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        val responseCode = billingResult.responseCode
        val debugMessage = billingResult.debugMessage
        MTLog.d(this, "onPurchasesUpdated: $responseCode $debugMessage")
        when (responseCode) {
            BillingResponseCode.OK -> {
                if (purchases == null) {
                    processPurchases(null)
                } else {
                    processPurchases(purchases)
                }
            }
            BillingResponseCode.USER_CANCELED -> {
                MTLog.d(this, "onPurchasesUpdated: User canceled the purchase")
            }
            BillingResponseCode.ITEM_ALREADY_OWNED -> {
                MTLog.d(this, "onPurchasesUpdated: The user already owns this item")
            }
            BillingResponseCode.DEVELOPER_ERROR -> {
                MTLog.w(
                    this, "onPurchasesUpdated: Developer error means that Google Play " +
                            "does not recognize the configuration. If you are just getting started, " +
                            "make sure you have configured the application correctly in the " +
                            "Google Play Console. The SKU product ID must match and the APK you " +
                            "are using must be signed with release keys."
                )
            }
        }
    }

    override fun refreshAvailableSubscriptions() {
        querySkuDetails()
    }

    private fun querySkuDetails() {
        billingClient.querySkuDetailsAsync(
            SkuDetailsParams.newBuilder()
                .setSkusList(IBillingManager.ALL_VALID_SUBSCRIPTIONS)
                .setType(BillingClient.SkuType.SUBS)
                .build(),
            this
        )
    }

    override fun onSkuDetailsResponse(billingResult: BillingResult, skuDetailsList: List<SkuDetails>?) {
        val responseCode = billingResult.responseCode
        val debugMessage = billingResult.debugMessage
        when (responseCode) {
            BillingResponseCode.OK -> {
                MTLog.d(this, "onSkuDetailsResponse: $responseCode $debugMessage")
                if (skuDetailsList == null) {
                    _skusWithSkuDetails.postValue(emptyMap())
                } else
                    _skusWithSkuDetails.postValue(HashMap<String, SkuDetails>()
                        .apply {
                            for (details in skuDetailsList) {
                                put(details.sku, details)
                            }
                        }.also { postedValue ->
                            MTLog.i(this, "onSkuDetailsResponse: count ${postedValue.size}")
                        })
            }
            BillingResponseCode.SERVICE_DISCONNECTED,
            BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingResponseCode.BILLING_UNAVAILABLE,
            BillingResponseCode.ITEM_UNAVAILABLE,
            BillingResponseCode.DEVELOPER_ERROR,
            BillingResponseCode.ERROR -> {
                MTLog.w(this, "onSkuDetailsResponse: $responseCode $debugMessage") // expected
            }
            BillingResponseCode.USER_CANCELED,
            BillingResponseCode.FEATURE_NOT_SUPPORTED,
            BillingResponseCode.ITEM_ALREADY_OWNED,
            BillingResponseCode.ITEM_NOT_OWNED -> {
                MTLog.e(this, "onSkuDetailsResponse: $responseCode $debugMessage") // not expected
            }
        }
    }

    override fun refreshPurchases() {
        queryPurchases()
    }

    private fun queryPurchases() {
        if (!billingClient.isReady) {
            MTLog.w(this, "queryPurchases() > BillingClient is not ready")
            if (this.billingClientConnected == false) {
                startConnection()
            }
            return
        }
        val result = billingClient.queryPurchases(BillingClient.SkuType.SUBS)
        if (result.purchasesList == null) {
            processPurchases(null)
        } else {
            processPurchases(result.purchasesList)
        }
    }

    private fun processPurchases(purchasesList: List<Purchase>?) {
        if (isUnchangedPurchaseList(purchasesList)) {
            MTLog.d(this, "processPurchases: Purchase list has not changed")
            return
        }
        purchases.postValue(purchasesList)
        purchasesList?.let {
            logAcknowledgementStatus(purchasesList)
            it.forEach { purchase ->
                handlePurchase(purchase)
            }
            logAcknowledgementStatus(purchasesList)
        }
        val sku: String = purchasesList?.map { purchase ->
            purchase.sku
        }?.firstOrNull { sku ->
            sku.isNotEmpty()
        }.orEmpty()
        setCurrentSubscription(sku)
    }

    private fun logAcknowledgementStatus(purchasesList: List<Purchase>) {
        var ackYes = 0
        var ackNo = 0
        for (purchase in purchasesList) {
            if (purchase.isAcknowledged) {
                ackYes++
            } else {
                ackNo++
            }
        }
        MTLog.d(this, "logAcknowledgementStatus: acknowledged = $ackYes | unacknowledged= $ackNo")
    }

    private fun isUnchangedPurchaseList(purchasesList: List<Purchase>?): Boolean {
        return false // TODO optimized to avoid updates with identical data.
    }

    override fun launchBillingFlow(activity: IActivity, sku: String) {
        val skuDetails = _skusWithSkuDetails.value?.get(sku) ?: run {
            MTLog.w(this, "Could not find SkuDetails to make purchase.")
            return
        }
        launchBillingFlow(activity, skuDetails)
    }

    private fun launchBillingFlow(activity: IActivity, skuDetails: SkuDetails) {
        val theActivity = activity.activity ?: run {
            MTLog.w(this, "Could not find activity to make purchase.")
            return
        }

        val billingResult = billingClient.launchBillingFlow( // results delivered > onPurchasesUpdated()
            theActivity,
            BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetails)
                .build()
        )
        val responseCode = billingResult.responseCode
        val debugMessage = billingResult.debugMessage
    }

    private fun handlePurchase(purchase: Purchase) {
        if (!purchase.isAcknowledged) {
            acknowledgePurchase(purchase.purchaseToken)
        }
        setCurrentSubscription(purchase.sku)
    }

    private fun acknowledgePurchase(purchaseToken: String) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { billingResult ->
            val responseCode = billingResult.responseCode
            val debugMessage = billingResult.debugMessage
        }
    }

    override fun isHasSubscription(): Boolean? {
        return getCurrentSubscription()?.isNotEmpty()
    }

    override fun getCurrentSubscription(): String? {
        if (_currentSubSku == null) {
            if (this.cacheRepository.hasKey(PREF_KEY_SUBSCRIPTION)) {
                _currentSubSku = this.cacheRepository.getValueNN(PREF_KEY_SUBSCRIPTION, PREF_KEY_SUBSCRIPTION_DEFAULT)
            }
        }
        return _currentSubSku
    }

    private fun setCurrentSubscription(sku: String) {
        if (_currentSubSku == sku) {
            return // same
        }
        _currentSubSku = sku
        this.cacheRepository.saveAsync(PREF_KEY_SUBSCRIPTION, sku)
        broadcastCurrentSkuChanged()
    }

    private fun broadcastCurrentSkuChanged() {
        this.listenersWR.keys.forEach { listener ->
            listener.onBillingResult(this._currentSubSku)
        }
    }

    override fun addListener(listener: OnBillingResultListener) {
        listenersWR[listener] = null
        listener.onBillingResult(getCurrentSubscription())
    }

    override fun removeListener(listener: OnBillingResultListener) {
        listenersWR.remove(listener)
    }
}