package org.mtransit.android.billing

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsResult
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import org.mtransit.android.billing.IBillingManager.OnBillingResultListener
import org.mtransit.android.common.repository.LocalPreferenceRepository
import org.mtransit.android.commons.Constants
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.pref.liveDataN
import org.mtransit.android.ui.view.common.IActivity
import org.mtransit.android.util.SystemSettingManager
import java.util.WeakHashMap
import javax.inject.Inject
import javax.inject.Singleton


// DEBUG: `adb shell setprop log.tag.BillingClient VERBOSE`
@Singleton
class MTBillingManager @Inject constructor(
    @ApplicationContext appContext: Context,
    private val lclPrefRepository: LocalPreferenceRepository,
) : MTLog.Loggable,
    IBillingManager,
    BillingClientStateListener, // connection to billing
    PurchasesResponseListener, // purchases requested
    PurchasesUpdatedListener, // purchases updated
    ProductDetailsResponseListener // product ID details (name, price...)
{

    companion object {
        private val LOG_TAG = MTBillingManager::class.java.simpleName

        private const val PREF_KEY_SUBSCRIPTION = "pSubscription"
        private val PREF_KEY_SUBSCRIPTION_DEFAULT: String? = null
    }

    override fun getLogTag(): String = LOG_TAG

    private var billingClientConnected: Boolean? = false

    private val billingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    override val currentSubscription: LiveData<String?> by lazy {
        lclPrefRepository.pref.liveDataN(
            PREF_KEY_SUBSCRIPTION, PREF_KEY_SUBSCRIPTION_DEFAULT
        ).distinctUntilChanged()
    }

    private val _currentSubscription: String?
        get() = currentSubscription.value

    override val hasSubscription: LiveData<Boolean?> by lazy {
        this.currentSubscription.map { it?.isNotBlank() }
    }

    private val isUsingFirebaseTestLab: Boolean by lazy {
        SystemSettingManager.isUsingFirebaseTestLab(appContext)
    }

    override fun showingPaidFeatures() = (hasSubscription.value == true
            && !isUsingFirebaseTestLab)
    // || (org.mtransit.android.commons.Constants.DEBUG && org.mtransit.android.BuildConfig.DEBUG) // DEBUG

    private val _listenersWR = WeakHashMap<OnBillingResultListener, Void?>()

    private val _productIdsWithDetails = MutableLiveData<Map<String, ProductDetails>>()

    override val productIdsWithDetails = _productIdsWithDetails

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
            queryProductDetails()
            queryPurchases()
        } else {
            MTLog.w(this, "Billing setup NOT successful! ${billingResult.responseCode}: ${billingResult.debugMessage}")
            billingClientConnected = false // will try again at next data refresh triggered from UI
        }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?,
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
                            "Google Play Console. The product ID must match and the APK you " +
                            "are using must be signed with release keys."
                )
            }
        }
    }

    override fun refreshAvailableSubscriptions() {
        queryProductDetails()
    }

    private fun queryProductDetails() {
        if (!billingClient.isReady) {
            MTLog.d(this, "queryProductDetails() > BillingClient is not ready")
            if (this.billingClientConnected == false) {
                startConnection()
            }
            return
        }
        billingClient.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder()
                .setProductList(
                    IBillingManager.ALL_VALID_SUBSCRIPTIONS.map { productId ->
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductType(ProductType.SUBS)
                            .setProductId(productId)
                            .build()
                    }
                ).build(),
            this
        )
    }

    override fun onProductDetailsResponse(billingResult: BillingResult, productDetailsResult: QueryProductDetailsResult) {
        onProductDetailsResponse(billingResult, productDetailsResult.productDetailsList)
    }

private fun onProductDetailsResponse(billingResult: BillingResult, productDetailsList: List<ProductDetails>) {
        when (billingResult.responseCode) {
            BillingResponseCode.OK -> {
                _productIdsWithDetails.postValue(
                    productDetailsList.associateBy { details ->
                        if (Constants.DEBUG) {
                            details.subscriptionOfferDetails?.forEach {
                                MTLog.d(this, "onProductDetailsResponse() > offer details: $it")
                                it.installmentPlanDetails.let { installmentPlanDetails ->
                                    MTLog.d(this, "onProductDetailsResponse() > installment plan details: $installmentPlanDetails")
                                }
                                it.pricingPhases.pricingPhaseList.forEach { pricingPhase ->
                                    MTLog.d(this, "onProductDetailsResponse() > pricing phase: $pricingPhase")
                                }
                            }
                        }
                        details.productId
                    }
                        .also { postedValue ->
                            MTLog.d(this, "onProductDetailsResponse() > found ${postedValue.size} product details")
                        }
                )
            }

            BillingResponseCode.SERVICE_DISCONNECTED,
            BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingResponseCode.BILLING_UNAVAILABLE,
            BillingResponseCode.ITEM_UNAVAILABLE,
            BillingResponseCode.DEVELOPER_ERROR,
            BillingResponseCode.ERROR -> {
                MTLog.w(this, "Error while fetching product details! ${billingResult.responseCode}: ${billingResult.debugMessage}")
            }

            BillingResponseCode.USER_CANCELED,
            BillingResponseCode.FEATURE_NOT_SUPPORTED,
            BillingResponseCode.ITEM_ALREADY_OWNED,
            BillingResponseCode.ITEM_NOT_OWNED -> {
                MTLog.e(this, "Unexpected error while fetching product details! ${billingResult.responseCode}: ${billingResult.debugMessage}")
            }
        }
    }

    override fun refreshPurchases() {
        queryPurchases()
    }

    private fun queryPurchases() {
        if (!billingClient.isReady) {
            MTLog.d(this, "queryPurchases() > BillingClient is not ready")
            if (this.billingClientConnected == false) {
                startConnection()
            }
            return
        }
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(ProductType.SUBS)
                .build(),
            this
        )
    }

    override fun onQueryPurchasesResponse(billingResult: BillingResult, purchasesList: List<Purchase>) {
        if (billingResult.responseCode != BillingResponseCode.OK) {
            MTLog.w(this, "Error while querying purchases! ${billingResult.responseCode}: ${billingResult.debugMessage}")
        }
        processPurchases(purchasesList)
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
            purchasesList?.flatMap { purchase ->
                purchase.products
            }?.firstOrNull { product ->
                product.isNotEmpty()
            }.orEmpty()
        )
    }

    private fun isUnchangedPurchaseList(@Suppress("unused") purchasesList: List<Purchase>?): Boolean {
        return false // TODO optimized to avoid updates with identical data.
    }

    override fun launchBillingFlow(activity: IActivity, productId: String): Boolean {
        val productDetails = _productIdsWithDetails.value?.get(productId) ?: run {
            MTLog.w(this, "Could not find ProductDetails to make purchase.")
            return false
        }
        return launchBillingFlow(activity, productDetails)
    }

    private fun launchBillingFlow(activity: IActivity, productDetails: ProductDetails): Boolean {
        val theActivity = activity.activity ?: run {
            MTLog.w(this, "Could not find activity to make purchase.")
            return false
        }

        val offerToken = productDetails.subscriptionOfferDetails?.getOrNull(IBillingManager.OFFER_DETAILS_IDX)?.offerToken ?: run {
            MTLog.w(this, "Could not find offer token for '${productDetails.productId}' purchase.")
            return false
        }
        val productDetailsParamsList =
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
            )
        val billingResult = billingClient.launchBillingFlow( // results delivered > onPurchasesUpdated()
            theActivity,
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
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
        setCurrentSubscription(
            purchase.products
                .firstOrNull { product ->
                    product.isNotEmpty()
                }.orEmpty()
        )
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

    private fun setCurrentSubscription(productId: String) {
        if (_currentSubscription == productId) {
            return // same
        }
        this.lclPrefRepository.pref.edit {
            putString(PREF_KEY_SUBSCRIPTION, productId)
        }
        broadcastCurrentProductIdChanged()
    }

    private fun broadcastCurrentProductIdChanged() {
        this._listenersWR.keys.forEach { listener ->
            listener.onBillingResult(this._currentSubscription)
        }
    }

    override fun addListener(listener: OnBillingResultListener) {
        _listenersWR[listener] = null
        listener.onBillingResult(this._currentSubscription)
    }

    override fun removeListener(listener: OnBillingResultListener) {
        _listenersWR.remove(listener)
    }
}