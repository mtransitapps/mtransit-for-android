package org.mtransit.android.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLngBounds
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.LocaleUtils
import org.mtransit.android.commons.data.Area
import org.mtransit.android.data.IAgencyProperties.Companion.DEFAULT_LONG_VERSION_CODE
import org.mtransit.android.data.IAgencyProperties.Companion.DEFAULT_VERSION_CODE
import java.util.Locale

@Entity(tableName = "agency_properties")
data class AgencyProperties(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "type")
    override val type: DataSourceType,
    @ColumnInfo(name = "short_name")
    override val shortName: String,
    @ColumnInfo(name = "long_name")
    val longName: String, // unused?
    @ColumnInfo(name = "color_int")
    override val colorInt: Int? = null,
    @Embedded(prefix = "area")
    override val area: Area,
    @ColumnInfo(name = "pkg")
    override val pkg: String,
    @ColumnInfo(name = "long_version_code")
    override val longVersionCode: Long = DEFAULT_LONG_VERSION_CODE, // #onModulesUpdated
    @ColumnInfo(name = "available_version_code")
    override val availableVersionCode: Int = DEFAULT_VERSION_CODE,
    @ColumnInfo(name = "is_installed")
    val isInstalled: Boolean = true, // #onModulesUpdated
    @ColumnInfo(name = "is_enabled")
    override val isEnabled: Boolean = true, // #onModulesUpdated
    @ColumnInfo(name = "is_rts")
    override val isRTS: Boolean = false,
    @ColumnInfo(name = "logo")
    override val logo: JPaths? = null,
    @ColumnInfo(name = "max_valid_sec")
    val maxValidSec: Int = -1,
    @ColumnInfo(name = "trigger")
    val trigger: Int = 0, // #onModulesUpdated
    @ColumnInfo(name = "contact_us_web")
    val contactUsWeb: String? = null,
    @ColumnInfo(name = "contact_us_web_fr")
    val contactUsWebFr: String? = null,
) : IAgencyNearbyUIProperties, IAgencyUpdatableProperties {

    @Ignore
    @JvmOverloads
    constructor(
        id: String,
        type: DataSourceType,
        shortName: String,
        longName: String,
        color: String? = null,
        area: Area,
        pkg: String,
        longVersionCode: Long,
        availableVersionCode: Int,
        isInstalled: Boolean,
        isEnabled: Boolean,
        isRTS: Boolean = false,
        logo: JPaths? = null,
        maxValidSec: Int = -1,
        trigger: Int = 0,
        contactUsWeb: String? = null,
        contactUsWebFr: String? = null,
    ) : this(
        id,
        type,
        shortName,
        longName,
        color?.let { ColorUtils.parseColor(it) },
        area,
        pkg,
        longVersionCode,
        availableVersionCode,
        isInstalled,
        isEnabled,
        isRTS,
        logo,
        maxValidSec,
        trigger,
        contactUsWeb.takeIf { it?.isNotBlank() == true }, // ignore empty
        contactUsWebFr.takeIf { it?.isNotBlank() == true }, // ignore empty
    )

    @Ignore
    override val authority = id

    val shortNameLC: String
        get() = shortName.lowercase(Locale.getDefault()) // device language used

    @Suppress("unused")
    fun hasColor() = this.colorInt != null

    @Ignore
    val versionCode: Int = this.longVersionCode.toInt()

    @Suppress("unused")
    @Ignore
    val versionCodeMajor: Int = (this.longVersionCode shr 32).toInt()

    override val updateAvailable: Boolean
        get() = this.versionCode < this.availableVersionCode

    val maxValidSecSorted: Int
        get() = if (this.maxValidSec == 0) {
            Integer.MAX_VALUE // unlimited
        } else {
            this.maxValidSec
        }

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

    fun hasContactUs() = !this.contactUsWeb.isNullOrBlank()

    @Ignore
    val contactUsWebForLang = if (LocaleUtils.isFR() && !this.contactUsWebFr.isNullOrBlank()) this.contactUsWebFr else this.contactUsWeb
}
