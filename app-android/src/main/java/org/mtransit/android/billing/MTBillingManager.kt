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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mtransit.android.common.repository.LocalPreferenceRepository
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.pref.liveDataN
import org.mtransit.android.ui.view.common.IActivity
import org.mtransit.android.util.SystemSettingManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DEBUG: `adb shell setprop log.tag.BillingClient VERBOSE`
 */
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
        private val LOG_TAG: String = MTBillingManager::class.java.simpleName

        private const val LOG_COMPLETE_DETAILS = false
        // private const val LOG_COMPLETE_DETAILS = true // DEBUG

        private const val PREF_KEY_SUBS_PRODUCT_ID = "pSubscription"
        private const val PREF_KEY_SUBS_PRODUCT_ID_NONE = ""
        private val PREF_KEY_SUBS_PRODUCT_ID_UNKNOWN: String? = null
        private val PREF_KEY_SUBS_PRODUCT_ID_DEFAULT: String? = PREF_KEY_SUBS_PRODUCT_ID_UNKNOWN

        private val OVERRIDE_CURRENT_SUBS_PRODUCT_ID: String? = null
        // private val OVERRIDE_CURRENT_SUBS_PRODUCT_ID: String? = "f_monthly_subscription_1".takeIf { Constants.DEBUG } // DEBUG
    }

    override fun getLogTag() = LOG_TAG

    private var billingClientConnected: Boolean? = false

    private val billingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .enableAutoServiceReconnection()
        .build()

    override val currentSubsProductId: LiveData<String?> by lazy {
        OVERRIDE_CURRENT_SUBS_PRODUCT_ID?.let { return@lazy MutableLiveData(it) }
        lclPrefRepository.pref.liveDataN(
            PREF_KEY_SUBS_PRODUCT_ID, PREF_KEY_SUBS_PRODUCT_ID_DEFAULT
        ).distinctUntilChanged()
    }

    override suspend fun getCachedHasSubscription() =
        getCachedCurrentSubsProductId()?.isNotEmpty()

    override suspend fun getCachedCurrentSubsProductId(): String? = withContext(Dispatchers.IO) {
        OVERRIDE_CURRENT_SUBS_PRODUCT_ID?.let { return@withContext it }
        lclPrefRepository.pref.getString(
            PREF_KEY_SUBS_PRODUCT_ID, PREF_KEY_SUBS_PRODUCT_ID_DEFAULT
        )
    }

    override val hasSubscription: LiveData<Boolean?> by lazy {
        this.currentSubsProductId.map { it?.isNotBlank() }
    }

    private val isUsingFirebaseTestLab: Boolean by lazy {
        SystemSettingManager.isUsingFirebaseTestLab(appContext)
    }

    override var fullDemoMode: Boolean? = null

    override fun showingPaidFeatures() = (hasSubscription.value == true
            && !isUsingFirebaseTestLab)
            || fullDemoMode == true
    // || (org.mtransit.android.commons.Constants.DEBUG && org.mtransit.android.BuildConfig.DEBUG) // DEBUG

    private val _productIdsWithDetails = MutableLiveData<Map<String, ProductDetails>>()

    override val availableProductIdsWithDetails = _productIdsWithDetails

    init {
        startConnection()
    }

    private fun startConnection() {
        billingClientConnected = null // unknown
        billingClient.startConnection(this)
    }

    override fun onBillingServiceDisconnected() {
        MTLog.d(this, "onBillingServiceDisconnected()")
        billingClientConnected = false // will try again at next data refresh triggered from UI
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        MTLog.d(this, "onBillingSetupFinished(${billingResult.toStringPlus(short = true)})")
        if (billingResult.responseCode == BillingResponseCode.OK) {
            billingClientConnected = true
            queryAvailableProductDetails()
            queryPurchases()
        } else {
            MTLog.w(this, "Billing setup NOT successful! ${billingResult.responseCode}: ${billingResult.debugMessage}")
            billingClientConnected = false // will try again at next data refresh triggered from UI
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        MTLog.d(this, "onPurchasesUpdated(${billingResult.toStringPlus(short = true)}, ${purchases?.size})")
        MTLog.i(this, "onPurchasesUpdated() > purchases [${purchases?.size}]: ${purchases?.flatMap { it.products }?.joinToString()}.")
        if (LOG_COMPLETE_DETAILS) {
            purchases?.forEach {
                MTLog.d(this, "onPurchasesUpdated() > - purchase: ${it.toStringPlus(short = true)}")
            }
        }
        when (billingResult.responseCode) {
            BillingResponseCode.OK -> {
                processPurchases(purchases.orEmpty())
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
        MTLog.d(this, "refreshAvailableSubscriptions()")
        queryAvailableProductDetails()
    }

    private fun queryAvailableProductDetails() {
        if (!billingClient.isReady) {
            MTLog.d(this, "queryAvailableProductDetails() > BillingClient is not ready")
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
        MTLog.d(this, "onProductDetailsResponse(${billingResult.toStringPlus(short = true)}, ${productDetailsResult.productDetailsList.size})")
        if (!LOG_COMPLETE_DETAILS) {
            productDetailsResult.productDetailsList.let {
                MTLog.i(this, "onProductDetailsResponse() > product IDs [${it.size}]: ${it.joinToString { productDetails -> productDetails.productId }}.")
            }
        }
        when (billingResult.responseCode) {
            BillingResponseCode.OK -> {
                _productIdsWithDetails.postValue(
                    productDetailsResult.productDetailsList.associateBy { productDetails ->
                        if (LOG_COMPLETE_DETAILS) {
                            MTLog.d(this, "onProductDetailsResponse() > - product details: ${productDetails.toStringPlus(short = true)}")
                        }
                        productDetails.productId
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
        MTLog.d(this, "refreshPurchases()")
        if (this.billingClient.isReady && this.billingClientConnected == true && this.hasSubscription.value != null) {
            MTLog.d(this, "refreshPurchases() > SKIP (client ready & connected | current subscription status known)")
            return
        }
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
        MTLog.d(this, "onQueryPurchasesResponse(${billingResult.toStringPlus(short = true)}, ${purchasesList.size})")
        MTLog.i(this, "onQueryPurchasesResponse() > purchases [${purchasesList.size}]: ${purchasesList.flatMap { it.products }.joinToString()}.")
        if (LOG_COMPLETE_DETAILS) {
            purchasesList.forEach {
                MTLog.d(this, "onQueryPurchasesResponse() > - purchase: ${it.toStringPlus(short = true)}")
            }
        }
        if (billingResult.responseCode == BillingResponseCode.OK) {
            processPurchases(purchasesList)
        } else {
            MTLog.w(this, "Error while querying purchases! ${billingResult.responseCode}: ${billingResult.debugMessage}")
            handlePurchasesError()
        }
    }

    private fun processPurchases(purchasesList: List<Purchase>) {
        MTLog.d(this, "processPurchases(${purchasesList.size})")
        purchasesList
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            .filter { !it.isAcknowledged }
            .forEach { purchase ->
                acknowledgePurchase(purchase.purchaseToken)
            }
        val purchasedProduct = purchasesList
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            .flatMap { it.products }
            .firstOrNull { it.isNotEmpty() }
            ?: PREF_KEY_SUBS_PRODUCT_ID_NONE
        setCurrentSubscription(purchasedProduct)
    }

    override fun launchBillingFlow(activity: IActivity, productId: String): Boolean {
        MTLog.d(this, "launchBillingFlow($productId)")
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
        val productDetailsParamsList = listOf(
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
        MTLog.d(this, "launchBillingFlow() > billing flow launch result: ${billingResult.toStringPlus(short = true)})")
        if (billingResult.responseCode != BillingResponseCode.OK) {
            MTLog.w(this, "Error while launching billing flow! ${billingResult.responseCode}: ${billingResult.debugMessage}")
        }
        return billingResult.responseCode == BillingResponseCode.OK
    }

    private fun acknowledgePurchase(purchaseToken: String) {
        billingClient.acknowledgePurchase(
            AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build()
        ) { billingResult ->
            MTLog.d(this, "onAcknowledgePurchaseResponse(${billingResult.toStringPlus(short = true)})")
            if (billingResult.responseCode != BillingResponseCode.OK) {
                MTLog.w(this, "Error while acknowledging purchase! ${billingResult.responseCode}: ${billingResult.debugMessage}")
            }
        }
    }

    private fun handlePurchasesError() {
        MTLog.w(this, "handlePurchasesError()")
        val cachedProductId = this.lclPrefRepository.pref.getString(PREF_KEY_SUBS_PRODUCT_ID, PREF_KEY_SUBS_PRODUCT_ID_DEFAULT)
        if (cachedProductId != PREF_KEY_SUBS_PRODUCT_ID_UNKNOWN) return // keep cached subscription value
        setCurrentSubscription(PREF_KEY_SUBS_PRODUCT_ID_NONE) // assume no subscription until successful purchases fetched
    }

    private fun setCurrentSubscription(productId: String) {
        MTLog.d(this, "setCurrentSubscription($productId)")
        if (this.currentSubsProductId.value == productId) return // same
        this.lclPrefRepository.pref.edit {
            putString(PREF_KEY_SUBS_PRODUCT_ID, productId)
        }
    }
}
