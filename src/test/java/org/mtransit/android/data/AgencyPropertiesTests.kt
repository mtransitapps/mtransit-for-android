package org.mtransit.android.data

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mtransit.android.commons.CollectionUtils
import org.mtransit.android.commons.LocationUtils

class AgencyPropertiesTests {

    companion object {
        const val ID = "id"
        val DST = DataSourceType.TYPE_BUS
        const val SHORT_NAME = "shortName"
        const val LONG_NAME = "longName"
        val AREA = LocationUtils.THE_WORLD
    }

    @Test
    fun testShortNameComparator() {
        // Arrange
        val agencies = listOf(
            AgencyProperties(ID, DST, "A", LONG_NAME, color = null, AREA),
            AgencyProperties(ID, DST, "Z", LONG_NAME, color = null, AREA),
            AgencyProperties(ID, DST, "b", LONG_NAME, color = null, AREA),
        )
        // Act
        CollectionUtils.sort(
            agencies,
            AgencyProperties.SHORT_NAME_COMPARATOR
        )
        // Assert
        assertEquals(3, agencies.size)
        assertEquals("A", agencies[0].shortName)
        assertEquals("b", agencies[1].shortName)
        assertEquals("Z", agencies[2].shortName)
    }

    @Test
    fun testRemoveType() {
        // Arrange
        val agencies = mutableListOf(
            AgencyProperties(ID, DataSourceType.TYPE_BUS, SHORT_NAME, LONG_NAME, color = null, AREA),
            AgencyProperties(ID, DataSourceType.TYPE_RAIL, SHORT_NAME, LONG_NAME, color = null, AREA),
            AgencyProperties(ID, DataSourceType.TYPE_BUS, SHORT_NAME, LONG_NAME, color = null, AREA),
            AgencyProperties(ID, DataSourceType.TYPE_BIKE, SHORT_NAME, LONG_NAME, color = null, AREA),
            AgencyProperties(ID, DataSourceType.TYPE_BUS, SHORT_NAME, LONG_NAME, color = null, AREA),
        )
        // Act
        AgencyProperties.removeType(agencies, DataSourceType.TYPE_BUS)
        // Assert
        assertEquals(2, agencies.size)
    }
}