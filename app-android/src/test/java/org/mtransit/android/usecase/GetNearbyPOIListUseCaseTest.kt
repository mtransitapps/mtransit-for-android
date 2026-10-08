package org.mtransit.android.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.or
import org.mockito.kotlin.whenever
import org.mockito.stubbing.OngoingStubbing
import org.mtransit.android.commons.MainDispatcherRule
import org.mtransit.android.commons.data.ALL_AGENCIES
import org.mtransit.android.commons.data.CA_CRC_EXO
import org.mtransit.android.commons.data.CA_LE_RICHELAIN_ROUSSILLON_EXO
import org.mtransit.android.commons.data.CA_LONGUEUIL_RTL
import org.mtransit.android.commons.data.CA_MTL_BIXI
import org.mtransit.android.commons.data.CA_MTL_BIXI_BERRY_GILFORD
import org.mtransit.android.commons.data.CA_MTL_BIXI_DROLET_MARIE_ANNE
import org.mtransit.android.commons.data.CA_MTL_BIXI_GILFORD_ST_DENIS
import org.mtransit.android.commons.data.CA_MTL_BIXI_HONORE_BEAUGRAND_METRO
import org.mtransit.android.commons.data.CA_MTL_BIXI_MENTANA_MARIE_ANNE
import org.mtransit.android.commons.data.CA_MTL_BIXI_MONT_ROYAL_BOYER
import org.mtransit.android.commons.data.CA_MTL_BIXI_MONT_ROYAL_HOTEL_DE_VILLE
import org.mtransit.android.commons.data.CA_MTL_BIXI_MONT_ROYAL_LAVAL
import org.mtransit.android.commons.data.CA_MTL_BIXI_MONT_ROYAL_MENTANA
import org.mtransit.android.commons.data.CA_MTL_BIXI_MONT_ROYAL_METRO_PL_GERALD_GODIN
import org.mtransit.android.commons.data.CA_MTL_BIXI_MONT_ROYAL_RESTER
import org.mtransit.android.commons.data.CA_MTL_BIXI_MONT_ROYAL_ST_HUBERT
import org.mtransit.android.commons.data.CA_MTL_BIXI_PARC_DU_MAIL
import org.mtransit.android.commons.data.CA_MTL_REM
import org.mtransit.android.commons.data.CA_MTL_STM_BUS
import org.mtransit.android.commons.data.CA_MTL_STM_SUBWAY
import org.mtransit.android.commons.data.CA_RICHELIEU_EXO
import org.mtransit.android.commons.data.CA_STE_JULIE_EXO
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.data.mkCA_CRC_EXO_TERM_BROSSARD_Q13
import org.mtransit.android.commons.data.mkCA_CRC_EXO_TERM_BROSSARD_Q15
import org.mtransit.android.commons.data.mkCA_CRC_EXO_TERM_BROSSARD_Q3
import org.mtransit.android.commons.data.mkCA_LE_RICHELAIN_ROUSSILLON_EXO_TERM_BROSSARD_Q16
import org.mtransit.android.commons.data.mkCA_LE_RICHELAIN_ROUSSILLON_EXO_TERM_BROSSARD_Q17
import org.mtransit.android.commons.data.mkCA_LE_RICHELAIN_ROUSSILLON_EXO_TERM_BROSSARD_Q18
import org.mtransit.android.commons.data.mkCA_LE_RICHELAIN_ROUSSILLON_EXO_TERM_BROSSARD_Q19
import org.mtransit.android.commons.data.mkCA_LONGUEUIL_RTL_9700_LEDUC
import org.mtransit.android.commons.data.mkCA_LONGUEUIL_RTL_TERM_BROSSARD
import org.mtransit.android.commons.data.mkCA_MTL_REM_BROSSARD
import org.mtransit.android.commons.data.mkCA_MTL_REM_DU_QUARTIER
import org.mtransit.android.commons.data.mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_53251
import org.mtransit.android.commons.data.mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_53252
import org.mtransit.android.commons.data.mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_53253
import org.mtransit.android.commons.data.mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_54115
import org.mtransit.android.commons.data.mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_54257
import org.mtransit.android.commons.data.mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_61814
import org.mtransit.android.commons.data.mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_N_53724
import org.mtransit.android.commons.data.mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_N_53725
import org.mtransit.android.commons.data.mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_N_53754
import org.mtransit.android.commons.data.mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_N_53755
import org.mtransit.android.commons.data.mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_N_53756
import org.mtransit.android.commons.data.mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_N_54008
import org.mtransit.android.commons.data.mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_N_54119
import org.mtransit.android.commons.data.mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_S_53275
import org.mtransit.android.commons.data.mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_S_53876
import org.mtransit.android.commons.data.mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_S_54237
import org.mtransit.android.commons.data.mkCA_MTL_STM_BUS_MONT_ROYAL_BERRI_52083
import org.mtransit.android.commons.data.mkCA_MTL_STM_BUS_MONT_ROYAL_BERRI_52084
import org.mtransit.android.commons.data.mkCA_MTL_STM_BUS_MONT_ROYAL_BERRI_58725
import org.mtransit.android.commons.data.mkCA_MTL_STM_BUS_MONT_ROYAL_METRO_BERRI_61665
import org.mtransit.android.commons.data.mkCA_MTL_STM_BUS_MONT_ROYAL_METRO_PONTIAC_61802
import org.mtransit.android.commons.data.mkCA_MTL_STM_BUS_MONT_ROYAL_ST_DENIS_52055
import org.mtransit.android.commons.data.mkCA_MTL_STM_BUS_MONT_ROYAL_ST_DENIS_52057
import org.mtransit.android.commons.data.mkCA_MTL_STM_BUS_MONT_ROYAL_ST_DENIS_61896
import org.mtransit.android.commons.data.mkCA_MTL_STM_BUS_MONT_ROYAL_ST_DENIS_61897
import org.mtransit.android.commons.data.mkCA_MTL_STM_SUBWAY_HONORE_BEAUGRAND
import org.mtransit.android.commons.data.mkCA_MTL_STM_SUBWAY_MONT_ROYAL
import org.mtransit.android.commons.data.mkCA_RICHELIEU_TERM_BROSSARD_Q4
import org.mtransit.android.commons.data.mkCA_STE_JULIE_EXO_TERM_BROSSARD_Q5
import org.mtransit.android.commons.distanceToInMetersJVM
import org.mtransit.android.commons.getAroundCoveredDistanceInMetersJVM
import org.mtransit.android.commons.updateDistanceMJVM
import org.mtransit.android.data.AgencyBaseProperties
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.POIConnectionComparator
import org.mtransit.android.data.POIManager
import org.mtransit.android.data.dataSourceTypeId
import org.mtransit.android.data.toPOIM
import org.mtransit.android.data.uuid
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.ui.fragment.POIViewModel
import org.mtransit.android.ui.location.UILocationUtils
import org.mtransit.commons.CommonsApp
import org.mtransit.commons.sortWithAnd
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetNearbyPOIListUseCaseTest {

    @get:Rule
    private val mainDispatcherRule = MainDispatcherRule()

    private val poiRepository: POIRepository = mock { }

    private val subject = GetNearbyPOIListUseCase(
        poiRepository = poiRepository,
        getAroundCoveredDistanceInMeters = { lat, lng, aroundDiff ->
            getAroundCoveredDistanceInMetersJVM(lat, lng, aroundDiff)
        },
        ioDispatcher = mainDispatcherRule.testDispatcher
    )

    @BeforeTest
    fun setUp() {
        CommonsApp.setup(false)
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @Test
    fun test_POI_Nearby_Connection_REM_Brossard() = runTest(mainDispatcherRule.testDispatcher) {
        val mainAgency = CA_MTL_REM
        val mainPOI = mkCA_MTL_REM_BROSSARD(4001L)
        whenever_POIMsAroundLoc(CA_CRC_EXO, mainPOI) doReturn mutableListOf<POIManager>().apply {
            listOf(483L, 484L, 486L).forEach {
                add(mkCA_CRC_EXO_TERM_BROSSARD_Q3(routeId = it).toPOIM())
                add(mkCA_CRC_EXO_TERM_BROSSARD_Q3(routeId = it, isNoPickup = true).toPOIM())
            }
            listOf(481L, 482L).forEach {
                add(mkCA_CRC_EXO_TERM_BROSSARD_Q13(routeId = it).toPOIM())
                add(mkCA_CRC_EXO_TERM_BROSSARD_Q13(routeId = it, isNoPickup = true).toPOIM())
            }
            listOf(485L, 487L, 488L).forEach {
                add(mkCA_CRC_EXO_TERM_BROSSARD_Q15(routeId = it).toPOIM())
                add(mkCA_CRC_EXO_TERM_BROSSARD_Q15(routeId = it, isNoPickup = true).toPOIM())
            }
        }.updateDistanceMJVM(lat = mainPOI.lat, lng = mainPOI.lng)
        whenever_POIMsAroundLoc(CA_LE_RICHELAIN_ROUSSILLON_EXO, mainPOI) doReturn mutableListOf<POIManager>().apply {
            listOf(554L).forEach {
                add(mkCA_LE_RICHELAIN_ROUSSILLON_EXO_TERM_BROSSARD_Q16(routeId = it).toPOIM())
                add(mkCA_LE_RICHELAIN_ROUSSILLON_EXO_TERM_BROSSARD_Q16(routeId = it, isNoPickup = true).toPOIM())
            }
            listOf(451L, 452L, 454L).forEach {
                add(mkCA_LE_RICHELAIN_ROUSSILLON_EXO_TERM_BROSSARD_Q17(routeId = it).toPOIM())
                add(mkCA_LE_RICHELAIN_ROUSSILLON_EXO_TERM_BROSSARD_Q17(routeId = it, isNoPickup = true).toPOIM())
            }
            listOf(450L, 456L, 457L).forEach {
                add(mkCA_LE_RICHELAIN_ROUSSILLON_EXO_TERM_BROSSARD_Q18(routeId = it).toPOIM())
                add(mkCA_LE_RICHELAIN_ROUSSILLON_EXO_TERM_BROSSARD_Q18(routeId = it, isNoPickup = true).toPOIM())
            }
            listOf(455L, 552L).forEach {
                add(mkCA_LE_RICHELAIN_ROUSSILLON_EXO_TERM_BROSSARD_Q19(routeId = it).toPOIM())
                add(mkCA_LE_RICHELAIN_ROUSSILLON_EXO_TERM_BROSSARD_Q19(routeId = it, isNoPickup = true).toPOIM())
            }
        }.updateDistanceMJVM(lat = mainPOI.lat, lng = mainPOI.lng)
        whenever_POIMsAroundLoc(CA_LONGUEUIL_RTL, mainPOI) doReturn mutableListOf<POIManager>().apply {
            listOf(4L, 14L, 32L, 38L, 44L, 114L, 132L).forEach {
                add(mkCA_LONGUEUIL_RTL_TERM_BROSSARD(routeId = it).toPOIM())
                add(mkCA_LONGUEUIL_RTL_TERM_BROSSARD(routeId = it, isNoPickup = true).toPOIM())
            }
            add(mkCA_LONGUEUIL_RTL_9700_LEDUC(38L).toPOIM()) // should be ignored too far / irrelevant
            add(mkCA_LONGUEUIL_RTL_9700_LEDUC(999L).toPOIM()) // fake route (distinct from Term Brossard routes) // should be ignored too far / irrelevant
        }.updateDistanceMJVM(lat = mainPOI.lat, lng = mainPOI.lng)
        whenever_POIMsAroundLoc(CA_MTL_REM, mainPOI) doReturn mutableListOf<POIManager>().apply {
            listOf(4001L).forEach {
                add(mkCA_MTL_REM_BROSSARD(routeId = it).toPOIM()) // should be ignored (main POI)
                add(mkCA_MTL_REM_BROSSARD(routeId = it, isNoPickup = true).toPOIM()) // should be kept (same Route & Direction as main POI)
                add(mkCA_MTL_REM_DU_QUARTIER(routeId = it).toPOIM()) // should be ignored too far / irrelevant
                add(mkCA_MTL_REM_DU_QUARTIER(routeId = it, isNoPickup = true).toPOIM()) // should be ignored too far / irrelevant
            }
        }.updateDistanceMJVM(lat = mainPOI.lat, lng = mainPOI.lng)
        whenever_POIMsAroundLoc(CA_RICHELIEU_EXO, mainPOI) doReturn mutableListOf<POIManager>().apply {
            listOf(300L).forEach {
                add(mkCA_RICHELIEU_TERM_BROSSARD_Q4(routeId = it).toPOIM())
                add(mkCA_RICHELIEU_TERM_BROSSARD_Q4(routeId = it, isNoPickup = true).toPOIM())
            }
        }.updateDistanceMJVM(lat = mainPOI.lat, lng = mainPOI.lng)
        whenever_POIMsAroundLoc(CA_STE_JULIE_EXO, mainPOI) doReturn mutableListOf<POIManager>().apply {
            listOf(600L).forEach {
                add(mkCA_STE_JULIE_EXO_TERM_BROSSARD_Q5(routeId = it).toPOIM())
                add(mkCA_STE_JULIE_EXO_TERM_BROSSARD_Q5(routeId = it, isNoPickup = true).toPOIM())
            }
        }.updateDistanceMJVM(lat = mainPOI.lat, lng = mainPOI.lng)
        (ALL_AGENCIES - setOf(CA_CRC_EXO, CA_LE_RICHELAIN_ROUSSILLON_EXO, CA_LONGUEUIL_RTL, CA_MTL_REM, CA_RICHELIEU_EXO, CA_STE_JULIE_EXO)).forEach {
            whenever_POIMsAroundLoc(it, mainPOI) doReturn mutableListOf()
        }

        val result = subject.invoke(
            lat = mainPOI.lat,
            lng = mainPOI.lng,
            allAgencies = ALL_AGENCIES,
            minSize = 1,
            maxSize = UILocationUtils.MAX_POI_NEARBY_POIS_LIST,
            minCoverageInMeters = UILocationUtils.MIN_POI_NEARBY_POIS_LIST_COVERAGE_IN_METERS,
            getMaxDistanceInMeters = getMaxDistanceInMeters,
            mainAgency = mainAgency,
            excludeAgency = POIViewModel.NEARBY_CONNECTIONS_EXCLUDE_AGENCY,
            excludePOI = { POIViewModel.NEARBY_CONNECTIONS_EXCLUDE_POI(it, mainPOI) },
        ).sortWithAnd(mkPOIConnectionComparator(targetedPOI = mainPOI))

        assertEquals(27, result.size)
        with(result.map { it.dataSourceTypeId }.distinct()) {
            assertEquals(2, size)
            assertEquals(DataSourceType.TYPE_LIGHT_RAIL.id, this[0])
            assertEquals(DataSourceType.TYPE_BUS.id, this[1])
        }
        with(result.filter { it.dataSourceTypeId == DataSourceType.TYPE_LIGHT_RAIL.id }) {
            assertEquals(1, size)
            assertEquals(mkCA_MTL_REM_BROSSARD(4001L, isNoPickup = true).uuid, this[0].uuid)
        }
        with(result.filter { it.dataSourceTypeId == DataSourceType.TYPE_BUS.id }) {
            assertEquals(26, size)
            var index = -1
            assertEquals(mkCA_LONGUEUIL_RTL_TERM_BROSSARD(4L).uuid, this[++index].uuid)
            assertEquals(mkCA_LONGUEUIL_RTL_TERM_BROSSARD(14L).uuid, this[++index].uuid)
            assertEquals(mkCA_LONGUEUIL_RTL_TERM_BROSSARD(32L).uuid, this[++index].uuid)
            assertEquals(mkCA_LONGUEUIL_RTL_TERM_BROSSARD(38L).uuid, this[++index].uuid)
            assertEquals(mkCA_LONGUEUIL_RTL_TERM_BROSSARD(44L).uuid, this[++index].uuid)
            assertEquals(mkCA_LONGUEUIL_RTL_TERM_BROSSARD(114L).uuid, this[++index].uuid)
            assertEquals(mkCA_LONGUEUIL_RTL_TERM_BROSSARD(132L).uuid, this[++index].uuid)
            assertEquals(mkCA_RICHELIEU_TERM_BROSSARD_Q4(300L).uuid, this[++index].uuid)
            assertEquals(mkCA_LE_RICHELAIN_ROUSSILLON_EXO_TERM_BROSSARD_Q18(450L).uuid, this[++index].uuid)
            assertEquals(mkCA_LE_RICHELAIN_ROUSSILLON_EXO_TERM_BROSSARD_Q17(451L).uuid, this[++index].uuid)
            assertEquals(mkCA_LE_RICHELAIN_ROUSSILLON_EXO_TERM_BROSSARD_Q17(452L).uuid, this[++index].uuid)
            assertEquals(mkCA_LE_RICHELAIN_ROUSSILLON_EXO_TERM_BROSSARD_Q17(454L).uuid, this[++index].uuid)
            assertEquals(mkCA_LE_RICHELAIN_ROUSSILLON_EXO_TERM_BROSSARD_Q19(455L).uuid, this[++index].uuid)
            assertEquals(mkCA_LE_RICHELAIN_ROUSSILLON_EXO_TERM_BROSSARD_Q18(456L).uuid, this[++index].uuid)
            assertEquals(mkCA_LE_RICHELAIN_ROUSSILLON_EXO_TERM_BROSSARD_Q18(457L).uuid, this[++index].uuid)
            assertEquals(mkCA_CRC_EXO_TERM_BROSSARD_Q13(481L).uuid, this[++index].uuid)
            assertEquals(mkCA_CRC_EXO_TERM_BROSSARD_Q13(482L).uuid, this[++index].uuid)
            assertEquals(mkCA_CRC_EXO_TERM_BROSSARD_Q3(483L).uuid, this[++index].uuid)
            assertEquals(mkCA_CRC_EXO_TERM_BROSSARD_Q3(484L).uuid, this[++index].uuid)
            assertEquals(mkCA_CRC_EXO_TERM_BROSSARD_Q15(485L).uuid, this[++index].uuid)
            assertEquals(mkCA_CRC_EXO_TERM_BROSSARD_Q3(486L).uuid, this[++index].uuid)
            assertEquals(mkCA_CRC_EXO_TERM_BROSSARD_Q15(487L).uuid, this[++index].uuid)
            assertEquals(mkCA_CRC_EXO_TERM_BROSSARD_Q15(488L).uuid, this[++index].uuid)
            assertEquals(mkCA_LE_RICHELAIN_ROUSSILLON_EXO_TERM_BROSSARD_Q19(552L).uuid, this[++index].uuid)
            assertEquals(mkCA_LE_RICHELAIN_ROUSSILLON_EXO_TERM_BROSSARD_Q16(554L).uuid, this[++index].uuid)
            assertEquals(mkCA_STE_JULIE_EXO_TERM_BROSSARD_Q5(600L).uuid, this[++index].uuid)
            assertEquals(this.size, ++index)
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @Test
    fun test_POI_Nearby_Connection_Metro_Honore_Beaugrand() = runTest(mainDispatcherRule.testDispatcher) {
        val mainAgency = CA_MTL_BIXI
        val mainPOI = CA_MTL_BIXI_HONORE_BEAUGRAND_METRO
        whenever_POIMsAroundLoc(CA_MTL_BIXI, mainPOI) doReturn mutableListOf<POIManager>().apply {
            add(CA_MTL_BIXI_HONORE_BEAUGRAND_METRO.toPOIM())
            add(CA_MTL_BIXI_PARC_DU_MAIL.toPOIM())
        }.updateDistanceMJVM(lat = mainPOI.lat, lng = mainPOI.lng)
        whenever_POIMsAroundLoc(CA_MTL_STM_BUS, mainPOI) doReturn mutableListOf<POIManager>().apply {
            listOf(85L, 189L).forEach {
                add(mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_53251(routeId = it).toPOIM())
            }
            listOf(26L, 364L).forEach {
                add(mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_53252(routeId = it).toPOIM())
            }
            listOf(18L to false, 364L to true).forEach { (routeId, isNoPickup) ->
                add(mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_53253(routeId = routeId, isNoPickup = isNoPickup).toPOIM())
            }
            listOf(186L, 187L, 486L, 487L).forEach {
                add(mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_54115(routeId = it, isNoPickup = true).toPOIM())
            }
            listOf(26L, 28L, 362L).forEach {
                add(mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_54257(routeId = it).toPOIM())
            }
            listOf(189L).forEach {
                add(mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_61814(routeId = it, isNoPickup = true).toPOIM())
            }
            listOf(187L).forEach {
                add(mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_N_53724(routeId = it).toPOIM())
            }
            listOf(186L).forEach {
                add(mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_N_53725(routeId = it).toPOIM())
            }
            listOf(189L).forEach {
                add(mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_N_53754(routeId = it).toPOIM())
            }
            listOf(185L).forEach {
                add(mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_N_53755(routeId = it).toPOIM())
            }
            listOf(85L).forEach {
                add(mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_N_53756(routeId = it).toPOIM())
            }
            listOf(487L).forEach {
                add(mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_N_54008(routeId = it).toPOIM())
            }
            listOf(486L).forEach {
                add(mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_N_54119(routeId = it).toPOIM())
            }
            listOf(18L to false, 370L to false, 370L to true).forEach { (routeId, isNoPickup) ->
                add(mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_S_53275(routeId = routeId, isNoPickup = isNoPickup).toPOIM())
            }
            listOf(141L).forEach {
                add(mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_S_53876(routeId = it).toPOIM())
            }
            listOf(18L to false, 141L to true, 362L to false).forEach { (routeId, isNoPickup) ->
                add(mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_S_54237(routeId = routeId, isNoPickup = isNoPickup).toPOIM())
            }
        }.updateDistanceMJVM(lat = mainPOI.lat, lng = mainPOI.lng)
        whenever_POIMsAroundLoc(CA_MTL_STM_SUBWAY, mainPOI) doReturn mutableListOf<POIManager>().apply {
            add(mkCA_MTL_STM_SUBWAY_HONORE_BEAUGRAND().toPOIM())
            add(mkCA_MTL_STM_SUBWAY_HONORE_BEAUGRAND(isNoPickup = true).toPOIM())
        }.updateDistanceMJVM(lat = mainPOI.lat, lng = mainPOI.lng)
        (ALL_AGENCIES - setOf(CA_MTL_BIXI, CA_MTL_STM_BUS, CA_MTL_STM_SUBWAY)).forEach {
            whenever_POIMsAroundLoc(it, mainPOI) doReturn mutableListOf()
        }

        val result = subject.invoke(
            lat = mainPOI.lat,
            lng = mainPOI.lng,
            allAgencies = ALL_AGENCIES,
            minSize = 1,
            maxSize = UILocationUtils.MAX_POI_NEARBY_POIS_LIST,
            minCoverageInMeters = UILocationUtils.MIN_POI_NEARBY_POIS_LIST_COVERAGE_IN_METERS,
            getMaxDistanceInMeters = getMaxDistanceInMeters,
            mainAgency = mainAgency,
            excludeAgency = POIViewModel.NEARBY_CONNECTIONS_EXCLUDE_AGENCY,
            excludePOI = { POIViewModel.NEARBY_CONNECTIONS_EXCLUDE_POI(it, mainPOI) },
        ).sortWithAnd(mkPOIConnectionComparator(targetedPOI = mainPOI))

        assertEquals(16, result.size)
        with(result.map { it.dataSourceTypeId }.distinct()) {
            assertEquals(3, size)
            assertEquals(DataSourceType.TYPE_BIKE.id, this[0])
            assertEquals(DataSourceType.TYPE_SUBWAY.id, this[1])
            assertEquals(DataSourceType.TYPE_BUS.id, this[2])
        }
        with(result.filter { it.dataSourceTypeId == DataSourceType.TYPE_BIKE.id }) {
            assertEquals(1, size)
            assertEquals(CA_MTL_BIXI_PARC_DU_MAIL.uuid, this[0].uuid)
        }
        with(result.filter { it.dataSourceTypeId == DataSourceType.TYPE_SUBWAY.id }) {
            assertEquals(1, size)
            assertEquals(mkCA_MTL_STM_SUBWAY_HONORE_BEAUGRAND().uuid, this[0].uuid)
        }
        with(result.filter { it.dataSourceTypeId == DataSourceType.TYPE_BUS.id }) {
            assertEquals(14, size)
            var index = -1
            assertEquals(mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_53253(18L).uuid, this[++index].uuid)
            assertEquals(mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_53252(26L).uuid, this[++index].uuid)
            assertEquals(mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_54257(28L).uuid, this[++index].uuid)
            assertEquals(mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_53251(85L).uuid, this[++index].uuid)
            assertEquals(mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_S_53876(141L).uuid, this[++index].uuid)
            assertEquals(mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_N_53755(185L).uuid, this[++index].uuid)
            assertEquals(mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_N_53725(186L).uuid, this[++index].uuid)
            assertEquals(mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_N_53724(187L).uuid, this[++index].uuid)
            assertEquals(mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_N_53754(189L).uuid, this[++index].uuid)
            assertEquals(mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_54257(362L).uuid, this[++index].uuid)
            assertEquals(mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_53252(364L).uuid, this[++index].uuid)
            assertEquals(mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_S_53275(370L).uuid, this[++index].uuid)
            assertEquals(mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_N_54119(486L).uuid, this[++index].uuid)
            assertEquals(mkCA_MTL_STM_BUS_HONORE_BEAUGRAND_METRO_TERM_N_54008(487L).uuid, this[++index].uuid)
            assertEquals(this.size, ++index)
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @Test
    fun test_POI_Nearby_Connection_Metro_Mont_Royal() = runTest(mainDispatcherRule.testDispatcher) {
        val mainAgency = CA_MTL_STM_SUBWAY
        val mainPOI = mkCA_MTL_STM_SUBWAY_MONT_ROYAL(2L, 1) // Côte-Vertu
        whenever_POIMsAroundLoc(CA_MTL_BIXI, mainPOI) doReturn mutableListOf<POIManager>().apply {
            add(CA_MTL_BIXI_MONT_ROYAL_METRO_PL_GERALD_GODIN.toPOIM())
            add(CA_MTL_BIXI_MONT_ROYAL_RESTER.toPOIM())
            add(CA_MTL_BIXI_MONT_ROYAL_ST_HUBERT.toPOIM())
            add(CA_MTL_BIXI_DROLET_MARIE_ANNE.toPOIM())
            add(CA_MTL_BIXI_GILFORD_ST_DENIS.toPOIM())
            add(CA_MTL_BIXI_MONT_ROYAL_MENTANA.toPOIM())
            add(CA_MTL_BIXI_MONT_ROYAL_LAVAL.toPOIM())
            add(CA_MTL_BIXI_MONT_ROYAL_BOYER.toPOIM())
            add(CA_MTL_BIXI_MONT_ROYAL_HOTEL_DE_VILLE.toPOIM())
            add(CA_MTL_BIXI_MENTANA_MARIE_ANNE.toPOIM())
            add(CA_MTL_BIXI_BERRY_GILFORD.toPOIM())
        }.updateDistanceMJVM(lat = mainPOI.lat, lng = mainPOI.lng)
        whenever_POIMsAroundLoc(CA_MTL_STM_BUS, mainPOI) doReturn mutableListOf<POIManager>().apply {
            add(mkCA_MTL_STM_BUS_MONT_ROYAL_BERRI_52084(97L, 1).toPOIM())
            add(mkCA_MTL_STM_BUS_MONT_ROYAL_BERRI_52084(368L, 1).toPOIM())
            add(mkCA_MTL_STM_BUS_MONT_ROYAL_BERRI_52083(11L, 1).toPOIM())
            add(mkCA_MTL_STM_BUS_MONT_ROYAL_BERRI_58725(97L, 2).toPOIM())
            add(mkCA_MTL_STM_BUS_MONT_ROYAL_METRO_BERRI_61665(711L, 2).toPOIM())
            add(mkCA_MTL_STM_BUS_MONT_ROYAL_METRO_PONTIAC_61802(11L, 2).toPOIM())
            add(mkCA_MTL_STM_BUS_MONT_ROYAL_ST_DENIS_52055(368L, 2).toPOIM())
            add(mkCA_MTL_STM_BUS_MONT_ROYAL_ST_DENIS_61896(31L, 3).toPOIM())
            add(mkCA_MTL_STM_BUS_MONT_ROYAL_ST_DENIS_61896(361L, 3).toPOIM())
            add(mkCA_MTL_STM_BUS_MONT_ROYAL_ST_DENIS_61897(31L, 4).toPOIM())
            add(mkCA_MTL_STM_BUS_MONT_ROYAL_ST_DENIS_61897(361L, 4).toPOIM())
            add(mkCA_MTL_STM_BUS_MONT_ROYAL_ST_DENIS_52057(97L, 1).toPOIM())
        }.updateDistanceMJVM(lat = mainPOI.lat, lng = mainPOI.lng)
        whenever_POIMsAroundLoc(CA_MTL_STM_SUBWAY, mainPOI) doReturn mutableListOf<POIManager>().apply {
            add(mkCA_MTL_STM_SUBWAY_MONT_ROYAL(2L, 1).toPOIM()) // Côte-Vertu
            add(mkCA_MTL_STM_SUBWAY_MONT_ROYAL(2L, 2).toPOIM()) // Montmorency
        }.updateDistanceMJVM(lat = mainPOI.lat, lng = mainPOI.lng)
        (ALL_AGENCIES - setOf(CA_MTL_BIXI, CA_MTL_STM_BUS, CA_MTL_STM_SUBWAY)).forEach {
            whenever_POIMsAroundLoc(it, mainPOI) doReturn mutableListOf()
        }

        val result = subject.invoke(
            lat = mainPOI.lat,
            lng = mainPOI.lng,
            allAgencies = ALL_AGENCIES,
            minSize = 1,
            maxSize = UILocationUtils.MAX_POI_NEARBY_POIS_LIST,
            minCoverageInMeters = UILocationUtils.MIN_POI_NEARBY_POIS_LIST_COVERAGE_IN_METERS,
            getMaxDistanceInMeters = getMaxDistanceInMeters,
            mainAgency = mainAgency,
            excludeAgency = POIViewModel.NEARBY_CONNECTIONS_EXCLUDE_AGENCY,
            excludePOI = { POIViewModel.NEARBY_CONNECTIONS_EXCLUDE_POI(it, mainPOI) },
        ).sortWithAnd(mkPOIConnectionComparator(targetedPOI = mainPOI))

        assertEquals(10, result.size)
        with(result.map { it.dataSourceTypeId }.distinct()) {
            assertEquals(3, size)
            assertEquals(DataSourceType.TYPE_SUBWAY.id, this[0])
            assertEquals(DataSourceType.TYPE_BIKE.id, this[1])
            assertEquals(DataSourceType.TYPE_BUS.id, this[2])
        }
        with(result.filter { it.dataSourceTypeId == DataSourceType.TYPE_SUBWAY.id }) {
            assertEquals(1, size)
            assertEquals(mkCA_MTL_STM_SUBWAY_MONT_ROYAL(2L, 2).uuid, this[0].uuid) // Montmorency
        }
        with(result.filter { it.dataSourceTypeId == DataSourceType.TYPE_BIKE.id }) {
            assertEquals(3, size)
            assertEquals(CA_MTL_BIXI_MONT_ROYAL_METRO_PL_GERALD_GODIN.uuid, this[0].uuid)
            assertEquals(CA_MTL_BIXI_MONT_ROYAL_RESTER.uuid, this[1].uuid)
            assertEquals(CA_MTL_BIXI_MONT_ROYAL_ST_HUBERT.uuid, this[2].uuid)
        }
        with(result.filter { it.dataSourceTypeId == DataSourceType.TYPE_BUS.id }) {
            assertEquals(6, size)
            assertEquals(mkCA_MTL_STM_BUS_MONT_ROYAL_BERRI_52083(11L, 1).uuid, this[0].uuid)
            assertEquals(mkCA_MTL_STM_BUS_MONT_ROYAL_METRO_PONTIAC_61802(11L, 2).uuid, this[1].uuid)
            assertEquals(mkCA_MTL_STM_BUS_MONT_ROYAL_BERRI_52084(97L, 1).uuid, this[2].uuid)
            assertEquals(mkCA_MTL_STM_BUS_MONT_ROYAL_BERRI_58725(97L, 2).uuid, this[3].uuid)
            assertEquals(mkCA_MTL_STM_BUS_MONT_ROYAL_BERRI_52084(368L, 1).uuid, this[4].uuid)
            assertEquals(mkCA_MTL_STM_BUS_MONT_ROYAL_METRO_BERRI_61665(711L, 2).uuid, this[5].uuid)
        }
    }

    private fun whenever_POIMsAroundLoc(
        agency: AgencyBaseProperties,
        mainPOI: POI,
    ): OngoingStubbing<MutableList<POIManager>> = whenever {
        poiRepository.findPOIMsAroundLoc(
            agency = eq(agency),
            lat = eq(mainPOI.lat),
            lng = eq(mainPOI.lng),
            aroundDiff = or(eq(0.01), eq(0.02)),
            avoidLoading = eq(true),
            noPickup = eq(false),
        )
    }

    private val getMaxDistanceInMeters: (maxDistanceInMeters: Float, dst: DataSourceType) -> Float = { maxDistanceInMeters, dst ->
        when (dst) {
            DataSourceType.TYPE_BUS -> maxDistanceInMeters
            DataSourceType.TYPE_BIKE -> maxDistanceInMeters * 1.5f
            DataSourceType.TYPE_SUBWAY -> maxDistanceInMeters * 2f
            DataSourceType.TYPE_RAIL -> maxDistanceInMeters * 2f
            DataSourceType.TYPE_LIGHT_RAIL -> maxDistanceInMeters * 2f
            DataSourceType.TYPE_FERRY -> maxDistanceInMeters * 2f
            else -> {
                maxDistanceInMeters
            }
        }.coerceAtMost(2f * 2f * UILocationUtils.MIN_POI_NEARBY_POIS_LIST_COVERAGE_IN_METERS)
    }

    private fun mkPOIConnectionComparator(targetedPOI: POI? = null) = POIConnectionComparator(
        targetedPOI = targetedPOI,
        maxDistanceInMeters = { dataSourceTypeId ->
            when (dataSourceTypeId) {
                DataSourceType.TYPE_BIKE.id -> 2.5f * UILocationUtils.MIN_POI_NEARBY_POIS_LIST_COVERAGE_IN_METERS
                else -> 2f * UILocationUtils.MIN_POI_NEARBY_POIS_LIST_COVERAGE_IN_METERS
            }
        },
        computeDistance = { poi1, poi2 -> poi1.distanceToInMetersJVM(poi2) },
        sameAgency1st = POIViewModel.NEARBY_CONNECTIONS_SAME_AGENCY_1ST,
    )
}
