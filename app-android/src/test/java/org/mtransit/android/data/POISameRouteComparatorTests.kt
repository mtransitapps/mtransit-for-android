package org.mtransit.android.data

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mtransit.android.commons.data.DataSourceTypeId
import org.mtransit.android.commons.data.Route
import org.mtransit.android.commons.data.RouteTripStop
import org.mtransit.android.commons.data.Stop
import org.mtransit.android.commons.data.Trip

class POISameRouteComparatorTests {

    private val dst = DataSourceTypeId.RAIL
    private val authority = "authority"

    private val subject = POIManager.POISameRouteComparator()

    @Suppress("DEPRECATION")
    @Test
    fun `test POISameRouteComparator`() {
        val routeVH = Route(1L, "11", "Vaudreuil / Hudson", "F16179")
        val routeSH = Route(3L, "13", "Mont-St-Hilaire", "CA5898")
        val routeCA = Route(4L, "14", "Candiac", "CA5898")
        val routeMA = Route(6L, "15", "Mascouche", "999AC6")
        val tripVhVe = Trip(100L, Trip.HEADSIGN_TYPE_STRING, "Vendôme", routeVH.id)
        val tripVhVh = Trip(101L, Trip.HEADSIGN_TYPE_STRING, "Hudson", routeVH.id)
        val tripShCt = Trip(300L, Trip.HEADSIGN_TYPE_STRING, "Centrale", routeSH.id)
        val tripShSh = Trip(301L, Trip.HEADSIGN_TYPE_STRING, "St-Hilaire", routeSH.id)
        val tripCaVe = Trip(400L, Trip.HEADSIGN_TYPE_STRING, "Vendôme", routeCA.id)
        val tripCaVh = Trip(401L, Trip.HEADSIGN_TYPE_STRING, "Candiac", routeCA.id)
        val tripMaCt = Trip(600L, Trip.HEADSIGN_TYPE_STRING, "Centrale", routeMA.id)
        val tripMaMa = Trip(601L, Trip.HEADSIGN_TYPE_STRING, "Mascouche", routeMA.id)
        val stopCentrale = Stop(11010, "", "Centrale", 45.500002, -73.566668)
        val stopVendome = Stop(11160, "", "Vendôme", 45.473771, -73.603101)
        val stopStLambert = Stop(11440, "", "St-Lambert", 45.499634, -73.505824)
        val excludedPOI = RouteTripStop(authority, dst, routeSH, tripShCt, stopCentrale, false).toPOIM()
        val poiListSortedByDistance = listOf(
            // Centrale
            RouteTripStop(authority, dst, routeMA, tripMaCt, stopCentrale, false).toPOIM(),
            RouteTripStop(authority, dst, routeMA, tripMaMa, stopCentrale, false).toPOIM(),
            RouteTripStop(authority, dst, routeSH, tripShSh, stopCentrale, false).toPOIM(),
            // Vendome
            RouteTripStop(authority, dst, routeVH, tripVhVh, stopVendome, false).toPOIM(),
            RouteTripStop(authority, dst, routeVH, tripVhVe, stopVendome, false).toPOIM(),
            RouteTripStop(authority, dst, routeCA, tripCaVe, stopVendome, false).toPOIM(),
            RouteTripStop(authority, dst, routeCA, tripCaVh, stopVendome, false).toPOIM(),
            // St-Lambert
            RouteTripStop(authority, dst, routeSH, tripShCt, stopStLambert, false).toPOIM(),
            RouteTripStop(authority, dst, routeSH, tripShSh, stopStLambert, false).toPOIM(),
        )

        subject.targetedRouteTripStop = (excludedPOI.poi as? RouteTripStop)
        val result = poiListSortedByDistance.sortedWith(subject)

        var i = 0
        assertEquals("authority-3-301-11010", (result[i++].poi as? RouteTripStop)?.uuid)
        assertEquals("authority-6-600-11010", (result[i++].poi as? RouteTripStop)?.uuid)
        assertEquals("authority-6-601-11010", (result[i++].poi as? RouteTripStop)?.uuid)
        assertEquals("authority-1-101-11160", (result[i++].poi as? RouteTripStop)?.uuid)
        assertEquals("authority-1-100-11160", (result[i++].poi as? RouteTripStop)?.uuid)
        assertEquals("authority-4-400-11160", (result[i++].poi as? RouteTripStop)?.uuid)
        assertEquals("authority-4-401-11160", (result[i++].poi as? RouteTripStop)?.uuid)
        assertEquals("authority-3-300-11440", (result[i++].poi as? RouteTripStop)?.uuid)
        assertEquals("authority-3-301-11440", (result[i++].poi as? RouteTripStop)?.uuid)
        assertEquals(9, i)
    }
}