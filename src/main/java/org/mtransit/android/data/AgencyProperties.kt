package org.mtransit.android.data

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.LocationUtils
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

data class AgencyProperties(
    val id: String,
    val type: DataSourceType,
    val shortName: String,
    val longName: String,
    val colorInt: Int? = null,
    val area: LocationUtils.Area,
    val isRTS: Boolean = false,
    var logo: JPaths? = null,
) {

    companion object {
        @JvmStatic
        val SHORT_NAME_COMPARATOR: Comparator<AgencyProperties> = Comparator { lap, rap ->
            lap.shortNameLC.compareTo(rap.shortNameLC)
        }

        fun removeType(agencies: MutableCollection<AgencyProperties>?, typeToRemove: DataSourceType) {
            agencies?.let {
                agencies.removeAll {
                    it.type == typeToRemove
                }
            }
        }
    }

    @JvmOverloads
    constructor(
        id: String,
        type: DataSourceType,
        shortName: String,
        longName: String,
        color: String? = null,
        area: LocationUtils.Area,
        isRTS: Boolean = false
    ) : this(
        id,
        type,
        shortName,
        longName,
        color?.let { ColorUtils.parseColor(it) },
        area,
        isRTS
    )

    val authority = id

    val shortNameLC: String
        get() = shortName.toLowerCase(Locale.getDefault()) // device language used

    fun hasColor() = this.colorInt != null

    fun isInArea(area: LocationUtils.Area?): Boolean {
        return LocationUtils.Area.areOverlapping(area, this.area)
    }

    fun isEntirelyInside(area: LatLngBounds?): Boolean {
        return area?.let {
            it.contains(LatLng(this.area.minLat, this.area.minLng))
                    && it.contains(LatLng(this.area.maxLat, this.area.maxLng))
        } ?: false
    }

    fun isInArea(area: LatLngBounds?): Boolean {
        return areOverlapping(area, this.area)
    }

    private fun areOverlapping(area1: LatLngBounds?, area2: LocationUtils.Area?): Boolean {
        if (area1 == null || area2 == null) {
            return false // no data to compare
        }
        if (LocationUtils.isInside(area1.southwest.latitude, area1.southwest.longitude, area2)) {
            return true // min lat, min lng
        }
        if (LocationUtils.isInside(area1.southwest.latitude, area1.northeast.longitude, area2)) {
            return true // min lat, max lng
        }
        if (LocationUtils.isInside(area1.northeast.latitude, area1.southwest.longitude, area2)) {
            return true // max lat, min lng
        }
        if (LocationUtils.isInside(area1.northeast.latitude, area1.northeast.longitude, area2)) {
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

    private fun areCompletelyOverlapping(area1: LatLngBounds, area2: LocationUtils.Area): Boolean {
        val area1MinLat = area1.southwest.latitude.coerceAtMost(area1.northeast.latitude)
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

    fun isEntirelyInside(otherArea: LocationUtils.Area?): Boolean {
        return area.isEntirelyInside(otherArea)
    }
}
