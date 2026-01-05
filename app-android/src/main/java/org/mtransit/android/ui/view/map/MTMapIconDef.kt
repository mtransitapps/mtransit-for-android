package org.mtransit.android.ui.view.map

import androidx.annotation.DrawableRes

data class MTMapIconDef(
    @get:DrawableRes val resId: Int,
    val anchorU: Float = 0.5f,
    val anchorV: Float = 1.0f,
    val flat: Boolean = false,
)
