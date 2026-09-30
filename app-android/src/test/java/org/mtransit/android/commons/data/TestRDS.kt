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
