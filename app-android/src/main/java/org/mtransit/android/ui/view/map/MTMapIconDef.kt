package org.mtransit.android.ui.view.map

import androidx.annotation.DrawableRes

data class MTMapIconDef(
    @get:DrawableRes val resId: Int,
    @get:DrawableRes val smallResId: Int = resId,
    val flat: Boolean = false,
    val anchorU: Float = 0.5f,
    val anchorV: Float = 1.0f,
    val inforWindowAnchorU: Float = 0.5f,
    val inforWindowAnchorV: Float = 0.5f,
    val replaceColor: Boolean = false,
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
}
