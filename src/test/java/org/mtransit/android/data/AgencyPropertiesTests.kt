package org.mtransit.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mtransit.android.commons.CollectionUtils

class AgencyPropertiesTests {

    @Test
    fun testShortNameComparator() {
        val agencies = ArrayList<AgencyProperties>()
        var shortName: String?
        //
        shortName = null
        agencies.add(AgencyProperties(null, null, shortName, null, null, null, false))
        shortName = "A"
        agencies.add(AgencyProperties(null, null, shortName, null, null, null, false))
        shortName = "Z"
        agencies.add(AgencyProperties(null, null, shortName, null, null, null, false))
        shortName = "b"
        agencies.add(AgencyProperties(null, null, shortName, null, null, null, false))
        shortName = null
        agencies.add(AgencyProperties(null, null, shortName, null, null, null, false))
        //
        CollectionUtils.sort(
            agencies,
            AgencyProperties.SHORT_NAME_COMPARATOR
        )
        //
        assertNull(agencies[0].shortName)
        assertNull(agencies[1].shortName)
        assertEquals("A", agencies[2].shortName)
        assertEquals("b", agencies[3].shortName)
        assertEquals("Z", agencies[4].shortName)
    }
}