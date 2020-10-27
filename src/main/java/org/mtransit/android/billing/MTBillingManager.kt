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

    override fun getLogTag(): String = LOG_TAG

    private var billingClientConnected: Boolean? = false

    private var billingClient = BillingClient.newBuilder(appContext.requireContext())
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private var _currentSubSku: String? = null

    private val _listenersWR = WeakHashMap<OnBillingResultListener, Void?>()

    private val _skusWithSkuDetails = MutableLiveData<Map<String, SkuDetails>>()

    override val skusWithSkuDetails = _skusWithSkuDetails

    init {
        startConnection()
    }

    private fun startConnection() {
        billingClientConnected = null // unknown
        billingClient.startConnection(this)
    }

    override fun onBillingServiceDisconnected() {
        billingClientConnected = false // will try again at next data refresh triggered from UI
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingResponseCode.OK) {
            billingClientConnected = true
            querySkuDetails()
            queryPurchases()
        } else {
            MTLog.w(this, "Billing setup NOT successful! ${billingResult.responseCode}: ${billingResult.debugMessage}")
            billingClientConnected = false
        }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        when (billingResult.responseCode) {
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
        if (!billingClient.isReady) {
            MTLog.w(this, "querySkuDetails() > BillingClient is not ready")
            if (this.billingClientConnected == false) {
                startConnection()
            }
            return
        }
        billingClient.querySkuDetailsAsync(
            SkuDetailsParams.newBuilder()
                .setSkusList(IBillingManager.ALL_VALID_SUBSCRIPTIONS)
                .setType(BillingClient.SkuType.SUBS)
                .build(),
            this
        )
    }

    override fun onSkuDetailsResponse(billingResult: BillingResult, skuDetailsList: List<SkuDetails>?) {
        when (billingResult.responseCode) {
            BillingResponseCode.OK -> {
                if (skuDetailsList == null) {
                    _skusWithSkuDetails.postValue(emptyMap())
                } else
                    _skusWithSkuDetails.postValue(HashMap<String, SkuDetails>()
                        .apply {
                            for (details in skuDetailsList) {
                                put(details.sku, details)
                            }
                        }
                        .also { postedValue ->
                            MTLog.d(this, "onSkuDetailsResponse() > found ${postedValue.size} SKU details")
                        })
            }
            BillingResponseCode.SERVICE_DISCONNECTED,
            BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingResponseCode.BILLING_UNAVAILABLE,
            BillingResponseCode.ITEM_UNAVAILABLE,
            BillingResponseCode.DEVELOPER_ERROR,
            BillingResponseCode.ERROR -> {
                MTLog.w(this, "Error while fetching SKU details! ${billingResult.responseCode}: ${billingResult.debugMessage}")
            }
            BillingResponseCode.USER_CANCELED,
            BillingResponseCode.FEATURE_NOT_SUPPORTED,
            BillingResponseCode.ITEM_ALREADY_OWNED,
            BillingResponseCode.ITEM_NOT_OWNED -> {
                MTLog.e(this, "Unexpected error while fetching SKU details! ${billingResult.responseCode}: ${billingResult.debugMessage}")
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
        val purchasesResult = billingClient.queryPurchases(BillingClient.SkuType.SUBS)
        val billingResult = purchasesResult.billingResult
        if (billingResult.responseCode != BillingResponseCode.OK) {
            MTLog.w(this, "Error while querying purchases! ${billingResult.responseCode}: ${billingResult.debugMessage}")
        }
        if (purchasesResult.purchasesList == null) {
            processPurchases(null)
        } else {
            processPurchases(purchasesResult.purchasesList)
        }
    }

    private fun processPurchases(purchasesList: List<Purchase>?) {
        if (isUnchangedPurchaseList(purchasesList)) {
            MTLog.d(this, "processPurchases() > SKIP (purchase list has not changed)")
            return
        }
        purchasesList?.let { list ->
            list.forEach { purchase ->
                handlePurchase(purchase)
            }
        }
        setCurrentSubscription(
            purchasesList?.map { purchase ->
                purchase.sku
            }?.firstOrNull { sku ->
                sku.isNotEmpty()
            }.orEmpty()
        )
    }

    private fun isUnchangedPurchaseList(@Suppress("UNUSED_PARAMETER") purchasesList: List<Purchase>?): Boolean {
        return false // TODO optimized to avoid updates with identical data.
    }

    override fun launchBillingFlow(activity: IActivity, sku: String): Boolean {
        val skuDetails = _skusWithSkuDetails.value?.get(sku) ?: run {
            MTLog.w(this, "Could not find SkuDetails to make purchase.")
            return false
        }
        return launchBillingFlow(activity, skuDetails)
    }

    private fun launchBillingFlow(activity: IActivity, skuDetails: SkuDetails): Boolean {
        val theActivity = activity.activity ?: run {
            MTLog.w(this, "Could not find activity to make purchase.")
            return false
        }

        val billingResult = billingClient.launchBillingFlow( // results delivered > onPurchasesUpdated()
            theActivity,
            BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetails)
                .build()
        )
        if (billingResult.responseCode != BillingResponseCode.OK) {
            MTLog.w(this, "Error while launching billing flow! ${billingResult.responseCode}: ${billingResult.debugMessage}")
        }
        return billingResult.responseCode == BillingResponseCode.OK
    }

    private fun handlePurchase(purchase: Purchase) {
        if (!purchase.isAcknowledged) {
            acknowledgePurchase(purchase.purchaseToken)
        }
        setCurrentSubscription(purchase.sku)
    }

    private fun acknowledgePurchase(purchaseToken: String) {
        billingClient.acknowledgePurchase(
            AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build()
        ) { billingResult ->
            if (billingResult.responseCode != BillingResponseCode.OK) {
                MTLog.w(this, "Error while acknowledging purchase! ${billingResult.responseCode}: ${billingResult.debugMessage}")
            }
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
        this._listenersWR.keys.forEach { listener ->
            listener.onBillingResult(this._currentSubSku)
        }
    }

    override fun addListener(listener: OnBillingResultListener) {
        _listenersWR[listener] = null
        listener.onBillingResult(getCurrentSubscription())
    }

    override fun removeListener(listener: OnBillingResultListener) {
        _listenersWR.remove(listener)
    }
}