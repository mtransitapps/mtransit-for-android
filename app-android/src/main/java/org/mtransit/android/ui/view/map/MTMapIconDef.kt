package org.mtransit.android.ui.view.map

import androidx.annotation.DrawableRes

data class MTMapIconDef(
    @get:DrawableRes val resId: Int,
    @get:DrawableRes val smallResId: Int = resId,
    val flat: Boolean = false,
    val anchorU: Float = 0.5f,
    val anchorV: Float = 1.0f,
    val infoWindowAnchorU: Float = 0.5f,
    val infoWindowAnchorV: Float = 0.5f,
    val replaceColor: Boolean = false,
    val size: Int? = null,
    val smallSize: Int? = size?.div(2),
) {

    @DrawableRes
    fun getZoomResId(zoom: Float, markerCount: Int?) =
        getZoomResId(MTMapIconZoomGroup.from(zoom, markerCount))

    @DrawableRes
    fun getZoomResId(zoomGroup: MTMapIconZoomGroup?): Int {
        return when (zoomGroup) {
            null -> resId
            MTMapIconZoomGroup.SMALL -> smallResId
            MTMapIconZoomGroup.DEFAULT -> resId
        }
    }

    @DrawableRes
    fun getZoomSize(zoom: Float, markerCount: Int?) =
        getZoomSize(MTMapIconZoomGroup.from(zoom, markerCount))

    @DrawableRes
    fun getZoomSize(zoomGroup: MTMapIconZoomGroup?): Int? {
        return when (zoomGroup) {
            null -> size
            MTMapIconZoomGroup.SMALL -> smallSize
            MTMapIconZoomGroup.DEFAULT -> size
        }
    }
}
