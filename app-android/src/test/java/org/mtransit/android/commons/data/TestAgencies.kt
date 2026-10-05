package org.mtransit.android.commons.data

import org.mtransit.android.data.AgencyBaseProperties
import org.mtransit.android.data.DataSourceType

val CA_CRC_EXO = mkAgency(
    pkg = "org.mtransit.android.ca_chambly_richelieu_carignan_citcrc_bus",
    type = DataSourceType.TYPE_BUS,
    shortName = "exo CRC",
    area = Area(
        minLat = 45.297998,
        maxLat = 45.541144,
        minLng = -73.522258,
        maxLng = -73.14027
    ),
)


val CA_LE_RICHELAIN_ROUSSILLON_EXO = mkAgency(
    pkg = "org.mtransit.android.ca_le_richelain_citlr_bus",
    type = DataSourceType.TYPE_BUS,
    shortName = "EXO LR/RS",
    area = Area(
        minLat = 45.347647,
        maxLat = 45.541144,
        minLng = -73.649157,
        maxLng = -73.387584,
    ),
)

val CA_LONGUEUIL_RTL = mkAgency(
    pkg = "org.mtransit.android.ca_longueuil_rtl_bus",
    type = DataSourceType.TYPE_BUS,
    shortName = "RTL",
    area = Area(
        minLat = 45.3837659753068,
        maxLat = 45.6373480184109,
        minLng = -73.5780492556561,
        maxLng = -73.3172516037979,
    )
)

val CA_MTL_BIXI = mkAgency(
    pkg = "org.mtransit.android.ca_montreal_bixi_bike",
    type = DataSourceType.TYPE_BIKE,
    shortName = "Bixi",
    area = Area(
        minLat = 45.38,
        maxLat = 45.71,
        minLng = -73.94,
        maxLng = -71.87,
    ),
)

val CA_MTL_REM = mkAgency(
    pkg = "org.mtransit.android.ca_montreal_rem_light_rail",
    type = DataSourceType.TYPE_LIGHT_RAIL,
    shortName = "REM",
    area = Area(
        minLat = 45.431542,
        maxLat = 45.545713,
        minLng = -73.912393,
        maxLng = -73.430645,
    ),
)

val CA_MTL_STM_BUS = mkAgency(
    pkg = "org.mtransit.android.ca_montreal_stm_bus",
    type = DataSourceType.TYPE_BUS,
    shortName = "STM Bus",
    area = Area(
        minLat = 45.402668,
        maxLat = 45.701116,
        minLng = -73.966098,
        maxLng = -73.480581,
    ),
)

val CA_MTL_STM_SUBWAY = mkAgency(
    pkg = "org.mtransit.android.ca_montreal_stm_subway",
    type = DataSourceType.TYPE_SUBWAY,
    shortName = "STM Subway",
    area = Area(
        minLat = 45.446466,
        maxLat = 45.596572,
        minLng = -73.722422,
        maxLng = -73.521976,
    ),
)

val CA_RICHELIEU_EXO = mkAgency(
    pkg = "org.mtransit.android.ca_richelieu_citvr_bus",
    type = DataSourceType.TYPE_BUS,
    shortName = "EXO VR",
    area = Area(
        minLat = 45.436584,
        maxLat = 45.640965,
        minLng = -73.538626,
        maxLng = -72.945486,
    )
)

val CA_STE_JULIE_EXO = mkAgency(
    pkg = "org.mtransit.android.ca_ste_julie_omitsju_bus",
    type = DataSourceType.TYPE_BUS,
    shortName = "EXO SJU",
    area = Area(
        minLat = 45.436658,
        maxLat = 45.63294,
        minLng = -73.522217,
        maxLng = -72.960053,
    )
)

private fun mkAgency(
    pkg: String,
    shortName: String,
    type: DataSourceType,
    area: Area
) = AgencyBaseProperties(
    id = "$pkg.gtfs",
    pkg = pkg,
    type = type,
    shortName = shortName,
    area = area,
    isRDS = DataSourceTypeId.isRDSType(type.id),
)

val ALL_AGENCIES = listOf(
    CA_CRC_EXO,
    CA_LE_RICHELAIN_ROUSSILLON_EXO,
    CA_LONGUEUIL_RTL,
    CA_MTL_BIXI,
    CA_MTL_REM,
    CA_MTL_STM_BUS,
    CA_MTL_STM_SUBWAY,
    CA_RICHELIEU_EXO,
    CA_STE_JULIE_EXO,
)
