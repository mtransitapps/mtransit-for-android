package org.mtransit.android.ui.view.map

import androidx.annotation.ColorInt
import com.google.android.gms.maps.model.LatLng
import org.mtransit.android.commons.data.Area
import org.mtransit.android.commons.provider.vehiclelocations.model.VehicleLocation
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.POIManager

interface MapMarkerProvider {

    val poiMarkers: Collection<MTPOIMarker>? get() = null

    val pois: Collection<POIManager>? get() = null

    fun getPOI(position: Int): POIManager? = null

    val closestPOI: POIManager? get() = null

    @Suppress("unused")
    fun getPOI(uuid: String?): POIManager? = null

    val vehicleLocations: Collection<VehicleLocation>? get() = null

    @get:ColorInt
    val vehicleColorInt: Int? get() = null

    val vehicleType: DataSourceType? get() = null

    val visibleArea: Collection<LatLng>? get() = null

    fun getMapMarkerAlpha(position: Int, visibleArea: Area): Float? = null
}
