package org.mtransit.android.data

import org.mtransit.android.R
import org.mtransit.android.commons.data.DataSourceTypeId
import org.mtransit.android.datasource.DataSourcesConverters
import org.mtransit.android.ui.fragment.POIViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DataSourceTypeTest {

    @Test
    fun gondolaTypeIsAvailableAcrossTransitScreens() {
        val type = DataSourceType.parseId(6)
        assertEquals(DataSourceType.TYPE_GONDOLA, type)
        assertEquals(type, DataSourceType.parseNavResId(R.id.root_nav_gondola))
        assertNotNull(type)
        assertTrue(DataSourceTypeId.isGTFSType(type.id))
        assertTrue(DataSourceTypeId.isRDSType(type.id))
        assertFalse(type.isExtendedType)
        assertEquals(DataSourceStopType.STATION, type.stopType)
        assertTrue(type.isMenuList && type.isHomeScreen && type.isNearbyScreen && type.isMapScreen && type.isSearchable)
    }

    @Test
    fun gondolaTypeRoundTripsThroughDatabaseConverter() {
        val converter = DataSourcesConverters()
        val type = converter.fromTypeId(6)
        assertEquals(DataSourceType.TYPE_GONDOLA, type)
        assertEquals(6, converter.dataSourceTypeToTypeId(type))
    }

    @Test
    fun gondolaNearbyConnectionsUseTransitSearchDistance() {
        val distance = POIViewModel.GET_NEARBY_CONNECTIONS_MAX_DISTANCE_IN_METERS
        assertEquals(200f, distance(100f, DataSourceType.TYPE_GONDOLA, 0), 0f)
        assertEquals(100f, distance(100f, DataSourceType.TYPE_GONDOLA, 1), 0f)
    }
}
