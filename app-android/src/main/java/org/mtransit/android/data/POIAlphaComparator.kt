package org.mtransit.android.data

val POI_ALPHA_COMPARATOR = Comparator<POIManager> { lhs: POIManager, rhs: POIManager ->
    lhs.poi.compareToAlpha(rhs.poi)
}
