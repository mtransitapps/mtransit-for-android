package org.mtransit.android.ui.location

import android.content.Context
import android.location.Address
import androidx.core.text.isDigitsOnly
import com.google.android.gms.maps.model.LatLng
import org.mtransit.android.R

val Address.latLng: LatLng?
    get() {
        if (!hasLatitude() || !hasLongitude()) return null
        return LatLng(latitude, longitude)
    }

val Address.firstAddressLineOrNull: String? get() = this.takeIf { it.maxAddressLineIndex >= 0 }?.getAddressLine(0)

val Address.usefulFeatureName: String? get() = this.featureName?.takeIf { it.isNotBlank() && !featureName.isDigitsOnly() && it.length > 7 }

fun Address.toNameAndSubtitle(context: Context, accurate: Boolean): Pair<String, String?>? {
    if (!accurate) {
        return context.getString(R.string.place_pin_placed) to firstAddressLineOrNull?.let { context.getString(R.string.nearby_place_and_place, it) }
    }
    this.usefulFeatureName?.takeIf { it.isNotBlank() }?.let {
        return it to firstAddressLineOrNull
    }
    return firstAddressLineOrNull?.let { it to null }
}

fun Address.toNameOnly(context: Context, accurate: Boolean): String? {
    if (!accurate) {
        return firstAddressLineOrNull?.let { context.getString(R.string.nearby_place_and_place, it) }
    }
    this.usefulFeatureName?.takeIf { it.isNotBlank() }?.let {
        return it
    }
    return firstAddressLineOrNull
}
