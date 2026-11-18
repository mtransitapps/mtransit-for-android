package org.mtransit.android.ui.location

import android.content.Context
import androidx.annotation.AnyThread
import androidx.annotation.FloatRange
import org.mtransit.android.common.repository.DefaultPreferenceRepository
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.commons.data.Area
import org.mtransit.android.commons.data.LocationPOI
import org.mtransit.android.commons.metersToFeet
import org.mtransit.android.util.include
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import android.location.Address as AndroidAddress
import android.location.Location as AndroidLocation
import com.google.android.gms.maps.model.LatLng as GLatLng
import com.google.android.gms.maps.model.LatLngBounds as GLatLngBounds
import org.mtransit.android.commons.R as commonsR

object UILocationUtils : LocationUtils() {

    const val MIN_NEARBY_LIST = 10

    const val MAX_NEARBY_LIST = 20

    const val MAX_POI_NEARBY_POIS_LIST = 30

    // const val MIN_NEARBY_LIST_COVERAGE_IN_METERS = 0f // DEBUG
    const val MIN_NEARBY_LIST_COVERAGE_IN_METERS = 100f

    const val MIN_POI_NEARBY_POIS_LIST_COVERAGE_IN_METERS = 100f

    const val MAX_NEARBY_RELEVANT_COVERAGE_IN_METERS = 1_000.0f // 1 km

    const val PLACE_USE_ADDRESS_LAT_LNG_MAX_DISTANCE_IN_METER = 10.0f
    const val PLACE_SHOW_ADDRESS_SELECTED_MAX_DISTANCE_IN_METER = 33.0f

    private const val MIN_DISTANCE_IN_METER_VERY_ACCURATE = 10.0f // 10 m
    private const val MAX_DISTANCE_IN_METER_ACCURATE = 5000.0f // 5 km

    @AnyThread
    @JvmStatic
    fun getLocationString(
        context: Context,
        locationAddress: AndroidAddress?,
        accuracyInMeters: Float = 0.0F,
        distanceUnitsPref: String? = null,
        preferFeatureName: Boolean = false,
    ) = buildString {
        val isAccurate = accuracyInMeters < MAX_DISTANCE_IN_METER_ACCURATE
        if (locationAddress != null) {
            if (preferFeatureName && isAccurate && locationAddress.usefulFeatureName != null) {
                append(locationAddress.usefulFeatureName)
            } else if (accuracyInMeters <= MIN_DISTANCE_IN_METER_VERY_ACCURATE && locationAddress.maxAddressLineIndex >= 0) {
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
        appendDistance(isAccurate, accuracyInMeters, distanceUnitsPref)
    }

    private fun StringBuilder.appendDistance(isAccurate: Boolean, accuracyInMeters: Float, distanceUnitsPref: String?) {
        if (isAccurate && accuracyInMeters > 0.0f && distanceUnitsPref != null) {
            append(" ± ").append(getDistanceString(accuracyInMeters, accuracyInMeters, distanceUnitsPref))
        }
    }

    @JvmStatic
    @AnyThread
    fun updateDistanceWithStringNN(distanceUnitsPref: String, poi: LocationPOI, currentLocation: AndroidLocation) {
        if (!poi.hasLocation) return
        val newDistance = distanceToInMeters(currentLocation.latitude, currentLocation.longitude, poi.lat, poi.lng)
        if (poi.distance >= 0f && poi.distance == newDistance && poi.distanceString != null) return
        poi.distance = newDistance
        poi.distanceString = getDistanceString(poi.distance, currentLocation.accuracy, distanceUnitsPref)
    }

    @AnyThread
    private fun getDistanceString(distanceInMeters: Float, accuracyInMeters: Float, distanceUnitsPref: String) =
        when (distanceUnitsPref) {
            DefaultPreferenceRepository.PREFS_DISTANCE_UNITS_IMPERIAL -> {
                val distanceInSmall = distanceInMeters.metersToFeet
                val accuracyInSmall = accuracyInMeters.metersToFeet
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
                val niceAccuracyInBigUnit = (accuracyInBigUnit * 10f).roundToInt().toFloat() / 10f
                append("< ").append(niceAccuracyInBigUnit).append(" ").append(bigUnit)
            } else {
                val niceAccuracyInSmallUnit = accuracy.roundToInt()
                append("< ").append(getSimplerDistance(niceAccuracyInSmallUnit, accuracy)).append(" ").append(smallUnit)
            }
        } else {
            if (distance > (smallPerBig / threshold)) {
                val distanceInBigUnit = distance / smallPerBig
                val niceDistanceInBigUnit = (distanceInBigUnit * 10f).roundToInt().toFloat() / 10f
                append(niceDistanceInBigUnit).append(" ").append(bigUnit)
            } else {
                val niceDistanceInSmallUnit = distance.roundToInt()
                append(getSimplerDistance(niceDistanceInSmallUnit, accuracy)).append(" ").append(smallUnit)
            }
        }
    }

    fun computeArea(center: GLatLng, visibleLocation: GLatLng): Area {
        val visibleLocationOppCenter = GLatLng(
            center.latitude - (visibleLocation.latitude - center.latitude),
            center.longitude - (visibleLocation.longitude - center.longitude)
        )
        return Area(
            minLat = min(visibleLocationOppCenter.latitude, visibleLocation.latitude),
            maxLat = max(visibleLocationOppCenter.latitude, visibleLocation.latitude),
            minLng = min(visibleLocationOppCenter.longitude, visibleLocation.longitude),
            maxLng = max(visibleLocationOppCenter.longitude, visibleLocation.longitude)
        )
    }

    fun MutableCollection<GLatLng>.keepCenteredOnAnd(
        center: GLatLng,
        @FloatRange(0.0, 1.0) horizontalFactor: Double = 1.0,
        @FloatRange(0.0, 1.0) verticalFactor: Double = 1.0,
    ) = apply {
        val llBound = GLatLngBounds.Builder().include(this@keepCenteredOnAnd).build()
        val northeastOppCenter = GLatLng(
            center.latitude - (llBound.northeast.latitude - center.latitude).times(verticalFactor),
            center.longitude - (llBound.northeast.longitude - center.longitude).times(horizontalFactor),
        )
        add(northeastOppCenter)
        val southwestOppCenter = GLatLng(
            center.latitude - (llBound.southwest.latitude - center.latitude).times(verticalFactor),
            center.longitude - (llBound.southwest.longitude - center.longitude).times(horizontalFactor),
        )
        add(southwestOppCenter)
    }
}
