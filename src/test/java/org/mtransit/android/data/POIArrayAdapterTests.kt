package org.mtransit.android.data

import org.junit.Assert.assertEquals
import org.junit.Test

class POIArrayAdapterTests {

    @Test
    fun test_optimizeMaxButtonPerLines_1_3() {
        val result = POIArrayAdapter.optimizeMaxButtonPerLines(0 + 1, 3)

        assertEquals(1, result)
    }

    @Test
    fun test_optimizeMaxButtonPerLines_2_3() {
        val result = POIArrayAdapter.optimizeMaxButtonPerLines(1 + 1, 3)

        assertEquals(2, result)
    }

    @Test
    fun test_optimizeMaxButtonPerLines_3_3() {
        val result = POIArrayAdapter.optimizeMaxButtonPerLines(2 + 1, 3)

        assertEquals(3, result)
    }

    @Test
    fun test_optimizeMaxButtonPerLines_4_3() {
        val result = POIArrayAdapter.optimizeMaxButtonPerLines(3 + 1, 3)

        assertEquals(3, result)
    }

    @Test
    fun test_optimizeMaxButtonPerLines_5_3() {
        val result = POIArrayAdapter.optimizeMaxButtonPerLines(4 + 1, 3)

        assertEquals(3, result)
    }

    @Test
    fun test_optimizeMaxButtonPerLines_6_3() {
        val result = POIArrayAdapter.optimizeMaxButtonPerLines(5 + 1, 3)

        assertEquals(3, result)
    }

    @Test
    fun test_optimizeMaxButtonPerLines_7_3() {
        val result = POIArrayAdapter.optimizeMaxButtonPerLines(6 + 1, 3)

        assertEquals(3, result)
    }


    @Test
    fun test_optimizeMaxButtonPerLines_8_6() {
        val result = POIArrayAdapter.optimizeMaxButtonPerLines(7 + 1, 6)

        assertEquals(4, result)
    }

}