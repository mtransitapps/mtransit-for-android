package org.mtransit.android.commons.data

fun mkCA_CRC_EXO_TERM_BROSSARD_Q3(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    agency = CA_CRC_EXO,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 75870,
    stopName = "Term Brossard Q:3",
    stopLat = 45.43652,
    stopLng = -73.43275,
    isNoPickup = isNoPickup,
)

fun mkCA_CRC_EXO_TERM_BROSSARD_Q13(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    agency = CA_CRC_EXO,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 75873,
    stopName = "Term Brossard Q:13",
    stopLat = 45.43741,
    stopLng = -73.43162,
    isNoPickup = isNoPickup,
)

fun mkCA_CRC_EXO_TERM_BROSSARD_Q15(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    agency = CA_CRC_EXO,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 75875,
    stopName = "Term Brossard Q:15",
    stopLat = 45.43727,
    stopLng = -73.43211,
    isNoPickup = isNoPickup,
)

fun mkCA_LE_RICHELAIN_ROUSSILLON_EXO_TERM_BROSSARD_Q16(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    agency = CA_LE_RICHELAIN_ROUSSILLON_EXO,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 75876,
    stopName = "Term Brossard Q:16",
    stopLat = 45.43720,
    stopLng = -73.43238,
    isNoPickup = isNoPickup,
)

fun mkCA_LE_RICHELAIN_ROUSSILLON_EXO_TERM_BROSSARD_Q17(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    agency = CA_LE_RICHELAIN_ROUSSILLON_EXO,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 75877,
    stopName = "Term Brossard Q:17",
    stopLat = 45.43712,
    stopLng = -73.43265,
    isNoPickup = isNoPickup,
)

fun mkCA_LE_RICHELAIN_ROUSSILLON_EXO_TERM_BROSSARD_Q18(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    agency = CA_LE_RICHELAIN_ROUSSILLON_EXO,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 75878,
    stopName = "Term Brossard Q:18",
    stopLat = 45.43706,
    stopLng = -73.43290,
    isNoPickup = isNoPickup,
)

fun mkCA_LE_RICHELAIN_ROUSSILLON_EXO_TERM_BROSSARD_Q19(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    agency = CA_LE_RICHELAIN_ROUSSILLON_EXO,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 75879,
    stopName = "Term Brossard Q:19",
    stopLat = 45.43699,
    stopLng = -73.43318,
    isNoPickup = isNoPickup,
)

fun mkCA_LONGUEUIL_RTL_TERM_BROSSARD(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    agency = CA_LONGUEUIL_RTL,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 5767,
    stopName = "Terminus Brossard",
    stopLat = 45.43690,
    stopLng = -73.43154,
    isNoPickup = isNoPickup,
)

fun mkCA_LONGUEUIL_RTL_9700_LEDUC(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    agency = CA_LONGUEUIL_RTL,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 4940, // 34940
    stopName = "9700 Leduc",
    stopLat = 45.44148,
    stopLng = -73.43727,
    isNoPickup = isNoPickup,
)

val CA_MTL_BIXI_HONORE_BEAUGRAND_METRO = makeBikeStation(
    agency = CA_MTL_BIXI,
    id = 643,
    name = "Métro Honoré-Beaugrand (Sherbrooke / Honoré-Beaugrand)",
    lat = 45.59685627935051,
    lng = -73.53526366878214,
)

val CA_MTL_BIXI_PARC_DU_MAIL = makeBikeStation(
    agency = CA_MTL_BIXI,
    id = 793,
    name = "Parc du Mail (Bois de Coulonges / du Mail)",
    lat = 45.59833498124187,
    lng = -73.54596734046936,
)

val CA_MTL_BIXI_MONT_ROYAL_METRO_PL_GERALD_GODIN = makeBikeStation(
    agency = CA_MTL_BIXI,
    id = 347,
    name = "Métro Mont-Royal (Place Gérald-Godin)",
    lat = 45.52434980398798,
    lng = -73.58144380187696,
)

val CA_MTL_BIXI_MONT_ROYAL_RESTER = makeBikeStation(
    agency = CA_MTL_BIXI,
    id = 184,
    name = "Resther / du Mont-Royal",
    lat = 45.52560376254711,
    lng = -73.58171582221985,
)

val CA_MTL_BIXI_MONT_ROYAL_ST_HUBERT = makeBikeStation(
    agency = CA_MTL_BIXI,
    id = 1182,
    name = "St-Hubert / du Mont-Royal",
    lat = 45.52569462946273,
    lng = -73.58117117351865,
)

val CA_MTL_BIXI_DROLET_MARIE_ANNE = makeBikeStation(
    agency = CA_MTL_BIXI,
    id = 134,
    name = "Drolet / Marie-Anne",
    lat = 45.522402573414084,
    lng = -73.58117669820786,
)

val CA_MTL_BIXI_GILFORD_ST_DENIS = makeBikeStation(
    agency = CA_MTL_BIXI,
    id = 742,
    name = "Gilford / St-Denis",
    lat = 45.52497612960628,
    lng = -73.58558893203735,
)

val CA_MTL_BIXI_MONT_ROYAL_MENTANA = makeBikeStation(
    agency = CA_MTL_BIXI,
    id = 37,
    name = "de Mentana / du Mont-Royal",
    lat = 45.526894329885835,
    lng = -73.5799089088323,
)

val CA_MTL_BIXI_MONT_ROYAL_LAVAL = makeBikeStation(
    agency = CA_MTL_BIXI,
    id = 183,
    name = "Laval / du Mont-Royal",
    lat = 45.52229075870213,
    lng = -73.58389645814896,
)

val CA_MTL_BIXI_MONT_ROYAL_BOYER = makeBikeStation(
    agency = CA_MTL_BIXI,
    id = 169,
    name = "Boyer / du Mont-Royal",
    lat = 45.52749327748997,
    lng = -73.57999514846595,
)

val CA_MTL_BIXI_MONT_ROYAL_HOTEL_DE_VILLE = makeBikeStation(
    agency = CA_MTL_BIXI,
    id = 182,
    name = "de l'Hôtel-de-Ville / du Mont-Royal",
    lat = 45.522178004145694,
    lng = -73.58460858464241,
)

val CA_MTL_BIXI_MENTANA_MARIE_ANNE = makeBikeStation(
    agency = CA_MTL_BIXI,
    id = 188,
    name = "de Mentana / Marie-Anne",
    lat = 45.526026565221414,
    lng = -73.57785880565643,
)

val CA_MTL_BIXI_BERRY_GILFORD = makeBikeStation(
    agency = CA_MTL_BIXI,
    id = 190,
    name = "Berri / Gilford",
    lat = 45.52677632748293,
    lng = -73.58606100082396,
)

fun mkCA_MTL_REM_BROSSARD(routeId: Long = 4001L, isNoPickup: Boolean = false) = makeRDS(
    agency = CA_MTL_REM,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 10001,
    stopName = "Brossard",
    stopLat = 45.43800,
    stopLng = -73.43065,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_REM_DU_QUARTIER(routeId: Long = 4001L, isNoPickup: Boolean = false) = makeRDS(
    agency = CA_MTL_REM,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 10004,
    stopName = "Du Quartier",
    stopLat = 45.44695,
    stopLng = -73.43352,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_53251(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    agency = CA_MTL_STM_BUS,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 53251,
    stopName = "Ston Honoré-Beaugrand",
    stopLat = 45.59645,
    stopLng = -73.53470,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_53252(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    agency = CA_MTL_STM_BUS,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 53252,
    stopName = "Ston Honoré-Beaugrand",
    stopLat = 45.59679,
    stopLng = -73.53502,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_53253(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    agency = CA_MTL_STM_BUS,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 53253,
    stopName = "Ston Honoré-Beaugrand",
    stopLat = 45.59679,
    stopLng = -73.53466,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_54115(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    agency = CA_MTL_STM_BUS,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 54115,
    stopName = "Ston Honoré-Beaugrand",
    stopLat = 45.59700,
    stopLng = -73.53489,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_54257(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    agency = CA_MTL_STM_BUS,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 54257,
    stopName = "Ston Honoré-Beaugrand",
    stopLat = 45.59694,
    stopLng = -73.53456,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_61814(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    agency = CA_MTL_STM_BUS,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 61814,
    stopName = "Ston Honoré-Beaugrand",
    stopLat = 45.59683,
    stopLng = -73.53595,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_N_53724(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    agency = CA_MTL_STM_BUS,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 53724,
    stopName = "Term N (Honoré-Beaugrand)",
    stopLat = 45.59707,
    stopLng = -73.53624,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_N_53725(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    agency = CA_MTL_STM_BUS,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 53725,
    stopName = "Term N (Honoré-Beaugrand)",
    stopLat = 45.59702,
    stopLng = -73.53609,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_N_53754(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    agency = CA_MTL_STM_BUS,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 53754,
    stopName = "Term N (Honoré-Beaugrand)",
    stopLat = 45.59693,
    stopLng = -73.53575,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_N_53755(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    agency = CA_MTL_STM_BUS,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 53755,
    stopName = "Term N (Honoré-Beaugrand)",
    stopLat = 45.59704,
    stopLng = -73.53663,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_N_53756(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    agency = CA_MTL_STM_BUS,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 53756,
    stopName = "Term N (Honoré-Beaugrand)",
    stopLat = 45.59714,
    stopLng = -73.53648,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_N_54008(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    agency = CA_MTL_STM_BUS,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 54008,
    stopName = "Term N (Honoré-Beaugrand)",
    stopLat = 45.59695,
    stopLng = -73.53632,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_N_54119(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    agency = CA_MTL_STM_BUS,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 54119,
    stopName = "Term N (Honoré-Beaugrand)",
    stopLat = 45.59685,
    stopLng = -73.53602,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_S_53275(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    agency = CA_MTL_STM_BUS,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 53275,
    stopName = "Term S (Honoré-Beaugrand)",
    stopLat = 45.59653,
    stopLng = -73.53427,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_S_53876(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    agency = CA_MTL_STM_BUS,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 53876,
    stopName = "Term S (Honoré-Beaugrand)",
    stopLat = 45.59655,
    stopLng = -73.53462,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_S_54237(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    agency = CA_MTL_STM_BUS,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 54237,
    stopName = "Term S (Honoré-Beaugrand)",
    stopLat = 45.59691,
    stopLng = -73.53415,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_MONT_ROYAL_BERRI_52083(
    routeId: Long,
    originalDirectionId: Int = 0,
    directionId: Long = routeId * 100L + originalDirectionId,
    isNoPickup: Boolean = false,
) = makeRDS(
    agency = CA_MTL_STM_BUS,
    routeId = routeId,
    directionId = directionId,
    stopId = 52083,
    stopName = "Mont-Royal / Berri (Mont-Royal)",
    stopLat = 45.52464,
    stopLng = -73.58222,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_MONT_ROYAL_BERRI_52084(
    routeId: Long,
    originalDirectionId: Int = 0,
    directionId: Long = routeId * 100L + originalDirectionId,
    isNoPickup: Boolean = false,
) = makeRDS(
    agency = CA_MTL_STM_BUS,
    routeId = routeId,
    directionId = directionId,
    stopId = 52084,
    stopName = "Mont-Royal / Berri (Mont-Royal)",
    stopLat = 45.52480,
    stopLng = -73.58207,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_MONT_ROYAL_BERRI_58725(
    routeId: Long,
    originalDirectionId: Int = 0,
    directionId: Long = routeId * 100L + originalDirectionId,
    isNoPickup: Boolean = false,
) = makeRDS(
    agency = CA_MTL_STM_BUS,
    routeId = routeId,
    directionId = directionId,
    stopId = 58725,
    stopName = "Mont-Royal / Berri (Mont-Royal)",
    stopLat = 45.52507,
    stopLng = -73.58203,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_MONT_ROYAL_ST_DENIS_52055(
    routeId: Long,
    originalDirectionId: Int = 0,
    directionId: Long = routeId * 100L + originalDirectionId,
    isNoPickup: Boolean = false,
) = makeRDS(
    agency = CA_MTL_STM_BUS,
    routeId = routeId,
    directionId = directionId,
    stopId = 52055,
    stopName = "Du Mont-Royal / Saint-Denis",
    stopLat = 45.524127,
    stopLng = -73.582857,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_MONT_ROYAL_ST_DENIS_52057(
    routeId: Long,
    originalDirectionId: Int = 0,
    directionId: Long = routeId * 100L + originalDirectionId,
    isNoPickup: Boolean = false,
) = makeRDS(
    agency = CA_MTL_STM_BUS,
    routeId = routeId,
    directionId = directionId,
    stopId = 52057,
    stopName = "Du Mont-Royal / Saint-Denis",
    stopLat = 45.523809,
    stopLng = -73.582962,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_MONT_ROYAL_ST_DENIS_61896(
    routeId: Long,
    originalDirectionId: Int = 0,
    directionId: Long = routeId * 100L + originalDirectionId,
    isNoPickup: Boolean = false,
) = makeRDS(
    agency = CA_MTL_STM_BUS,
    routeId = routeId,
    directionId = directionId,
    stopId = 61896,
    stopName = "Saint-Denis / Du Mont-Royal",
    stopLat = 45.524160,
    stopLng = -73.583150,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_MONT_ROYAL_ST_DENIS_61897(
    routeId: Long,
    originalDirectionId: Int = 0,
    directionId: Long = routeId * 100L + originalDirectionId,
    isNoPickup: Boolean = false,
) = makeRDS(
    agency = CA_MTL_STM_BUS,
    routeId = routeId,
    directionId = directionId,
    stopId = 61897,
    stopName = "Saint-Denis / Du Mont-Royal",
    stopLat = 45.523690,
    stopLng = -73.582546,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_MONT_ROYAL_METRO_BERRI_61665(
    routeId: Long,
    originalDirectionId: Int = 0,
    directionId: Long = routeId * 100L + originalDirectionId,
    isNoPickup: Boolean = false,
) = makeRDS(
    agency = CA_MTL_STM_BUS,
    routeId = routeId,
    directionId = directionId,
    stopId = 61665,
    stopName = "Berri / Mont-Royal (Mont-Royal)",
    stopLat = 45.524644,
    stopLng = -73.581394,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_BUS_MONT_ROYAL_METRO_PONTIAC_61802(
    routeId: Long,
    originalDirectionId: Int = 0,
    directionId: Long = routeId * 100L + originalDirectionId,
    isNoPickup: Boolean = false,
) = makeRDS(
    agency = CA_MTL_STM_BUS,
    routeId = routeId,
    directionId = directionId,
    stopId = 61802,
    stopName = "Mont-Royal / Pontiac (Mont-Royal)",
    stopLat = 45.525346,
    stopLng = -73.581796,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_SUBWAY_HONORE_BEAUGRAND(routeId: Long = 1L, isNoPickup: Boolean = false) = makeRDS(
    agency = CA_MTL_STM_SUBWAY,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 18,
    stopName = "Honoré-Beaugrand",
    stopLat = 45.59657,
    stopLng = -73.53536,
    isNoPickup = isNoPickup,
)

fun mkCA_MTL_STM_SUBWAY_MONT_ROYAL(
    routeId: Long = 2L,
    originalDirectionId: Int = 0,
    directionId: Long = routeId * 100L + originalDirectionId,
    isNoPickup: Boolean = false,
) = makeRDS(
    agency = CA_MTL_STM_SUBWAY,
    routeId = routeId,
    directionId = directionId,
    stopId = 8,
    stopName = "Mont-Royal",
    stopLat = 45.52482,
    stopLng = -73.58171,
    isNoPickup = isNoPickup,
)

fun mkCA_RICHELIEU_TERM_BROSSARD_Q4(routeId: Long = 300L, isNoPickup: Boolean = false) = makeRDS(
    agency = CA_RICHELIEU_EXO,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 75871,
    stopName = "Term Brossard Q:4",
    stopLat = 45.43658,
    stopLng = -73.43248,
    isNoPickup = isNoPickup,
)

fun mkCA_STE_JULIE_EXO_TERM_BROSSARD_Q5(routeId: Long, isNoPickup: Boolean = false) = makeRDS(
    agency = CA_STE_JULIE_EXO,
    routeId = routeId,
    directionId = routeId * 100L + (if (isNoPickup) 9L else 0L),
    stopId = 75872,
    stopName = "Term Brossard Q:5",
    stopLat = 45.43666,
    stopLng = -73.43224,
    isNoPickup = isNoPickup,
)
