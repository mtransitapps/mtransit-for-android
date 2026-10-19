package org.mtransit.android.ui.location

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import org.mtransit.android.ui.location.UILocationUtils.keepCenteredOnAnd
import org.mtransit.android.util.include
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

    @Test
    fun test_keepCenteredOn_1_latLng() {
        val center = LatLng(45.49997, -73.56672) // Centrale station
        mutableSetOf(
            center,
            LatLng(45.49963, -73.50582), // Saint-Lambert station
        ).keepCenteredOnAnd(center).let { result ->
            LatLngBounds.builder().include(result).build().let {
                assertEquals(45.50031, it.northeast.latitude, 0.000_001)
                assertEquals(-73.50582, it.northeast.longitude, 0.000_001)
                assertEquals(45.49963, it.southwest.latitude, 0.000_001)
                assertEquals(-73.62762, it.southwest.longitude, 0.000_001)
            }
        }
    }

    @Test
    fun test_keepCenteredOn_1_latLng_vertical_only() {
        val center = LatLng(45.49997, -73.56672) // Centrale station
        mutableSetOf(
            center,
            LatLng(45.49963, -73.50582), // Saint-Lambert station
        ).keepCenteredOnAnd(center, 0.0, 1.0).let { result ->
            LatLngBounds.builder().include(result).build().let {
                assertEquals(45.50031, it.northeast.latitude, 0.000_001)
                assertEquals(-73.50582, it.northeast.longitude, 0.000_001)
                assertEquals(45.49963, it.southwest.latitude, 0.000_001)
                assertEquals(-73.56672, it.southwest.longitude, 0.000_001)
            }
        }
    }

    @Test
    fun test_keepCenteredOn_1_latLng_horizontal_only() {
        val center = LatLng(45.49997, -73.56672) // Centrale station
        mutableSetOf(
            center,
            LatLng(45.49963, -73.50582), // Saint-Lambert station
        ).keepCenteredOnAnd(center, 1.0, 0.0).let { result ->
            LatLngBounds.builder().include(result).build().let {
                assertEquals(45.49997, it.northeast.latitude, 0.000_001)
                assertEquals(-73.50582, it.northeast.longitude, 0.000_001)
                assertEquals(45.49963, it.southwest.latitude, 0.000_001)
                assertEquals(-73.62762, it.southwest.longitude, 0.000_001)
            }
        }
    }

    @Test
    fun test_keepCenteredOn_many_latLng() {
        val center = LatLng(45.51245,-73.53313) // Jean-Drapeau station
        mutableSetOf(
            center,
            LatLng(45.51438,-73.56191), // Berri-UQAM station
            LatLng(45.52463,-73.52198), // Longueuil-UdeS station
        ).keepCenteredOnAnd(center).let { result ->
            LatLngBounds.builder().include(result).build().let {
                assertEquals(45.52463, it.northeast.latitude, 0.000_001)
                assertEquals(-73.50435, it.northeast.longitude, 0.000_001)
                assertEquals(45.50027, it.southwest.latitude, 0.000_001)
                assertEquals(-73.56191, it.southwest.longitude, 0.000_001)
            }
        }
    }
}
