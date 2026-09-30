package org.mtransit.android.ui.view.map

import org.mtransit.android.commons.data.Area
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AreaExtTest {

    @Test
    fun test_toLngLngList() {
        val area = Area(
            minLat = 45.51,
            maxLat = 45.53,
            minLng = -73.58,
            maxLng = -73.56,
        )

        val result = area.toLngLngList()

        assertEquals(2, result.size)
        assertNotNull(result.getOrNull(0)) {
            assertEquals(45.51, it.latitude)
            assertEquals(-73.58, it.longitude)
        }
        assertNotNull(result.getOrNull(1)) {
            assertEquals(45.53, it.latitude)
            assertEquals(-73.56, it.longitude)
        }
    }

    @Test
    fun test_toLngLngBounds() {
        val area = Area(
            minLat = 45.51,
            maxLat = 45.53,
            minLng = -73.58,
            maxLng = -73.56,
        )

        val result = area.toLatLngBounds()

        with(result.northeast) {
            assertEquals(45.53, latitude)
            assertEquals(-73.56, longitude)
        }
        with(result.southwest) {
            assertEquals(45.51, latitude)
            assertEquals(-73.58, result.southwest.longitude)
        }
    }
}
