package org.mtransit.android.ad.rewarded

import com.google.android.gms.ads.rewarded.RewardItem

fun RewardItem.toStringPlus() = buildString {
    append("RewardItem{")
    append("amount:$amount")
    append(",")
    append("type:$type")
    append("}")
}
