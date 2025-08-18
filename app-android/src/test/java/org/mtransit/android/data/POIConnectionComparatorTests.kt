package org.mtransit.android.data

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mtransit.android.commons.data.DataSourceTypeId
import org.mtransit.android.commons.data.Direction
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.data.Route
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.data.Stop
import kotlin.math.abs

@Suppress("DEPRECATION")
class POIConnectionComparatorTests {

    private val dst = DataSourceTypeId.RAIL
    private val authority = "a"

    private val computeDistance: (POI, POI) -> Float? = mock {
        on { invoke(any(), any()) }.thenAnswer {
            val poi1 = it.arguments[0] as POI? ?: return@thenAnswer null
            val poi2 = it.arguments[1] as POI? ?: return@thenAnswer null
            if (abs(poi1.lat - poi2.lat) < 0.0001
                && abs(poi1.lng - poi2.lng) < 0.0001
            ) {
                0f
            } else {
                100f
            }
        }
    }

    private val subject = POIConnectionComparator(
        computeDistance = computeDistance
    )

    @Test
    fun `test POISameRouteComparator - same stop`() {
        val routeVH = Route(1L, "11", "Vaudreuil / Hudson", "F16179")
        val routeSH = Route(3L, "13", "Mont-St-Hilaire", "CA5898")
        val routeCA = Route(4L, "14", "Candiac", "CA5898")
        val routeMA = Route(6L, "15", "Mascouche", "999AC6")
        val directionVhVe = Direction(100L, Direction.HEADSIGN_TYPE_STRING, "Vendôme", routeVH.id)
        val directionVhVh = Direction(101L, Direction.HEADSIGN_TYPE_STRING, "Hudson", routeVH.id)
        val directionShCt = Direction(300L, Direction.HEADSIGN_TYPE_STRING, "Centrale", routeSH.id)
        val directionShSh = Direction(301L, Direction.HEADSIGN_TYPE_STRING, "St-Hilaire", routeSH.id)
        val directionCaVe = Direction(400L, Direction.HEADSIGN_TYPE_STRING, "Vendôme", routeCA.id)
        val directionCaVh = Direction(401L, Direction.HEADSIGN_TYPE_STRING, "Candiac", routeCA.id)
        val directionMaCt = Direction(600L, Direction.HEADSIGN_TYPE_STRING, "Centrale", routeMA.id)
        val directionMaMa = Direction(601L, Direction.HEADSIGN_TYPE_STRING, "Mascouche", routeMA.id)
        val stopCentrale = Stop(11010, "", "Centrale", 45.500002, -73.566668)
        val stopVendome = Stop(11160, "", "Vendôme", 45.473771, -73.603101)
        val stopStLambert = Stop(11440, "", "St-Lambert", 45.499634, -73.505824)
        val excludedPOI = RouteDirectionStop(authority, dst, routeSH, directionShCt, stopCentrale, false).toPOIM()
        val poiListSortedByDistance = listOf(
            // Centrale
            RouteDirectionStop(authority, dst, routeMA, directionMaCt, stopCentrale, false).toPOIM(),
            RouteDirectionStop(authority, dst, routeMA, directionMaMa, stopCentrale, false).toPOIM(),
            RouteDirectionStop(authority, dst, routeSH, directionShSh, stopCentrale, false).toPOIM(),
            // Vendome
            RouteDirectionStop(authority, dst, routeVH, directionVhVh, stopVendome, false).toPOIM(),
            RouteDirectionStop(authority, dst, routeVH, directionVhVe, stopVendome, false).toPOIM(),
            RouteDirectionStop(authority, dst, routeCA, directionCaVe, stopVendome, false).toPOIM(),
            RouteDirectionStop(authority, dst, routeCA, directionCaVh, stopVendome, false).toPOIM(),
            // St-Lambert
            RouteDirectionStop(authority, dst, routeSH, directionShCt, stopStLambert, false).toPOIM(),
            RouteDirectionStop(authority, dst, routeSH, directionShSh, stopStLambert, false).toPOIM(),
        )
        subject.targetedPOI = excludedPOI.poi
        val result = poiListSortedByDistance.sortedWith(subject)

        var i = 0
        assertEquals("$authority-3-301-11010", (result[i++].poi as? RouteDirectionStop)?.uuid)
        assertEquals("$authority-6-600-11010", (result[i++].poi as? RouteDirectionStop)?.uuid)
        assertEquals("$authority-6-601-11010", (result[i++].poi as? RouteDirectionStop)?.uuid)
        assertEquals("$authority-1-101-11160", (result[i++].poi as? RouteDirectionStop)?.uuid)
        assertEquals("$authority-1-100-11160", (result[i++].poi as? RouteDirectionStop)?.uuid)
        assertEquals("$authority-4-400-11160", (result[i++].poi as? RouteDirectionStop)?.uuid)
        assertEquals("$authority-4-401-11160", (result[i++].poi as? RouteDirectionStop)?.uuid)
        assertEquals("$authority-3-300-11440", (result[i++].poi as? RouteDirectionStop)?.uuid)
        assertEquals("$authority-3-301-11440", (result[i++].poi as? RouteDirectionStop)?.uuid)
        assertEquals(9, i)
    }

    @Test
    fun `test POISameRouteComparator - same terminal - different stop`() {
        val route23 = Route(23L, "23", "Ste-Hélène / Jacques-Cartier", "")
        val route29 = Route(29L, "29", "Collectivité Nouvelle", "")
        val route98 = Route(98L, "98", "Promenades St-Bruno / St-Bruno-De-Montarville", "")

        val direction23Tr = Direction(2300L, Direction.HEADSIGN_TYPE_STRING, "Tremblay", route23.id)
        val direction23TL = Direction(2301L, Direction.HEADSIGN_TYPE_STRING, "Term Longueuil", route23.id)
        val direction29Eg = Direction(2900L, Direction.HEADSIGN_TYPE_STRING, "Église", route29.id)
        val direction29TL = Direction(2901L, Direction.HEADSIGN_TYPE_STRING, "Term Longueuil", route29.id)
        val direction98Pa = Direction(9800L, Direction.HEADSIGN_TYPE_STRING, "Parent", route98.id)
        val direction98TL = Direction(9801L, Direction.HEADSIGN_TYPE_STRING, "Term Longueuil", route98.id)
        val stopTL34417 = Stop(4417, "34417", "Term Longueuil", 45.5235276580726, -73.5208026778995)
        val excludedPOI = RouteDirectionStop(authority, dst, route98, direction98TL, stopTL34417, false).toPOIM()
        val poiListSortedByDistance = listOf(
            // 23
            RouteDirectionStop(authority, dst, route23, direction23TL, stopTL34417, false).toPOIM(),
            RouteDirectionStop(authority, dst, route23, direction23Tr, stopTL34417, false).toPOIM(),
            // 29
            RouteDirectionStop(authority, dst, route29, direction29TL, stopTL34417, false).toPOIM(),
            RouteDirectionStop(authority, dst, route29, direction29Eg, stopTL34417, false).toPOIM(),
            // 98 same route as targeted
            RouteDirectionStop(authority, dst, route98, direction98Pa, stopTL34417, false).toPOIM(),
        )
        subject.targetedPOI = excludedPOI.poi
        assertEquals(true, subject.isAlmostSameLocation(poiListSortedByDistance[0], poiListSortedByDistance[1]))

        assertEquals(true, subject.isConnection(poiListSortedByDistance[4].poi))

        val result = poiListSortedByDistance.sortedWith(subject)

        var i = 0
        assertEquals("$authority-98-9800-4417", (result[i++].poi as? RouteDirectionStop)?.uuid)
        assertEquals("$authority-23-2301-4417", (result[i++].poi as? RouteDirectionStop)?.uuid)
        assertEquals("$authority-23-2300-4417", (result[i++].poi as? RouteDirectionStop)?.uuid)
        assertEquals("$authority-29-2901-4417", (result[i++].poi as? RouteDirectionStop)?.uuid)
        assertEquals("$authority-29-2900-4417", (result[i++].poi as? RouteDirectionStop)?.uuid)
        assertEquals(5, i)

    }
}