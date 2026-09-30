package org.mtransit.android.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.stubbing.OngoingStubbing
import org.mtransit.android.commons.MainDispatcherRule
import org.mtransit.android.commons.data.ALL_AGENCIES
import org.mtransit.android.commons.data.CA_CRC_EXO
import org.mtransit.android.commons.data.CA_LE_RICHELAIN_ROUSSILLON_EXO
import org.mtransit.android.commons.data.CA_LONGUEUIL_RTL
import org.mtransit.android.commons.data.CA_MTL_REM
import org.mtransit.android.commons.data.CA_RICHELIEU_EXO
import org.mtransit.android.commons.data.CA_STE_JULIE_EXO
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.data.RouteDirectionStop
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
import org.mtransit.android.commons.data.mkCA_RICHELIEU_TERM_BROSSARD
import org.mtransit.android.commons.data.mkCA_STE_JULIE_EXO_TERM_BROSSARD_Q5
import org.mtransit.android.commons.distanceToInMetersJVM
import org.mtransit.android.commons.getAroundCoveredDistanceInMetersJVM
import org.mtransit.android.commons.updateDistanceMJVM
import org.mtransit.android.data.AgencyBaseProperties
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.POIConnectionComparator
import org.mtransit.android.data.POIManager
import org.mtransit.android.data.isNoPickup
import org.mtransit.android.data.isSameRoute
import org.mtransit.android.data.toPOIM
import org.mtransit.android.data.uuid
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.ui.location.UILocationUtils
import org.mtransit.commons.sortWithAnd
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
        }
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
                add(mkCA_RICHELIEU_TERM_BROSSARD(routeId = it).toPOIM())
                add(mkCA_RICHELIEU_TERM_BROSSARD(routeId = it, isNoPickup = true).toPOIM())
            }
        }.updateDistanceMJVM(lat = mainPOI.lat, lng = mainPOI.lng)
        whenever_POIMsAroundLoc(CA_STE_JULIE_EXO, mainPOI) doReturn mutableListOf<POIManager>().apply {
            listOf(600L).forEach {
                add(mkCA_STE_JULIE_EXO_TERM_BROSSARD_Q5(routeId = it).toPOIM())
                add(mkCA_STE_JULIE_EXO_TERM_BROSSARD_Q5(routeId = it, isNoPickup = true).toPOIM())
            }
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
            excludeAgency = { agency ->
                !agency.type.isNearbyScreen
                    || agency.type == DataSourceType.TYPE_MODULE
            },
            excludePOI = {
                it.poi.uuid == mainPOI.uuid
                    || (it.poi.isNoPickup && !it.poi.isSameRoute(mainPOI))
            },
        ).sortWithAnd(mkPOIConnectionComparator(targetedPOI = mainPOI))

        assertEquals(27, result.size)
        var index = -1
        assertEquals(mkCA_MTL_REM_BROSSARD(4001L, isNoPickup = true).uuid, result[++index].uuid)
        assertEquals(mkCA_LONGUEUIL_RTL_TERM_BROSSARD(4L).uuid, result[++index].uuid)
        assertEquals(mkCA_LONGUEUIL_RTL_TERM_BROSSARD(14L).uuid, result[++index].uuid)
        assertEquals(mkCA_LONGUEUIL_RTL_TERM_BROSSARD(32L).uuid, result[++index].uuid)
        assertEquals(mkCA_LONGUEUIL_RTL_TERM_BROSSARD(38L).uuid, result[++index].uuid)
        assertEquals(mkCA_LONGUEUIL_RTL_TERM_BROSSARD(44L).uuid, result[++index].uuid)
        assertEquals(mkCA_LONGUEUIL_RTL_TERM_BROSSARD(114L).uuid, result[++index].uuid)
        assertEquals(mkCA_LONGUEUIL_RTL_TERM_BROSSARD(132L).uuid, result[++index].uuid)
        assertEquals(mkCA_RICHELIEU_TERM_BROSSARD(300L).uuid, result[++index].uuid)
        assertEquals(mkCA_LE_RICHELAIN_ROUSSILLON_EXO_TERM_BROSSARD_Q18(450L).uuid, result[++index].uuid)
        assertEquals(mkCA_LE_RICHELAIN_ROUSSILLON_EXO_TERM_BROSSARD_Q17(451L).uuid, result[++index].uuid)
        assertEquals(mkCA_LE_RICHELAIN_ROUSSILLON_EXO_TERM_BROSSARD_Q17(452L).uuid, result[++index].uuid)
        assertEquals(mkCA_LE_RICHELAIN_ROUSSILLON_EXO_TERM_BROSSARD_Q17(454L).uuid, result[++index].uuid)
        assertEquals(mkCA_LE_RICHELAIN_ROUSSILLON_EXO_TERM_BROSSARD_Q19(455L).uuid, result[++index].uuid)
        assertEquals(mkCA_LE_RICHELAIN_ROUSSILLON_EXO_TERM_BROSSARD_Q18(456L).uuid, result[++index].uuid)
        assertEquals(mkCA_LE_RICHELAIN_ROUSSILLON_EXO_TERM_BROSSARD_Q18(457L).uuid, result[++index].uuid)
        assertEquals(mkCA_CRC_EXO_TERM_BROSSARD_Q13(481L).uuid, result[++index].uuid)
        assertEquals(mkCA_CRC_EXO_TERM_BROSSARD_Q13(482L).uuid, result[++index].uuid)
        assertEquals(mkCA_CRC_EXO_TERM_BROSSARD_Q3(483L).uuid, result[++index].uuid)
        assertEquals(mkCA_CRC_EXO_TERM_BROSSARD_Q3(484L).uuid, result[++index].uuid)
        assertEquals(mkCA_CRC_EXO_TERM_BROSSARD_Q15(485L).uuid, result[++index].uuid)
        assertEquals(mkCA_CRC_EXO_TERM_BROSSARD_Q3(486L).uuid, result[++index].uuid)
        assertEquals(mkCA_CRC_EXO_TERM_BROSSARD_Q15(487L).uuid, result[++index].uuid)
        assertEquals(mkCA_CRC_EXO_TERM_BROSSARD_Q15(488L).uuid, result[++index].uuid)
        assertEquals(mkCA_LE_RICHELAIN_ROUSSILLON_EXO_TERM_BROSSARD_Q19(552L).uuid, result[++index].uuid)
        assertEquals(mkCA_LE_RICHELAIN_ROUSSILLON_EXO_TERM_BROSSARD_Q16(554L).uuid, result[++index].uuid)
        assertEquals(mkCA_STE_JULIE_EXO_TERM_BROSSARD_Q5(600L).uuid, result[++index].uuid)
        assertEquals(result.size, ++index)
    }

    private fun whenever_POIMsAroundLoc(
        agency: AgencyBaseProperties,
        mainPOI: RouteDirectionStop
    ): OngoingStubbing<MutableList<POIManager>> = whenever {
        poiRepository.findPOIMsAroundLoc(
            agency = agency,
            lat = mainPOI.lat,
            lng = mainPOI.lng,
            aroundDiff = 0.01,
            avoidLoading = true,
            noPickup = false
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
        }.coerceAtMost(100f * 2f * 2f)
    }

    private fun mkPOIConnectionComparator(targetedPOI: POI? = null) = POIConnectionComparator(
        targetedPOI = targetedPOI,
        maxDistanceInMeters = { dataSourceTypeId ->
            when (dataSourceTypeId) {
                DataSourceType.TYPE_BIKE.id -> 2.5f * UILocationUtils.MIN_POI_NEARBY_POIS_LIST_COVERAGE_IN_METERS
                else -> 2f * UILocationUtils.MIN_POI_NEARBY_POIS_LIST_COVERAGE_IN_METERS
            }
        },
        computeDistance = { pOI, pOI1 -> pOI.distanceToInMetersJVM(pOI1) }
    )
}
