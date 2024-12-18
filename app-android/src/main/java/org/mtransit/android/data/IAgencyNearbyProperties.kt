package org.mtransit.android.data

import android.location.Location
import com.google.android.gms.maps.model.LatLngBounds
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.commons.data.Area
import org.mtransit.android.util.containsEntirely
import org.mtransit.android.util.toLatLngBounds
import kotlin.math.max
import kotlin.math.min

interface IAgencyNearbyProperties : IAgencyProperties {

    companion object {

        fun isLocationInside(location: Location, area: Area): Boolean {
            return area.isInside(location.latitude, location.longitude)
        }

        fun isInArea(agency: IAgencyNearbyProperties, area: Area?): Boolean {
            return Area.areOverlapping(area, agency.area)
        }

        fun isEntirelyInside(agency: IAgencyNearbyProperties, area: Area?): Boolean {
            return agency.area.isEntirelyInside(area)
        }

        fun isInArea(agency: IAgencyNearbyProperties, area: LatLngBounds?): Boolean {
            return areOverlapping(area, agency.area)
        }

        fun isEntirelyInside(agency: IAgencyNearbyProperties, area: LatLngBounds?): Boolean {
            return area?.containsEntirely(agency.area.toLatLngBounds()) == true
        }

        private fun areOverlapping(area1: LatLngBounds?, area2: Area?): Boolean {
            if (area1 == null || area2 == null) {
                return false // no data to compare
            }
            if (Area.isInside(area1.southwest.latitude, area1.southwest.longitude, area2)) {
                return true // min lat, min lng
            }
            if (Area.isInside(area1.southwest.latitude, area1.northeast.longitude, area2)) {
                return true // min lat, max lng
            }
            if (Area.isInside(area1.northeast.latitude, area1.southwest.longitude, area2)) {
                return true // max lat, min lng
            }
            if (Area.isInside(area1.northeast.latitude, area1.northeast.longitude, area2)) {
                return true // max lat, max lng
            }
            if (isInside(area2.minLat, area2.minLng, area1)) {
                return true // min lat, min lng
            }
            if (isInside(area2.minLat, area2.maxLng, area1)) {
                return true // min lat, max lng
            }
            if (isInside(area2.maxLat, area2.minLng, area1)) {
                return true // max lat, min lng
            }
            return if (isInside(area2.maxLat, area2.maxLng, area1)) {
                true // max lat, max lng
            } else areCompletelyOverlapping(area1, area2)
        }

        private fun isInside(lat: Double, lng: Double, area: LatLngBounds?): Boolean {
            return area?.let {
                val minLat = it.southwest.latitude.coerceAtMost(it.northeast.latitude)
                val maxLat = it.southwest.latitude.coerceAtLeast(it.northeast.latitude)
                val minLng = it.southwest.longitude.coerceAtMost(it.northeast.longitude)
                val maxLng = it.southwest.longitude.coerceAtLeast(it.northeast.longitude)
                return LocationUtils.isInside(lat, lng, minLat, maxLat, minLng, maxLng)
            } ?: false
        }

        private fun areCompletelyOverlapping(area1: LatLngBounds, area2: Area): Boolean {
            val area1MinLat = min(area1.southwest.latitude, area1.northeast.latitude)
            val area1MaxLat = max(area1.southwest.latitude, area1.northeast.latitude)
            val area1MinLng = min(area1.southwest.longitude, area1.northeast.longitude)
            val area1MaxLng = max(area1.southwest.longitude, area1.northeast.longitude)
            if (area1MinLat >= area2.minLat && area1MaxLat <= area2.maxLat //
                && area2.minLng >= area1MinLng && area2.maxLng <= area1MaxLng
            ) {
                return true // area 1 wider than area 2 but area 2 higher than area 1
            }
            @Suppress("RedundantIf")
            return if (area2.minLat >= area1MinLat && area2.maxLat <= area1MaxLat //
                && area1MinLng >= area2.minLng && area1MaxLng <= area2.maxLng
            ) {
                true // area 2 wider than area 1 but area 1 higher than area 2
            } else false
        }
    }

    val area: Area

    fun isInArea(area: Area?): Boolean

    fun isEntirelyInside(area: LatLngBounds?): Boolean

    fun isInArea(area: LatLngBounds?): Boolean

    fun isEntirelyInside(otherArea: Area?): Boolean
}