package org.mtransit.android.ui.view.map

import androidx.annotation.DrawableRes

/**
 * Same default values as [com.google.android.gms.maps.model.MarkerOptions]
 */
data class MTMapIconDef(
    @get:DrawableRes val resId: Int,
    @get:DrawableRes val smallResId: Int = resId,
    @get:DrawableRes val mediumResId: Int = resId,
    val flat: Boolean = true,
    val anchorU: Float = 0.5f,
    val anchorV: Float = if (flat) 0.5f else 1.0f,
    val infoWindowAnchorU: Float = 0.5f,
    val infoWindowAnchorV: Float = if (flat) 0.5f else 0.0f,
    val replaceColor: Boolean = false,
    val oneSize: Boolean = false,
    val size: Int? = null,
    val smallSize: Int? = if (oneSize) size else size?.div(2),
    val mediumSize: Int? = if (oneSize) size else smallSize?.plus(smallSize.div(2))
) {

    @DrawableRes
    fun getZoomResId(zoom: Float, markerCount: Int?) =
        getZoomResId(MTMapIconZoomGroup.from(zoom, markerCount))

    @DrawableRes
    fun getZoomResId(zoomGroup: MTMapIconZoomGroup?): Int {
        return when (zoomGroup) {
            MTMapIconZoomGroup.SMALL -> smallResId
            MTMapIconZoomGroup.MEDIUM -> mediumResId
            MTMapIconZoomGroup.DEFAULT, null -> resId
        }
    }

    fun getZoomSize(zoom: Float, markerCount: Int?) =
        getZoomSize(MTMapIconZoomGroup.from(zoom, markerCount))

    fun getZoomSize(zoomGroup: MTMapIconZoomGroup?): Int? {
        return when (zoomGroup) {
            MTMapIconZoomGroup.SMALL -> smallSize
            MTMapIconZoomGroup.MEDIUM -> mediumSize
            MTMapIconZoomGroup.DEFAULT, null -> size
        }
    }
}
