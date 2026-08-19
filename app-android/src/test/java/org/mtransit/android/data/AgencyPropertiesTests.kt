package org.mtransit.android.data

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.data.DataSourceType.TYPE_BIKE
import org.mtransit.android.data.DataSourceType.TYPE_BUS
import org.mtransit.android.data.DataSourceType.TYPE_RAIL

class AgencyPropertiesTests {

    companion object {
        const val ID = "id"
        val DST = TYPE_BUS
        const val SHORT_NAME = "shortName"
        const val LONG_NAME = "longName"
        val AREA = LocationUtils.THE_WORLD
        const val PKG = "com.package.app"
        const val VERSION = -1
        const val VERSION_L = -1L
        const val INSTALLED = true
        const val ENABLED = true
    }

    @Test
    fun testShortNameComparator() {
        // Arrange
        val agencies = mutableListOf(
            AgencyProperties(ID, DST, "A", LONG_NAME, color = null, timeZoneId = null, AREA, PKG, VERSION_L, VERSION, INSTALLED, ENABLED),
            AgencyProperties(ID, DST, "Z", LONG_NAME, color = null, timeZoneId = null, AREA, PKG, VERSION_L, VERSION, INSTALLED, ENABLED),
            AgencyProperties(ID, DST, "b", LONG_NAME, color = null, timeZoneId = null, AREA, PKG, VERSION_L, VERSION, INSTALLED, ENABLED),
        )
        // Act
        agencies.sortWith(IAgencyProperties.SHORT_NAME_COMPARATOR)
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
            AgencyProperties(ID, TYPE_BUS, SHORT_NAME, LONG_NAME, color = null, timeZoneId = null, AREA, PKG, VERSION_L, VERSION, INSTALLED, ENABLED),
            AgencyProperties(ID, TYPE_RAIL, SHORT_NAME, LONG_NAME, color = null, timeZoneId = null, AREA, PKG, VERSION_L, VERSION, INSTALLED, ENABLED),
            AgencyProperties(ID, TYPE_BUS, SHORT_NAME, LONG_NAME, color = null, timeZoneId = null, AREA, PKG, VERSION_L, VERSION, INSTALLED, ENABLED),
            AgencyProperties(ID, TYPE_BIKE, SHORT_NAME, LONG_NAME, color = null, timeZoneId = null, AREA, PKG, VERSION_L, VERSION, INSTALLED, ENABLED),
            AgencyProperties(ID, TYPE_BUS, SHORT_NAME, LONG_NAME, color = null, timeZoneId = null, AREA, PKG, VERSION_L, VERSION, INSTALLED, ENABLED),
        )
        // Act
        IAgencyProperties.removeType(agencies, TYPE_BUS)
        // Assert
        assertEquals(2, agencies.size)
    }
}
