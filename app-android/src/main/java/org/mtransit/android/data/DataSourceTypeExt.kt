package org.mtransit.android.data

import android.content.Context
import org.mtransit.android.R
import org.mtransit.android.commons.capitalize
import org.mtransit.android.commons.data.POI

fun DataSourceType.getPoiShortNameK(context: Context): CharSequence {
    if (this.stopType == DataSourceStopType.PLACE || this.stopType == DataSourceStopType.MODULE) {
        return context.getString(this.stopType.stopsStringResId)
    }
    return context.getString(
        R.string.agency_type_stops_short_name,
        context.getString(shortNamesResId).lowercase(),
        context.getString(this.stopType.stopsStringResId).lowercase()
    ).capitalize()
}

fun DataSourceType.getNearbyNameK(context: Context): CharSequence =
    context.getString(
        R.string.agency_type_stops_nearby,
        context.getString(this.stopType.stopsStringResId).lowercase()
    ).capitalize()

@JvmName("updateSupportedTypePOI")
fun <LIST : List<POI>> LIST.updateSupportedType(agencyProperties: IAgencyProperties?) =
    this.updateSupportedType(agencyProperties?.getSupportedType())

@JvmName("updateSupportedTypePOI")
fun <LIST : List<POI>> LIST.updateSupportedType(supportedType: DataSourceType?) = this.apply {
    if (supportedType?.isExtendedType == true) {
        forEach {
            it.dataSourceTypeId = supportedType.id
        }
    }
}

@JvmName("updateSupportedTypePOIM")
fun <LIST : List<POIManager>> LIST.updateSupportedType(agencyProperties: IAgencyProperties?) =
    this.updateSupportedType(agencyProperties?.getSupportedType())

@JvmName("updateSupportedTypePOIM")
fun <LIST : List<POIManager>> LIST.updateSupportedType(supportedType: DataSourceType?) = this.apply {
    if (supportedType?.isExtendedType == true) {
        forEach {
            it.poi.dataSourceTypeId = supportedType.id
        }
    }
}

fun POIManager.updateSupportedType(agencyProperties: IAgencyProperties?) =
    this.updateSupportedType(agencyProperties?.getSupportedType())

fun POIManager.updateSupportedType(supportedType: DataSourceType?) = this.apply {
    poi.updateSupportedType(supportedType)
}

fun POI.updateSupportedType(agencyProperties: IAgencyProperties?) =
    this.updateSupportedType(agencyProperties?.getSupportedType())

fun POI.updateSupportedType(supportedType: DataSourceType?) = this.apply {
    if (supportedType?.isExtendedType == true) {
        dataSourceTypeId = supportedType.id
    }
}
