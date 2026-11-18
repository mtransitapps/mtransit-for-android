package org.mtransit.android.ad.rewarded

// import com.google.android.gms.ads.rewarded.RewardItem // #gmaLegacy
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem // #gmaNextGen

fun RewardItem.toStringPlus() = buildString {
    append("RewardItem{")
    append("amount:$amount")
    append(",")
    append("type:$type")
    append("}")
}
