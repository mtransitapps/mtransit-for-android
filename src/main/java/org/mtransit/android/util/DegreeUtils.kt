package org.mtransit.android.util

object DegreeUtils {

    @JvmStatic
    fun convertToPositive360Degree(degree: Int): Int {
        var newDegree = degree;
        while (newDegree < 0) {
            newDegree += 360
        }
        while (newDegree > 360) {
            newDegree -= 360
        }
        return newDegree
    }
}