package org.mtransit.android.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.LocationUtils
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@Entity(tableName = "agency_properties")
data class AgencyProperties(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "type")
    val type: DataSourceType,
    @ColumnInfo(name = "short_name")
    val shortName: String,
    @ColumnInfo(name = "long_name")
    val longName: String,
    @ColumnInfo(name = "color_int")
    val colorInt: Int? = null,
    @Embedded(prefix = "area")
    val area: LocationUtils.Area,
    @ColumnInfo(name = "pkg")
    val pkg: String,
    @ColumnInfo(name = "long_version_code")
    val longVersionCode: Long = DEFAULT_VERSION_CODE,
    @ColumnInfo(name = "is_installed")
    val isInstalled: Boolean = true,
    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean = true,
    @ColumnInfo(name = "is_rts")
    val isRTS: Boolean = false,
    @ColumnInfo(name = "logo")
    val logo: JPaths? = null,
    @ColumnInfo(name = "max_valid_sec")
    val maxValidSec: Int = -1
) {

    companion object {

        const val DEFAULT_VERSION_CODE = -1L

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
        pkg: String,
        longVersionCode: Long,
        isInstalled: Boolean,
        isEnabled: Boolean,
        isRTS: Boolean = false,
        logo: JPaths? = null,
        maxValidSec: Int = -1
    ) : this(
        id,
        type,
        shortName,
        longName,
        color?.let { ColorUtils.parseColor(it) },
        area,
        pkg,
        longVersionCode,
        isInstalled,
        isEnabled,
        isRTS,
        logo,
        maxValidSec
    )

    @Ignore
    val authority = id

    val shortNameLC: String
        get() = shortName.toLowerCase(Locale.getDefault()) // device language used

    fun hasColor() = this.colorInt != null

    val maxValidSecSorted: Int
        get() = if (this.maxValidSec == 0) {
            Integer.MAX_VALUE // unlimited
        } else {
            this.maxValidSec
        }

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
