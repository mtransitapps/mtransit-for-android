@file:Suppress("unused")

package org.mtransit.android.billing

import com.android.billingclient.api.ProductDetails
import kotlinx.datetime.DatePeriod

private const val LOG_TAG = "BillingKtx"

private const val P4W3D = "P4W3D"

val ProductDetails.PricingPhase.billingDatePeriod: DatePeriod?
    get() = DatePeriod.parseOrNull(this.billingPeriod)

fun ProductDetails.toStringPlus(short: Boolean = false) = buildString {
    append(if (short) "PD{" else "ProductDetails{")
    append("title: $title, ")
    append("description: $description, ")
    append("name: $name, ")
    append("productId: $productId, ")
    subscriptionOfferDetails?.takeIf { it.isNotEmpty() }?.let { append("$productType: ${it.toStringPlus(short = short)}") }
    oneTimePurchaseOfferDetailsList?.takeIf { it.isNotEmpty() }?.let { append("$productType: ${it.size}") } // TODO more details (unused currently)
    append("}")
}

fun List<ProductDetails.SubscriptionOfferDetails>.toStringPlus(short: Boolean = false) = buildString {
    append(if (short) "SODs{" else "SubscriptionOfferDetails{")
    append("[${size}]:")
    append(joinToString { subscriptionOfferDetails -> subscriptionOfferDetails.toStringPlus(short = short) })
    append("}")
}

fun ProductDetails.SubscriptionOfferDetails.toStringPlus(short: Boolean = false) = buildString {
    append(if (short) "SOD{" else "SubscriptionOfferDetails{")
    append("basePlanId: $basePlanId, ")
    installmentPlanDetails?.let { append("installmentPlanId: ${it.toStringPlus(short = short)}, ") }
    offerId?.let { append("offerId: $it, ") }
    offerTags.takeIf { it.isNotEmpty() }?.let { append("offerTags: $it, ") }
    append("offerToken: ${offerToken.length}, ")
    append("pricingPhases: ${pricingPhases.toStringPlus(short = short)}, ")
    append("}")
}

fun ProductDetails.PricingPhases.toStringPlus(short: Boolean = false) = buildString {
    append(if (short) "PHs{" else "PricingPhases{")
    append("[${pricingPhaseList.size}]:")
    append(pricingPhaseList.joinToString { it.toStringPlus(short = short) })
    append("}")
}

fun ProductDetails.PricingPhase.toStringPlus(short: Boolean = false) = buildString {
    append(if (short) "PH{" else "PricingPhase{")
    if (short) {
        append("billing: $billingPeriod")
        billingCycleCount.takeIf { it > 0 }?.let { append("[$it]") }
        append(", ")
    } else {
        append("billingPeriod: $billingPeriod, ")
        billingCycleCount.takeIf { it > 0 }?.let { append("billingCycleCount: $it, ") }
    }
    append(buildString {
        append(if (short) "recurrence: " else "recurrenceMode: ")
        when (recurrenceMode) {
            ProductDetails.RecurrenceMode.INFINITE_RECURRING -> append("infinite")
            ProductDetails.RecurrenceMode.FINITE_RECURRING -> append("finite")
            ProductDetails.RecurrenceMode.NON_RECURRING -> append("non-recurring")
            else -> append("unknown")
        }
        append(", ")
    })
    if (short) {
        append("price: $formattedPrice ($priceAmountMicros, $priceCurrencyCode), ")
    } else {
        append("formattedPrice: $formattedPrice, ")
        append("priceAmountMicros: $priceAmountMicros, ")
        append("priceCurrencyCode: $priceCurrencyCode, ")
    }
    append("}")
}

fun ProductDetails.InstallmentPlanDetails.toStringPlus(short: Boolean = false) = buildString {
    append(if (short) "IPD{" else "InstallmentPlanDetails{")
    if (short) {
        append("installment: $installmentPlanCommitmentPaymentsCount, ")
        append("subsequent: $subsequentInstallmentPlanCommitmentPaymentsCount")
    } else {
        append("installmentPlanCommitmentPayments: $installmentPlanCommitmentPaymentsCount, ")
        append("subsequentCommitmentPayments: $subsequentInstallmentPlanCommitmentPaymentsCount")
    }
    append("{")
}
