package org.mtransit.android.ui.location

import com.google.android.gms.maps.model.LatLng
import kotlin.test.Test
import kotlin.test.assertEquals

class UILocationUtilsTest {

    @Test
    fun test_computeArea() {
        val center = LatLng(45.52, -73.57)
        val visibleLocation = LatLng(45.51, -73.56)

        val result = UILocationUtils.computeArea(center, visibleLocation)

        assertEquals(45.51, result.minLat, 0.000_001)
        assertEquals(45.53, result.maxLat, 0.000_001)
        assertEquals(-73.58, result.minLng, 0.000_001)
        assertEquals(-73.56, result.maxLng, 0.000_001)
    }

    @Test
    fun test_computeArea2() {
        val center = LatLng(45.581_090, -73.179_303)
        val visibleLocation = LatLng(45.577_820, -73.178_810)

        val result = UILocationUtils.computeArea(center, visibleLocation)

        assertEquals(45.577_820, result.minLat, 0.000_001)
        assertEquals(45.584_361, result.maxLat, 0.000_001)
        assertEquals(-73.179_796, result.minLng, 0.000_001)
        assertEquals(-73.178_810, result.maxLng, 0.000_001)
    }
}
