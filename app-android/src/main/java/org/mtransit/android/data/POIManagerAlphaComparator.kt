package org.mtransit.android.data

import org.mtransit.android.commons.data.POI_ALPHA_COMPARATOR

val POI_MANAGER_ALPHA_COMPARATOR = Comparator<POIManager> { lhs: POIManager, rhs: POIManager ->
    POI_ALPHA_COMPARATOR.compare(lhs.poi, rhs.poi)
}
