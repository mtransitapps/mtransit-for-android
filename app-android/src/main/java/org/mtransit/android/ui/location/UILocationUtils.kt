package org.mtransit.android.ui.location

import android.content.Context
import android.location.Address
import android.location.Location
import androidx.annotation.AnyThread
import org.mtransit.android.common.repository.DefaultPreferenceRepository
import org.mtransit.android.commons.LocationUtils
import kotlin.math.roundToInt
import org.mtransit.android.commons.R as commonsR

object UILocationUtils : LocationUtils() {

    @AnyThread
    @JvmStatic
    fun getLocationString(context: Context, locationAddress: Address?, accuracyInMeters: Float, distanceUnitsPref: String) = buildString {
        val isAccurate = accuracyInMeters < 5000.0f
        if (locationAddress != null) {
            if (isAccurate && locationAddress.maxAddressLineIndex > 0) {
                append(locationAddress.getAddressLine(0))
            } else if (isAccurate && locationAddress.thoroughfare != null) {
                append(locationAddress.thoroughfare)
            } else if (locationAddress.locality != null) {
                append(locationAddress.locality)
            } else if (!isAccurate && locationAddress.subAdminArea != null) {
                append(locationAddress.subAdminArea)
            } else if (isAccurate) {
                append(context.getString(commonsR.string.unknown_address))
            }
        } else if (isAccurate) {
            append(context.getString(commonsR.string.unknown_address))
        }
        if (isAccurate && accuracyInMeters > 0.0f) {
            append(" ± ").append(getDistanceString(accuracyInMeters, accuracyInMeters, distanceUnitsPref))
        }
    }

    @JvmStatic
    @AnyThread
    fun updateDistanceWithStringNN(distanceUnitsPref: String, poi: LocationPOI, currentLocation: Location) {
        val accuracyInMeters = currentLocation.accuracy
        if (!poi.hasLocation()) return
        val newDistance = distanceToInMeters(currentLocation.latitude, currentLocation.longitude, poi.getLat(), poi.getLng())
        if (poi.getDistance() > 1 && newDistance == poi.getDistance() && poi.getDistanceString() != null) return
        poi.setDistance(newDistance)
        poi.setDistanceString(getDistanceString(poi.getDistance(), accuracyInMeters, distanceUnitsPref))
    }

    @AnyThread
    private fun getDistanceString(distanceInMeters: Float, accuracyInMeters: Float, distanceUnitsPref: String) =
        when (distanceUnitsPref) {
            DefaultPreferenceRepository.PREFS_DISTANCE_UNITS_IMPERIAL -> {
                val distanceInSmall = distanceInMeters * FEET_PER_M
                val accuracyInSmall = accuracyInMeters * FEET_PER_M
                getDistance(distanceInSmall, accuracyInSmall, FEET_PER_MILE, 10, "ft", "mi")
            }

            else -> { // use Metric (default)
                getDistance(distanceInMeters, accuracyInMeters, METER_PER_KM, 1, "m", "km")
            }
        }

    @AnyThread
    private fun getDistance(distance: Float, accuracy: Float, smallPerBig: Float, threshold: Int, smallUnit: String?, bigUnit: String?) = buildString {
        if (accuracy > distance) {
            if (accuracy > (smallPerBig / threshold)) {
                val accuracyInBigUnit = accuracy / smallPerBig
                val niceAccuracyInBigUnit = (accuracyInBigUnit * 10).roundToInt().toFloat() / 10
                append("< ").append(niceAccuracyInBigUnit).append(" ").append(bigUnit)
            } else {
                val niceAccuracyInSmallUnit = accuracy.roundToInt()
                append("< ").append(getSimplerDistance(niceAccuracyInSmallUnit, accuracy)).append(" ").append(smallUnit)
            }
        } else {
            if (distance > (smallPerBig / threshold)) {
                val distanceInBigUnit = distance / smallPerBig
                val niceDistanceInBigUnit = (distanceInBigUnit * 10).roundToInt().toFloat() / 10
                append(niceDistanceInBigUnit).append(" ").append(bigUnit)
            } else {
                val niceDistanceInSmallUnit = distance.roundToInt()
                append(getSimplerDistance(niceDistanceInSmallUnit, accuracy)).append(" ").append(smallUnit)
            }
        }
    }
}
