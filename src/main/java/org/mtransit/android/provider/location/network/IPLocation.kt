package org.mtransit.android.provider.location.network

import com.google.gson.annotations.SerializedName

data class IPLocation(
    @SerializedName("latitude")
    val latitude: Float?,
    @SerializedName("longitude")
    val longitude: Float?,
)
