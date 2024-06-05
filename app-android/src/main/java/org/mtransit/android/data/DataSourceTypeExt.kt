package org.mtransit.android.data

import org.mtransit.android.commons.data.POI

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
