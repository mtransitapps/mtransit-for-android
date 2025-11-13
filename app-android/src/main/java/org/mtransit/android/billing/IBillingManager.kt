package org.mtransit.android.billing

import androidx.lifecycle.LiveData
import com.android.billingclient.api.ProductDetails
import org.mtransit.android.data.IAgencyProperties
import org.mtransit.android.data.NewsProviderProperties
import org.mtransit.android.provider.experiments.ExperimentsProvider
import org.mtransit.android.provider.experiments.ExperimentsProvider.Companion.EXP_ALLOW_TWITTER_NEWS_FOR_FREE
import org.mtransit.android.provider.experiments.ExperimentsProvider.Companion.EXP_ALLOW_TWITTER_NEWS_FOR_FREE_DEFAULT
import org.mtransit.android.ui.view.common.IActivity
import org.mtransit.android.util.UIFeatureFlags

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
        val PRODUCT_ID_STARTS_WITH_N = "n_"

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
        val NEW_BASIC_SUBSCRIPTIONS = "n_basic" // TODO enable on Play console

        @JvmField
        val NEW_PRO_SUBSCRIPTIONS = "n_pro" // TODO create on play console

        @JvmField
        val NEW_SUBSCRIPTIONS = arrayListOf(
            NEW_BASIC_SUBSCRIPTIONS,
            NEW_PRO_SUBSCRIPTIONS
        ).takeIf { UIFeatureFlags.F_NEW_IN_APP_SUBS } ?: emptyList()

        @JvmField
        val FLEXIBLE_SUBSCRIPTIONS = arrayListOf(
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
        val ORIGINAL_SUBSCRIPTIONS = arrayListOf(
            "weekly_subscription", // Inactive
            "monthly_subscription", // Active - offered by default for months
            "yearly_subscription" // Active - never offered
        )

        @JvmField
        val ALL_VALID_SUBSCRIPTIONS = FLEXIBLE_SUBSCRIPTIONS + ORIGINAL_SUBSCRIPTIONS + NEW_SUBSCRIPTIONS
    }

    val productIdsWithDetails: LiveData<Map<String, ProductDetails>>

    val currentSubscription: LiveData<String?>

    val hasSubscription: LiveData<Boolean?>

    fun showingPaidFeatures(): Boolean

    fun refreshAvailableSubscriptions()

    fun refreshPurchases()

    fun launchBillingFlow(activity: IActivity, productId: String): Boolean

    fun addListener(listener: OnBillingResultListener)

    fun removeListener(listener: OnBillingResultListener)

    interface OnBillingResultListener {
        fun onBillingResult(productId: String?)
    }
}

fun <T : IAgencyProperties> List<T>.filterExpansiveAgencies(billingManager: IBillingManager, experimentsProvider: ExperimentsProvider) =
    filterExpansiveAgencies(
        billingManager.showingPaidFeatures(),
        experimentsProvider.get(EXP_ALLOW_TWITTER_NEWS_FOR_FREE, EXP_ALLOW_TWITTER_NEWS_FOR_FREE_DEFAULT)
    )

fun <T : IAgencyProperties> List<T>.filterExpansiveAgencies(showingPaidFeatures: Boolean, allowTwitterNewsForFree: Boolean): List<T> {
    if (showingPaidFeatures) return this // keep all for paid users
    return filterTo(ArrayList()) { agency ->
        filterExpansiveAgencyAuthorities(agency.authority, allowTwitterNewsForFree)
    }
}

fun List<String>.filterExpansiveAgencyAuthorities(billingManager: IBillingManager, experimentsProvider: ExperimentsProvider) =
    filterExpansiveAgencyAuthorities(
        billingManager.showingPaidFeatures(),
        experimentsProvider.get(EXP_ALLOW_TWITTER_NEWS_FOR_FREE, EXP_ALLOW_TWITTER_NEWS_FOR_FREE_DEFAULT),
    )

fun List<String>.filterExpansiveAgencyAuthorities(showingPaidFeatures: Boolean, allowTwitterNewsForFree: Boolean): List<String> {
    if (showingPaidFeatures) return this // keep all for paid users
    return filterTo(ArrayList()) { authority ->
        filterExpansiveAgencyAuthorities(authority, allowTwitterNewsForFree)
    }
}

fun List<NewsProviderProperties>.filterExpansiveNewsProviders(billingManager: IBillingManager, experimentsProvider: ExperimentsProvider) =
    filterExpansiveNewsProviders(
        billingManager.showingPaidFeatures(),
        experimentsProvider.get(EXP_ALLOW_TWITTER_NEWS_FOR_FREE, EXP_ALLOW_TWITTER_NEWS_FOR_FREE_DEFAULT)
    )

fun List<NewsProviderProperties>.filterExpansiveNewsProviders(showingPaidFeatures: Boolean, allowTwitterNewsForFree: Boolean): List<NewsProviderProperties> {
    if (showingPaidFeatures) return this // keep all for paid users
    return filterTo(ArrayList()) { agency ->
        filterExpansiveAgencyAuthorities(agency.authority, allowTwitterNewsForFree)
    }
}

private val filterExpansiveAgencyAuthorities: (String, allowTwitterNewsForFree: Boolean) -> Boolean = { authority, allowTwitterNewsForFree ->
    (
            (allowTwitterNewsForFree || !authority.contains("news.twitter")) // Twitter/X API
                    && !authority.contains("provider.place") // Google Place Search API
            )
}