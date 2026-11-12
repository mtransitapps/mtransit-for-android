package org.mtransit.android.provider.location.network

import com.google.gson.annotations.SerializedName

data class IPLocation(
    @SerializedName("latitude")
    val latitude: Float? = null,
    @SerializedName("longitude")
    val longitude: Float? = null,
    @SerializedName("country_code")
    val countryCode: String? = null,
)
