package org.mtransit.android.data

import android.location.Location
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Ignore
import com.google.android.gms.maps.model.LatLngBounds
import org.mtransit.android.commons.data.Area
import org.mtransit.android.data.IAgencyProperties.Companion.DEFAULT_LONG_VERSION_CODE

// all these properties are not dynamic: only change when module updated / data changed
data class AgencyBaseProperties(
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "type")
    override val type: DataSourceType,
    @ColumnInfo(name = "short_name")
    override val shortName: String, // sort
    @ColumnInfo(name = "color_int")
    override val colorInt: Int? = null,
    @Embedded(prefix = "area")
    override val area: Area, // nearby
    @ColumnInfo(name = "pkg")
    override val pkg: String,
    @ColumnInfo(name = "long_version_code")
    val longVersionCode: Long = DEFAULT_LONG_VERSION_CODE, // #onModulesUpdated
    @ColumnInfo(name = "is_installed")
    val isInstalled: Boolean = true, // #onModulesUpdated
    @ColumnInfo(name = "is_enabled")
    override val isEnabled: Boolean = true, // #onModulesUpdated
    @ColumnInfo(name = "is_rts") // do not change to avoid breaking change
    override val isRDS: Boolean = false,
    @ColumnInfo(name = "logo")
    override val logo: JPaths? = null,
    @ColumnInfo(name = "trigger")
    val trigger: Int = 0, // #onModulesUpdated
    @ColumnInfo(name = "extended_type")
    override val extendedType: DataSourceType? = null,
) : IAgencyNearbyUIProperties {
    @Ignore
    override val authority = id

    override fun isInArea(area: Area?): Boolean {
        return IAgencyNearbyProperties.isInArea(this, area)
    }

    override fun isEntirelyInside(area: LatLngBounds?): Boolean {
        return IAgencyNearbyProperties.isEntirelyInside(this, area)
    }

    override fun isInArea(area: LatLngBounds?): Boolean {
        return IAgencyNearbyProperties.isInArea(this, area)
    }

    override fun isEntirelyInside(otherArea: Area?): Boolean {
        return IAgencyNearbyProperties.isEntirelyInside(this, area)
    }

    fun isLocationInside(location: Location): Boolean {
        return IAgencyNearbyProperties.isLocationInside(location, area)
    }
}