package org.mtransit.android.billing

import androidx.lifecycle.LiveData
import com.android.billingclient.api.ProductDetails
import org.mtransit.android.ui.view.common.IActivity

interface IBillingManager {

    @Suppress("MayBeConstant")
    companion object {

        @JvmField
        val OFFER_DETAILS_IDX = 0 // 1 offer / subscription (for now)

        @JvmField
        val PRODUCT_ID_SUBSCRIPTION = "_subscription_"

        @JvmField
        val PRODUCT_ID_STARTS_WITH_F = "f_"

        @JvmField
        val DEFAULT_PRICE_CAT = "1"

        @JvmField
        val WEEKLY = "weekly"

        @JvmField
        val MONTHLY = "monthly"

        @JvmField
        val YEARLY = "yearly"

        @JvmField
        val DEFAULT_PERIOD_CAT = MONTHLY

        @JvmField
        val AVAILABLE_SUBSCRIPTIONS = arrayListOf(
            PRODUCT_ID_STARTS_WITH_F + WEEKLY + PRODUCT_ID_SUBSCRIPTION + "1",
            PRODUCT_ID_STARTS_WITH_F + WEEKLY + PRODUCT_ID_SUBSCRIPTION + "2",
            PRODUCT_ID_STARTS_WITH_F + WEEKLY + PRODUCT_ID_SUBSCRIPTION + "3",
            PRODUCT_ID_STARTS_WITH_F + WEEKLY + PRODUCT_ID_SUBSCRIPTION + "4",
            PRODUCT_ID_STARTS_WITH_F + WEEKLY + PRODUCT_ID_SUBSCRIPTION + "5",
            PRODUCT_ID_STARTS_WITH_F + WEEKLY + PRODUCT_ID_SUBSCRIPTION + "7",
            PRODUCT_ID_STARTS_WITH_F + WEEKLY + PRODUCT_ID_SUBSCRIPTION + "10",
            PRODUCT_ID_STARTS_WITH_F + MONTHLY + PRODUCT_ID_SUBSCRIPTION + "1",
            PRODUCT_ID_STARTS_WITH_F + MONTHLY + PRODUCT_ID_SUBSCRIPTION + "2",
            PRODUCT_ID_STARTS_WITH_F + MONTHLY + PRODUCT_ID_SUBSCRIPTION + "3",
            PRODUCT_ID_STARTS_WITH_F + MONTHLY + PRODUCT_ID_SUBSCRIPTION + "4",
            PRODUCT_ID_STARTS_WITH_F + MONTHLY + PRODUCT_ID_SUBSCRIPTION + "5",
            PRODUCT_ID_STARTS_WITH_F + MONTHLY + PRODUCT_ID_SUBSCRIPTION + "7",
            PRODUCT_ID_STARTS_WITH_F + MONTHLY + PRODUCT_ID_SUBSCRIPTION + "10",
            PRODUCT_ID_STARTS_WITH_F + YEARLY + PRODUCT_ID_SUBSCRIPTION + "1",
            PRODUCT_ID_STARTS_WITH_F + YEARLY + PRODUCT_ID_SUBSCRIPTION + "2",
            PRODUCT_ID_STARTS_WITH_F + YEARLY + PRODUCT_ID_SUBSCRIPTION + "3",
            PRODUCT_ID_STARTS_WITH_F + YEARLY + PRODUCT_ID_SUBSCRIPTION + "4",
            PRODUCT_ID_STARTS_WITH_F + YEARLY + PRODUCT_ID_SUBSCRIPTION + "5",
            PRODUCT_ID_STARTS_WITH_F + YEARLY + PRODUCT_ID_SUBSCRIPTION + "7",
            PRODUCT_ID_STARTS_WITH_F + YEARLY + PRODUCT_ID_SUBSCRIPTION + "10",
        )

        @JvmField
        val ALL_VALID_SUBSCRIPTIONS = AVAILABLE_SUBSCRIPTIONS + arrayListOf(
            "weekly_subscription", // Inactive
            "monthly_subscription", // Active - offered by default for months
            "yearly_subscription" // Active - never offered
        )
    }

    val productIdsWithDetails: LiveData<Map<String, ProductDetails>>

    fun refreshAvailableSubscriptions()

    fun refreshPurchases()

    fun isHasSubscription(): Boolean?

    fun getCurrentSubscription(): String?

    fun launchBillingFlow(activity: IActivity, productId: String) : Boolean

    fun addListener(listener: OnBillingResultListener)

    fun removeListener(listener: OnBillingResultListener)

    interface OnBillingResultListener {
        fun onBillingResult(productId: String?)
    }
}