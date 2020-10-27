package org.mtransit.android.billing

import androidx.lifecycle.LiveData
import com.android.billingclient.api.SkuDetails
import org.mtransit.android.ui.view.common.IActivity

interface IBillingManager {

    @Suppress("MayBeConstant")
    companion object {
        @JvmField
        val SKU_SUBSCRIPTION = "_subscription_"

        @JvmField
        val SKU_STARTS_WITH_F = "f_"

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
            SKU_STARTS_WITH_F + WEEKLY + SKU_SUBSCRIPTION + "1",
            SKU_STARTS_WITH_F + WEEKLY + SKU_SUBSCRIPTION + "2",
            SKU_STARTS_WITH_F + WEEKLY + SKU_SUBSCRIPTION + "3",
            SKU_STARTS_WITH_F + WEEKLY + SKU_SUBSCRIPTION + "4",
            SKU_STARTS_WITH_F + WEEKLY + SKU_SUBSCRIPTION + "5",
            SKU_STARTS_WITH_F + WEEKLY + SKU_SUBSCRIPTION + "7",
            SKU_STARTS_WITH_F + WEEKLY + SKU_SUBSCRIPTION + "10",
            SKU_STARTS_WITH_F + MONTHLY + SKU_SUBSCRIPTION + "1",
            SKU_STARTS_WITH_F + MONTHLY + SKU_SUBSCRIPTION + "2",
            SKU_STARTS_WITH_F + MONTHLY + SKU_SUBSCRIPTION + "3",
            SKU_STARTS_WITH_F + MONTHLY + SKU_SUBSCRIPTION + "4",
            SKU_STARTS_WITH_F + MONTHLY + SKU_SUBSCRIPTION + "5",
            SKU_STARTS_WITH_F + MONTHLY + SKU_SUBSCRIPTION + "7",
            SKU_STARTS_WITH_F + MONTHLY + SKU_SUBSCRIPTION + "10",
            SKU_STARTS_WITH_F + YEARLY + SKU_SUBSCRIPTION + "1",
            SKU_STARTS_WITH_F + YEARLY + SKU_SUBSCRIPTION + "2",
            SKU_STARTS_WITH_F + YEARLY + SKU_SUBSCRIPTION + "3",
            SKU_STARTS_WITH_F + YEARLY + SKU_SUBSCRIPTION + "4",
            SKU_STARTS_WITH_F + YEARLY + SKU_SUBSCRIPTION + "5",
            SKU_STARTS_WITH_F + YEARLY + SKU_SUBSCRIPTION + "7",
            SKU_STARTS_WITH_F + YEARLY + SKU_SUBSCRIPTION + "10",
        )

        @JvmField
        val ALL_VALID_SUBSCRIPTIONS = AVAILABLE_SUBSCRIPTIONS + arrayListOf(
            "weekly_subscription", // Inactive
            "monthly_subscription", // Active - offered by default for months
            "yearly_subscription" // Active - never offered
        )
    }

    val skusWithSkuDetails: LiveData<Map<String, SkuDetails>>

    fun refreshAvailableSubscriptions()

    fun refreshPurchases()

    fun isHasSubscription(): Boolean?

    fun getCurrentSubscription(): String?

    fun launchBillingFlow(activity: IActivity, sku: String) : Boolean

    fun addListener(listener: OnBillingResultListener)

    fun removeListener(listener: OnBillingResultListener)

    interface OnBillingResultListener {
        fun onBillingResult(sku: String?)
    }
}