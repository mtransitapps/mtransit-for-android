package org.mtransit.android.provider

import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place

val Place.displayName: String? // same as Place SDK v4/5+ (min SDK 23/24)
    get() = this.name

val Place.location: LatLng? // same as Place SDK v4/5+ (min SDK 23/24)
    get() = this.latLng

val Place.iconMaskUrl: String? // same as Place SDK v4/5+ (min SDK 23/24)
    get() = this.iconUrl

val Field_DISPLAY_NAME = Place.Field.NAME // same as Place SDK v4/5+ (min SDK 23/24)
val Field_LOCATION = Place.Field.LAT_LNG // same as Place SDK v4/5+ (min SDK 23/24)
val Field_ICON_MASK_URL = Place.Field.ICON_URL // same as Place SDK v4/5+ (min SDK 23/24)
