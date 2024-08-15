package org.mtransit.android.data

import org.mtransit.android.commons.ComparatorUtils

class POIAlphaComparator : Comparator<POIManager?> {
    override fun compare(lhs: POIManager?, rhs: POIManager?): Int {
        val lhsPoi = lhs?.poi
        val rhsPoi = rhs?.poi
        if (lhsPoi == null && rhsPoi == null) {
            return ComparatorUtils.SAME
        }
        if (lhsPoi == null) {
            return ComparatorUtils.BEFORE
        } else if (rhsPoi == null) {
            return ComparatorUtils.AFTER
        }
        return lhsPoi.compareToAlpha(null, rhsPoi)
    }
}