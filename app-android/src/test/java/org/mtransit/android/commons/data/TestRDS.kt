package org.mtransit.android.commons.data

fun mkCA_CRC_EXO_TERM_BROSSARD_Q3(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    authority = CA_CRC_EXO.authority,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 75870,
    stopLat = 45.43652,
    stopLng = -73.43275,
    isNoPickup = isNoPickup,
)

fun mkCA_CRC_EXO_TERM_BROSSARD_Q13(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    authority = CA_CRC_EXO.authority,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 75873,
    stopLat = 45.43741,
    stopLng = -73.43162,
    isNoPickup = isNoPickup,
)

fun mkCA_CRC_EXO_TERM_BROSSARD_Q15(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    authority = CA_CRC_EXO.authority,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 75875,
    stopLat = 45.43727,
    stopLng = -73.43211,
    isNoPickup = isNoPickup,
)

fun mkCA_LE_RICHELAIN_ROUSSILLON_EXO_TERM_BROSSARD_Q16(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    authority = CA_LE_RICHELAIN_ROUSSILLON_EXO.authority,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 75876,
    stopLat = 45.43720,
    stopLng = -73.43238,
    isNoPickup = isNoPickup,
)

fun mkCA_LE_RICHELAIN_ROUSSILLON_EXO_TERM_BROSSARD_Q17(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    authority = CA_LE_RICHELAIN_ROUSSILLON_EXO.authority,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 75877,
    stopLat = 45.43712,
    stopLng = -73.43265,
    isNoPickup = isNoPickup,
)

fun mkCA_LE_RICHELAIN_ROUSSILLON_EXO_TERM_BROSSARD_Q18(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    authority = CA_LE_RICHELAIN_ROUSSILLON_EXO.authority,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 75878,
    stopLat = 45.43706,
    stopLng = -73.43290,
    isNoPickup = isNoPickup,
)

fun mkCA_LE_RICHELAIN_ROUSSILLON_EXO_TERM_BROSSARD_Q19(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    authority = CA_LE_RICHELAIN_ROUSSILLON_EXO.authority,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 75879,
    stopLat = 45.43699,
    stopLng = -73.43318,
    isNoPickup = isNoPickup,
)

fun mkCA_LONGUEUIL_RTL_TERM_BROSSARD(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    authority = CA_LONGUEUIL_RTL.authority,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 5767,
    stopLat = 45.43690,
    stopLng = -73.43154,
    isNoPickup = isNoPickup,
)

fun mkCA_LONGUEUIL_RTL_9700_LEDUC(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    authority = CA_LONGUEUIL_RTL.authority,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 4940, // 34940
    stopLat = 45.44148,
    stopLng = -73.43727,
    isNoPickup = isNoPickup,
)

val CA_MTL_BIXI_HONORE_BEAUGRAND_METRO = makeBikeStation(
    authority = CA_MTL_BIXI.authority,
    id = 643,
    lat = 45.59685627935051,
    lng = -73.53526366878214,
)

val CA_MTL_BIXI_PARC_DU_MAIL = makeBikeStation(
    authority = CA_MTL_BIXI.authority,
    id = 793,
    lat = 45.59833498124187,
    lng = -73.54596734046936,
)

fun mkCA_MTL_REM_BROSSARD(routeId: Long = 4001L, isNoPickup: Boolean = false) = makeRDS(
    authority = CA_MTL_REM.authority,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 10001,
    stopLat = 45.43800,
    stopLng = -73.43065,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_REM_DU_QUARTIER(routeId: Long = 4001L, isNoPickup: Boolean = false) = makeRDS(
    authority = CA_MTL_REM.authority,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 10004,
    stopLat = 45.44695,
    stopLng = -73.43352,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_53251(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    authority = CA_MTL_STM_BUS.authority,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 53251,
    stopLat = 45.59645,
    stopLng = -73.53470,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_53252(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    authority = CA_MTL_STM_BUS.authority,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 53252,
    stopLat = 45.59679,
    stopLng = -73.53502,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_53253(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    authority = CA_MTL_STM_BUS.authority,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 53253,
    stopLat = 45.59679,
    stopLng = -73.53466,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_54115(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    authority = CA_MTL_STM_BUS.authority,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 54115,
    stopLat = 45.59700,
    stopLng = -73.53489,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_54257(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    authority = CA_MTL_STM_BUS.authority,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 54257,
    stopLat = 45.59694,
    stopLng = -73.53456,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_61814(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    authority = CA_MTL_STM_BUS.authority,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 61814,
    stopLat = 45.59683,
    stopLng = -73.53595,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_N_53724(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    authority = CA_MTL_STM_BUS.authority,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 53724,
    stopLat = 45.59707,
    stopLng = -73.53624,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_N_53725(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    authority = CA_MTL_STM_BUS.authority,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 53725,
    stopLat = 45.59702,
    stopLng = -73.53609,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_N_53754(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    authority = CA_MTL_STM_BUS.authority,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 53754,
    stopLat = 45.59693,
    stopLng = -73.53575,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_N_53755(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    authority = CA_MTL_STM_BUS.authority,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 53755,
    stopLat = 45.59704,
    stopLng = -73.53663,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_N_53756(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    authority = CA_MTL_STM_BUS.authority,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 53756,
    stopLat = 45.59714,
    stopLng = -73.53648,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_N_54008(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    authority = CA_MTL_STM_BUS.authority,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 54008,
    stopLat = 45.59695,
    stopLng = -73.53632,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_N_54119(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    authority = CA_MTL_STM_BUS.authority,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 54119,
    stopLat = 45.59685,
    stopLng = -73.53602,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_S_53275(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    authority = CA_MTL_STM_BUS.authority,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 53275,
    stopLat = 45.59653,
    stopLng = -73.53427,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_S_53876(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    authority = CA_MTL_STM_BUS.authority,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 53876,
    stopLat = 45.59655,
    stopLng = -73.53462,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_S_54237(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    authority = CA_MTL_STM_BUS.authority,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 54237,
    stopLat = 45.59691,
    stopLng = -73.53415,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_SUBWAY_HONORE_BEAUGRAND(routeId: Long = 1L, isNoPickup: Boolean = false) = makeRDS(
    authority = CA_MTL_STM_SUBWAY.authority,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 18,
    stopLat = 45.59657,
    stopLng = -73.53536,
    isNoPickup = isNoPickup,
)

fun mkCA_RICHELIEU_TERM_BROSSARD(routeId: Long = 300L, isNoPickup: Boolean = false) = makeRDS(
    authority = CA_RICHELIEU_EXO.authority,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 75871,
    stopLat = 45.43658,
    stopLng = -73.43248,
    isNoPickup = isNoPickup,
)

fun mkCA_STE_JULIE_EXO_TERM_BROSSARD_Q5(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    authority = CA_STE_JULIE_EXO.authority,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 75872,
    stopLat = 45.43666,
    stopLng = -73.43224,
    isNoPickup = isNoPickup,
)
